package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.RouteTable;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public static DhtApp NODE;

    /**
     * Client，Server共用线程池
     */
    public final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 4);

    /**
     * 全局路由表
     */
    public RouteTable routes;
    /**
     * 全局黑名单
     */
    private BlackList blackList;
    /**
     * 本节点信息
     */
    private NodeInfo self;
    /**
     * 本节点ID
     */
    private NodeKey selfKey;

    /**
     * DHT 客户端
     */
    private DhtClient client;
    /**
     * DHT 客户端
     */
    private DhtServer server;
    private Krpc krpc;

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
            blackList = new BlackList();
            routes = new RouteTable(self);
            krpc = new Krpc(self.getKey());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public NodeInfo getSelf() {
        return self;
    }

    public void setSelf(NodeInfo self) {
        this.self = self;
    }

    public NodeKey getSelfKey() {
        return selfKey;
    }

    public void setSelfKey(NodeKey selfKey) {
        this.selfKey = selfKey;
        self.setKey(selfKey);
    }

    public boolean isBlackItem(String ip, int port) {
        return blackList.contains(ip, port);
    }

    public boolean isBlackItem(NodeInfo info) {
        return blackList.contains(info.getAddress().getHostAddress(), info.getPort());
    }

    public void addBlackItem(String ip, int port) {
        blackList.add(ip, port);
    }

    private void startClient() {
        client = new DhtClient(self, routes, krpc);
        client.init();
    }


    private void startServer() {
        new Thread(() -> {
            server = new DhtServer(self, routes, krpc);
            server.init();
        }).start();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        executor.shutdown();
    }

    public void init() {
        initDefaultConfig();
        startServer();
        startClient();
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

    /**
     * TODO 自定义配置启动
     *
     * @param config
     */
    public static void boot(DhtConfig config) {
        NODE = new DhtApp();
        NODE.init();
    }


    public static void main(String[] args) throws IOException {
        InputStream in = DhtApp.class.getClassLoader().getResourceAsStream("logger.properties");
        LogManager.getLogManager().readConfiguration(in);
        DhtApp boot = DhtApp.boot();
    }
}
