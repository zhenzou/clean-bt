package me.zzhen.bt.dht.base;

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
import java.util.HashMap;
import java.util.Map;

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
    private InetSocketAddress address;
    /**
     * 资源下载的端口
     */
    private int port;


    private static String peerId = "-UT-";

    public static final int BLOCK_SIZE = 16 * 1024;


    public MetadataWorker(InetAddress address, int port, String hash) {
        this.address = new InetSocketAddress(address, port);
        this.port = port;
        this.hash = hash;
    }

    @Override
    public void run() {
        if (!DhtApp.NODE.isBlackItem(address)) fetchMetadata();
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
        head.addNode(MSG_UT_METADATA, new IntNode(1));
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
            logger.info("send:" + new String(baos.toByteArray()));
            out.write(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param out
     * @param piece 当前的piece数
     * @param ut
     */
    private void fetchPieces(OutputStream out, int piece, int ut) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(EXTENDED);
        baos.write(ut);
        try {
            baos.write(buildRequestMsg(piece).encode());
            sendMessage(out, baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param data
     * @param curPiece
     * @param pieceNum
     */
    private int readPiece(ByteArrayOutputStream pieces, byte[] data, int curPiece, int pieceNum) {
        DictionaryNode node = null;
        try {
            node = DictionaryNode.decode(new ByteArrayInputStream(data));
        } catch (DecodingException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int msgType = Integer.parseInt(node.getNode(MSG_TYPE).toString());
        int piece = Integer.parseInt(node.getNode(MSG_PIECE).toString());
        int total = Integer.parseInt(node.getNode(MSG_TOTAL_SIZE).toString());
        logger.info("node:" + node);
        logger.info("mstType:" + msgType);
        logger.info("total:size:" + total);
        logger.info("piece:" + piece);
        int msgLen = node.encode().length;
        pieces.write(data, msgLen, data.length - msgLen);
        return piece;//测试
    }


    /**
     *
     */
    public void fetchMetadata() {
        try (Socket socket = new Socket()) {
            logger.info("init");
            socket.setSoTimeout(DhtConfig.CONN_TIMEOUT);
            socket.connect(address);
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
            ByteArrayOutputStream pieces = null;
            while (true) {
                byte[] head = IO.readKBytes(in, 6);
                int length = Utils.bytesToInt(head, 0, 4);
                logger.info("length:" + length);
                logger.info("head:" + Utils.bytesToBin(head));
                if (length == 0) return;
                int msgId = head[4];
                int extendId = head[5];
                logger.info("type:" + msgId);
                logger.info("extend:" + extendId);
                byte[] data = IO.readAllBytes(in);
                logger.info("data length:" + data.length);
                logger.info("data:" + new String(data));
                //可能有bit field，所以可能会超过length
//                if (data.length < length-2) return;
                if (msgId == EXTENDED) {
                    if (extendId == 0) {
                        logger.info("收到最后的握手信息");
                        if (pieces != null) return;
                        logger.info("解析握手信息");

                        DictionaryNode meta = DictionaryNode.decode(new ByteArrayInputStream(data, 0, length - 3));
                        DictionaryNode m = (DictionaryNode) meta.getNode("m");
                        ut = Integer.parseInt(m.getNode(MSG_UT_METADATA).toString());
                        int size = Integer.parseInt(meta.getNode(MSG_METADATA_SIZE).toString());
                        logger.info("total size:" + size);
                        logger.info("ut_metadata:" + ut);
                        totalPiece = size / BLOCK_SIZE;
                        if (size % BLOCK_SIZE != 0) totalPiece++;
                        logger.info("piece num :" + totalPiece);
//                        bytes = new byte[totalPiece][];
                        pieces = new ByteArrayOutputStream(size);
                        fetchPieces(out, curPiece, ut);
                    } else {
                        if (pieces == null) continue;
                        logger.info("已经握手完毕，准备接收数据");
                        int piece = readPiece(pieces, data, curPiece, totalPiece);
                        logger.info("pieces:" + String.valueOf(pieces));
                        if (piece < totalPiece) {
                            logger.info("piece 小于 total piece ");
                            if (piece != curPiece) {
                                logger.info("piece 不等于 total piece ");
                                continue;
                            }
                            curPiece++;
                            fetchPieces(out, curPiece, ut);
                        } else {
                            logger.info("ut::::" + String.valueOf(pieces));
                            DictionaryNode decode = DictionaryNode.decode(new ByteArrayInputStream(pieces.toByteArray()));
                            logger.info("UT_METADATA:" + decode.toString());
                            return;
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            DhtApp.NODE.addBlackItem(address);
            logger.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private class MetadataErrorHandler implements ErrorHandler {

        @Override
        public int skip(int pos, char cur, InputStream input) {
            int count = 0;
            int c = -1;
            try {
                while ((c = input.read()) != -1 && c != IntNode.INT_END) {
                    count++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        public boolean toNext() {
            return true;
        }
    }

    public static void main(String[] args) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName("93.150.96.225");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int port = 54513;
        String peer = "6CB5AD8F57E59F6B8B15A0C6BEE5DC714B3D27D3";
        new MetadataWorker(address, port, peer).run();
    }
}
