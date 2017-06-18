package me.zzhen.bt.dht.krpc;


import me.zzhen.bt.bencode.DictNode;
import me.zzhen.bt.bencode.ListNode;
import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.bencode.StringNode;
import me.zzhen.bt.dht.*;
import me.zzhen.bt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.zzhen.bt.dht.krpc.Krpc.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-20.
 * Description:
 * 处理本节点请求的响应
 *
 * @author zzhen zzzhen1994@gmail.com
 */

public interface ResponseHandler {

    void onFindNodeResp(NodeInfo src, NodeId target, DictNode resp);

    void onAnnouncePeerResp(NodeInfo src,  DictNode resp);

    void onPingResp(NodeInfo src);

    void onGetPeersResp(NodeInfo src, NodeId target, DictNode resp);
}

