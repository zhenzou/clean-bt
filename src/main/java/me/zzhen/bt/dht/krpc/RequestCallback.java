package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictionaryNode;
import me.zzhen.bt.dht.base.NodeInfo;

import java.net.InetAddress;

/**
 * Project:CleanBT
 * Create Time: 16-12-24.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface RequestCallback {

    /**
     * 执行请求
     *
     * @param request 发出请求的数据
     * @param target  请求的目标节点
     * @param method  请求的方法,Krpc
     */
    void request(DictionaryNode request, NodeInfo target, String method);

    void response(InetAddress address, int port, DictionaryNode data);

    /**
     * announce_peer回调
     *  @param address
     * @param port
     * @param data
     */
    void onAnnouncePeer(InetAddress address, int port, String data);

    void onPing(InetAddress address, int port, DictionaryNode data);

    void onGetPeer(InetAddress address, int port, DictionaryNode data);

    void onFindNode(InetAddress address, int port, DictionaryNode data);

    boolean requested(NodeInfo node);
}
