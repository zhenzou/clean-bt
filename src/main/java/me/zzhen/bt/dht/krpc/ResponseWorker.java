package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.TokenManager;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Token;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
    private DictionaryNode request;
    private InetAddress address;
    private int port;
    private DatagramSocket socket;

    public ResponseWorker(DatagramSocket socket, InetAddress address, int port, DictionaryNode request) {
        this.socket = socket;
        this.request = request;
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        response(address, port, request);
    }

    public void response(InetAddress address, int port, DictionaryNode node) {
        Node method = node.getNode("q");
        logger.info(method + "  request from :" + address.getHostAddress() + ":" + port);
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
                doResponseAnnouncePeer(address, port, arg);
                break;
            default:
                break;
        }
    }

    private void doResponsePing(InetAddress address, int port, Node t) {
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("id", new StringNode(DhtApp.NODE.getSelfKey().getValue()));
        arg.addNode("r", arg);
        doResponse(address, port, resp);
    }

    private void doResponseGetPeers(InetAddress address, int port, Node t, Node hash) {
        DictionaryNode resp = Message.makeResponse(t);
        //TODO PeerManager
        List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(new NodeKey(hash.decode()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (NodeInfo info : infos) {
            try {
                baos.write(info.compactNodeInfo());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        StringNode nodes = new StringNode(baos.toByteArray());
        DictionaryNode arg = Message.makrArg();
        arg.addNode("nodes", nodes);
        Token token = TokenManager.newToken(DhtApp.NODE.getSelf(), Krpc.METHOD_GET_PEERS);
        arg.addNode("token", new StringNode(token.id + ""));
        resp.addNode("r", arg);
        doResponse(address, port, resp);
    }

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
        DictionaryNode arg = Message.makrArg();
        arg.addNode("nodes", nodes);//TODO values
        resp.addNode("r", arg);
        doResponse(address, port, resp);
    }

    private void doResponseAnnouncePeer(InetAddress address, int port, Node t) {
        DictionaryNode resp = Message.makeResponse(t);
        DictionaryNode arg = Message.makrArg();
        List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(new NodeKey(t.decode()));
        ListNode nodes = new ListNode();
        for (NodeInfo info : infos) {
            nodes.addNode(new StringNode(info.compactNodeInfo()));
        }
        arg.addNode("nodes", nodes);//TODO values
        resp.addNode("r", arg);
        doResponse(address, port, resp);
    }

    public void doResponse(InetAddress address, int port, DictionaryNode resp) {
        byte[] data = resp.encode();//TODO optimize
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);
            socket.send(packet);
            logger.info("send response" + ":" + address.getHostAddress() + ":" + port);
        } catch (SocketTimeoutException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
