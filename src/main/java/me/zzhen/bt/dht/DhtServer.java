package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static me.zzhen.bt.dht.Krpc.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 * DHT客户端，用于执行对其他DHT节点的请求操作
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtServer {

    private NodeInfo self;
    private RouteTable routeTable = new RouteTable(self);//暂时不存数据库，经常更新
    private Krpc krpc;
    private static final Logger logger = LoggerFactory.getLogger(DhtServer.class.getName());


    class RequestHandler extends Thread {
        private DictionaryNode request;
        private InetAddress address;
        private int port;

        public RequestHandler(InetAddress address, int port, DictionaryNode request) {
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
            DictionaryNode resp = Response.makeResponse(DhtApp.self().getSelf().getKey());
            resp.addNode("t", t);
            doResponse(address, port, resp);
        }

        private void doResponseGetPeers(InetAddress address, int port, Node t) {
            DictionaryNode resp = Response.makeResponse(DhtApp.self().getSelf().getKey());
            resp.addNode("t", t);
            //TODO PeerManager
            DictionaryNode arg = (DictionaryNode) resp.getNode("r");
            List<NodeInfo> infos = routeTable.closest8Nodes(new NodeKey(t.decode()));
            ListNode nodes = new ListNode();
            for (NodeInfo info : infos) {
                nodes.addNode(new StringNode(info.compactNodeInfo()));
            }
            arg.addNode("nodes", nodes);
            Token token = TokenManager.newToken(DhtApp.self().getSelf().getKey());
            arg.addNode("token", new StringNode(token.token + ""));
            doResponse(address, port, resp);
        }

        private void doResponseFindNode(InetAddress address, int port, Node t) {
            DictionaryNode resp = Response.makeResponse(DhtApp.self().getSelf().getKey());
            resp.addNode("t", t);
            DictionaryNode arg = (DictionaryNode) resp.getNode("r");
            List<NodeInfo> infos = routeTable.closest8Nodes(new NodeKey(t.decode()));
            ListNode nodes = new ListNode();
            for (NodeInfo info : infos) {
                nodes.addNode(new StringNode(info.compactNodeInfo()));
            }
            arg.addNode("nodes", nodes);//TODO values
            doResponse(address, port, resp);
        }

        private void doResponseAnnouncePeer(InetAddress address, int port, Node t) {
            DictionaryNode resp = Response.makeResponse(DhtApp.self().getSelf().getKey());
            resp.addNode("t", t);
            DictionaryNode arg = (DictionaryNode) resp.getNode("r");
            List<NodeInfo> infos = routeTable.closest8Nodes(new NodeKey(t.decode()));
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


    public DhtServer(NodeInfo self, RouteTable routeTable, Krpc krpc) {
        this.self = self;
        this.routeTable = routeTable;
        this.krpc = krpc;
    }

    private void join() {
        for (NodeInfo nodeInfo : DhtApp.BOOTSTRAP_NODE) {
            krpc.findNode(nodeInfo, self.getKey().getValue());
        }
    }

    private void listen() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DhtConfig.SERVER_PORT)) {
                while (true) {
                    byte[] bytes = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(bytes, 1024);
                    socket.receive(packet);
                    int length = packet.getLength();
                    Node node = Decoder.decode(bytes, 0, length).get(0);
//                    krpc.onRequest(socket.getInetAddress(), socket.getPort(), (DictionaryNode) node);
                    new RequestHandler(packet.getAddress(), packet.getPort(), (DictionaryNode) node);
                    logger.info("handle request::" + new String(node.decode()));
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public void init() {
        listen();
        join();
    }
}
