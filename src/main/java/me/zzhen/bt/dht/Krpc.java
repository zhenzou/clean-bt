package me.zzhen.bt.dht;

import me.zzhen.bt.decoder.*;

import javax.print.attribute.standard.RequestingUserName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Krpc {


    public boolean ping(NodeKey node) {

        return false;
    }

    public NodeInfo findNode(NodeKey node) {
        return new NodeInfo("127.0.0.1", 1234, node);
    }

    public List<NodeInfo> getPeers(NodeKey rescource) {
        return new ArrayList<>();
    }

    /**
     * 向整个DHT中加入 key 为 resource，val 为当前节点ID的值
     *
     * @param resource
     */
    public void announcePeer(NodeKey resource) {

    }

    public static void main(String[] args) throws UnsupportedEncodingException, UnknownHostException {
        DictionaryNode query = new DictionaryNode();
        query.addNode("t", new StringNode("1111".getBytes()));
        query.addNode("y", new StringNode("q".getBytes()));
        query.addNode("q", new StringNode("ping".getBytes()));
        DictionaryNode arg = new DictionaryNode();
        arg.addNode("info_hash", new StringNode("F6EF97E88EA15435D8D5D3BC55B00C2E090F396F".getBytes()));
        arg.addNode("id", new StringNode("F6EF97E88EA15435D8D5D3BC55B00C2E090F396F".getBytes()));
        query.addNode("a", arg);
//        urlConnection.

        byte[] encode = query.encode();
        System.out.println(new String(encode));

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress byName = InetAddress.getByName("router.utorrent.com");
            DatagramPacket packet = new DatagramPacket(encode, 0, encode.length, byName, 6881);
            socket.send(packet);
            byte[] getByte = new byte[1000];
            DatagramPacket result = new DatagramPacket(getByte, 1000);
            socket.setSoTimeout(1000000);
            socket.receive(result);
            Decoder decoder = new Decoder(getByte);
            decoder.parse();
            Node node = decoder.getValue().get(0);
            System.out.println(node.decode());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
