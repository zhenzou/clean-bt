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

    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);


    public Krpc(NodeKey local, RouteTable table) {
        this.local = local;
        this.table = table;
    }


    public boolean ping(NodeInfo node) {
        DictionaryNode msg = makeRequest(node.getKey(), METHOD_PING);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("id", new StringNode(local.getValue()));
        msg.addNode("a", arg);
        request(msg, node, METHOD_PING);
        return false;
    }

    public void findNode(NodeInfo node, byte[] target) {
        DictionaryNode msg = makeRequest(new NodeKey(target), METHOD_FIND_NODE);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode(target));
        arg.addNode("id", new StringNode(local.getValue()));
        msg.addNode("a", arg);
        request(msg, node, METHOD_FIND_NODE);
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param node
     * @param peer
     */
    public Token getPeers(NodeInfo node, NodeKey peer) {
        DictionaryNode msg = makeRequest(peer, METHOD_GET_PEERS);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("info_hash", new StringNode(peer.getValue()));
        arg.addNode("id", new StringNode(local.getValue()));
        msg.addNode("a", arg);
        Map<String, Node> map = msg.getValue();
        for (Map.Entry<String, Node> entry : map.entrySet()) {
            System.out.println(entry.getValue().decode().length);
            System.out.println(entry.getKey() + ":" + Utils.toHex(entry.getValue().encode()));
        }
        DictionaryNode a = (DictionaryNode) msg.getNode("a");
        Node id = a.getNode("id");
        System.out.println(id.encode().length);
        Node info_hash = a.getNode("info_hash");
        System.out.println(info_hash.encode().length);
        System.out.println(msg.encode().length);
        request(msg, node, METHOD_GET_PEERS);
        return null;
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
        DictionaryNode req = makeRequest(peer, METHOD_ANNOUNCE_PEER);
        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode("32303a32f54e697351ff4aec29cdbaabf2fbe3467cc267"));
        arg.addNode("id", new StringNode(local.getValue()));
        req.addNode("a", arg);
        byte[] encode = req.encode();
    }

    private void request(Node arg, NodeInfo node, String method) {
        executor.execute(new RequestWorker(arg, node, method));
    }


    class RequestWorker extends Thread {
        private Node arg;
        private NodeInfo target;
        private String method;
        private Set<NodeInfo> nodeSet = new HashSet<>();
        private Queue<NodeInfo> nodeQueue = new ArrayDeque<>();//TODO 完善


        public RequestWorker(Node arg, NodeInfo target, String method) {
            this.arg = arg;
            this.target = target;
            this.method = method;
        }

        private void request(Node arg, NodeInfo target, String method) {
            byte[] encode = arg.encode();
            DictionaryNode data = null;
            InetAddress addr = null;
            int port = 0;
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress address = target.getAddress();
                logger.info("request---" + method + ":" + address.getHostAddress() + ":" + target.getPort() + "---" + new String(encode));
                socket.setSoTimeout(30 * 1000);
                DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, address, target.getPort());
                socket.send(packet);
                logger.info("send " + method + ":" + address.getHostAddress() + ":" + target.getPort());
                byte[] getByte = new byte[1024];
                DatagramPacket result = new DatagramPacket(getByte, 1024);
                addr = result.getAddress();
                port = result.getPort();
                socket.receive(result);
                logger.info("received from " + method + ":" + address.getHostAddress() + ":" + target.getPort());
                data = (DictionaryNode) Decoder.parse(getByte, 0, result.getLength());

                Node y = data.getNode("y");
                getReceivedType(y.toString());
                DictionaryNode resp = (DictionaryNode) data.getNode("r");
                if (resp != null) {
                    switch (method) {
                        case METHOD_PING:
                            handlePingResp(addr, port, resp);
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
                            handleFindNodeResp(arg, resp, method);
                            break;
                        case METHOD_ANNOUNCE_PEER:
                            handleAnnouncePeerResp(arg, resp, method);
                            break;
                        default:
                            break;
                    }
                }
            } catch (SocketTimeoutException e) {
                logger.error(e.getMessage());
                if (!nodeQueue.isEmpty()) {
                    target = nodeQueue.poll();
                    request(arg, target, method);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        @Override
        public void run() {
            RequestWorker.this.request(arg, target, method);
        }

    }

    private String getReceivedType(String c) {
        String method;
        switch (c) {
            case "r":
                method = "response";
                logger.info("response");
                break;
            case "e":
                method = "error";
                logger.info("error");
                break;
            case "q":
                method = "request";
                logger.info("request");
                break;
            default:
                method = "default";
                break;
        }
        return method;
    }


    private void handlePingResp(InetAddress addr, int port, DictionaryNode arg) {
        DictionaryNode data = makeResponse(local);
        data.addNode("t", arg.getNode("t"));
        response(new NodeInfo(addr, port, new NodeKey(arg.getNode("id").decode())), data);
    }

    private void handleAnnouncePeerResp(Node arg, DictionaryNode resp, String method) {

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
                logger.info("nodes length:" + decode.length);
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

        DictionaryNode arg1 = (DictionaryNode) arg;
        byte[] info_hashes = arg1.getNode("info_hash").decode();
        logger.info("infohash:" + Utils.toHex(info_hashes));
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
                logger.info("target: length:" + decode.length);
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

    private void response(NodeInfo nodeInfo, Node data) {
        byte[] encode = data.encode();
        if (nodeInfo != null) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(30 * 1000);
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

    private DictionaryNode makeRequest(NodeKey key, String method) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(tokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("q"));
        node.addNode("q", new StringNode(method));
        return node;
    }

    private DictionaryNode makeResponse(NodeKey key) {
        DictionaryNode node = new DictionaryNode();
        DictionaryNode r = new DictionaryNode();
        r.addNode("id", new StringNode(local.getValue()));
        node.addNode("r", r);
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
        logger.info("response from " + address.getHostAddress() + ":" + port);
        Node method = node.getNode("y");
        DictionaryNode arg = (DictionaryNode) node.getNode("r");
        Node id = arg.getNode("id");
        switch (method.toString()) {
            case METHOD_PING:
                table.addNode(new NodeInfo(address, port, new NodeKey(id.decode())));
                handlePingResp(address, port, node);
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

    public static void main(String[] args) throws UnsupportedEncodingException, UnknownHostException {

        Krpc krpc = new Krpc(NodeKey.genRandomKey(), new RouteTable());
        krpc.getPeers(DHTServer.BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }


}