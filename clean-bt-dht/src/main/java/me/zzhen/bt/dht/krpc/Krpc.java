package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.*;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc implements RequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(Krpc.class.getName());

    public static final String METHOD_PING = "ping";
    public static final String METHOD_ANNOUNCE_PEER = "announce_peer";
    public static final String METHOD_GET_PEERS = "get_peers";
    public static final String METHOD_FIND_NODE = "find_node";


    private DatagramSocket socket;

    /**
     * 主要的线程发送线程池
     */
    private ExecutorService sender = Executors.newFixedThreadPool(2);

    /**
     * 请求处理线程池
     */
    private ExecutorService receiver = Executors.newFixedThreadPool(2);

    /**
     * 获取MetaData的线程池
     */
    private ExecutorService fetcher = Executors.newFixedThreadPool(2);

    public Krpc(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * ping 目标节点
     *
     * @param node 目标节点的信息
     * @return
     */
    public void ping(NodeInfo node) {
        DictNode request = Message.makeReq(node.getKey(), METHOD_PING);
        DictNode arg = Message.makeArg();
        request.addNode("a", arg);
        send(request, node);
    }

    /**
     * 向目标节点发出findNode请求
     *
     * @param target 目标节点
     * @param id     findNode 的目标节点的id
     * @return
     */
    public void findNode(NodeInfo target, NodeKey id) {
        DictNode msg = Message.makeReq(id, METHOD_FIND_NODE);
        DictNode arg = Message.makeArg();
        arg.addNode("target", new StringNode(id.getValue()));
        msg.addNode("a", arg);
        send(msg, target);
    }

    /**
     * @param target
     * @param id
     */
    public void getPeers(NodeInfo target, NodeKey id) {
        DictNode msg = Message.makeReq(id, METHOD_GET_PEERS);
        DictNode arg = Message.makeArg();
        arg.addNode("info_hash", new StringNode(id.getValue()));
        msg.addNode("a", arg);
        send(msg, target);
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
//        DictNode req = Message.makeReq(peer, METHOD_ANNOUNCE_PEER);
//        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
//        DictNode makeArg = new DictNode();
//        makeArg.addNode("info_hash", new StringNode(self.getValue()));
//        makeArg.addNode("port", new IntNode(DhtConfig.SERVER_PORT));
//        makeArg.addNode("id", new StringNode(self.getValue()));
//        req.addNode("a", makeArg);
//        send(req, null, METHOD_ANNOUNCE_PEER);
    }

    /**
     * 使用连接池将数据发送出去
     *
     * @param data
     * @param target
     * @return
     */
    public void send(DictNode data, NodeInfo target) {
        if (!Dht.NODE.isBlackItem(target)) {
            sender.execute(new DataSendWorker(socket, data, target));
        }
    }

    /**
     * 处理响应的方法,包括请求的响应和请求
     * nbvc lkjhncn m
     * @param address
     * @param port
     * @param node
     */
    public void response(InetAddress address, int port, DictNode node) {
        Dht.NODE.removeBlackItem(address, port);
        if (Message.isResp(node)) {
            receiver.execute(new ResponseProcessor(node, address, port, this));
        } else if (Message.isReq(node)) {
            receiver.execute(new ResponseWorker(socket, address, port, node, this));
        } else if (Message.isErr(node)) {
            //TODO
        }
    }

    /**
     * 响应ping请求
     * 在线程池中执行
     *
     * @param src
     * @param t
     */
    @Override
    public void onPing(NodeInfo src, Node t) {
        DictNode resp = Message.makeResp(t);
        DictNode arg = Message.makeArg();
        send(resp, src);
    }


    /**
     * 响应get_peer请求
     *
     * @param src 请求来源
     * @param t
     * @param id  请求的资源ID
     */
    @Override
    public void onGetPeer(NodeInfo src, Node t, Node id) {
        if (!checkId(src, t, id)) return;
        DictNode resp = Message.makeResp(t);
        DictNode arg = Message.makeArg();
        if (Dht.NODE.isCrawlMode()) {
            arg.addNode("id", new StringNode(Dht.NODE.id(id.decode()).getValue()));
            arg.addNode("nodes", new StringNode(""));
        } else {
            List<InetSocketAddress> peers = PeerManager.PM.getPeers(new NodeKey(id.decode()));
            if (peers != null) {
                ListNode values = new ListNode();
                for (InetSocketAddress peer : peers) {
                    StringNode node = new StringNode(PeerManager.compact(peer));
                    values.addNode(node);
                }
                arg.addNode("values", values);
            } else {
                List<NodeInfo> infos = Dht.NODE.routes.closest8Nodes(new NodeKey(id.decode()));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (NodeInfo info : infos) {
                    try {
                        baos.write(info.compactNodeInfo());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                StringNode nodes = new StringNode(baos.toByteArray());
                arg.addNode("nodes", nodes);
            }
        }
        Token token = TokenManager.newTokenToken(new NodeKey(id.decode()), Krpc.METHOD_GET_PEERS);
        arg.addNode("token", new StringNode(token.id + ""));
        resp.addNode("r", arg);
        send(resp, src);
    }

    /**
     * 响应find_node请求
     *
     * @param src
     * @param t
     * @param id
     */
    @Override
    public void onFindNode(NodeInfo src, Node t, Node id) {
        if (Dht.NODE.isCrawlMode()) return;
        if (!checkId(src, t, id)) return;
        DictNode resp = Message.makeResp(t);
        List<NodeInfo> infos = new ArrayList<>();
        infos.add(Dht.NODE.self());
        List<NodeInfo> close = Dht.NODE.routes.closest8Nodes(new NodeKey(id.decode()));
        if (close.size() == 8) infos.addAll(close.subList(0, 7));
        else infos.addAll(close);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (NodeInfo info : infos) {
            try {
                baos.write(info.compactNodeInfo());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        StringNode nodes = new StringNode(baos.toByteArray());
        DictNode arg = Message.makeArg();
        arg.addNode("nodes", nodes);
        resp.addNode("r", arg);
        send(resp, src);
    }

    /**
     * 响应announce_peer请求
     *
     * @param src
     * @param t     请求的t参数，响应的时候需要返回
     * @param id    请求携带的资源种子id
     * @param token
     */
    @Override
    public void onAnnouncePeer(NodeInfo src, Node t, Node id, Node token) {
        logger.info("info_hash:" + Utils.toHex(id.decode()));
        logger.info("Address:" + src.getFullAddress());
        if (!Dht.NODE.isBlackItem(src.getFullAddress())) {
            //检测响应token是否过期
            TokenManager.getToken(Long.parseLong(token.toString())).ifPresent(tt -> {
                if (tt.isToken && tt.method.equals(Krpc.METHOD_GET_PEERS)) {
                    fetcher.execute(new MetadataWorker(src.address, src.port, id.decode()));
                }
            });
        } else {
            logger.info("this is a black item");
        }
        if (!Dht.NODE.isCrawlMode()) {
            DictNode resp = Message.makeResp(t);
            DictNode arg = Message.makeArg();
            resp.addNode("r", arg);
            send(resp, src);
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
    @Override
    public void error(NodeInfo src, Node t, Node id, int errno, String msg) {
        DictNode errMsg = Message.makeErr(t, errno, msg);
        send(errMsg, src);
    }

    /**
     * 检测请求中id值是否合法
     *
     * @param src
     * @param t
     * @param id
     * @return
     */
    private boolean checkId(NodeInfo src, Node t, Node id) {
        if (id.decode().length != 20) {
            error(src, t, id, Message.ERRNO_PROTOCOL, "invalid id");
            return false;
        }
        return true;
    }
}