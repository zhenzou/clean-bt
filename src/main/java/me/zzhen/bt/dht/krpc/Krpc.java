package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.*;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description: 全局只有一个Krpc
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc implements RequestCallback {

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
        DictionaryNode request = Message.makeRequest(node.getKey(), METHOD_PING);
        DictionaryNode arg = Message.makeArg();
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
        DictionaryNode msg = Message.makeRequest(id, METHOD_FIND_NODE);
        DictionaryNode arg = Message.makeArg();
        arg.addNode("target", new StringNode(id.getValue()));
        msg.addNode("a", arg);
        send(msg, target);
    }

    /**
     * @param target
     * @param id
     */
    public void getPeers(NodeInfo target, NodeKey id) {
        DictionaryNode msg = Message.makeRequest(id, METHOD_GET_PEERS);
        DictionaryNode arg = Message.makeArg();
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
//        DictionaryNode req = Message.makeRequest(peer, METHOD_ANNOUNCE_PEER);
//        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
//        DictionaryNode arg = new DictionaryNode();
//        arg.addNode("info_hash", new StringNode(self.getValue()));
//        arg.addNode("port", new IntNode(DhtConfig.SERVER_PORT));
//        arg.addNode("id", new StringNode(self.getValue()));
//        req.addNode("a", arg);
//        send(req, null, METHOD_ANNOUNCE_PEER);
    }

    /**
     * 使用连接池将数据发送出去
     *
     * @param data
     * @param target
     * @return
     */
    public void send(DictionaryNode data, NodeInfo target) {
        if (!DhtApp.NODE.isBlackItem(target)) {
            sender.execute(new DataSendWorker(socket, data, target));
        }
    }

    /**
     * 处理响应的方法,包括请求的响应和请求
     *
     * @param address
     * @param port
     * @param node
     */
    public void response(InetAddress address, int port, DictionaryNode node) {
        DhtApp.NODE.removeBlackItem(address, port);
        if (Message.isResponse(node)) {
            receiver.execute(new ResponseProcessor(node, address, port, this));
        } else if (Message.isRequest(node)) {
            receiver.execute(new ResponseWorker(socket, address, port, node, this));
        } else if (Message.isError(node)) {
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
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = Message.makeArg();
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
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = Message.makeArg();
        if (DhtApp.NODE.isCrawlMode()) {
            arg.addNode("id", new StringNode(DhtApp.NODE.id(id.decode()).getValue()));
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
                List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(new NodeKey(id.decode()));
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
        if (DhtApp.NODE.isCrawlMode()) return;
        if (!checkId(src, t, id)) return;
        DictionaryNode resp = Message.makeResponse(t);
        List<NodeInfo> infos = new ArrayList<>();
        infos.add(DhtApp.NODE.getSelf());
        List<NodeInfo> close = DhtApp.NODE.routes.closest8Nodes(new NodeKey(id.decode()));
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
        DictionaryNode arg = Message.makeArg();
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
        logger.info("Address:" + src.getAddress().getHostAddress() + ",port:" + src.getPort());
        if (!DhtApp.NODE.isBlackItem(src.getAddress(), src.getPort())) {
            //检测响应token是否过期
            TokenManager.getToken(Long.parseLong(token.toString())).ifPresent(tt -> {
                if (tt.isToken && tt.method.equals(Krpc.METHOD_GET_PEERS)) {
                    fetcher.execute(new MetadataWorker(src.getAddress(), src.getPort(), id.decode()));
                }
            });
        } else {
            logger.info("this is a black item");
        }
        if (!DhtApp.NODE.isCrawlMode()) {
            DictionaryNode resp = Message.makeResponse(t);
            DictionaryNode arg = Message.makeArg();
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
        DictionaryNode errMsg = Message.makeError(t, errno, msg);
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