package me.zzhen.bt.dht;

import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 * 首先简单的实现,不考虑性能
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTable {

    private List<NodeInfo> nodeInfos = new ArrayList<>();

    public void addNode(NodeInfo node) {
        nodeInfos.add(node);
    }

    public void addNode(List<NodeInfo> nodes) {
        nodeInfos.addAll(nodes);
    }

    public NodeInfo getNode(int i) {
        return nodeInfos.get(i);
    }

    public List<NodeInfo> closest16Nodes(NodeInfo node) {
        return closestKNodes(node.getKey(), 16);
    }

    public List<NodeInfo> closest16Nodes(NodeKey key) {
        return closestKNodes(key, 16);
    }

    public List<NodeInfo> closestKNodes(NodeInfo node, int k) {
        return closestKNodes(node.getKey(), k);
    }

    /**
     * 暂时
     *
     * @param key
     * @param k
     * @return
     */
    public List<NodeInfo> closestKNodes(NodeKey key, int k) {
        List<NodeInfo> nodes = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            nodes.add(nodeInfos.get(i));
        }
        return nodes;
    }
}
