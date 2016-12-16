package me.zzhen.bt.dht;

import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import me.zzhen.bt.bencode.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 2);


    public Krpc(NodeKey local, RouteTable table) {
        this.local = local;
        this.table = table;
    }


    public boolean ping(NodeInfo node) {
        DictionaryNode arg = new DictionaryNode();
        System.out.println("id::::" + toHex(local.getValue()));
        arg.addNode("id", new StringNode(local.getValue()));
        executor.execute(() -> request(arg, node, METHOD_PING));
        return false;
    }

    public void findNode(NodeInfo node, byte[] target) {
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode(target));
        arg.addNode("id", new StringNode(local.getValue()));
        executor.execute(() -> request(arg, node, METHOD_PING));
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param node
     * @param peer
     */
    public Token getPeers(NodeInfo node, NodeKey peer) {
        DictionaryNode arg = new DictionaryNode();
//        arg.addNode("info_hash", new StringNode(hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
        arg.addNode("info_hash", new StringNode(peer.getValue()));
        arg.addNode("id", new StringNode(local.getValue()));
        executor.execute(() -> request(arg, node, METHOD_PING));
//        request(arg, table.getNode(DHTServer.BOOTSTRAP_NODE[0].getKey()), METHOD_GET_PEERS);
        return null;
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
        DictionaryNode req = makeRequest(peer);
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
        DictionaryNode req = makeRequest(node.getKey());
        req.addNode("q", new StringNode(method));
        req.addNode("a", arg);
        byte[] encode = req.encode();
        DictionaryNode data = null;
        logger.info("req string" + new String(encode));
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5 * 1000);
            InetAddress address = node.getAddress();
            logger.info("request ip:" + address.getHostAddress());
            logger.info("request port:" + node.getPort());
            DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, address, node.getPort());
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
        } catch (SocketTimeoutException e) {
            logger.error(e.getMessage());
            if (!nodeQueue.isEmpty()) {
                node = nodeQueue.poll();
                request(arg, node, method);
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        Node y = data.getNode("y");
        switch (y.toString()) {
            case "r":
                logger.info("response");
                break;
            case "e":
                logger.info("error");
                break;
            default:
                logger.info("default");
                break;
        }
        DictionaryNode resp = (DictionaryNode) data.getNode("r");
        if (resp != null) {
            switch (method) {
                case METHOD_PING:
                    handleFindNodeResp(arg, resp, method);
                    break;
                case METHOD_GET_PEERS:
                    DictionaryNode arg1 = (DictionaryNode) arg;
                    byte[] info_hashes = arg1.getNode("info_hash").decode();
                    for (byte info_hash : info_hashes) {
                        System.out.print(info_hash + ",");
                    }
                    System.out.println();
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

    private void handleFindNodeResp(Node arg, DictionaryNode resp, String method) {
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
            if (!nodeQueue.isEmpty()) {
                NodeInfo node = nodeQueue.poll();
                request(arg, node, method);
            }
        } else {
            logger.info("found peers");
            logger.info("nodes :" + values.getValue().size());
            List<Node> value = values.getValue();
            int len = value.size();
            for (int i = 0; i < len; i++) {
                byte[] decode = values.get(i).decode();
                logger.info("node: length:" + decode.length);
                InetAddress nodeAddr = IO.getAddrFromBytes(decode, 0);
                int port = Utils.bytes2Int(decode, 4, 2);
                System.out.println(" Peer:IP" + nodeAddr.getHostAddress());
                System.out.println(" Peer:Port" + port);
                UtMetadata utMetadata = null;
                try {
                    utMetadata = new UtMetadata(nodeAddr, port);
                    utMetadata.fetchMetadata("546cf15f724d19c4319cc17b179d7e035f89c1f4", local);
                    break;
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    if (i == len - 1) tokenManager.clear();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (i == len - 1) tokenManager.clear();
                    continue;
                }
            }
        }
    }

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
            if (!nodeQueue.isEmpty()) {
                NodeInfo node = nodeQueue.poll();
                request(arg, node, method);
            }
        } else {
            logger.info("found peers");
            logger.info("nodes :" + values.getValue().size());
            List<Node> value = values.getValue();
            int len = value.size();
            for (int i = 0; i < len; i++) {
                byte[] decode = values.get(i).decode();
                logger.info("node: length:" + decode.length);
                InetAddress nodeAddr = IO.getAddrFromBytes(decode, 0);
                int port = Utils.bytes2Int(decode, 4, 2);
                System.out.println(" Peer:IP" + nodeAddr.getHostAddress());
                System.out.println(" Peer:Port" + port);
                UtMetadata utMetadata = null;
                try {
                    utMetadata = new UtMetadata(nodeAddr, port);
                    utMetadata.fetchMetadata("546cf15f724d19c4319cc17b179d7e035f89c1f4", local);
                    break;
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    if (i == len - 1) tokenManager.clear();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (i == len - 1) tokenManager.clear();
                    continue;
                }
            }
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
        NodeInfo nodeInfo = table.getNode(node);
        byte[] encode = data.encode();
        if (nodeInfo != null) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(5 * 1000);
                InetAddress address = nodeInfo.getAddress();
                logger.info("request ip:" + address.getHostAddress());
                logger.info("request port:" + nodeInfo.getPort());
                DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, address, nodeInfo.getPort());
                socket.send(packet);
            } catch (SocketTimeoutException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
    }

    private void error(Node data, NodeKey node) {

    }

    private DictionaryNode makeRequest(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("q"));
        return node;
    }

    private DictionaryNode makeResponse(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("r"));
        return node;
    }

    private DictionaryNode makeError(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("e"));
        return node;
    }

    public void onResponse(InetAddress address, int port, DictionaryNode node) {
        Node method = node.getNode("y");
        Node t = node.getNode("t");
        DictionaryNode arg = (DictionaryNode) node.getNode("r");
        Node id = arg.getNode("id");
        switch (method.toString()) {
            case METHOD_PING:
                table.addNode(new NodeInfo(address, port, new NodeKey(id.decode())));
                onPing(t, arg);
                break;
            case METHOD_GET_PEERS:
                DictionaryNode arg1 = arg;
                byte[] info_hashes = arg1.getNode("info_hash").decode();
                for (byte info_hash : info_hashes) {
                    System.out.print(info_hash + ",");
                }
                System.out.println();
                break;
            case METHOD_FIND_NODE:
                break;
            case METHOD_ANNOUNCE_PEER:
                break;
            default:
                break;
        }

    }

    private void onPing(Node t, DictionaryNode arg) {
        Node id = arg.getNode("id");
        DictionaryNode node = makeResponse(local);
        node.addNode("t", t);
        response(node, new NodeKey(id.decode()));
    }


    public static void main(String[] args) throws UnsupportedEncodingException, UnknownHostException {
    }


}