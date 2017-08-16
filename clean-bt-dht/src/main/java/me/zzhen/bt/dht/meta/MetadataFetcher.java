package me.zzhen.bt.dht.meta;

import me.zzhen.bt.bencode.Bencode;
import me.zzhen.bt.bencode.DecoderException;
import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.IntNode;
import me.zzhen.bt.dht.NodeInfo;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

import static me.zzhen.bt.dht.MetadataWorker.BLOCK_SIZE;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class MetadataFetcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MetadataFetcher.class);

    private static final int MSG_REQUEST = 0;
    private static final int MSG_DATA = 1;
    private static final int MSG_REJECT = 2;

    private static final String MSG_TYPE = "msg_type";
    private static final String MSG_PIECE = "piece";
    private static final String MSG_TOTAL_SIZE = "total_size";
    private static final String MSG_UT_METADATA = "ut_metadata";
    private static final String MSG_METADATA_SIZE = "metadata_size";

    /**
     * BitTorrent 协议标识符，BEP03 规定
     */
    private static final String PSTR = "BitTorrent protocol";
    /**
     * BEP 保留字段，非零字节表示扩展协议
     */
    private static final byte[] RESERVED = {0, 0, 0, 0, 0, 16, 0, 1};
    private static final int EXTENDED = 20;

    private Map<SocketChannel, Instant> channels = new HashMap<>();

    private final BlockingQueue<MetadataHolder> queue;

    private Channel<MetadataHolder> channel = Channel.simpleChannel(1024);

    private Selector selector;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 最多同时监听的的Channel树
     */
    private final int capacity;

    private final NodeInfo self;

    /**
     * 当前监听的channel数量
     */
    private int size;

    /**
     * total 2* capacity
     *
     * @param capacity 容量，以及等待的队列长度
     * @param self
     */
    public MetadataFetcher(int capacity, NodeInfo self) {
        this.capacity = capacity;
        queue = new LinkedBlockingDeque<>(capacity);
        channel = Channel.simpleChannel(capacity);
        this.self = self;
    }


    /**
     * 构建请求相应piece的消息
     *
     * @param piece piece数
     * @return
     */
    private DictNode makeRequest(int piece) {
        DictNode msg = new DictNode();
        msg.addNode(MSG_TYPE, new IntNode(MSG_REQUEST));
        msg.addNode(MSG_PIECE, new IntNode(piece));
        return msg;
    }

    /**
     * 构建握手消息
     *
     * @return
     */
    private DictNode makeShake() {
        DictNode msg = new DictNode();
        DictNode head = new DictNode();
        head.addNode(MSG_UT_METADATA, new IntNode(1));
        msg.addNode("m", head);
        return msg;
    }


    /**
     * 向发送announce_peer的节点发出握手消息，准备接收Metadata
     */
    private void sendHandShake(SocketChannel channel, byte[] hash) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(49 + 19);
        buffer.put((byte) PSTR.length());
        buffer.put(PSTR.getBytes());
        buffer.put(RESERVED);
        buffer.put(hash);
        buffer.put(self.getId().getValue());
        buffer.flip();
        try {
            while (buffer.hasRemaining()) channel.write(buffer);
        } catch (IOException ignored) {
        }
    }

    /**
     * 判断是否握手成功
     *
     * @return
     */
    private static boolean checkHandShake(byte[] bytes) {
        if (bytes.length < 68) return false;
        if ((bytes[25] & 0x10) == 0) return false;
        return true;
    }

    /**
     * @param channel 发送的socket的输出流
     * @param piece   当前的piece数
     * @param ut      接收到的Ut_metadata字段值，需要发给其他节点
     */
    private void requestPiece(SocketChannel channel, int piece, int ut) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(EXTENDED);
        baos.write(ut);
        try {
            baos.write(makeRequest(piece).encode());
            sendMessage(channel, baos.toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 发送支持扩展协议的握手消息
     */
    private void sendExtHandShake(SocketChannel channel) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{EXTENDED, 0});
            baos.write(makeShake().encode());
            sendMessage(channel, baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 使用out发送BT扩展协议的消息，主要是添加length头部
     *
     * @param data
     */
    private void sendMessage(SocketChannel channel, byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 4);
        int len = data.length;
        byte[] lens = Utils.int2Bytes(len);
        try {
            baos.write(lens);
            baos.write(data);
            ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
            while (wrap.hasRemaining()) channel.write(wrap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO Callback
     */
    public void output() {

    }

    public void commit(String address, int port, byte[] hash) {
        commit(new MetadataHolder(address, port, hash));
    }


    /**
     * 暂时不去重 not thread safe
     *
     * @param holder
     */
    public void commit(MetadataHolder holder) {
        logger.debug("当前Channel数:" + channels.size() + "size:" + queue.size());
        try {
            queue.put(holder);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        logger.debug("size:" + size + "queue:" + queue.size());
    }


    public void done() {
        try {
            lock.lock();
            size--;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从队列中的任务都取出来，注册到
     */
    private void register() {
        if (queue.isEmpty()) return;
        while (size < capacity && !queue.isEmpty()) {
            try {
                MetadataHolder holder = queue.take();
                SocketChannel channel = SocketChannel.open();
                channel.configureBlocking(false);
                channels.put(channel, Instant.now());
                channel.connect(holder.address);
                channel.register(selector, SelectionKey.OP_CONNECT, holder);
                size++;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 刷新channel的等待时间
     *
     * @param channel
     */
    public void fresh(SocketChannel channel) {
        channels.put(channel, Instant.now());
    }

    public void clearChannels() {
        Instant now = Instant.now();
        List<SocketChannel> remove = new ArrayList<>();
        channels.forEach((channel, time) -> {
            if (time.plusSeconds(15).isBefore(now)) {
                try {
                    channel.close();
                    remove.add(channel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            selector.selectNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("remove:" + remove.size());
        for (SocketChannel channel : remove) {
            channels.remove(channel);
        }
    }

    private void closeChannel(SelectionKey key, SocketChannel channel) {
        try {
            key.cancel();
            channel.close();
            channels.remove(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] readChannel(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(16384 + 1024);
        int read = -1;
        int len = 0;
        while ((read = channel.read(buffer)) > 0) {
            len += read;
        }
        buffer.flip();
        byte[] data = new byte[len];
        buffer.get(data);
        return data;
    }

    @Override
    public void run() {

        ByteBuffer shaked = ByteBuffer.allocate(68);
        try {
            selector = Selector.open();
            while (true) {
                logger.debug("selected");
                int i = selector.select(5000);
                register();
                if (i < 1) {
                    continue;
                }
                logger.debug("selected:{}", i);
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                SocketChannel channel = null;
                SelectionKey key = null;
                while (iterator.hasNext()) {
                    try {
                        key = iterator.next();
                        iterator.remove();
                        MetadataHolder holder = (MetadataHolder) key.attachment();
                        channel = (SocketChannel) key.channel();
                        fresh(channel);
                        if (key.isWritable()) {
                            if (holder.state == MetadataState.START) {
                                logger.debug("连接成功");
                                sendHandShake(channel, holder.hash);
                                holder.state = MetadataState.TO_SHAKEN;
                                logger.debug("发出握手消息");
                                channel.register(selector, SelectionKey.OP_READ, holder);
                            } else if (holder.state == MetadataState.SHAKEN) {
                                sendExtHandShake(channel);
                                holder.state = MetadataState.TO_EXT_SHAKEN;
                                channel.register(selector, SelectionKey.OP_READ, holder);
                            } else if (holder.state == MetadataState.DATA) {
                                if (holder.totalPiece != 0) {
                                    if (holder.currentPiece < holder.totalPiece) {
                                        requestPiece(channel, holder.currentPiece, holder.ut);
                                        logger.debug("请求第" + holder.currentPiece + "块数据");
                                        channel.register(selector, SelectionKey.OP_READ, holder);
                                    }
                                } else {
                                    logger.debug("piece还没有初始化");
                                }
                            }
                        } else if (key.isReadable()) {
                            ByteBuffer data = ByteBuffer.allocate(20 * 1024);
                            if (holder.state == MetadataState.TO_SHAKEN) {
                                byte[] array = readChannel(channel);
                                logger.debug("read:count:" + array.length + "shaked:" + shaked);
                                logger.debug("read:::" + new String(array));
                                if (!checkHandShake(array)) {
                                    logger.warn("握手失败");
                                    closeChannel(key, channel);
                                    continue;
                                }
                                holder.state = MetadataState.SHAKEN;
                                logger.debug("握手成功，准备发出扩展握手信息");
                                channel.register(selector, SelectionKey.OP_WRITE, holder);
                            } else if (holder.state == MetadataState.TO_EXT_SHAKEN) {
                                byte[] bytes = readChannel(channel);
                                logger.debug("size:" + bytes.length);
                                if (bytes.length == 0) closeChannel(key, channel);
                                int length = Utils.bytes2Int(bytes, 0, 4);
                                if (length == 0) closeChannel(key, channel);
                                int msgId = bytes[4];
                                int extendId = bytes[5];
                                logger.debug("type:" + msgId);
                                logger.debug("extend:" + extendId);
                                if (msgId == EXTENDED) {
                                    if (extendId == 0) {
                                        logger.debug("收到最后的握手信息:{}", new String(bytes));
                                        DictNode meta = Bencode.decodeDict(new ByteArrayInputStream(bytes, 6, length - 6));
                                        DictNode m = (DictNode) meta.getNode("m");
                                        int ut = Integer.parseInt(m.getNode(MSG_UT_METADATA).toString());
                                        holder.ut = ut;
                                        int size = Integer.parseInt(meta.getNode(MSG_METADATA_SIZE).toString());
                                        holder.totalPiece = size / BLOCK_SIZE;
                                        if (size % BLOCK_SIZE != 0) holder.totalPiece++;
                                        holder.data = new ByteArrayOutputStream(size);
                                        holder.state = MetadataState.DATA;
                                    }
                                }
                            } else if (holder.state == MetadataState.DATA) {
                                byte[] array = readChannel(channel);
                                if (holder.currentPiece < holder.totalPiece) {
                                    logger.debug("应该收到第:" + holder.currentPiece + " 块信息");
//                                    logger.info("data:" + new String(array));
                                    DictNode node = null;
                                    try {
                                        node = Bencode.decodeDict(new ByteArrayInputStream(array, 6, array.length));
                                        int piece = Integer.parseInt(node.getNode(MSG_PIECE).toString());
                                        logger.debug("node:" + node);
                                        int msgLen = node.encode().length;
                                        if (piece == holder.currentPiece) {
                                            logger.info("piece match");
                                            holder.data.write(array, msgLen + 6, array.length - msgLen);
                                            holder.currentPiece++;
                                        }
                                        if (holder.currentPiece == holder.totalPiece) {
                                            byte[] bytes = holder.data.toByteArray();
                                            DictNode meta = Bencode.decodeDict(new ByteArrayInputStream(bytes));
                                            logger.debug("infohash:" + Utils.toHex(holder.hash) + ",name" + meta.getNode("name") + ",files" + meta.getNode("files"));
                                            closeChannel(key, channel);
                                        }
                                        channel.register(selector, SelectionKey.OP_WRITE, holder);
                                    } catch (DecoderException e) {
                                        logger.error(e.getMessage());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            data.clear();
                        }
                    } catch (Exception e) {
                        if (channel != null) {
                            closeChannel(key, channel);
                        }
                        logger.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (selector != null)
                    selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
