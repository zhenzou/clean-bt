package me.zzhen.bt.dht.krpc;


import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Token;
import me.zzhen.bt.dht.base.TokenManager;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static me.zzhen.bt.dht.krpc.Krpc.*;

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
    private RequestCallback callback;

    /**
     * @param resp     响应的内容
     * @param address
     * @param port
     * @param callback 处理完响应的回调
     */
    public ResponseProcessor(DictionaryNode resp, InetAddress address, int port, RequestCallback callback) {
        this.address = address;
        this.port = port;
        this.resp = resp;
        this.callback = callback;
    }


    @Override
    public void run() {
        long id = Long.parseLong(resp.getNode("t").toString());
        Token token = TokenManager.getToken(id);
        if (token == null) return;
        resp = (DictionaryNode) resp.getNode("r");
        DhtApp.NODE.addNode(new NodeInfo(address, port, new NodeKey(resp.getNode("id").decode())));
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
    }

    /**
     * 最终发出请求的方法
     * TODO NIO
     *
     * @param target
     * @return
     */
    private DictionaryNode doRequest(NodeInfo target) {
        if (target == null) return null;
//        logger.info("routes:" + DhtApp.NODE.routes.size() + ":" + "left:" + requestQueue.size());
        byte[] data = resp.encode();//TODO optimize
        DictionaryNode resp = null;
        try {
            InetAddress address = target.getAddress();
//            logger.info("resp to:" + method + ":" + address.getHostAddress() + ":" + target.getPort() + ":" + String.valueOf(target.getKey()));
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, target.getPort());
//            socket.send(packet);
            byte[] getByte = new byte[1024];
            DatagramPacket result = new DatagramPacket(getByte, 1024);
//            socket.receive(result);
//            InetAddress addr = result.getAddress();
//            int port = result.getPort();
//            logger.info("received from " + method + ":" + addr.getHostAddress() + ":" + port);
            resp = (DictionaryNode) Decoder.decode(getByte, 0, result.getLength()).get(0);
            Node y = resp.getNode("y");
            //接到响应,将
            DhtApp.NODE.addNode(target);
//            logger.info(getReceivedType(y.toString()));
            return resp;
        } catch (IOException e) {
            logger.error(e.getMessage());
            DhtApp.NODE.addBlackItem(target.getAddress(), target.getPort());
            resp = Message.makeError(target, 202, e.getMessage());
            resp.addNode("t", this.resp.getNode("t"));
            return null;
        }
    }

    private String getReceivedType(String c) {
        String method;
        switch (c) {
            case "r":
                method = "response";
                break;
            case "e":
                method = "error";
                break;
            case "q":
                method = "resp";
                break;
            default:
                method = "default";
                break;
        }
        return method;
    }


    /**
     * 处理ping方法的响应
     */
    private void processPing() {
        //TODO 检测t和对应的ID
        DhtApp.NODE.routes.addNode(target);
    }

    /**
     * 处理get_peers的响应
     */
    private void processGetPeers() {
        //TODO 真正的实现Token管理
//        long token = Long.parseLong(Utils.toHex(resp.getNode("id").decode()), 16);
        ListNode values = (ListNode) resp.getNode("values");
        if (values == null) {
            StringNode nodes = (StringNode) resp.getNode("nodes");
            byte[] decode = nodes.decode();
            logger.info("length :" + decode.length);
            for (int i = 0; i < decode.length; i += 26) {
                NodeInfo nodeInfo = new NodeInfo(decode, i);
//                logger.info(nodeInfo.getAddress() + ":" + nodeInfo.getPort());
                callback.request(resp, nodeInfo, method);
            }
        } else {
            logger.info("nodes :" + values.getValue().size());
            //TODO fetch
            //TODO 回调
        }
    }

    /**
     * 处理findNode的响应
     */
    private void processFindNode() {
        StringNode nodes = (StringNode) resp.getNode("nodes");
        byte[] decode = nodes.decode();
//                logger.info("find node decode len :" + decode.length);
        int len = decode.length;
        for (int i = 0; i < len; i += 26) {
            NodeInfo nodeInfo = new NodeInfo(decode, i);
            if (nodeInfo.getKey().equals(key)) {
                logger.info("found node :" + nodeInfo.getAddress().getHostAddress() + ":" + nodeInfo.getPort());
            }
            logger.info(nodeInfo.getAddress() + ":" + nodeInfo.getPort());
            callback.request(this.resp, nodeInfo, method);
        }
    }


    /**
     * 检查返回来的t的值是否与发出请求的t值一致
     *
     * @param resp
     * @return
     */
    private boolean check(DictionaryNode resp) {
        if (resp == null) return false;
        Node t = resp.getNode("t");
        Node st = this.resp.getNode("t");
        return st.equals(t);
    }

    private void processAnnouncePeer(DictionaryNode resp) {

    }
}

