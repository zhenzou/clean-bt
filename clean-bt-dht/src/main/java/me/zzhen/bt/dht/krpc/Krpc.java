package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.IntNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc {

    private static final Logger logger = LoggerFactory.getLogger(Krpc.class);

    public static final String METHOD_PING = "ping";
    public static final String METHOD_ANNOUNCE_PEER = "announce_peer";
    public static final String METHOD_GET_PEERS = "get_peers";
    public static final String METHOD_FIND_NODE = "find_node";


    private DatagramSocket socket;

    /**
     * 自身节点信息
     */
    private NodeInfo self;

    /**
     * 处理请求
     */
    private RequestHandler requestHandler;

    /**
     * 处理响应
     */
    private ResponseHandler responseHandler;
    /**
     * 主要的线程发送线程池
     */
    private ExecutorService sender = Executors.newFixedThreadPool(4);
    private ExecutorService processor = Executors.newFixedThreadPool(1);


    public Krpc(NodeInfo self) throws SocketException {
        socket = new DatagramSocket();
        this.self = self;
    }

    public Krpc(DatagramSocket socket, NodeInfo self) {
        this.socket = socket;
        this.self = self;
    }

    /**
     * ping 目标节点
     *
     * @param dest 目标节点的信息
     */
    public void ping(NodeInfo dest) {
        DictNode arg = makeArg();
        request(METHOD_PING, arg, dest.getId(), dest);
    }


    /**
     * 向目标节点发出find_node请求
     *
     * @param dest   目标节点
     * @param target findNode 的目标节点的id
     */
    public void findNode(NodeInfo dest, NodeId target) {
        DictNode arg = makeArg();
        arg.addNode("target", new StringNode(target.getValue()));
        request(METHOD_FIND_NODE, arg, target, dest);
    }

    /**
     * 向目标节点发出get_peers请求
     *
     * @param target
     * @param id
     */
    public void getPeers(NodeInfo target, NodeId id) {
        DictNode arg = makeArg();
        arg.addNode("info_hash", new StringNode(id.getValue()));
        request(METHOD_GET_PEERS, arg, id, target);
    }

    /**
     * 向整个DHT中加入 id 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeId peer) {
        DictNode req = Message.makeReq(peer, METHOD_ANNOUNCE_PEER);
        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
        DictNode makeArg = new DictNode();
//        makeArg.addNode("info_hash", new StringNode(self.getValue()));
        makeArg.addNode("port", new IntNode(DhtConfig.SERVER_PORT));
        makeArg.addNode("id", new StringNode(self.getId().getValue()));
        req.addNode("a", makeArg);
    }

    public void send(DictNode data, NodeInfo target) {
        if (target.fullAddress().equals(self.fullAddress())) {
            return;
        }
        sender.execute(() -> {
            byte[] encode = data.encode();
            try {
                DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, InetAddress.getByName(target.address), target.port);
                socket.send(packet);
            } catch (IOException e) {
                logger.warn(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 使用连接池将数据发送出去
     *
     * @param arg
     * @param target
     * @returny
     */
    public void request(String method, DictNode arg, NodeId id, NodeInfo target) {
        DictNode req = Message.makeReq(id, method);
        req.addNode("a", arg);
//        logger.info("request:{}", new String(req.encode()));
        send(req, target);
    }

    /**
     * 处理响应的方法,包括请求的响应和请求
     * nbvc lkjhncn m
     *
     * @param address
     * @param port
     * @param dict
     */
    public void handle(InetAddress address, int port, DictNode dict) {
        processor.execute(() -> {
            Message message = new Message(address.getHostAddress(), port, dict);
            if (Message.isResp(dict)) {
                handleResponse(message);
            } else if (Message.isReq(dict)) {
                handleRequest(message);
            } else if (Message.isErr(dict)) {
                //TODO
            } else {
                logger.warn("unrecognized data");
            }
        });
    }

    public void handleResponse(Message message) {
        Node t = message.arg.getNode("t");
        long token;
        try {
            token = Long.parseLong(t.toString());
        } catch (NumberFormatException e) {
            logger.warn("resp token :{}", t.toString());
            return;
        }
        Optional<Token> optional = TokenManager.getToken(token);
        optional.ifPresent(tk -> {
            DictNode resp = (DictNode) message.arg.getNode("r");
            Node id = resp.getNode("id");
            NodeInfo src = null;
            try {
                src = new NodeInfo(message.address, message.port, new NodeId(id.decode()));
                switch (tk.method) {
                    case METHOD_PING:
                        responseHandler.onPingResp(src);
                        break;
                    case METHOD_GET_PEERS:
                        responseHandler.onGetPeersResp(src, tk.target, resp);
                        break;
                    case METHOD_FIND_NODE:
                        responseHandler.onFindNodeResp(src, tk.target, resp);
                        break;
                    case METHOD_ANNOUNCE_PEER:
                        responseHandler.onAnnouncePeerResp(src, resp);
                        break;
                    default:
                        break;
                }
            } catch (IllegalArgumentException e) {
                error(src, t, Message.ERRNO_PROTOCOL, "invalid id");
            }
        });
    }

    public void handleRequest(Message message) {
        Node t = message.arg.getNode("t");
        DictNode arg = (DictNode) message.arg.getNode("a");

        NodeId id;
        try {
            id = new NodeId(arg.getNode("id").decode());
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage());
            return;
        }
        NodeInfo src = new NodeInfo(message.address, message.port, id);
        switch (message.arg.getNode("q").toString()) {
            case METHOD_PING:
                requestHandler.onPingReq(src, message.arg);
                break;
            case METHOD_GET_PEERS:
                requestHandler.onGetPeerReq(src, t, arg.getNode("info_hash"));
                break;
            case METHOD_FIND_NODE:
                requestHandler.onFindNodeReq(src, t, arg.getNode("target"));
                break;
            case METHOD_ANNOUNCE_PEER:
                int i = Integer.parseInt(arg.getNode("implied_port").toString());
                int p = src.port;
                if (i == 0) {
                    p = Integer.parseInt(arg.getNode("port").toString());
                }
                requestHandler.onAnnouncePeerReq(src, p, arg.getNode("token"), arg.getNode("info_hash"));
                break;
            default:
                error(src, t, Message.ERRNO_UNKNOWN, "unknown method");
                break;
        }
    }

    /**
     * 请求不合法，响应错误信息
     *
     * @param src
     * @param t
     * @param errno
     * @param msg
     */
    public void error(NodeInfo src, Node t, int errno, String msg) {
        DictNode errMsg = Message.makeErr(t, errno, msg);
        send(errMsg, src);
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }


    public void setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public DictNode makeArg() {
        DictNode node = new DictNode();
        node.addNode("id", new StringNode(self.getId().getValue()));
        return node;
    }

}