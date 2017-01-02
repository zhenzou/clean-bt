package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;

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
    private DictionaryNode request;

    /**
     * 情趣的来源地址
     */
    private InetAddress address;

    /**
     * 请求的来源端口
     */
    private int port;

    private RequestCallback callback;

    /**
     * 全局socket
     */
    private DatagramSocket socket;

    public ResponseWorker(DatagramSocket socket, InetAddress address, int port, DictionaryNode request, RequestCallback callback) {
        this.socket = socket;
        this.request = request;
        this.address = address;
        this.port = port;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (DhtApp.NODE.isBlackItem(address, port)) return;
        response(address, port, request);
    }

    public void response(InetAddress address, int port, DictionaryNode request) {
        Node method = request.getNode("q");
//        logger.info(method + "  send from :" + address.getHostAddress() + ":" + port);
        Node t = request.getNode("t");
        DictionaryNode arg = (DictionaryNode) request.getNode("a");
        Node id = arg.getNode("id");
        NodeKey key = new NodeKey(id.decode());
        NodeInfo info = new NodeInfo(address, port, key);
        DhtApp.NODE.addNode(info);
        switch (method.toString()) {
            case METHOD_PING:
                callback.onPing(info, request);
                break;
            case METHOD_GET_PEERS:
                callback.onGetPeer(info, t, arg.getNode("info_hash"));
                break;
            case METHOD_FIND_NODE:
                callback.onGetPeer(info, t, arg.getNode("target"));
                break;
            case METHOD_ANNOUNCE_PEER:
                doResponseAnnouncePeer(address, port, key, t, arg);
                break;
            default:
                break;
        }
    }


    /**
     * 响应announce_peer请求
     *
     * @param t   token
     * @param req 请求内容
     */
    private void doResponseAnnouncePeer(InetAddress address, int port, NodeKey key, Node t, DictionaryNode req) {
        Node impliedPort = req.getNode("implied_port");
        if (impliedPort == null || "0".equals(String.valueOf(impliedPort))) {
            port = Integer.parseInt(req.getNode("port").toString());
        }
        logger.info("info_hash:" + Utils.toHex(req.getNode("info_hash").decode()));
        logger.info("address:" + address.getHostAddress());
        logger.info("port:" + port);

        callback.onAnnouncePeer(new NodeInfo(address, port, key), t, req.getNode("info_hash"));
    }
}
