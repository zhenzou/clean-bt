package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.krpc.Krpc;
import me.zzhen.bt.dht.krpc.Message;
import me.zzhen.bt.dht.krpc.RequestHandler;
import me.zzhen.bt.dht.krpc.ResponseHandler;
import me.zzhen.bt.dht.routetable.RouteTable;
import me.zzhen.bt.util.IO;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtServer implements RequestHandler, ResponseHandler {

    private static final Logger logger = LoggerFactory.getLogger(DhtServer.class);

    /**
     * 本节点信息
     */
    private NodeInfo self;

    /**
     * 全局配置
     */
    private final DhtConfig config;

    /**
     * 全局路由表
     */
    public RouteTable routes;

    /**
     * 保存全部的DHT节点信息，便于操作
     */
    private Map<String, NodeInfo> nodes = new HashMap<>();

    /**
     * 全局黑名单 IP:PORT
     */
    private volatile Blacklist blacklist;

    /**
     * 全局Krpc客户端
     */
    private Krpc krpc;

    /**
     * 全局Socket,负责请求和响应
     */
    private DatagramSocket socket;

    private ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);

    /**
     * 获取MetaData的线程池
     */
    private ExecutorService fetcher = Executors.newFixedThreadPool(1);

    /**
     * DHT 网络启动节点
     */
    public static final NodeInfo[] BOOTSTRAP_NODES = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };

    public DhtServer(DhtConfig config) {
        this.config = config;
    }

    private void listen() {
        new Thread(() -> {
            try {
                byte[] bytes = new byte[4096];
                do {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    DictNode dict = null;
                    try {
                        socket.receive(packet);
                        int length = packet.getLength();
                        dict = Bencode.decodeDict(new ByteArrayInputStream(bytes, 0, length));
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        krpc.handler(address, port, dict);
                    } catch (RuntimeException | IOException e) {
                        logger.error(e.getMessage());
//                        if (dict != null) {
//                            logger.error("data:{}", dict.toString());
//                        }
                    }
                } while (true);
            } finally {
                socket.close();
            }
        }).start();
    }

    /**
     * 刷新路由表的不获取的节点
     */
    private void refresh() {
        if (routes.length() == 0) {
            join();
        }
        if (isCrawlMode()) {
            //定时,自动向邻居节点发送find_node请求
            List<NodeInfo> nodes = routes.closestKNodes(self.getId(), config.getAutoFindSize());
            logger.info("refresh len:{}", nodes.size());
            logger.info("routes len before:{}", routes.length());
            nodes.forEach(node -> krpc.findNode(node, self.getId()));
            nodes.forEach(node -> routes.remove(node));
            logger.info("routes len after:{}", routes.length());
        }
    }

    private void join() {
        for (NodeInfo target : BOOTSTRAP_NODES) {
            krpc.findNode(target, self.getId());
        }
    }

    public void init() {
        try {
            self = new NodeInfo(config.serverIp, config.serverPort, NodeId.defaultId());
            blacklist = Blacklist.defaultBlacklist(config.getBlacklistSize());
            routes = new RouteTable(config.getRouteTableSize());
            socket = new DatagramSocket(config.serverPort);
            krpc = new Krpc(socket, self);
            krpc.setRequestHandler(this);
            krpc.setResponseHandler(this);
            PeerManager.init();
            //定时清理无效节点
            executors.scheduleAtFixedRate(this::refresh, config.getAutoFind(), config.getAutoFind(), TimeUnit.SECONDS);
            //定时清理过期Token
            executors.scheduleAtFixedRate(TokenManager::clearTokens, config.getTTimeout(), config.getTTimeout(), TimeUnit.MINUTES);
        } catch (SocketException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    /**
     * 启动默认配置的Server和Client
     * 必须先初始化
     *
     * @return
     */
    public void start() {
        listen();
        join();
    }

    public NodeInfo self() {
        return self;
    }

    /**
     * {@code }
     * 为了爬虫模式优化
     *
     * @param id
     * @return
     */
    public NodeId id(byte[] id) {
        byte[] value = self.getId().getValue();
        System.arraycopy(value, 15, id, 15, 5);
        return new NodeId(id);
    }

    public void addNode(NodeInfo node) {
        routes.addNode(node);
    }

    public boolean isBlackItem(String address, int port) {
        return blacklist.is(address, port);
    }

    public void addBlackItem(String address, int port) {
        blacklist.put(address, port);
    }

    public boolean isCrawlMode() {
        return config.getMode() == DhtConfig.CRAWL_MODE;
    }

    /**
     * 响应find_node 请求
     *
     * @param src 请求源节点
     * @param t   请求t参数
     * @param id  请求的目标DHT节点的id
     */
    @Override
    public void onFindNodeReq(NodeInfo src, Node t, Node id) {
        DictNode resp = Message.makeResp(t);
        List<NodeInfo> infos = new ArrayList<>();
        infos.add(self());
        NodeId nodeId = new NodeId(id.decode());
        List<NodeInfo> close = routes.closest8Nodes(new NodeId(id.decode()));

        if (close.size() == 8) infos.addAll(close.subList(0, 7));
        else infos.addAll(close);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (NodeInfo info : infos) {
            try {
                baos.write(info.compactNodeInfo());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isCrawlMode()) {
                krpc.findNode(info, nodeId);
            }
        }
        StringNode nodes = new StringNode(baos.toByteArray());
        DictNode arg = krpc.makeArg();
        arg.addNode("nodes", nodes);
        resp.addNode("r", arg);
        krpc.send(resp, src);

    }

    /**
     * 响应announce_peer请求
     *
     * @param src      请求源节点
     * @param t        请求t参数
     * @param infoHash 请求的目标DHT节点的id
     */
    @Override
    public void onAnnouncePeerReq(NodeInfo src, int port, Node t, Node infoHash) {
        logger.info("info_hash:" + Utils.toHex(infoHash.decode()));
//        logger.info("Address:" + src.getFullAddress());
//        if (!isBlackItem(src.address, src.port)) {
//            //检测响应token是否过期
//            TokenManager.getToken(Long.parseLong(token.toString())).ifPresent(tt -> {
//                if (tt.isToken && tt.method.equals(Krpc.METHOD_GET_PEERS)) {
//                    fetcher.execute(new MetadataWorker(src.address, src.port, id.decode()));
//                }
//            });
//
//        } else {
//            logger.info("this is a black item");
//        }
        fetcher.execute(new MetadataWorker(src.address, port, infoHash.decode()));

        if (isCrawlMode()) {
            DictNode resp = Message.makeResp(t);
            DictNode arg = krpc.makeArg();
            resp.addNode("r", arg);
            krpc.send(resp, src);
        }
    }

    /**
     * 响应ping请求
     *
     * @param src 请求源节点
     * @param t   请求t参数
     */
    @Override
    public void onPingReq(NodeInfo src, Node t) {
        DictNode resp = Message.makeResp(t);
        krpc.send(resp, src);
    }

    /**
     * 响应get_peer请求
     *
     * @param src 请求源节点
     * @param t   请求t参数
     * @param id  请求的目标Peer的info_hash
     */
    @Override
    public void onGetPeerReq(NodeInfo src, Node t, Node id) {
        DictNode resp = Message.makeResp(t);
        DictNode arg = krpc.makeArg();
        if (isCrawlMode()) {
            arg.addNode("nodes", new StringNode(""));
        } else {
            List<InetSocketAddress> peers = PeerManager.PM.getPeers(new NodeId(id.decode()));
            if (peers != null) {
                ListNode values = new ListNode();
                for (InetSocketAddress peer : peers) {
                    StringNode node = new StringNode(PeerManager.compact(peer));
                    values.addNode(node);
                }
                arg.addNode("values", values);
            } else {
                List<NodeInfo> infos = routes.closest8Nodes(new NodeId(id.decode()));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (NodeInfo info : infos) {
                    try {
                        baos.write(info.compactNodeInfo());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                StringNode nodes = new StringNode(baos.toByteArray());
                arg.addNode("nodes", nodes);
            }
        }
        Token token = TokenManager.newTokenToken(new NodeId(id.decode()), Krpc.METHOD_GET_PEERS);
        arg.addNode("token", new StringNode(token.id + ""));
        resp.addNode("r", arg);
        krpc.send(resp, src);
    }

    @Override
    public void onFindNodeResp(NodeInfo src, NodeId target, DictNode resp) {
        StringNode nodes = (StringNode) resp.getNode("nodes");
        if (nodes == null) return;
        byte[] decode = nodes.decode();
        int len = decode.length;
        if (len % 26 != 0) {
            logger.error("find node makeResp is not correct");
            return;
        }
        boolean found = false;
        for (int i = 0; i < len; i += 26) {
            NodeInfo node = NodeInfo.fromBytes(decode, i);
            if (node.getId().equals(target)) {
                found = true;
                logger.info("found node :" + node.getFullAddress());
            }
            if (!node.getFullAddress().equals(self.getFullAddress())) {
//                krpc.findNode(node, target);
                logger.info(node.getFullAddress());
                addNode(node);
            }
        }
        if (!found) {
            List<NodeInfo> infos = routes.closest8Nodes(target);
            for (NodeInfo info : infos) {
                krpc.findNode(info, target);
            }
        }
    }

    /**
     * TODO
     *
     * @param src
     * @param resp
     */
    @Override
    public void onAnnouncePeerResp(NodeInfo src, DictNode resp) {

    }

    @Override
    public void onPingResp(NodeInfo src) {
        routes.addNode(src);
    }

    @Override
    public void onGetPeersResp(NodeInfo src, NodeId target, DictNode resp) {
        ListNode values = (ListNode) resp.getNode("values");
        if (values == null) {
            StringNode nodes = (StringNode) resp.getNode("nodes");
            byte[] decode = nodes.decode();
            for (int i = 0; i < decode.length; i += 26) {
                NodeInfo nodeInfo = NodeInfo.fromBytes(decode, i);
                krpc.getPeers(nodeInfo, target);
            }
        } else {
            logger.info("nodes :" + values.getValue().size());
            List<InetSocketAddress> peers = values.getValue().stream().map(node -> {
                byte[] bytes = node.decode();
                return new InetSocketAddress(Utils.getAddrFromBytes(bytes, 0), Utils.bytes2Int(bytes, 4, 2));
            }).collect(Collectors.toList());
            PeerManager.PM.addAllPeer(target, peers);
        }
    }


    public static void main(String[] args) throws IOException {
        InputStream in = DhtServer.class.getClassLoader().getResourceAsStream("logger.properties");
        if (in != null) LogManager.getLogManager().readConfiguration(in);
        String address = IO.localIp();
        DhtServer server = new DhtServer(DhtConfig.config(address, DhtConfig.SERVER_PORT));
        server.init();
        logger.info("self:{}", server.self.getFullAddress());
        server.start();
    }
}
