package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.PeerManager;
import me.zzhen.bt.dht.base.RouteTable;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtApp {

    private static final Logger logger = LoggerFactory.getLogger(DhtApp.class.getName());

    /**
     * 全局节点单例
     */
    public static DhtApp NODE;

    /**
     * 全局路由表
     */
    public RouteTable routes;
    /**
     * 全局黑名单
     */
    private volatile Set<InetSocketAddress> blacklist;
    /**
     * 本节点信息
     */
    private NodeInfo self;
    /**
     * 本节点ID
     */
    private NodeKey selfKey;

    /**
     * DHT 客户端,好像没什么用
     */
    private DhtClient client;
    /**
     * DHT 服务端
     */
    private DhtServer server;
    /**
     * 全局Krpc
     */
    private Krpc krpc;

    /**
     * 全局Socket,负责请求和响应
     */
    private DatagramSocket socket;

    /**
     * DHT 网络启动节点
     */
    public static final NodeInfo[] BOOTSTRAP_NODE = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };

    /**
     * 默认配置
     */
    private void initDefaultConfig() {
        try {
            InetAddress address = InetAddress.getByName(DhtConfig.SERVER_IP);
            selfKey = NodeKey.genRandomKey();
            self = new NodeInfo(address, DhtConfig.SERVER_PORT, selfKey);
            blacklist = new HashSet<>(DhtConfig.BLACKLIST_SIZE);
            routes = new RouteTable(self);
            socket = new DatagramSocket(DhtConfig.SERVER_PORT);
            krpc = new Krpc(selfKey, socket);
            PeerManager.init();
        } catch (UnknownHostException | SocketException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public NodeInfo getSelf() {
        return self;
    }

    public NodeKey getSelfKey() {
        return selfKey;
    }

    public void setSelfKey(NodeKey selfKey) {
        this.selfKey = selfKey;
        self.setKey(selfKey);
    }

    public void addNode(NodeInfo node) {
        if (!node.equals(self))
            routes.addNode(node);
    }

    public boolean isBlackItem(InetAddress ip, int port) {
        return blacklist.contains(new InetSocketAddress(ip, port));
    }

    public void removeBlackItem(InetAddress ip, int port) {
        blacklist.remove(new InetSocketAddress(ip, port));
    }

    public boolean isBlackItem(InetSocketAddress address) {
        return blacklist.contains(address);
    }

    public boolean isBlackItem(NodeInfo info) {
        return blacklist.contains(new InetSocketAddress(info.getAddress().getHostAddress(), info.getPort()));
    }

    public void addBlackItem(InetAddress ip, int port) {
        blacklist.add(new InetSocketAddress(ip, port));
    }

    public void addBlackItem(InetSocketAddress address) {
        blacklist.add(address);
    }

    private void startClient() {
        client = new DhtClient(self, routes, krpc);
        client.init();
    }

    private void startServer() {
        server = new DhtServer(socket, self, krpc);
        server.init();
    }


    public void init() {
        initDefaultConfig();
        startServer();
    }

    /**
     * 启动默认配置的Server和Client
     *
     * @return
     */
    public static DhtApp boot() {
        NODE = new DhtApp();
        NODE.init();
        return NODE;
    }


    public static void main(String[] args) throws IOException {
        InputStream in = DhtApp.class.getClassLoader().getResourceAsStream("logger.properties");
        if (in != null) LogManager.getLogManager().readConfiguration(in);
        DhtApp boot = DhtApp.boot();
    }
}
