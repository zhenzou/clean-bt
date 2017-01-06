package me.zzhen.bt.dht.base;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.bencode.IntNode;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private static final String PSTR = "BitTorrent protocol";
    /**
     * BEP 保留字段，非零字节表示扩展协议
     */
    private static final byte[] RESERVED = {0, 0, 0, 0, 0, 16, 0, 1};

    public static final byte PEER_MSG_CHOKE = 0;
    public static final byte PEER_MSG_UNCHOKE = 1;
    public static final byte PEER_MSG_INTERESTED = 2;
    public static final byte PEER_MSG_NOT_INTERESTED = 3;
    public static final byte PEER_MSG_HAVE = 4;
    public static final byte PEER_MSG_BITFIELD = 5;
    public static final byte PEER_MSG_REQUEST = 6;
    public static final byte PEER_MSG_PIECE = 7;
    public static final byte PEER_MSG_CANCEL = 8;

    private static final int EXTENDED = 20;

    /**
     * 请求的资源的info_hash
     */
    private byte[] hash;

    /**
     * 请求的来源地址
     */
    private InetSocketAddress address;


    public static final int BLOCK_SIZE = 16 * 1024;


    public MetadataWorker(InetAddress address, int port, byte[] hash) {
        if (hash.length != 20) return;
        this.address = new InetSocketAddress(address, port);
        this.hash = hash;
    }

    @Override
    public void run() {
        if (!DhtApp.NODE.isBlackItem(address)) fetchMetadata();
    }


    /**
     * 构建握手消息
     *
     * @return
     */
    private DictionaryNode makeShake() {
        DictionaryNode msg = new DictionaryNode();
        DictionaryNode head = new DictionaryNode();
        head.addNode(MSG_UT_METADATA, new IntNode(1));
        msg.addNode("m", head);
        return msg;
    }

    /**
     * 构建请求相应piece的消息
     *
     * @param piece piece数
     * @return
     */
    private DictionaryNode makeRequest(int piece) {
        DictionaryNode msg = new DictionaryNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REQUEST));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        return msg;
    }

    /**
     * 向发送announce_peer的节点发出握手消息，准备接收Metadata
     *
     * @param out
     */
    private void sendHandShake(OutputStream out) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(49 + 19);
        try {
            baos.write(PSTR.length());
            baos.write(PSTR.getBytes());
            baos.write(RESERVED);
            baos.write(hash);
            baos.write(NodeKey.genRandomKey().getValue());
            out.write(baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 判断是否握手成功
     *
     * @param in
     * @return
     */
    private boolean onHandShake(InputStream in) {
        byte[] bytes = IO.readKBytes(in, 68);
        if (bytes.length < 68) return false;
        if ((bytes[25] & 0x10) == 0) return false;
        return true;
    }


    /**
     * 发送支持扩展协议的握手消息
     *
     * @param out
     */
    private void sendExtHandShake(OutputStream out) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{EXTENDED, 0});
            baos.write(makeShake().encode());
            sendMessage(out, baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 使用out发送BT扩展协议的消息，主要是添加length头部
     *
     * @param out
     * @param data
     */
    private void sendMessage(OutputStream out, byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 4);
        int len = data.length;
        byte[] lens = Utils.intToBytes(len);
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
     * @param out   发送的socket的输出流
     * @param piece 当前的piece数
     * @param ut    接收到的Ut_metadata字段值，需要发给其他节点
     */
    private void requestPiece(OutputStream out, int piece, int ut) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(EXTENDED);
        baos.write(ut);
        try {
            baos.write(makeRequest(piece).encode());
            sendMessage(out, baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从接受到的Data中读取相应的piece信息
     *
     * @param pieces 保存的metadata
     * @param data   接收到的数据
     * @return 接收到的第几块piece
     */
    private int readPiece(ByteArrayOutputStream pieces, byte[] data) {
        DictionaryNode node = null;
        try {
            node = DictionaryNode.decode(new ByteArrayInputStream(data));
        } catch (DecodingException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int piece = Integer.parseInt(node.getNode(MSG_PIECE).toString());
        logger.info("node:" + node);
        int msgLen = node.encode().length;
        pieces.write(data, msgLen, data.length - msgLen);
        return piece;
    }


    /**
     * 获取Torrent的Metadata,BEP09
     */
    private void fetchMetadata() {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(DhtConfig.CONN_TIMEOUT);
            socket.connect(address);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            sendHandShake(out);
            if (!onHandShake(in)) return;
            logger.info("handshake success");
            //握手成功,继续发送支持扩展协议的消息
            sendExtHandShake(out);
            int totalPiece = 0;
            int curPiece = 0;
            int ut = 0;
            ByteArrayOutputStream pieces = null;
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
//                可能有bit field，所以可能会超过length,length也包括了msgId，extendId两个字节，所以要减2
                if (data.length < length - 2) return;
                if (msgId == EXTENDED) {
                    if (extendId == 0) {
                        logger.info("收到最后的握手信息");
                        DictionaryNode meta = DictionaryNode.decode(new ByteArrayInputStream(data, 0, length - 3));
                        DictionaryNode m = (DictionaryNode) meta.getNode("m");
                        ut = Integer.parseInt(m.getNode(MSG_UT_METADATA).toString());
                        int size = Integer.parseInt(meta.getNode(MSG_METADATA_SIZE).toString());
                        totalPiece = size / BLOCK_SIZE;
                        if (size % BLOCK_SIZE != 0) totalPiece++;
                        pieces = new ByteArrayOutputStream(size);
                        requestPiece(out, curPiece, ut);
                    } else {
                        if (pieces == null) continue;
                        logger.info("已经握手完毕，正在接收数据");
                        int piece = readPiece(pieces, data);
                        if (piece < totalPiece) {
                            if (piece != curPiece) {
                                requestPiece(out, curPiece, ut);//收到的错误的piece，再次请求相同的piece
                                continue;
                            }
                            curPiece++;
                            requestPiece(out, curPiece, ut);//请求下一个piece
                        } else {
                            DictionaryNode decode = DictionaryNode.decode(new ByteArrayInputStream(pieces.toByteArray()));
                            logger.info("infohash=" + Utils.toHex(hash) + ",name=" + decode.getNode("name") + ",files=" + decode.getNode("files"));
                            return;
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
