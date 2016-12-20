package me.zzhen.bt.dht.krpc;


import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Response;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;

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
class RequestWorker implements Callable<Response> {

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
     * 本次请求已经请求过的节点
     */
    private Set<NodeInfo> requestedNode = new HashSet<>();

    /**
     * 待请求的节点队列
     */
    private Queue<NodeInfo> requestQueue = new ArrayDeque<>();//TODO 完善


    public RequestWorker(DictionaryNode request, NodeInfo target, String method) {
        this.request = request;
        this.target = target;
        this.method = method;
        if (method.equals(METHOD_FIND_NODE)) {
            DictionaryNode a = (DictionaryNode) request.getNode("a");
            key = new NodeKey(a.getNode("target").decode());
        }
    }


    @Override
    public Response call() throws Exception {
        Response resp = null;
        switch (method) {
            case METHOD_PING:
                resp = doPingRequest(target);
                break;
            case METHOD_GET_PEERS:
                resp = doGetPeersRequest(target);
                break;
            case METHOD_FIND_NODE:
                resp = doFindNodeRequest(target);
                break;
            case METHOD_ANNOUNCE_PEER:
                resp = doAnnounceRequest(request);
                break;
            default:
                break;
        }

        return resp;
    }

    /**
     * 最终发出请求的方法
     *
     * @return
     */
    private DictionaryNode doRequest(NodeInfo target) {
        logger.info("routetable------------" + DhtApp.NODE.routes.size());
        byte[] data = request.encode();//TODO optimize
        DictionaryNode resp = null;
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = target.getAddress();
            logger.info("doRequest---" + method + ":" + address.getHostAddress() + ":" + target.getPort() + "---" + String.valueOf(target.getKey()));
            socket.setSoTimeout(30 * 1000);
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, target.getPort());
            socket.send(packet);
            byte[] getByte = new byte[1024];
            DatagramPacket result = new DatagramPacket(getByte, 1024);
            socket.receive(result);
            InetAddress addr = result.getAddress();
            int port = result.getPort();
            logger.info("received from " + method + ":" + addr.getHostAddress() + ":" + port);
            resp = (DictionaryNode) Decoder.decode(getByte, 0, result.getLength()).get(0);
            Node y = resp.getNode("y");
//            logger.info(getReceivedType(y.toString()));
            return resp;

        } catch (IOException e) {
            logger.error(e.getMessage());
            DhtApp.NODE.addBlackItem(target.getAddress().getHostAddress(), target.getPort());
            resp = Response.makeError(target.getKey(), 202, e.getMessage());
            resp.addNode("t", request.getNode("t"));
        }
        return resp;
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
    private Response doPingRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);//处理异常
        Node t = resp.getNode("t");
        return new Response(method, resp.getNode("r"), Utils.bytes2Int(t.decode()));
    }

    private Response doGetPeersRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);
        if (!Response.isError(resp)) {
            int token = Utils.bytes2Int(resp.getNode("token").decode());
            ListNode values = (ListNode) resp.getNode("values");
            if (values == null) {
                logger.info("not find peers");
                StringNode nodes = (StringNode) resp.getNode("nodes");
                byte[] decode = nodes.decode();
                logger.info("decode len :" + decode.length);
                for (int i = 0; i < decode.length; i += 26) {
                    NodeInfo nodeInfo = new NodeInfo(decode, i);
                    if (!requestedNode.contains(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
                        requestedNode.add(nodeInfo);
                        requestQueue.add(nodeInfo);
                    }
                }
                if (!requestQueue.isEmpty()) {
                    NodeInfo node = requestQueue.poll();
                    return doGetPeersRequest(node);
                } else {
                    return new Response(method, Response.makeError(target.getKey(), 202, "找不到更多的节点"), token);
                }
            } else {
                logger.info("found peers");
                logger.info("nodes :" + values.getValue().size());
                return new Response(method, values, token);
            }
        } else {
            if (!requestQueue.isEmpty()) {
                NodeInfo node = requestQueue.poll();
                return doGetPeersRequest(node);
            } else {
                return new Response(method, resp, Utils.bytes2Int(request.getNode("t").decode()));//Error?
            }
        }

    }

    /**
     * @param target
     * @return
     */
    private Response doFindNodeRequest(NodeInfo target) {
        DictionaryNode resp = doRequest(target);
        int token = Utils.bytes2Int(resp.getNode("t").decode());
        logger.info("token:" + token);
        if (!Response.isError(resp)) {
            DictionaryNode arg = (DictionaryNode) resp.getNode("r");
            StringNode nodes = (StringNode) arg.getNode("nodes");
            byte[] decode = nodes.decode();
            logger.info("find node decode len :" + decode.length);
            int len = decode.length;
            for (int i = 0; i < len; i += 26) {
                NodeInfo nodeInfo = new NodeInfo(decode, i);
                if (nodeInfo.getKey().equals(key)) {
                    logger.info("fond node :" + nodeInfo.getAddress().getHostAddress() + ":" + nodeInfo.getPort());
                } else {
                    if (!requestedNode.contains(nodeInfo) && !DhtApp.NODE.isBlackItem(nodeInfo)) {
                        requestedNode.add(nodeInfo);
                        requestQueue.add(nodeInfo);
                        DhtApp.NODE.routes.addNode(nodeInfo);
                    }
//                        logger.info("IP:Port" + nodeInfo.getAddress().getHostAddress() + ":" + nodeInfo.getPort());
                }
            }
            if (!requestQueue.isEmpty()) {
                NodeInfo node = requestQueue.poll();
                return doFindNodeRequest(node);
            } else {
                return new Response(method, Response.makeError(target.getKey(), 202, "找不到更多的节点"), token);
            }
//                return new Response(method, arg, token);
        } else {
            if (!requestQueue.isEmpty()) {
                NodeInfo node = requestQueue.poll();
                return doFindNodeRequest(node);
            } else {
                return new Response(method, Response.makeError(target.getKey(), 202, "找不到更多的节点"), token);
            }
        }
    }


    private Response doAnnounceRequest(DictionaryNode resp) {

        return null;

    }

}

