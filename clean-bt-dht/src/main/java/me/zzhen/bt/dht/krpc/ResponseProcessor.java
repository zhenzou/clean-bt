package me.zzhen.bt.dht.krpc;


import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.*;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Project:CleanBT
 * Create Time: 16-12-20.
 * Description:
 * 处理本节点请求的响应
 *
 * @author zzhen zzzhen1994@gmail.com
 */
class ResponseProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResponseProcessor.class.getName());

    /**
     * 接收到的响应内容
     */
    private DictionaryNode resp;

    /**
     * 请求的目标DHT节点的信息
     */
    private NodeInfo target;

    /**
     * 请求的目标资源或者节点ID
     */
    private NodeKey key;

    /**
     * 响应的方法,需要通过查找token确定
     */
    private String method;


    /**
     * 响应节点的IP地址
     */
    private InetAddress address;

    /**
     * 响应节点的端口
     */
    private int port;
    /**
     * 回调
     */
    private Krpc krpc;

    /**
     * @param resp    响应的内容
     * @param address
     * @param port
     * @param krpc    处理完响应的回调
     */
    public ResponseProcessor(DictionaryNode resp, InetAddress address, int port, Krpc krpc) {
        this.address = address;
        this.port = port;
        this.resp = resp;
        this.krpc = krpc;
    }


    @Override
    public void run() {
        try {
            long id = Long.parseLong(resp.getNode("t").toString());
            Optional<Token> optional = TokenManager.getToken(id);
            optional.ifPresent(token -> {
                key = token.target;
                resp = (DictionaryNode) resp.getNode("r");
                byte[] ids = resp.getNode("id").decode();
                if (ids.length != 20) return;
                DhtApp.NODE.addNode(new NodeInfo(address, port, new NodeKey(ids)));
                method = token.method;
                switch (method) {
                    case METHOD_PING:
                        processPing();
                        break;
                    case METHOD_GET_PEERS:
                        processGetPeers();
                        break;
                    case METHOD_FIND_NODE:
                        processFindNode();
                        break;
                    case METHOD_ANNOUNCE_PEER:
                        processAnnouncePeer(resp);
                        break;
                    default:
                        break;
                }
            });
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * 处理ping方法的响应
     */
    private void processPing() {
//        DhtApp.NODE.routes.addNode(target);
    }

    /**
     * 处理get_peers的响应
     */
    private void processGetPeers() {
        ListNode values = (ListNode) resp.getNode("values");
        if (values == null) {
            StringNode nodes = (StringNode) resp.getNode("nodes");
            byte[] decode = nodes.decode();
            for (int i = 0; i < decode.length; i += 26) {
                NodeInfo nodeInfo = NodeInfo.fromBytes(decode, i);
                krpc.send(resp, nodeInfo);
            }
        } else {
            logger.info("nodes :" + values.getValue().size());
            List<InetSocketAddress> peers = values.getValue().stream().map(node -> {
                byte[] bytes = node.decode();
                return new InetSocketAddress(Utils.getAddrFromBytes(bytes, 0), Utils.bytes2Int(bytes, 4, 2));
            }).collect(Collectors.toList());
            PeerManager.PM.addAllPeer(key, peers);
        }
    }

    /**
     * 处理findNode的响应
     */
    private void processFindNode() {
        StringNode nodes = (StringNode) resp.getNode("nodes");
        if (nodes == null) return;
        byte[] decode = nodes.decode();
        int len = decode.length;
        if (len % 26 != 0) {
            logger.error("find node resp is not correct");
            return;
        }
        boolean found = false;
        for (int i = 0; i < len; i += 26) {
            NodeInfo node = NodeInfo.fromBytes(decode, i);
            if (node.getKey().equals(key)) {
                found = true;
                logger.info("found node :" + node.getAddress().getHostAddress() + ":" + node.getPort());
            }
            DhtApp.NODE.addNode(node);
        }
        if (!found) {
            List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(key);
            for (NodeInfo info : infos) {
                krpc.findNode(info, key);
            }
        }
    }


    /**
     * TODO 实现
     *
     * @param resp
     */
    private void processAnnouncePeer(DictionaryNode resp) {

    }
}

