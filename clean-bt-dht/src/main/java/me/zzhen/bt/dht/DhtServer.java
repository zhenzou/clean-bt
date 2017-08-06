package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.krpc.Krpc;
import me.zzhen.bt.dht.krpc.Message;
import me.zzhen.bt.dht.krpc.RequestHandler;
import me.zzhen.bt.dht.krpc.ResponseHandler;
import me.zzhen.bt.dht.meta.MetadataFetcher;
import me.zzhen.bt.dht.routetable.RouteTable;
import me.zzhen.bt.util.IO;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

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
    private final DatagramSocket socket;

    private ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);

    /**
     * 获取MetaData的线程池
     */
    private final MetadataFetcher fetcher;

    /**
     * DHT 网络启动节点
     */
    public static final NodeInfo[] BOOTSTRAP_NODES = {
        new NodeInfo("router.bittorrent.com", 6881, null),
        new NodeInfo("router.utorrent.com", 6881, null),
        new NodeInfo("dht.transmissionbt.com", 6881, null)
    };

    public DhtServer(DhtConfig config) throws SocketException {
        this.config = config;
        self = new NodeInfo(config.serverIp, config.serverPort, NodeId.defaultId());
        fetcher = new MetadataFetcher(1024, self);
        blacklist = Blacklist.defaultBlacklist(config.blacklistSize, config.blacklistExpired);
        routes = new RouteTable(config.routeTableSize);
        socket = new DatagramSocket(config.serverPort);
        krpc = new Krpc(socket, self);
        krpc.setRequestHandler(this);
        krpc.setResponseHandler(this);
        //定时清理无效节点
        executors.scheduleAtFixedRate(this::refresh, config.refireshInterval, config.refireshInterval, TimeUnit.SECONDS);
        //定时清理过期Token
        executors.scheduleAtFixedRate(TokenManager::clearTokens, config.tokenTimeout, config.tokenTimeout, TimeUnit.SECONDS);
    }

    private void listen() {
        new Thread(() -> {
            try {
                byte[] bytes = new byte[4096];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    DictNode dict;
                    try {
                        socket.receive(packet);
                        int length = packet.getLength();
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        if (address.getHostAddress().equals(config.serverIp)
                            || inBlacklist(address.getHostAddress(), port)) {
                            continue;
                        }
                        dict = Bencode.decodeDict(new ByteArrayInputStream(bytes, 0, length));
                        krpc.handle(address, port, dict);
                    } catch (RuntimeException | IOException e) {
                        logger.warn(e.getMessage());
                    }
                }
            } finally {
                socket.close();
            }
        }).start();
    }

    /**
     * 刷新路由表的不获取的节点
     */
    private void refresh() {
        if (routes.size() == 0) {
            join();
        }
        //定时,自动向邻居节点发送find_node请求
        List<NodeInfo> nodes = routes.closestKNodes(self.getId(), config.autoFindSize);
        logger.info("refresh len:{}", nodes.size());
        logger.info("routes len before:{}", routes.size());
        nodes.forEach(node -> krpc.findNode(node, id(node.id.getValue())));
        nodes.forEach(node -> krpc.getPeers(node, id(node.id.getValue())));
        NodeInfo[] infos = new NodeInfo[nodes.size()];
        nodes.toArray(infos);
        routes.remove(infos);
        logger.info("routes len after:{}", routes.size());
    }

    private void join() {
        for (NodeInfo target : BOOTSTRAP_NODES) {
            krpc.findNode(target, self.getId());
        }
    }

    /**
     * 启动默认配置的Server和Client
     * 必须先初始化
     */
    public void start() {
        listen();
        join();
        new Thread(fetcher).start();
    }

    public NodeInfo self() {
        return self;
    }

    /**
     * {@code }
     * 为了爬虫模式优化
     *
     * @param id
     * @return a node id close to id
     */
    public NodeId id(byte[] id) {
        byte[] value = self.getId().getValue();
        System.arraycopy(value, 15, id, 15, 5);
        return new NodeId(id);
    }

    public void addNode(NodeInfo node) {
        routes.addNode(node);
    }

    public boolean inBlacklist(String address, int port) {
        return blacklist.is(address, port);
    }

    public void addBlackItem(String address, int port) {
        blacklist.put(address, port);
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
        addNode(src);
//        DictNode resp = Message.makeResp(t);
//        List<NodeInfo> infos = new ArrayList<>();
//        infos.add(self());
//        NodeId nodeId = new NodeId(id.decode());
//        List<NodeInfo> close = routes.closest8Nodes(nodeId);
//        if (close.size() == 8) infos.addAll(close.subList(0, 7));
//        else infos.addAll(close);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        infos.forEach((info) -> IO.checkedWrite(baos, info.compactNodeInfo()));
//        StringNode nodes = new StringNode(baos.toByteArray());
//        DictNode arg = krpc.makeArg();
//        arg.addNode("nodes", nodes);
//        resp.addNode("r", arg);
//        krpc.send(resp, src);
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
        addNode(src);
        logger.debug("info_hash:" + Utils.toHex(infoHash.decode()));
        logger.debug("Address:" + src.fullAddress());
        //检测响应token是否过期
//        TokenManager.getToken(Long.parseLong(t.toString())).ifPresent(tt -> {
//            if (tt.isToken && tt.method.equals(Krpc.METHOD_GET_PEERS)) {
//                fetcher.commit(src.address, port, infoHash.decode());
//            }
//        });
        fetcher.commit(src.address, port, infoHash.decode());

        DictNode resp = Message.makeResp(t);
        DictNode arg = krpc.makeArg();
        resp.addNode("r", arg);
        krpc.send(resp, src);
    }

    /**
     * 响应ping请求
     *
     * @param src 请求源节点
     * @param t   请求t参数
     */
    @Override
    public void onPingReq(NodeInfo src, Node t) {
        addNode(src);
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
        addNode(src);
        DictNode resp = Message.makeResp(t);
        DictNode arg = krpc.makeArg();
        arg.addNode("id", new StringNode(id(id.decode()).getValue()));
        arg.addNode("nodes", new StringNode(""));
        Token token = TokenManager.newTokenToken(new NodeId(id.decode()), Krpc.METHOD_GET_PEERS);
        arg.addNode("token", new StringNode(token.id + ""));
        resp.addNode("r", arg);
        krpc.send(resp, src);
    }

    @Override
    public void onFindNodeResp(NodeInfo src, NodeId target, DictNode resp) {
        addNode(src);
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
                logger.info("found node :" + node.fullAddress());
            }
            if (!node.fullAddress().equals(self.fullAddress())) {
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
        addNode(src);
    }

    @Override
    public void onPingResp(NodeInfo src) {
        addNode(src);
    }

    @Override
    public void onGetPeersResp(NodeInfo src, NodeId target, DictNode resp) {
        addNode(src);
        ListNode values = (ListNode) resp.getNode("values");
        if (values == null) {
            StringNode nodes = (StringNode) resp.getNode("nodes");
            byte[] decode = nodes.decode();
            if (decode.length % 26 != 0) return;
            for (int i = 0; i < decode.length; i += 26) {
                NodeInfo info = NodeInfo.fromBytes(decode, i);
                addNode(info);
                krpc.getPeers(info, target);
            }
        }
    }


    public static void main(String[] args) throws IOException {
        InputStream in = DhtServer.class.getClassLoader().getResourceAsStream("logger.properties");
        if (in != null) LogManager.getLogManager().readConfiguration(in);
        String address = IO.localIp();
        DhtServer server = new DhtServer(DhtConfig.config(address, DhtConfig.SERVER_PORT));
        logger.info("self:{}", server.self.fullAddress());
        server.start();
    }
}
