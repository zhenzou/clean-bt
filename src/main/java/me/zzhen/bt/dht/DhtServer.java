package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.Decoder;
import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.TokenManager;
import me.zzhen.bt.dht.krpc.Krpc;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 * DHT客户端，用于执行对其他DHT节点的请求操作
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtServer {

    private static final Logger logger = LoggerFactory.getLogger(DhtServer.class.getName());

    private NodeInfo self;
    private Krpc krpc;
    private DatagramSocket socket;

    private ScheduledExecutorService autoFindNode = Executors.newScheduledThreadPool(1);

    public DhtServer(DatagramSocket socket, NodeInfo self, Krpc krpc) {
        this.socket = socket;
        this.self = self;
        this.krpc = krpc;
    }

    private void listen() {
        new Thread(() -> {
            try {
                byte[] bytes = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    socket.receive(packet);
                    if (DhtApp.NODE.isBlackItem(packet.getAddress(), packet.getPort())) continue;
                    int length = packet.getLength();
                    try {
                        DictionaryNode node = DictionaryNode.decode(new ByteArrayInputStream(bytes, 0, length));
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();
                        krpc.response(address, port, node);
                    } catch (RuntimeException e) {
//                        logger.error("data:" + packet.getLength() + Utils.toHex(bytes, 0, length));
//                        logger.error(e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            } finally {
                socket.close();
            }
        }).start();
    }

    private void join() {
        for (NodeInfo target : DhtApp.BOOTSTRAP_NODE) {
            krpc.findNode(target, self.getKey());
        }
        //定时,自动向邻居节点发送find_node请求
        autoFindNode.scheduleAtFixedRate(() -> DhtApp.NODE.routes.refresh(krpc), DhtConfig.AUTO_FIND, DhtConfig.AUTO_FIND, TimeUnit.SECONDS);
        //定时清理过期Token
        autoFindNode.scheduleAtFixedRate(TokenManager::clearTokens, DhtConfig.TOKEN_TIMEOUT, DhtConfig.TOKEN_TIMEOUT, TimeUnit.MINUTES);
    }

    public void init() {
        listen();
        join();
    }
}
