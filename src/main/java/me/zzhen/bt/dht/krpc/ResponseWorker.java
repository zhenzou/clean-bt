package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.TokenManager;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Response;
import me.zzhen.bt.dht.base.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

    public ResponseWorker(InetAddress address, int port, DictionaryNode request) {
        this.request = request;
        this.address = address;
        this.port = port;
    }


    @Override
    public void run() {
        onRequest(address, port, request);
    }

    public void onRequest(InetAddress address, int port, DictionaryNode node) {
        logger.info("response from " + address.getHostAddress() + ":" + port);
        Node method = node.getNode("y");
        Node t = node.getNode("t");
        DictionaryNode value = (DictionaryNode) node.getNode("a");
        Node id = value.getNode("id");
        switch (method.toString()) {
            case METHOD_PING:
                doResponsePing(address, port, t);
                break;
            case METHOD_GET_PEERS:
                DictionaryNode arg1 = value;
                byte[] info_hashes = arg1.getNode("info_hash").decode();
                for (byte info_hash : info_hashes) {
                    System.out.print(info_hash + ",");
                }
                System.out.println();
                doResponseGetPeers(address, port, value.getNode("info_hash"));
                break;
            case METHOD_FIND_NODE:
                Node target = value.getNode("target");
                doResponseFindNode(address, port, target);
                break;
            case METHOD_ANNOUNCE_PEER:
                doResponseAnnouncePeer(address, port, value);
                break;
            default:
                break;
        }
    }

    private void doResponsePing(InetAddress address, int port, Node t) {
        DictionaryNode resp = Response.makeResponse(DhtApp.NODE.getSelf().getKey());
        resp.addNode("t", t);
        doResponse(address, port, resp);
    }

    private void doResponseGetPeers(InetAddress address, int port, Node t) {
        DictionaryNode resp = Response.makeResponse(DhtApp.NODE.getSelf().getKey());
        resp.addNode("t", t);
        //TODO PeerManager
        DictionaryNode arg = (DictionaryNode) resp.getNode("r");
        List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(new NodeKey(t.decode()));
        ListNode nodes = new ListNode();
        for (NodeInfo info : infos) {
            nodes.addNode(new StringNode(info.compactNodeInfo()));
        }
        arg.addNode("nodes", nodes);
        Token token = TokenManager.newToken(DhtApp.NODE.getSelf().getKey());
        arg.addNode("token", new StringNode(token.token + ""));
        doResponse(address, port, resp);
    }

    private void doResponseFindNode(InetAddress address, int port, Node t) {
        DictionaryNode resp = Response.makeResponse(DhtApp.NODE.getSelf().getKey());
        resp.addNode("t", t);
        DictionaryNode arg = (DictionaryNode) resp.getNode("r");
        List<NodeInfo> infos = new ArrayList<>();
        infos.add(DhtApp.NODE.getSelf());//将自己添加到发送给别人的节点
        infos.addAll(DhtApp.NODE.routes.closest8Nodes(new NodeKey(t.decode())));
        ListNode nodes = new ListNode();
        for (NodeInfo info : infos) {
            nodes.addNode(new StringNode(info.compactNodeInfo()));
        }
        arg.addNode("nodes", nodes);//TODO values
        doResponse(address, port, resp);
    }

    private void doResponseAnnouncePeer(InetAddress address, int port, Node t) {
        DictionaryNode resp = Response.makeResponse(DhtApp.NODE.getSelf().getKey());
        resp.addNode("t", t);
        DictionaryNode arg = (DictionaryNode) resp.getNode("r");
        List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(new NodeKey(t.decode()));
        ListNode nodes = new ListNode();
        for (NodeInfo info : infos) {
            nodes.addNode(new StringNode(info.compactNodeInfo()));
        }
        arg.addNode("nodes", nodes);//TODO values
        doResponse(address, port, resp);
    }

    public void doResponse(InetAddress address, int port, DictionaryNode resp) {
        byte[] data = resp.encode();//TODO optimize
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(30 * 1000);
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);
            socket.send(packet);
            logger.info("responsed " + ":" + address.getHostAddress() + ":" + port);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
