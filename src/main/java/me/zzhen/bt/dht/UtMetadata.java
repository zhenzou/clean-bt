package me.zzhen.bt.dht;

import me.zzhen.bt.decoder.DictionaryNode;
import me.zzhen.bt.decoder.IntNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class UtMetadata {

    public static final int MSG_REQUEST = 0;
    public static final int MSG_DATA = 1;
    public static final int MSG_REJECT = 2;

    public static final String MSG_TYPE = "msg_type";
    public static final String MSG_PIECE = "piece";
    public static final String MSG_TOTAL_SIZE = "total_size";

    private Socket socket;

    private DictionaryNode data;
    private DictionaryNode msg;


    public UtMetadata(InetAddress address, int port) {
        try {
            socket = new Socket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void request(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REQUEST));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        send(msg.encode());
    }

    public void data(int piece, byte[] data, int totalSize) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_DATA));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        msg.addNode(MSG_TOTAL_SIZE, new IntNode(totalSize));
        byte[] encode = msg.encode();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(encode.length + data.length);
        try {
            baos.write(encode);
            baos.write('e');
            baos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        send(baos.toByteArray());
    }

    public void reject(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REJECT));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        send(msg.encode());
    }

    public void send(byte[] bytes) {

    }
}
