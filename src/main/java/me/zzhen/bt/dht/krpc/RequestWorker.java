package me.zzhen.bt.dht.krpc;


import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.AsynchronousByteChannel;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static me.zzhen.bt.dht.krpc.Krpc.*;
/**
 * Project:CleanBT
 * Create Time: 16-12-20.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */


/**
 * 请求的工作线程，每个请求启一个Worker线程，同一个请求的request参数不变，包括Token
 */
class RequestWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RequestWorker.class.getName());
    /**
     * 请求的参数
     */
    private DictionaryNode request;
    /**
     * 请求的目标DHT节点的信息
     */
    private NodeInfo target;

    /**
     * 请求的目标资源或者节点ID
     */
    private NodeKey key;
    /**
     * 请求的方法
     * TODO 使用注解限制值 只有ping，get_peers,find_node,announce_peer
     */
    private String method;


    /**
     * 全局发出请求的socket
     */
    private DatagramSocket socket;

    /**
     * 回调
     */
    private RequestCallback callback;

    /**
     * 本次请求已经请求过的节点
     * get_peers,find_node,announce_peer中要用
     */
    private Set<NodeInfo> requestedNode = new HashSet<>();

    /**
     * 待请求的节点队列
     */
    private Queue<NodeInfo> requestQueue = new ArrayDeque<>();//TODO 完善


    public RequestWorker(DictionaryNode request, NodeInfo target, String method, RequestCallback callback) {
        this.request = request;
        this.target = target;
        this.method = method;
        this.callback = callback;
        if (method.equals(METHOD_FIND_NODE)) {
            DictionaryNode a = (DictionaryNode) request.getNode("a");
            key = new NodeKey(a.getNode("target").decode());
        }
    }

    @Override
    public void run() {
        prepare();
        if (socket == null) return;
        switch (method) {
            case METHOD_PING:
                doPingRequest(target);
                break;
            case METHOD_GET_PEERS:
                doGetPeersRequest(target);
                break;
            case METHOD_FIND_NODE:
                doFindNodeRequest(target);
                break;
            case METHOD_ANNOUNCE_PEER:
                doAnnounceRequest(request);
                break;
            default:
                break;
        }
        socket.close();
    }

    private void prepare() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(DhtConfig.CONN_TIMEOUT);
        } catch (SocketException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
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
        logger.info("routes:" + DhtApp.NODE.routes.size() + ":" + "left:" + requestQueue.size());
        byte[] data = request.encode();//TODO optimize
        DictionaryNode resp = null;
        try {
            InetAddress address = target.getAddress();
//            logger.info("request to:" + method + ":" + address.getHostAddress() + ":" + target.getPort() + ":" + String.valueOf(target.getKey()));
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, target.getPort());
            socket.send(packet);
            byte[] getByte = new byte[1024];
            DatagramPacket result = new DatagramPacket(getByte, 1024);
            socket.receive(result);
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
            resp = Response.makeError(target.getKey(), 202, e.getMessage());
            resp.addNode("t", request.getNode("t"));
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
                method = "request";
                break;
            default:
                method = "default";
                break;
        }
        return method;
    }


    /**
     * @param target 请求的目标节点
     * @return 响应的主体部分
     */
    private void doPingRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);
        if (check(resp)) DhtApp.NODE.routes.addNode(target);
    }

    private void doGetPeersRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);
        if (!Response.isError(resp) && check(resp)) {
            //TODO 真正的实现Token管理
            long token = Long.parseLong(Utils.toHex(resp.getNode("token").decode()), 16);
            ListNode values = (ListNode) resp.getNode("values");
            if (values == null) {
                StringNode nodes = (StringNode) resp.getNode("nodes");
                byte[] decode = nodes.decode();
//                logger.info("decode len :" + decode.length);
                for (int i = 0; i < decode.length; i += 26) {
                    NodeInfo nodeInfo = new NodeInfo(decode, i);
//                    if (!requestedNode.contains(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
//                        requestedNode.add(nodeInfo);
//                        requestQueue.add(nodeInfo);
//                    }
                    if (!callback.requested(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
//                        Krpc.requested.add(nodeInfo);
//                        Krpc.toRequest.add(nodeInfo);
                        callback.request(request, nodeInfo, method);
                    }
                }
            } else {
                logger.info("nodes :" + values.getValue().size());
                //TODO fetch
                //TODO 回调
            }
        }
//        if (!requestQueue.isEmpty()) {
//            NodeInfo node = requestQueue.poll();
//            doGetPeersRequest(node);
//        }
    }

    /**
     * @param target
     * @return
     */
    private void doFindNodeRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);
        if (check(resp)) {
            if (!Response.isError(resp)) {
                DictionaryNode arg = (DictionaryNode) resp.getNode("r");
                StringNode nodes = (StringNode) arg.getNode("nodes");
                byte[] decode = nodes.decode();
//                logger.info("find node decode len :" + decode.length);
                int len = decode.length;
                for (int i = 0; i < len; i += 26) {
                    NodeInfo nodeInfo = new NodeInfo(decode, i);
                    if (nodeInfo.getKey().equals(key)) {
                        logger.info("found node :" + nodeInfo.getAddress().getHostAddress() + ":" + nodeInfo.getPort());
                    } else {
//                        if (!requestedNode.contains(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
//                            requestedNode.add(nodeInfo);
//                            requestQueue.add(nodeInfo);
//                            DhtApp.NODE.routes.addNode(nodeInfo);
//                        }
                        if (!callback.requested(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
//                            Krpc.requested.add(nodeInfo);
//                            Krpc.toRequest.add(nodeInfo);
                            callback.request(request, nodeInfo, method);
                        }
                    }
                }
            }
        }
//        if (!requestQueue.isEmpty()) {
//            NodeInfo node = requestQueue.poll();
//            doFindNodeRequest(node);
//        }
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
        Node st = request.getNode("t");
        return st.equals(t);
    }

    private void doAnnounceRequest(DictionaryNode resp) {

    }
}

