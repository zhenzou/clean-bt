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
    public List<NodeInfo> mNodeInfos = new ArrayList<>();


    public ResourceInfo() {

    }

    public void addNodeIndo(NodeInfo info) {
        mNodeInfos.add(info);
    }

    public NodeInfo getNodeInfo(int i) {
        return mNodeInfos.get(i);
    }

    public List<NodeInfo> getNodeList() {
        return mNodeInfos;
    }
}

