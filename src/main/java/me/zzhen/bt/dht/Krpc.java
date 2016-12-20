package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.*;
import me.zzhen.bt.utils.Utils;
import me.zzhen.bt.bencode.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc {

    private static final Logger logger = LoggerFactory.getLogger(Krpc.class.getName());


    private TokenManager tokenManager = new TokenManager();
    public static final String METHOD_PING = "ping";
    public static final String METHOD_ANNOUNCE_PEER = "announce_peer";
    public static final String METHOD_GET_PEERS = "get_peers";
    public static final String METHOD_FIND_NODE = "find_node";

    private final NodeKey self;
    private final RouteTable table;

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);

    public Krpc(NodeKey self, RouteTable table) {
        this.self = self;
        this.table = table;
    }


    public Response ping(NodeInfo node) {
        DictionaryNode msg = Request.makeRequest(node.getKey(), METHOD_PING);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("id", new StringNode(self.getValue()));
        msg.addNode("a", arg);
        return request(msg, node, METHOD_PING);
    }

    public Response findNode(NodeInfo node, byte[] target) {
        DictionaryNode msg = Request.makeRequest(new NodeKey(target), METHOD_FIND_NODE);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("target", new StringNode(target));
        arg.addNode("id", new StringNode(self.getValue()));
        msg.addNode("a", arg);
        return request(msg, node, METHOD_FIND_NODE);
    }

    /**
     * 每次请求都会有Token，看看能不能通过获取吧
     *
     * @param node
     * @param peer
     */
    public Response getPeers(NodeInfo node, NodeKey peer) {
        DictionaryNode msg = Request.makeRequest(peer, METHOD_GET_PEERS);
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("info_hash", new StringNode(peer.getValue()));
        arg.addNode("id", new StringNode(self.getValue()));
        msg.addNode("a", arg);
        return request(msg, node, METHOD_GET_PEERS);
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     * TODO
     *
     * @param peer
     */
    public Response announcePeer(NodeKey peer) {
        DictionaryNode req = Request.makeRequest(peer, METHOD_ANNOUNCE_PEER);
        req.addNode("q", new StringNode(METHOD_ANNOUNCE_PEER));
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("info_hash", new StringNode(self.getValue()));
        arg.addNode("port", new IntNode(DhtConfig.SERVER_PORT));
        arg.addNode("id", new StringNode(self.getValue()));
        req.addNode("a", arg);
        return request(req, null, METHOD_ANNOUNCE_PEER);
    }

    private Response request(DictionaryNode request, NodeInfo node, String method) {
        try {
            Future<Response> submit = executor.submit(new RequestWorker(request, node, method));
            logger.info("request -=-==========:" + (submit == null));
            return submit.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 请求的工作线程，每个请求启一个Worker线程，同一个请求的request参数不变，包括Token
     */
    class RequestWorker implements Callable<Response> {
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
//            requestQueue.add(target);//好像不能统一
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
            byte[] data = request.encode();//TODO optimize
            DictionaryNode resp = null;
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress address = target.getAddress();
                logger.info("doRequest---" + method + ":" + address.getHostAddress() + ":" + target.getPort() + "---" + new String(data));
                socket.setSoTimeout(30 * 1000);
                DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, target.getPort());
                socket.send(packet);
                logger.info("send " + method + ":" + address.getHostAddress() + ":" + target.getPort());
                byte[] getByte = new byte[1024];
                DatagramPacket result = new DatagramPacket(getByte, 1024);
                socket.receive(result);
                InetAddress addr = result.getAddress();
                int port = result.getPort();
                logger.info("received from " + method + ":" + addr.getHostAddress() + ":" + port);
                resp = (DictionaryNode) Decoder.decode(getByte, 0, result.getLength()).get(0);
                Node y = resp.getNode("y");
                logger.info(getReceivedType(y.toString()));
                return resp;
            } catch (IOException e) {
                logger.error(e.getMessage());
                DhtApp.self().addBlackItem(target.getAddress().getHostAddress(), target.getPort());
                resp = Response.makeError(target.getKey(), 202, e.getMessage());
                resp.addNode("t", request.getNode("t"));
            }
            return resp;
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
                        if (!requestedNode.contains(nodeInfo) && !DhtApp.self().isBlackItem(nodeInfo)) {
                            requestedNode.add(nodeInfo);
                            requestQueue.add(nodeInfo);
                        }
                    }
                    if (!requestQueue.isEmpty()) {
                        NodeInfo node = nodeQueue.poll();
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
                    NodeInfo node = nodeQueue.poll();
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
                    }else {
                        if (!requestedNode.contains(nodeInfo) && !DhtApp.self().isBlackItem(nodeInfo)) {
                            requestedNode.add(nodeInfo);
                            requestQueue.add(nodeInfo);
                        }
//                        logger.info("IP:Port" + nodeInfo.getAddress().getHostAddress() + ":" + nodeInfo.getPort());
                    }
                }
                if (!requestQueue.isEmpty()) {
                    NodeInfo node = nodeQueue.poll();
                    return doGetPeersRequest(node);
                } else {
                    return new Response(method, Response.makeError(target.getKey(), 202, "找不到更多的节点"), token);
                }
//                return new Response(method, arg, token);
            } else {
                if (!requestQueue.isEmpty()) {
                    NodeInfo node = nodeQueue.poll();
                    return doGetPeersRequest(node);
                } else {
                    return new Response(method, Response.makeError(target.getKey(), 202, "找不到更多的节点"), token);
                }
            }
        }


        private Response doAnnounceRequest(DictionaryNode resp) {

            return null;

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


        private Set<NodeInfo> nodeSet = new HashSet<>();
        private Queue<NodeInfo> nodeQueue = new ArrayDeque<>();//TODO 完善


        private String getMetadata(InetAddress nodeAddr, int port) {
            try (Socket socket = new Socket(nodeAddr, port)) {

            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return "";
        }

        private void response(NodeInfo nodeInfo, Node data) {
            byte[] encode = data.encode();
            if (nodeInfo != null) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(30 * 1000);
                    InetAddress address = nodeInfo.getAddress();
                    logger.info("doRequest ip:" + address.getHostAddress());
                    logger.info("doRequest port:" + nodeInfo.getPort());
                    DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, address, nodeInfo.getPort());
                    socket.send(packet);
                } catch (SocketTimeoutException e) {
                    logger.error(e.getMessage());
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
            }
        }
    }
}