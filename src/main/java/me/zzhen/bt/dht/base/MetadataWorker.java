package me.zzhen.bt.dht.base;

import com.sun.xml.internal.messaging.saaj.soap.StringDataContentHandler;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class MetadataWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MetadataWorker.class.getName());

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

    /**
     * 请求的资源的info_hash
     */
    private String hash;

    /**
     * 请求的来源地址
     */
    private InetAddress address;
    /**
     * 资源下载的端口
     */
    private int port;


    private static String peerId = "-UT-";

    public static final int BLOCK_SIZE = 16 * 1024;


    public MetadataWorker(InetAddress address, int port, String hash) {
        this.address = address;
        this.port = port;
        this.hash = hash;
    }

    @Override
    public void run() {
        fetchMetadata();
    }

    private void handleResp(byte[] data) {
        logger.info(data.length + "");
        int len = Utils.bytesToInt(data, 0, 4);
    }

    private byte[] readMessage(InputStream input) {
        return IO.readKBytes(input, 6);
    }

    public void sendKeepLive() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(Utils.intToBytes(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        send(baos.toByteArray());
    }

    public void request(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REQUEST));
        msg.addNode(MSG_PIECE, new IntNode(piece));
//        send(msg.encode());
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
//        send(baos.toByteArray());
    }

    public void reject(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REJECT));
        msg.addNode(MSG_PIECE, new IntNode(piece));
//        send(msg.encode());
    }


    public void send(OutputStream out, byte[] bytes) {
        try {
            out.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DictionaryNode buildShake() {
        DictionaryNode msg = new DictionaryNode();
        DictionaryNode head = new DictionaryNode();
        head.addNode("ut_metadata", new IntNode(1));
        msg.addNode("m", head);
        return msg;
    }

    private DictionaryNode buildRequestMsg(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REQUEST));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        return msg;
    }

    public void sendHandleShake(OutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(49 + 19);
//        baos.write(19 & 0xFF);
//        RESERVED[5] |= 0x10;

//            baos.write(PSTR.getBytes());
        baos.write(RESERVED);
        baos.write(Utils.hex2Bytes(hash));
        baos.write(NodeKey.genRandomKey().getValue());
        out.write(baos.toByteArray());

    }

    public boolean onHandShake(InputStream in) {
        byte[] bytes = IO.readKBytes(in, 68);
        if (bytes.length < 68) return false;
        if ((bytes[25] & 0x10) == 0) return false;
        return true;
    }


    private void sendExtHandShake(OutputStream out) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{EXTENDED, 0});
            baos.write(buildShake().encode());
            sendMessage(out, baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }


    private void sendMessage(OutputStream out, byte[] data) {
        int len = data.length;
        byte[] lens = Utils.intToBytes(len);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 4);
        try {
            baos.write(lens);
            baos.write(data);
            out.write(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void fetchPieces(OutputStream out, int ut, int pieceNum) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(EXTENDED);
        baos.write(ut);
        try {
            baos.write(buildRequestMsg(pieceNum).encode());
            out.write(baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param bytes
     * @param data
     * @param offset
     * @param pieceNum
     */
    private int readPiece(byte[][] bytes, byte[] data, int offset, int pieceNum) {
        DictionaryNode msg = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data, 2, data.length - 2);

        Map<String, Node> args = new HashMap<>();
        try {
            Decoder decoder = new Decoder(new ByteArrayInputStream(baos.toByteArray()));
            decoder.setHandler(new EventHandler() {
                @Override
                public Node handleDictionaryNode(String key, Node value) {
                    args.put(key, value);
                    return value;
                }
            });
            decoder.decode();
        } catch (DecodingException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        DictionaryNode node = new DictionaryNode(args);
        int msgType = Integer.parseInt(args.get(MSG_TYPE).toString());
        if (msgType != MSG_DATA) return -1;
        int p = Integer.parseInt(args.get(MSG_PIECE).toString());
        baos.reset();
        int msgLen = node.encode().length;
        baos.write(data, offset + msgLen, data.length - offset - msgLen);
        bytes[p] = baos.toByteArray();
        return p;
    }


    public void fetchMetadata() {
        try (Socket socket = new Socket()) {
            logger.info("init");
            socket.setSoTimeout(DhtConfig.CONN_TIMEOUT);
            socket.connect(new InetSocketAddress(address, port));
            logger.info("over");
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            sendHandleShake(out);
            logger.info("handshake over");
            //握手失败
            if (!onHandShake(in)) return;
            logger.info("handshake success");
            //握手成功,继续发送支持扩展协议的消息
            sendExtHandShake(out);
            byte[][] bytes = null;
            int totalPiece = 0;
            int curPiece = 0;
            int ut = 0;
            while (true) {
                byte[] head = IO.readKBytes(in, 6);
                int length = Utils.bytesToInt(head, 0, 4);
                logger.info("length:" + length);
                if (length == 0) return;
                int msgId = head[4];
                int extendId = head[5];
                logger.info("type:" + msgId);
                logger.info("extend:" + extendId);
                byte[] data = IO.readAllBytes(in);
                logger.info("data length:" + data.length);
                logger.info("data:" + new String(data));
                if (data.length != length) logger.error("msg error");
                ByteArrayOutputStream pieces = new ByteArrayOutputStream();
                if (msgId == EXTENDED) {
                    if (extendId == 0) {
                        logger.info("收到最后的握手信息");
                        if (bytes != null) return;
                        logger.info("解析握手信息");
                        DictionaryNode meta = (DictionaryNode) Decoder.decode(data, 0, length).get(0);
                        DictionaryNode m = (DictionaryNode) meta.getNode("m");
                        ut = Integer.parseInt(m.getNode(MSG_UT_METADATA).toString());
                        int size = Integer.parseInt(meta.getNode(MSG_METADATA_SIZE).toString());
                        totalPiece = size / BLOCK_SIZE;
                        if (size % BLOCK_SIZE != 0) totalPiece++;
                        logger.info("piece num :" + totalPiece);
                        bytes = new byte[totalPiece][];
                        fetchPieces(out, curPiece, ut);
                        curPiece++;
                    } else {
                        if (bytes == null) continue;
                        logger.info("已经握手完毕，准备接收数据");
                        int piece = readPiece(bytes, data, curPiece, totalPiece);
                        curPiece++;
                        if (piece > 0) {
                            if (piece == (totalPiece - 1)) {
                                for (byte[] b : bytes) {
                                    pieces.write(b);
                                    logger.info("ut::::" + pieces.toString());
                                }
                                return;
                            } else {
                                fetchPieces(out, piece + 1, ut);
                            }
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            DhtApp.NODE.addBlackItem(address, port);
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }


}
