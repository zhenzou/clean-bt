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
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static me.zzhen.bt.dht.krpc.Krpc.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-20.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class ResponseWorker extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ResponseWorker.class.getName());

    /**
     * 请求的全部内容
     */
    private DictionaryNode request;

    /**
     * 情趣的来源地址
     */
    private InetAddress address;

    /**
     * 请求的来源端口
     */
    private int port;

    private RequestCallback callback;

    /**
     * 全局socket
     */
    private DatagramSocket socket;

    public ResponseWorker(DatagramSocket socket, InetAddress address, int port, DictionaryNode request, RequestCallback callback) {
        this.socket = socket;
        this.request = request;
        this.address = address;
        this.port = port;
        this.callback = callback;

    }

    @Override
    public void run() {
        if (DhtApp.NODE.isBlackItem(address, port)) return;
        response(address, port, request);
    }

    public void response(InetAddress address, int port, DictionaryNode node) {
        Node method = node.getNode("q");
//        logger.info(method + "  request from :" + address.getHostAddress() + ":" + port);
        Node t = node.getNode("t");
        DictionaryNode arg = (DictionaryNode) node.getNode("a");
        Node id = arg.getNode("id");
        DhtApp.NODE.addNode(new NodeInfo(address, port, new NodeKey(id.decode())));
        switch (method.toString()) {
            case METHOD_PING:
                doResponsePing(address, port, t);
                break;
            case METHOD_GET_PEERS:
                doResponseGetPeers(address, port, t, arg.getNode("info_hash"));
                break;
            case METHOD_FIND_NODE:
                Node target = arg.getNode("target");
                doResponseFindNode(address, port, target);
                break;
            case METHOD_ANNOUNCE_PEER:
                doResponseAnnouncePeer(address, port, t, arg);
                break;
            default:
                break;
        }
    }

    /**
     * 响应ping请求
     *
     * @param address
     * @param port
     * @param t
     */
    private void doResponsePing(InetAddress address, int port, Node t) {
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("id", new StringNode(DhtApp.NODE.getSelfKey().getValue()));
        arg.addNode("r", arg);
        doResponse(address, port, resp);
    }

    /**
     * 响应get_peers请求
     *
     * @param address
     * @param port
     * @param t
     * @param hash
     */
    private void doResponseGetPeers(InetAddress address, int port, Node t, Node hash) {
        DictionaryNode resp = Message.makeResponse(t);
        List<InetSocketAddress> peers = PeerManager.PM.getPeers(new NodeKey(hash.decode()));
        DictionaryNode arg = Message.makeArg();
        if (peers != null) {
            ListNode values = new ListNode();
            for (InetSocketAddress peer : peers) {
                StringNode node = new StringNode(PeerManager.compact(peer));
                values.addNode(node);
            }
            arg.addNode("values", values);
        } else {
            List<NodeInfo> infos = new ArrayList<>();
            infos.add(DhtApp.NODE.getSelf());
            infos.addAll(DhtApp.NODE.routes.closest8Nodes(new NodeKey(hash.decode())));
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
        Token token = TokenManager.newToken(new NodeKey(hash.decode()), Krpc.METHOD_GET_PEERS);
        arg.addNode("token", new StringNode(token.id + ""));
        resp.addNode("r", arg);
        doResponse(address, port, resp);
    }

    /**
     * 响应find_node请求
     *
     * @param address
     * @param port
     * @param t
     */
    private void doResponseFindNode(InetAddress address, int port, Node t) {
        DictionaryNode resp = Message.makeResponse(t);
        List<NodeInfo> infos = new ArrayList<>();
        infos.add(DhtApp.NODE.getSelf());//将自己添加到发送给别人的节点
        infos.addAll(DhtApp.NODE.routes.closest8Nodes(new NodeKey(t.decode())));
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
        arg.addNode("nodes", nodes);//TODO values
        resp.addNode("r", arg);
        doResponse(address, port, resp);
    }

    /**
     * 响应announce_peer请求
     *
     * @param address 发送请求节点的IP地址
     * @param port    发送请求节点的端口
     * @param t       token
     * @param req     请求内容
     */
    private void doResponseAnnouncePeer(InetAddress address, int port, Node t, DictionaryNode req) {
        Node impliedPort = req.getNode("implied_port");
        if (impliedPort == null || "0".equals(String.valueOf(impliedPort))) {
            port = Integer.parseInt(req.getNode("port").toString());
        }
        logger.info("info_hash:" + Utils.toHex(req.getNode("info_hash").decode()));
        logger.info("address:" + address.getHostAddress());
        logger.info("port:" + port);
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = Message.makeArg();
        resp.addNode("r", arg);
        //TODO check token
        doResponse(address, port, resp);
        callback.onAnnouncePeer(address, port, Utils.toHex(req.getNode("info_hash").decode()).toLowerCase());
    }

    /**
     * 最终响应的方法
     *
     * @param address
     * @param port
     * @param arg     请求的参数,包括 id,info_hash,port,token
     */
    public void doResponse(InetAddress address, int port, DictionaryNode arg) {
        byte[] data = arg.encode();
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);
            socket.send(packet);
//            logger.info("send response" + ":" + address.getHostAddress() + ":" + port + ":" + arg.toString());
        } catch (SocketTimeoutException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
