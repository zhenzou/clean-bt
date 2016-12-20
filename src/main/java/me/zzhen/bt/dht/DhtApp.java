package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.RouteTable;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public RouteTable routes;

    /**
     * Client，Server共用一共线程池
     */
    public final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 4);

    private BlackList blackList;
    private NodeInfo self;
    private NodeKey selfKey;
    private DhtClient client;
    private DhtServer server;
    private Krpc krpc;


    public static final NodeInfo[] BOOTSTRAP_NODE = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };


    private void initDefaultConfig() {
        try {
            InetAddress address = InetAddress.getByName(DhtConfig.SERVER_IP);
            selfKey = NodeKey.genRandomKey();
            self = new NodeInfo(address, DhtConfig.SERVER_PORT, selfKey);
            blackList = new BlackList();
            routes = new RouteTable(self);
            krpc = new Krpc(self.getKey(), routes);
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
        DhtApp boot = DhtApp.boot();
    }
}
