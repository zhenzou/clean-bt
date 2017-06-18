package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.dht.NodeInfo;

/**
 * Project:CleanBT
 * Create Time: 16-12-24.
 * Description:
 * 处理请求的回调接口
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface RequestHandler {

    void onFindNodeReq(NodeInfo src, Node t, Node id);

    void onAnnouncePeerReq(NodeInfo src, int port, Node t, Node infoHash);

    void onPingReq(NodeInfo src, Node t);

    void onGetPeerReq(NodeInfo src, Node t, Node id);

}
