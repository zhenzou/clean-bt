package me.zzhen.bt.dht;

import com.sun.xml.internal.ws.wsdl.writer.document.BindingOperationType;
import me.zzhen.bt.base.Config;
import me.zzhen.bt.bencode.Decoder;
import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DHTServer {

    private static final Logger logger = LoggerFactory.getLogger(DHTServer.class.getName());
    private NodeKey key;
    private NodeInfo localNode;
    private RouteTable routeTable = new RouteTable();//暂时不存数据库，经常更新
    private Krpc krpc;


    public static final NodeInfo[] BOOTSTRAP_NODE = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };

    public DHTServer() {
        initDefaultConfig();
        for (NodeInfo nodeInfo : BOOTSTRAP_NODE) {
            routeTable.addNode(nodeInfo);
        }
        krpc = new Krpc(localNode.getKey(), routeTable);
        startClient();
        startServer();
        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }

    private void listen() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(6881)) {

                while (true) {
                    byte[] bytes = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(bytes, 1024);
                    socket.receive(packet);
                    int length = packet.getLength();
                    Decoder decoder = new Decoder(bytes, 0, length);
                    decoder.parse();
                    DictionaryNode node = (DictionaryNode) decoder.getValue().get(0);
                    krpc.onResponse(socket.getInetAddress(), socket.getPort(), node);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void join() {
        for (NodeInfo nodeInfo : BOOTSTRAP_NODE) {
            krpc.findNode(nodeInfo, key.getValue());
        }
    }

    public DHTServer(NodeInfo localNode) {
        this.localNode = localNode;
        startClient();
        startServer();
        krpc = new Krpc(localNode.getKey(), routeTable);
        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }

    private void startServer() {
        listen();
        join();
    }

    private void startClient() {
    }

    private void initDefaultConfig() {
        try {
            InetAddress address = InetAddress.getByAddress(Utils.ipToBytes(Config.SERVER_IP));
            key = NodeKey.genRandomKey();
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
