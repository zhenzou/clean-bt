package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.dht.base.NodeKey;

/**
 * Project:CleanBT
 * Create Time: 16-12-24.
 * Description:
 * 处理请求的回调接口
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface RequestCallback {

    void onAnnouncePeer(NodeInfo src, Node t, Node id);

    void onPing(NodeInfo src, Node t);

    void onGetPeer(NodeInfo src, Node t, Node id);

    void onFindNode(NodeInfo src, Node t, NodeKey id);
}
