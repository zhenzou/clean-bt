package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.MetadataWorker;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description: 全局只有个Krpc
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc implements RequestCallback {

    private static final Logger logger = LoggerFactory.getLogger(Krpc.class.getName());

    public static final String METHOD_PING = "ping";
    public static final String METHOD_ANNOUNCE_PEER = "announce_peer";
    public static final String METHOD_GET_PEERS = "get_peers";
    public static final String METHOD_FIND_NODE = "find_node";

    private final NodeKey self;

    private DatagramSocket socket;

    public static volatile Set<NodeInfo> requested = new HashSet<>();
    /**
     * 试试吧,现在开8个线程----以后切换AIO吧
     */
    private ExecutorService sender = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors() + 1);
    private ExecutorService receiver = Executors.newSingleThreadExecutor();
    private ExecutorService fetcher = Executors.newSingleThreadExecutor();

    public Krpc(NodeKey self, DatagramSocket socket) {
        this.self = self;
        this.socket = socket;
    }


    /**
     * ping 目标节点
     *
     * @param node 目标节点的信息
     * @return
     */
    public void ping(NodeInfo node) {
        DictionaryNode request = Message.makeRequest(node.getKey(), METHOD_PING);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("id", new StringNode(self.getValue()));
        request.addNode("a", arg);
        request(request, node, METHOD_PING);
    }

    /**
     * 向目标节点发出findNode请求
     *
     * @param target 目标节点
     * @param id     findNode 的目标节点的id
     * @return
     */
    public void findNode(NodeInfo target, NodeKey id) {
        DictionaryNode msg = Message.makeRequest(id, METHOD_FIND_NODE);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode(id.getValue()));
        arg.addNode("id", new StringNode(self.getValue()));
        msg.addNode("a", arg);
        request(msg, target, METHOD_FIND_NODE);
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param target
     * @param peer
     */
    public void getPeers(NodeInfo target, NodeKey peer) {
        DictionaryNode msg = Message.makeRequest(peer, METHOD_GET_PEERS);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("info_hash", new StringNode(peer.getValue()));
        arg.addNode("id", new StringNode(self.getValue()));
        msg.addNode("a", arg);
        request(msg, target, METHOD_GET_PEERS);
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
//        DictionaryNode req = Message.makeRequest(peer, METHOD_ANNOUNCE_PEER);
//        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
//        DictionaryNode arg = new DictionaryNode();
//        arg.addNode("info_hash", new StringNode(self.getValue()));
//        arg.addNode("port", new IntNode(DhtConfig.SERVER_PORT));
//        arg.addNode("id", new StringNode(self.getValue()));
//        req.addNode("a", arg);
//        request(req, null, METHOD_ANNOUNCE_PEER);
    }

    public boolean requested(NodeInfo node) {
        return requested.contains(node);
    }

    /**
     * @param request
     * @param target
     * @param method
     * @return
     */
    @Override
    public void request(DictionaryNode request, NodeInfo target, String method) {
        if (!DhtApp.NODE.isBlackItem(target)) {
            sender.execute(new RequestWorker(socket, request, target, method));
            requested.add(target);
            DhtApp.NODE.addNode(target);
        }
    }


    private String getMetadata(InetAddress nodeAddr, int port) {
        try (Socket socket = new Socket(nodeAddr, port)) {

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return "";
    }

    public void response(NodeInfo nodeInfo, Node data) {
        byte[] encode = data.encode();
        if (nodeInfo != null) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(30 * 1000);
                InetAddress address = nodeInfo.getAddress();
                DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, address, nodeInfo.getPort());
                socket.send(packet);
            } catch (SocketTimeoutException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
    }

    @Override
    public void onAnnouncePeer(InetAddress address, int port, String data) {
        fetcher.execute(new MetadataWorker(address, port, data));
    }

    @Override
    public void onPing(InetAddress address, int port, DictionaryNode data) {

    }

    @Override
    public void onGetPeer(InetAddress address, int port, DictionaryNode data) {

    }

    @Override
    public void onFindNode(InetAddress address, int port, DictionaryNode data) {

    }

    /**
     * 处理响应的方法,包括
     *
     * @param address
     * @param port
     * @param node
     */
    @Override
    public void response(InetAddress address, int port, DictionaryNode node) {
        if (Message.isResponse(node)) {
            receiver.execute(new ResponseProcessor(node, address, port, this));
        } else if (Message.isRequest(node)) {
            receiver.execute(new ResponseWorker(socket, address, port, node, this));
        }
        logger.info("routes:" + DhtApp.NODE.routes.size() + ":requested:" + requested.size());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        sender.shutdown();
    }
}