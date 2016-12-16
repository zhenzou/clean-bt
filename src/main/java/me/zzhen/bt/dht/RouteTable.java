package me.zzhen.bt.dht;

import me.zzhen.bt.base.Tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 * 首先简单的实现,不考虑性能
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTable {

    /**
     *
     */
    private class TreeNode {
        BitSet bit = new BitSet(1);
        TreeNode left;
        TreeNode right;
    }

    public static RouteTable init() {
        loadData();
        return new RouteTable();
    }

    private List<Bucket> buckets = new ArrayList<>();

    private class Bucket {

        Instant localChange;

        int low;
        int hi;

        public Bucket(int low, int hi) {
            this.low = low;
            this.hi = hi;
        }

        private Map<NodeKey, NodeInfo> infos = new HashMap<>();

        private List<NodeInfo> nodeInfos = new ArrayList<>(8);

        private int size = 0;
        private NodeInfo replacement;

        /**
         * TODO 真正的实现，现在只是单纯的返回null，即 bucket满了以后不再添加
         *
         * @param info
         * @return
         */
        public Tuple<Bucket, Bucket> spit(NodeInfo info) {
            int mid = (hi - low) / 2;
//            return new Tuple<>(new Bucket(low, mid), new Bucket(mid + 1, hi));
            return null;
        }


        /**
         *
         *
         * @param info
         */
        Tuple<Bucket, Bucket> addNode(NodeInfo info) {
            if (size == 8) {
                return spit(info);
            }
            size++;
            infos.put(info.getKey(), info);
            return null;
        }

        boolean contains(NodeKey key) {
            return infos.containsKey(key);
        }

        NodeInfo getNode(NodeKey key) {
            return infos.get(key);
        }

        /**
         * TODO 优化
         *
         * @param key
         * @return
         */
        boolean checkRange(NodeKey key) {
            return checkLow(key) && checkHigh(key);
        }

        private boolean checkHigh(NodeKey key) {
            int len = key.getValue().length;
            for (int i = hi; i < len; i++) {
                if (key.prefix(i)) return false;
            }
            return true;
        }

        private boolean checkLow(NodeKey key) {
            for (int i = low; i < hi; i++) {
                if (key.prefix(i)) return true;
            }
            return false;
        }

        public void refresh() {
            localChange = Instant.now();
        }

        /**
         * 判断 当亲的bucket在15分钟内有没有节点有更新
         * @return
         */
        public boolean isFresh() {
            return Instant.now().plus(15, ChronoUnit.MINUTES).isBefore(localChange);
        }
    }

    /**
     * TODO 加载以前保存的节点数据
     */
    private static void loadData() {

    }

    private Set<NodeInfo> nodeInfos = new HashSet<>();

    public synchronized void addNode(NodeInfo node) {
        for (Bucket bucket : buckets) {
            if (bucket.checkRange(node.key)) bucket.addNode(node);
        }
        nodeInfos.add(node);
    }


    public List<NodeInfo> closest8Nodes(NodeInfo node) {
        return closestKNodes(node.getKey(), 8);
    }

    public List<NodeInfo> closest8Nodes(NodeKey key) {
        return closestKNodes(key, 8);
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

        for (Bucket bucket : buckets) {
            if (bucket.checkRange(key)) {
                nodes.addAll(bucket.infos.values());
            }
        }
        int i = 0;
        for (NodeInfo node : nodes) {
            if (i == k) break;
            nodes.add(node);
        }
        return nodes;
    }

    public NodeInfo getNode(NodeKey node) {
        for (NodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getKey().equals(node)) return nodeInfo;
        }
        return null;
    }
}
