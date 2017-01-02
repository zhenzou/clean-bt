package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;

import java.net.InetAddress;

/**
 * Project:CleanBT
 * Create Time: 16-12-24.
 * Description:
 * 处理请求的回调接口
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface RequestCallback {

    /**
     * announce_peer回调
     *
     * @param address
     * @param port
     * @param data
     */
    void onAnnouncePeer(InetAddress address, int port, String data);

    void onPing(InetAddress address, int port, DictionaryNode data);

    void onGetPeer(InetAddress address, int port, NodeKey target);

    void onFindNode(InetAddress address, int port, NodeKey target);
}
