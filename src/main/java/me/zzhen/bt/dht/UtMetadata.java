package me.zzhen.bt.dht;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.IntNode;
import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class UtMetadata {

    private static final Logger logger = LoggerFactory.getLogger(UtMetadata.class.getName());

    public static final int MSG_REQUEST = 0;
    public static final int MSG_DATA = 1;
    public static final int MSG_REJECT = 2;

    public static final String MSG_TYPE = "msg_type";
    public static final String MSG_PIECE = "piece";
    public static final String MSG_TOTAL_SIZE = "total_size";
    public static final String MSG_UT_METADATA = "ut_metadata";
    public static final String MSG_METADATA_SIZE = "metadata_size";


    /**
     * BitTorrent 协议标识符，BEP03 规定
     */
    public static final String PSTR = "BitTorrent protocol";
    //    public static final byte[] RESERVED = {0, 0, 0, 0, 0, 16, 0, 1};
    public static final byte[] RESERVED = {19, 66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114,
            111, 116, 111, 99, 111, 108, 0, 0, 0, 0, 0, 16, 0, 1};

    public static final byte PEER_MSG_CHOKE = 0;
    public static final byte PEER_MSG_UNCHOKE = 1;
    public static final byte PEER_MSG_INTERESTED = 2;
    public static final byte PEER_MSG_NOT_INTERESTED = 3;
    public static final byte PEER_MSG_HAVE = 4;
    public static final byte PEER_MSG_BITFIELD = 5;
    public static final byte PEER_MSG_REQUEST = 6;
    public static final byte PEER_MSG_PIECE = 7;
    public static final byte PEER_MSG_CANCEL = 8;

    public static final int EXTENDED = 20;


    private Socket socket;


    private DictionaryNode data;
    private DictionaryNode msg;

    private NodeKey key;//暂时使用这些作为PeerId
    private String peerId = "-UT-";

    public static final int BLOCK_SIZE = 16 * 1024;

    public UtMetadata(InetAddress address, int port) throws IOException {
        logger.info("init");
        socket = new Socket();
        socket.setSoTimeout(10000);
        socket.connect(new InetSocketAddress(address, port));
        logger.info("over");
    }

    public void handleShake(String hexInfoHash, NodeKey key) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(49 + 19);
//        baos.write(19 & 0xFF);
//        RESERVED[5] |= 0x10;
        try {
//            baos.write(PSTR.getBytes());
            baos.write(RESERVED);
            baos.write(Utils.hex2Bytes(hexInfoHash));
            baos.write(key.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleResp(byte[] data) {
        logger.info(data.length + "");
        int len = Utils.bytes2Int(data, 0, 4);

    }

    private byte[] readMessage(InputStream input) {
        return IO.readKBytes(input, 6);
    }


    public void fetchMetadata(String infoHash, NodeKey key) {
        handleShake(infoHash, key);//先能用，以后再处理
        try (OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {
            DictionaryNode node = new DictionaryNode();
            DictionaryNode meta = new DictionaryNode();
            meta.addNode(MSG_UT_METADATA, new IntNode(1));
            node.addNode("m", meta);
            out.write(node.encode());
            out.flush();
            socket.shutdownOutput();
            logger.info("shaked");
            byte[] bytes = IO.readAllBytes(in);
            System.out.println("recived:" + new String(bytes));
            while (true) {
                byte[] msg = readMessage(in);
                logger.error("msg:" + new String(msg));
                int length = Utils.bytes2Int(msg, 0, 4);
                logger.error("length:" + length);
                if (length == 0) continue;
                int extend = msg[6];
                if (extend == EXTENDED) {
                    int extendId = msg[5];
                    if (extendId == 0) continue;
                    byte[] data = IO.readAllBytes(in);
                    logger.error(new String(data));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean onHandShake(byte[] bytes) {
        if ((bytes[25] & 0x10) == 0) return false;
        return true;
    }


    public void sendKeepLive() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(Utils.int2Bytes(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        send(baos.toByteArray());
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
        try {
            OutputStream out = socket.getOutputStream();
            out.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DictionaryNode buildMsg(DictionaryNode data) {
        DictionaryNode msg = new DictionaryNode();
        DictionaryNode head = new DictionaryNode();
        msg.addNode("ut_metadata", new IntNode(3));
        msg.addNode("m", head);
        return msg;
    }

    public static void main(String[] args) {
        try {
            UtMetadata utMetadata = new UtMetadata(InetAddress.getByName("107.139.136.217"), 60234);
            utMetadata.fetchMetadata("546cf15f724d19c4319cc17b179d7e035f89c1f4", NodeKey.genRandomKey());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
