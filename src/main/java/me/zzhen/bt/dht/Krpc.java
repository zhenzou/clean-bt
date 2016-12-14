package me.zzhen.bt.dht;

import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import me.zzhen.bt.decoder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

import static me.zzhen.bt.utils.Utils.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc {

    private static final Logger logger = LoggerFactory.getLogger(Krpc.class.getName());


    private TokenManager tokenManager = new TokenManager();
    public static final String METHOD_PING = "ping";
    public static final String METHOD_ANNOUNCE_PEER = "announce_peer";
    public static final String METHOD_GET_PEERS = "get_peers";
    public static final String METHOD_FIND_NODE = "find_node";

    private final NodeKey local;
    private final RouteTable table;


    private static final NodeInfo[] BOOTSTRAP_NODE = {
            new NodeInfo("router.bittorrent.com", 6881),
            new NodeInfo("router.utorrent.com", 6881),
            new NodeInfo("dht.transmissionbt.com", 6881)
    };

    public Krpc(NodeKey local, RouteTable table) {
        this.local = local;
        this.table = table;
    }


    public boolean ping(NodeKey node) {
        DictionaryNode arg = new DictionaryNode();
        System.out.println("id::::" + toHex(local.getValue()));
        arg.addNode("id", new StringNode(local.getValue()));
        return false;
    }

    public NodeInfo findNode(NodeKey node) {
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode("32303a32f54e697351ff4aec29cdbaabf2fbe3467cc267"));
        arg.addNode("id", new StringNode(local.getValue()));
        request(arg, null, METHOD_GET_PEERS);
        return new NodeInfo("127.0.0.1", 1234, node);
    }

    public NodeInfo getPeers(NodeKey peer) {
        DictionaryNode arg = new DictionaryNode();
//        arg.addNode("info_hash", new StringNode(hexToBytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
        arg.addNode("info_hash", new StringNode(peer.getValue()));
        arg.addNode("id", new StringNode(local.getValue()));
        request(arg, BOOTSTRAP_NODE[0], METHOD_GET_PEERS);
        return null;
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
        DictionaryNode req = (DictionaryNode) makeRequest(peer);
        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
        System.out.println("id::::" + toHex(local.getValue()));
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode("32303a32f54e697351ff4aec29cdbaabf2fbe3467cc267"));
        arg.addNode("id", new StringNode(local.getValue()));
        req.addNode("a", arg);
        byte[] encode = req.encode();
        System.out.println("s:" + new String(encode));
    }


    private void request(Node arg, NodeInfo node, String method) {
        DictionaryNode req = (DictionaryNode) makeRequest(node.getKey());
        req.addNode("q", new StringNode(method));
        req.addNode("a", arg);
        byte[] encode = req.encode();
        DictionaryNode data = null;
        logger.info("req string" + new String(encode));
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress bootAddress = node.getAddress();
            DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, bootAddress, node.getPort());
            socket.send(packet);
            logger.info("sent");
            byte[] getByte = new byte[1024];
            DatagramPacket result = new DatagramPacket(getByte, 1024);
            socket.setSoTimeout(100000);
            socket.receive(result);
            logger.info("received");
            Decoder decoder = new Decoder(getByte, 0, result.getLength());
            decoder.parse();
            data = (DictionaryNode) decoder.getValue().get(0);
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        DictionaryNode resp = (DictionaryNode) data.getNode("r");
        if (resp != null) {
            switch (method) {
                case METHOD_PING:
                    break;
                case METHOD_GET_PEERS:
                    handleGetPeerResp(arg, resp, method);

                    break;
                case METHOD_FIND_NODE:
                    break;
                case METHOD_ANNOUNCE_PEER:
                    break;
                default:
                    break;
            }
        }

    }

    private Set<NodeInfo> nodeSet = new HashSet<>();
    private Queue<NodeInfo> nodeQueue = new ArrayDeque<>();//TODO 完善

    private void handleGetPeerResp(Node arg, DictionaryNode resp, String method) {

        Node token = resp.getNode("token");
        ListNode values = (ListNode) resp.getNode("values");
        if (values == null) {
            logger.info("not find peers");
            StringNode nodes = (StringNode) resp.getNode("nodes");
            byte[] decode = nodes.decode();
            logger.info("decode len :" + decode.length);
            for (int i = 0; i < decode.length; i += 26) {
                NodeInfo nodeInfo = new NodeInfo(decode, i);
                if (!nodeSet.contains(nodeInfo)) {
                    nodeSet.add(nodeInfo);
                    nodeQueue.add(nodeInfo);
                }
            }
        } else {
            logger.info("found peers");
            for (Node dhtNode : values.getValue()) {
                byte[] decode = dhtNode.decode();
                InetAddress nodeAddr = IO.getAddrFromBytes(decode, 0);
                int port = Utils.bytesToInt(decode, 4, 2);
                getMetadata(nodeAddr, port);
                System.out.println(" String:IP" + nodeAddr.getHostAddress());
            }
        }
        if (!nodeQueue.isEmpty()) {
            NodeInfo node = nodeQueue.poll();
            request(arg, node, method);
        }
    }

    private String getMetadata(InetAddress nodeAddr, int port) {
        try (Socket socket = new Socket(nodeAddr, port)) {

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return "";
    }

    private void response(Node data, NodeKey node) {
        Node node1 = makeResponse(node);
    }

    private void error(Node data, NodeKey node) {

    }

    private Node makeRequest(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("q"));
        return node;
    }

    private Node makeResponse(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("r"));
        return node;
    }

    private Node makeError(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("e"));
        return node;
    }

    public static void tmp() {
        DictionaryNode query = new DictionaryNode();
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode("32303a32f54e697351ff4aec29cdbaabf2fbe3467cc267"));
        NodeKey NODE_KEY = NodeKey.generateKey();
        System.out.println("id::::" + toHex(NODE_KEY.getValue()));
        arg.addNode("id", new StringNode(NODE_KEY.getValue()));
        query.addNode("a", arg);
        byte[] encode = query.encode();
        System.out.println("s:" + new String(encode));
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress bootAddress = InetAddress.getByName("router.utorrent.com");
            System.out.println("ip:" + bootAddress.getHostAddress());
            DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, bootAddress, 6881);
            System.out.println("to send");
            socket.send(packet);
            System.out.println("send");
            byte[] getByte = new byte[1000];
            DatagramPacket result = new DatagramPacket(getByte, 1000);
            socket.setSoTimeout(10000);
            System.out.println("start");
            socket.receive(result);
            System.out.println("received");
            int length = result.getLength();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < length; i++) {
                baos.write(getByte[i]);
            }
            getByte = baos.toByteArray();
            String s = new String(getByte);
            System.out.println("reced s:::" + s);
            Decoder decoder = new Decoder(getByte);
            decoder.parse();
            DictionaryNode node = (DictionaryNode) decoder.getValue().get(0);
            System.out.println("node String ::" + node.toString());
            System.out.println(new String(node.encode()));
            DictionaryNode resp = (DictionaryNode) node.getNode("r");
            if (resp != null) {
                Node id = resp.getNode("id");
                Node nodes = resp.getNode("nodes");
                System.out.println("rec id::::" + Utils.toHex(id.encode()));
                System.out.println(nodes.toString());
            }
            byte[] ips = node.getNode("ip").encode();
            byte[] port = new byte[2];
            byte[] ip = new byte[4];
            for (int i = 0; i < 4; i++) {
                ip[i] = ips[i];
                System.out.println("ips:" + ip[i]);
            }
            for (int i = 0; i < 2; i++) {
                port[i] = ips[i + 4];
                System.out.println("ips:" + port[i]);
            }

            InetAddress byAddress = Inet4Address.getByAddress(ip);
            System.out.println("addr:" + byAddress.getHostAddress());
            System.out.println("port:" + bytesToInt(port));
            System.out.println("dcode" + node.getNode("ip").decode());
            System.out.println(new String(node.getNode("ip").encode(), "utf-8"));

            InetAddress btAddress = InetAddress.getByAddress(ip);
            int btPort = bytesToInt(port);

            byte[] btbuf = new byte[1000];
            DatagramPacket btPing = new DatagramPacket(encode, encode.length, btAddress, btPort);
            System.out.println("bt ping");
            socket.send(btPing);
            System.out.println("pingded");
            socket.receive(result);
            byte[] pingRet = new byte[1000];
            DatagramPacket pingResult = new DatagramPacket(pingRet, 1000);
            socket.receive(pingResult);
            System.out.println("end ping");
            length = result.getLength();
            baos = new ByteArrayOutputStream();
            for (int i = 0; i < length; i++) {
                baos.write(getByte[i]);
            }
            decoder = new Decoder(baos.toByteArray());
            decoder.parse();
            node = (DictionaryNode) decoder.getValue().get(0);
            System.out.println(node.getNode("r").decode());
            System.out.println(node.getNode("t").decode());
            System.out.println(node.getNode("y").decode());
            System.out.println(new String(node.encode()));

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException, UnknownHostException {
    }

}
