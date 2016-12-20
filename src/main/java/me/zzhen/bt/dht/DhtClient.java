package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.base.*;
import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 * DHT节点服务器，用于处理其他节点的请求
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtClient {

    private static final Logger logger = LoggerFactory.getLogger(DhtClient.class.getName());
    private NodeInfo self;
    private RouteTable routeTable;//暂时不存数据库，经常更新
    private Krpc krpc;


    public DhtClient() {
        krpc = new Krpc(self.getKey(), routeTable);
//        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }


    public DhtClient(NodeInfo self, RouteTable routeTable, Krpc krpc) {
        this.self = self;
        this.routeTable = routeTable;
        this.krpc = krpc;
//        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }


    public void ping(NodeInfo node) {
        if (!DhtApp.self().isBlackItem(node)) {
            krpc.ping(node);
        }
    }

    public void findNode(NodeInfo node, byte[] target) {
        if (!DhtApp.self().isBlackItem(node)) {
            Response resp = krpc.findNode(node, target);
            if (!Response.isError(resp.value)) {
                DictionaryNode value = (DictionaryNode) resp.value;
                byte[] decode = value.getNode("nodes").decode();
                Node id = value.getNode("id");
                int len = decode.length;
                for (int i = 0; i < len; i += 26) {
                    NodeInfo nodeInfo = new NodeInfo(decode, i);
                    if (!DhtApp.self().isBlackItem(nodeInfo)) routeTable.addNode(nodeInfo);
                }
            }
        }
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param node
     * @param peer
     */
    public void getPeers(NodeInfo node, NodeKey peer) {
        Response peers = krpc.getPeers(node, peer);
        if (!Response.isError(peers.value)) {
            ListNode resp = (ListNode) peers.value;
            List<Node> value = resp.getValue();
            int len = value.size();
            for (int i = 0; i < len; i++) {
                byte[] decode = value.get(i).decode();
                logger.info("target: length:" + decode.length);
                InetAddress nodeAddr = IO.getAddrFromBytes(decode, 0);
                int port = Utils.bytes2Int(decode, 4, 2);
                logger.info(" Peer:IP" + nodeAddr.getHostAddress());
                logger.info(" Peer:Port" + port);
                if (DhtApp.self().isBlackItem(nodeAddr.getHostAddress(), port)) continue;
                UtMetadata utMetadata = null;
                try {
                    utMetadata = new UtMetadata(nodeAddr, port);
                    utMetadata.fetchMetadata("546cf15f724d19c4319cc17b179d7e035f89c1f4", self.getKey());
                    break;
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    DhtApp.self().addBlackItem(nodeAddr.getHostAddress(), port);
                    if (i == len - 1) TokenManager.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                    DhtApp.self().addBlackItem(nodeAddr.getHostAddress(), port);
                    if (i == len - 1) TokenManager.clear();
                }
            }
        }
        TokenManager.remove(peers.token);
    }


    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
        Response response = krpc.announcePeer(peer);
    }

//    private Response request(DictionaryNode arg, NodeInfo node, String method) {
//        try {
//            return executor.submit(new Krpc.RequestWorker(arg, node, method)).get();
//        } catch (InterruptedException | ExecutionException e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//        }
//        return null;
//    }


    public NodeKey getKey() {
        return self.getKey();
    }

    public void setKey(NodeKey key) {
        self.setKey(key);
    }

    public RouteTable getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(RouteTable routeTable) {
        this.routeTable = routeTable;
    }


    public void init() {
        //test
        NodeKey key = new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4"));
        getPeers(DhtApp.BOOTSTRAP_NODE[0], key);
    }

    public static void main(String[] args) {
        DhtClient server = new DhtClient();
    }

}
