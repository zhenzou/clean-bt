package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.RouteTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtApp {

    private static final Logger logger = LoggerFactory.getLogger(DhtApp.class.getName());

    private static DhtApp SELF;
    private RouteTable routes;
    private BlackList blackList;
    private NodeInfo self;
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
            NodeKey key = NodeKey.genRandomKey();
            self = new NodeInfo(address, DhtConfig.SERVER_PORT, key);
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


    public static DhtApp self() {
        if (SELF == null) {
            SELF = new DhtApp();
            SELF.init();
        }
        return SELF;
    }

    public void init() {
        initDefaultConfig();
        startServer();
        startClient();
    }

    public static void main(String[] args) throws IOException {

        DhtApp self = DhtApp.self();
        self.init();
    }
}
