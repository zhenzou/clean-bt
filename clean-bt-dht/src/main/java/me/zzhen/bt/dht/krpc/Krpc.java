package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.IntNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.common.Channel;
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
    private ExecutorService sender = Executors.newFixedThreadPool(10);

    private Channel<Received> received = Channel.simpleChannel(1024);

    public Krpc(NodeInfo self) throws SocketException {
        socket = new DatagramSocket();
        this.self = self;
//        run();
    }

    public Krpc(DatagramSocket socket, NodeInfo self) {
        this.socket = socket;
        this.self = self;
//        run();
    }

    public void run() {
        new Thread(() -> {
            Optional<Received> next;
            while ((next = received.next()).isPresent()) {
                process(next.get());
            }
        }).start();
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
//        request(req, null, METHOD_ANNOUNCE_PEER);
    }

    public void send(DictNode data, NodeInfo target) {
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

    class Received {
        public final String address;
        public final int port;
        public final DictNode dict;

        Received(String address, int port, DictNode dict) {
            this.address = address;
            this.port = port;
            this.dict = dict;
        }
    }

    /**
     * 处理响应的方法,包括请求的响应和请求
     * nbvc lkjhncn m
     *
     * @param address
     * @param port
     * @param dict
     */
    public void handler(InetAddress address, int port, DictNode dict) {
//        received.push(new Received(address.getHostAddress(), port, dict));
        process(new Received(address.getHostAddress(), port, dict));
    }

    public void process(Received received) {
        DictNode dict = received.dict;
        String address = received.address;
        int port = received.port;
        if (Message.isResp(dict)) {
            Node t = dict.getNode("t");
            long tId;
            try {
                tId = Long.parseLong(t.toString());
            } catch (NumberFormatException e) {
                logger.warn("resp token :{}", t.toString());
                return;
            }
            //            long tId = Long.parseLong(dict.getNode("t").toString());
            Optional<Token> optional = TokenManager.getToken(tId);
            optional.ifPresent(token -> {
                DictNode resp = (DictNode) dict.getNode("r");
                Node id = resp.getNode("id");
                byte[] ids = id.decode();
                NodeInfo src = new NodeInfo(address, port, new NodeId(ids));

                if (!checkId(id)) {
                    error(src, t, id, Message.ERRNO_PROTOCOL, "invalid id");
                    return;
                }
                switch (token.method) {
                    case METHOD_PING:
                        responseHandler.onPingResp(src);
                        break;
                    case METHOD_GET_PEERS:
                        responseHandler.onGetPeersResp(src, token.target, resp);
                        break;
                    case METHOD_FIND_NODE:
                        responseHandler.onFindNodeResp(src, token.target, resp);
                        break;
                    case METHOD_ANNOUNCE_PEER:
                        responseHandler.onAnnouncePeerResp(src, resp);
                        break;
                    default:
                        break;
                }
            });
        } else if (Message.isReq(dict)) {
            Node t = dict.getNode("t");
            DictNode arg = (DictNode) dict.getNode("a");
            Node id = arg.getNode("id");

            NodeId key = new NodeId(id.decode());
            NodeInfo src = new NodeInfo(address, port, key);
            if (!checkId(id)) {
                error(src, t, id, Message.ERRNO_PROTOCOL, "invalid id");
                return;
            }
            Node method = dict.getNode("q");
//            logger.info(method.toString() + "  request from " + address + ":" + port);
            switch (method.toString()) {
                case METHOD_PING:
                    requestHandler.onPingReq(src, dict);
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
                    requestHandler.onAnnouncePeerReq(src, p, t, arg.getNode("info_hash"));
                    break;
                default:
                    error(src, t, id, Message.ERRNO_UNKNOWN, "unknown method");
                    break;
            }
        } else if (Message.isErr(dict)) {
            //TODO
        }
    }

    /**
     * 请求不合法，响应错误信息
     *
     * @param src
     * @param t
     * @param id
     * @param errno
     * @param msg
     */
    public void error(NodeInfo src, Node t, Node id, int errno, String msg) {
        DictNode errMsg = Message.makeErr(t, errno, msg);
        send(errMsg, src);
    }

    /**
     * 检测请求中id值是否合法
     *
     * @param id
     * @return 是否是合法的id
     */
    private boolean checkId(Node id) {
        return id.decode().length == 20;
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