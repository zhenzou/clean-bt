package me.zzhen.bt.dht;

import com.sun.xml.internal.ws.wsdl.writer.document.BindingOperationType;
import me.zzhen.bt.base.Config;
import me.zzhen.bt.bencode.Decoder;
import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DHTServer {

    private static final Logger logger = LoggerFactory.getLogger(DHTServer.class.getName());
    private NodeInfo localNode;
    private RouteTable routeTable = new RouteTable();//暂时不存数据库，经常更新
    private Krpc krpc;
    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);


    public static final NodeInfo[] BOOTSTRAP_NODE = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };

    public DHTServer() {
        initDefaultConfig();
        krpc = new Krpc(localNode.getKey(), routeTable);
        startClient();
        startServer();
//        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));

    }


    public DHTServer(NodeInfo localNode) {
        this.localNode = localNode;
        startClient();
        startServer();
        krpc = new Krpc(localNode.getKey(), routeTable);
        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }

    private void initDefaultConfig() {
        try {
            InetAddress address = InetAddress.getByName(Config.SERVER_IP);
            NodeKey key = NodeKey.genRandomKey();
            localNode = new NodeInfo(address, Config.SERVER_PORT, key);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    private void startClient() {

    }

    private void join() {
        for (NodeInfo nodeInfo : BOOTSTRAP_NODE) {
            krpc.findNode(nodeInfo, localNode.getKey().getValue());
        }
    }

    private void listen() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(Config.SERVER_PORT)) {
                while (true) {
                    byte[] bytes = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(bytes, 1024);
                    socket.receive(packet);
                    int length = packet.getLength();
                    Node node = Decoder.parse(bytes, 0, length).get(0);
                    krpc.onResponse(socket.getInetAddress(), socket.getPort(), (DictionaryNode) node);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void startServer() {
        join();
        listen();
    }


    public NodeKey getKey() {
        return localNode.getKey();
    }

    public void setKey(NodeKey key) {
        localNode.setKey(key);
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
