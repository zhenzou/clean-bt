package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.Bencode;
import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.dht.krpc.Krpc;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
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
public class Dht {

    private static final Logger logger = LoggerFactory.getLogger(Dht.class);

    /**
     * 全局节点单例
     */
    public static Dht NODE;

    private final DhtConfig config;

    /**
     * 全局路由表
     */
    public RouteTable routes;
    /**
     * 全局黑名单 IP:PORT
     */
    private volatile Set<String> blacklist;
    /**
     * 本节点信息
     */
    private NodeInfo self;
    /**
     * 全局Krpc
     */
    private Krpc krpc;

    /**
     * 全局Socket,负责请求和响应
     */
    private DatagramSocket socket;

    private ScheduledExecutorService autoFindNode = Executors.newScheduledThreadPool(1);

    /**
     * DHT 网络启动节点
     */
    public static final NodeInfo[] BOOTSTRAP_NODES = {
        new NodeInfo("router.bittorrent.com", 6881),
        new NodeInfo("router.utorrent.com", 6881),
        new NodeInfo("dht.transmissionbt.com", 6881)
    };

    public Dht(DhtConfig config) {
        this.config = config;
    }

    private void listen() {
        new Thread(() -> {
            try {
                byte[] bytes = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    socket.receive(packet);
                    if (Dht.NODE.isBlackItem(packet.getAddress(), packet.getPort())) continue;
                    int length = packet.getLength();
                    try {
                        DictNode node = Bencode.decodeDict(new ByteArrayInputStream(bytes, 0, length));
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        krpc.response(address, port, node);
                    } catch (RuntimeException e) {
                        logger.error("data:" + packet.getLength() + Utils.toHex(bytes, 0, length));
                        logger.error(e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            } finally {
                socket.close();
            }
        }).start();
    }

    private void join() {
        for (NodeInfo target : BOOTSTRAP_NODES) {
            krpc.findNode(target, self.getKey());
        }
        //定时,自动向邻居节点发送find_node请求
        autoFindNode.scheduleAtFixedRate(() -> routes.refresh(krpc), DhtConfig.AUTO_FIND, DhtConfig.AUTO_FIND, TimeUnit.SECONDS);
        //定时清理过期Token
        autoFindNode.scheduleAtFixedRate(TokenManager::clearTokens, DhtConfig.T_TIMEOUT, DhtConfig.T_TIMEOUT, TimeUnit.MINUTES);
    }

    public void init() {
        try {
            InetAddress address = InetAddress.getByName(config.serverIp);
            self = new NodeInfo(address, config.serverPort, NodeKey.genRandomKey());
            blacklist = new HashSet<>(config.getBlacklistSize());
            routes = new RouteTable(self);
            socket = new DatagramSocket(config.serverPort);
            krpc = new Krpc(socket);
            PeerManager.init();
        } catch (UnknownHostException | SocketException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
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

    public NodeKey getSelfKey() {
        return self.getKey();
    }

    public void setSelfKey(NodeKey selfKey) {
        self.setKey(selfKey);
    }

    /**
     * {@code }
     * 为了爬虫模式优化
     *
     * @param id
     * @return
     */
    public NodeKey id(byte[] id) {
        byte[] value = getSelfKey().getValue();
        System.arraycopy(value, 15, id, 15, 5);
        return new NodeKey(id);
    }

    public void addNode(NodeInfo node) {
        if (node.equals(self) || isBlackItem(node.getFullAddress())) return;
        routes.addNode(node);
    }

    public boolean isBlackItem(InetAddress ip, int port) {
        return blacklist.contains(ip.getHostName() + ":" + port);
    }

    public boolean isBlackItem(InetSocketAddress address) {
        return isBlackItem(address.getAddress(), address.getPort());
    }

    public boolean isBlackItem(String address) {
        return blacklist.contains(address);
    }

    public boolean isBlackItem(NodeInfo info) {
        return isBlackItem(info.getFullAddress());
    }

    public void removeBlackItem(InetAddress ip, int port) {
        blacklist.remove(ip.getHostName() + ":" + port);
    }

    public void addBlackItem(InetAddress ip, int port) {
        blacklist.add(ip.getHostName() + ":" + port);
    }

    public void addBlackItem(String address) {
        blacklist.add(address);
    }

    public boolean isCrawlMode() {
        return config.getMode() == DhtConfig.CRAWL_MODE;
    }

    public static void main(String[] args) throws IOException {
        InputStream in = Dht.class.getClassLoader().getResourceAsStream("logger.properties");
        if (in != null) LogManager.getLogManager().readConfiguration(in);
        NODE = new Dht(DhtConfig.defaultConfig());
        NODE.init();
        NODE.start();
    }
}
