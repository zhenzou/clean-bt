package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.RouteTable;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 * TODO 接受使用者的请求，然后在DHTServer中查找
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtClient {

    private static final Logger logger = LoggerFactory.getLogger(DhtClient.class.getName());
    private NodeInfo self;
    private RouteTable routes;//暂时不存数据库，经常更新
    private Krpc krpc;


    public DhtClient() {
        krpc = new Krpc( null);
    }


    public DhtClient(NodeInfo self, RouteTable routeTable, Krpc krpc) {
        this.self = self;
        this.routes = routeTable;
        this.krpc = krpc;
//        krpc.getPeers(BOOTSTRAP_NODE[0], new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4")));
    }


    public void ping(NodeInfo node) {
        if (!DhtApp.NODE.isBlackItem(node)) {
            krpc.ping(node);
        }
    }

    public void findNode(NodeInfo target, NodeKey key) {
        if (!DhtApp.NODE.isBlackItem(target)) {
            krpc.findNode(target, key);
//            if (!Response.isResponse(resp.value)) {
//                DictionaryNode value = (DictionaryNode) resp.value;
//                byte[] decode = value.getNode("nodes").decode();
//                Node id = value.getNode("id");
//                int len = decode.length;
//                for (int i = 0; i < len; i += 26) {
//                    NodeInfo nodeInfo = new NodeInfo(decode, i);
//                    if (!DhtApp.NODE.isBlackItem(nodeInfo)) routes.addNode(nodeInfo);
//                }
//            }
        }
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param node
     * @param peer
     */
    public void getPeers(NodeInfo node, NodeKey peer) {
        krpc.getPeers(node, peer);
//        if (!Response.isResponse(peers.value)) {
//            ListNode resp = (ListNode) peers.value;
//            List<Node> value = resp.getValue();
//            int len = value.size();
//            for (int i = 0; i < len; i++) {
//                byte[] decode = value.get(i).decode();
//                logger.info("target: length:" + decode.length);
//                InetAddress nodeAddr = IO.getAddrFromBytes(decode, 0);
//                int port = Utils.bytes2Int(decode, 4, 2);
//                logger.info(" Peer:IP" + nodeAddr.getHostAddress());
//                logger.info(" Peer:Port" + port);
//                if (DhtApp.NODE.isBlackItem(nodeAddr.getHostAddress(), port)) continue;
//                MetadataWorker utMetadata = null;
//                try {
//                    utMetadata = new MetadataWorker(nodeAddr, port);
//                    utMetadata.fetchMetadata("546cf15f724d19c4319cc17b179d7e035f89c1f4", self.getKey());
//                    break;
//                } catch (SocketTimeoutException e) {
//                    e.printStackTrace();
//                    logger.error(e.getMessage());
//                    DhtApp.NODE.addBlackItem(nodeAddr.getHostAddress(), port);
//                    if (i == len - 1) TokenManager.clear();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    DhtApp.NODE.addBlackItem(nodeAddr.getHostAddress(), port);
//                    if (i == len - 1) TokenManager.clear();
//                }
//            }
//        }
//        TokenManager.remove(peers.id);
    }


    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public void announcePeer(NodeKey peer) {
        krpc.announcePeer(peer);
    }

//    private Response send(DictionaryNode arg, NodeInfo node, String method) {
//        try {
//            return executor.submit(new Krpc.ResponseProcessor(arg, node, method)).get();
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

    public RouteTable getRoutes() {
        return routes;
    }

    public void setRoutes(RouteTable routes) {
        this.routes = routes;
    }


    public void init() {
//
//        try {
//            Thread.sleep(10000);    //test
//            NodeKey key = new NodeKey(Utils.hex2Bytes("546cf15f724d19c4319cc17b179d7e035f89c1f4"));
//            getPeers(DhtApp.NODE.getSelf(), key);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
    }

    public static void main(String[] args) {
        DhtClient server = new DhtClient();
    }

}
