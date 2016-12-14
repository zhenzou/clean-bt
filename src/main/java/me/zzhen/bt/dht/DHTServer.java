package me.zzhen.bt.dht;

import me.zzhen.bt.Config;
import me.zzhen.bt.decoder.Node;
import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DHTServer {
    private NodeKey key;
    private NodeInfo localNode;
    private RouteTable routeTable = new RouteTable();//暂时不存数据库，经常更新
    private Krpc krpc;

    public DHTServer() {
        initDefaultConfig();
        krpc = new Krpc(localNode.getKey(), routeTable);
        krpc.getPeers(new NodeKey(Utils.hexToBytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }

    public DHTServer(NodeInfo localNode) {
        this.localNode = localNode;
        startClient();
        startServer();
        krpc = new Krpc(localNode.getKey(), routeTable);
        krpc.getPeers(new NodeKey(Utils.hexToBytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }

    private void startServer() {

    }

    private void startClient() {

    }

    private void initDefaultConfig() {
        try {
            InetAddress address = InetAddress.getByAddress(Utils.ipToBytes(Config.SERVER_IP));
            key = NodeKey.generateKey();
            localNode = new NodeInfo(address, Config.SERVER_PORT, key);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public NodeKey getKey() {
        return key;
    }

    public void setKey(NodeKey key) {
        this.key = key;
    }

    public RouteTable getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(RouteTable routeTable) {
        this.routeTable = routeTable;
    }

    public static void main(String[] args) {
        DHTServer server = new DHTServer();
    }
}
