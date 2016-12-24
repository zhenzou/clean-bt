package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.Decoder;
import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.RouteTable;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 * DHT客户端，用于执行对其他DHT节点的请求操作
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtServer {

    private NodeInfo self;
    private Krpc krpc;
    private static final Logger logger = LoggerFactory.getLogger(DhtServer.class.getName());


    public DhtServer(NodeInfo self, Krpc krpc) {
        this.self = self;
        this.krpc = krpc;
    }

    private void join() {
        for (NodeInfo target : DhtApp.BOOTSTRAP_NODE) {
            krpc.findNode(target, self.getKey());
        }
    }

    private void listen() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DhtConfig.SERVER_PORT)) {
                while (true) {
                    byte[] bytes = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(bytes, 1024);
                    socket.receive(packet);
                    int length = packet.getLength();
                    Node node = Decoder.decode(bytes, 0, length).get(0);
                    krpc.response(socket, packet.getAddress(), packet.getPort(), (DictionaryNode) node);
//                    new ResponseWorker(packet.getAddress(), packet.getPort(), (DictionaryNode) node);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public void init() {
        listen();
        join();
    }
}
