package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.dht.Dht;
import me.zzhen.bt.dht.NodeInfo;
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
 * 发送数据的工作线程
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DataSendWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DataSendWorker.class.getName());

    /**
     * 请求的内容
     */
    private DictNode request;
    /**
     * 请求的目标DHT节点的信息
     */
    private NodeInfo target;

    /**
     * 全局发出请求的socket
     */
    private DatagramSocket socket;


    public DataSendWorker(DatagramSocket socket, DictNode request, NodeInfo target) {
        this.socket = socket;
        this.request = request;
        this.target = target;
    }


    @Override
    public void run() {
        if (target == null) return;
        byte[] data = request.encode();
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, InetAddress.getByName(target.address), target.port);
            socket.send(packet);
        } catch (IOException e) {
            logger.error(e.getMessage());
            Dht.NODE.addBlackItem(target.getFullAddress());
        }
    }
}
