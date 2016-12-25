package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Project:CleanBT
 * Create Time: 16-12-24.
 * Description:
 * 发出请求的工作线程
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RequestWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RequestWorker.class.getName());

    /**
     * 请求的方法
     */
    private final String method;
    /**
     * 请求的内容
     */
    private DictionaryNode request;
    /**
     * 请求的目标DHT节点的信息
     */
    private NodeInfo target;

    /**
     * 全局发出请求的socket
     */
    private DatagramSocket socket;


    public RequestWorker(DatagramSocket socket, DictionaryNode request, NodeInfo target, String method) {
        this.socket = socket;
        this.request = request;
        this.target = target;
        this.method = method;
    }


    @Override
    public void run() {
        if (target == null) return;
        byte[] data = request.encode();
        try {
            InetAddress address = target.getAddress();
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, target.getPort());
            socket.send(packet);
//            logger.info(method + " request to:" + ":" + address.getHostAddress() + ":" + target.getPort() + ":" + String.valueOf(target.getKey()));
        } catch (IOException e) {
            logger.error(e.getMessage());
            DhtApp.NODE.addBlackItem(target.getAddress(), target.getPort());
        }
    }
}
