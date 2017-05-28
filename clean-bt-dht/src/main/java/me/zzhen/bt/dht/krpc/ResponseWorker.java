package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.Dht;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

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

    /**
     * 请求的全部内容
     */
    private DictNode request;

    /**
     * 情趣的来源地址
     */
    private InetAddress address;

    /**
     * 请求的来源端口
     */
    private int port;

    private RequestProcessor callback;

    /**
     * 全局socket
     */
    private DatagramSocket socket;

    public ResponseWorker(DatagramSocket socket, InetAddress address, int port, DictNode request, RequestProcessor callback) {
        this.socket = socket;
        this.request = request;
        this.address = address;
        this.port = port;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (Dht.NODE.isBlackItem(address, port)) return;
        response(address, port, request);
    }

    public void response(InetAddress address, int port, DictNode request) {
        Node method = request.getNode("q");
//        logger.info(method + "  request from :" + address.getHostAddress() + ":" + port);
        Node t = request.getNode("t");
        DictNode arg = (DictNode) request.getNode("a");
        Node id = arg.getNode("id");

        NodeId key = new NodeId(id.decode());
        NodeInfo info = new NodeInfo(address, port, key);
        if (id.decode().length != 20) {
            callback.error(info, t, id, Message.ERRNO_PROTOCOL, "invalid id");
            return;
        }
        Optional<NodeInfo> optional = Dht.NODE.routes.getByAddr(address, port);
        if (optional.isPresent()) {
            if (!optional.get().getId().equals(key)) {
                callback.error(info, t, id, Message.ERRNO_PROTOCOL, "invalid id");
                Dht.NODE.addBlackItem(address, port);
                Dht.NODE.routes.removeByAddr(new InetSocketAddress(address, port));
                return;
            }
        }
        Dht.NODE.addNode(info);
        switch (method.toString()) {
            case METHOD_PING:
                callback.onPing(info, request);
                break;
            case METHOD_GET_PEERS:
                callback.onGetPeer(info, t, arg.getNode("info_hash"));
                break;
            case METHOD_FIND_NODE:
                callback.onFindNode(info, t, arg.getNode("target"));
                break;
            case METHOD_ANNOUNCE_PEER:
                doResponseAnnouncePeer(address, port, key, t, arg);
                break;
            default:
                callback.error(info, t, id, Message.ERRNO_UNKNOWN, "unknown method");
                break;
        }
    }


    /**
     * 响应announce_peer请求
     *
     * @param t   token
     * @param req 请求内容
     */
    private void doResponseAnnouncePeer(InetAddress address, int port, NodeId key, Node t, DictNode req) {
        Node impliedPort = req.getNode("implied_port");
        if (impliedPort == null || "0".equals(String.valueOf(impliedPort))) {
//            logger.info("implied_port:" + port);
            port = Integer.parseInt(req.getNode("port").toString());
        }
        callback.onAnnouncePeer(new NodeInfo(address, port, key), t, req.getNode("info_hash"), req.getNode("token"));
    }
}
