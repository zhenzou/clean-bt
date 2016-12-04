package me.zzhen.bt.dht;

import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class ResourceInfo {
    public List<NodeInfo> nodeInfos = new ArrayList<>();


    public ResourceInfo() {

    }

    public void addNodeIndo(NodeInfo info) {
        nodeInfos.add(info);
    }

    public NodeInfo getNodeInfo(int i) {
        return nodeInfos.get(i);
    }

    public List<NodeInfo> getNodeList() {
        return nodeInfos;
    }
}

