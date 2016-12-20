package me.zzhen.bt.dht.base;

import me.zzhen.bt.common.Tuple;

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


    private List<Bucket> buckets = new ArrayList<>();
    private Set<NodeInfo> nodeInfos = new HashSet<>();

    private int size;

    private TreeNode root = new TreeNode((byte) 0);//前缀树，根节点，不使用

    private final NodeInfo self;

    public RouteTable(NodeInfo self) {
        this.self = self;
    }

    public int size() {
        return size;
    }

    public static RouteTable init() {
        loadData();
//        return new RouteTable(self);
        return null;
    }


    /**
     * TODO 加载以前保存的节点数据
     */
    private static void loadData() {

    }


    public synchronized void addNode(NodeInfo node) {
        NodeKey key = node.getKey();
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            byte prefix = key.prefix(index);
            if (prefix == 0) {
                item = root.right;
            } else {
                item = root.left;
            }
            index++;
        }

        addNodeToBucket(node, item);
        size++;
//        for (int i = 0; i < 160; i++) {
//            byte prefix = key.prefix(i);
//            Bucket bucket = item.value;
//            if (item.value != null) {
//                addNodeToBucket(node, item);
//            }
//            if (prefix == item.key && item.value != null) {
//                if (bucket.size() == 8) {
//                    if (!bucket.checkRange(self.getKey())) return;
//                    Tuple<Bucket, Bucket> spit = bucket.spit();
//                    TreeNode left = new TreeNode((byte) 1);
//                    left.value = spit._1;
//                    TreeNode right = new TreeNode((byte) 0);
//                    right.value = spit._2;
//                    item.left = left;
//                    item.right = right;
//                    item.value = null;
//                    if (left.value.checkRange(key)) left.value.addNode(node);
//                    else right.value.addNode(node);
//                } else {
//                    bucket.addNode(node);
//                }
//            } else {
//                if (prefix == 0) {
//                    item = item.right;
//                }
//            }
//        }
    }

    private void addNodeToBucket(NodeInfo node, TreeNode item) {
        Bucket bucket = item.value;
        NodeKey key = node.getKey();
        if (bucket.size() == 8) {
            if (!bucket.checkRange(self.getKey())) return;
            Tuple<Bucket, Bucket> spit = bucket.spit();
            TreeNode left = new TreeNode((byte) 1);
            left.value = spit._1;
            TreeNode right = new TreeNode((byte) 0);
            right.value = spit._2;
            item.left = left;
            item.right = right;
            item.value = null;
            if (left.value.checkRange(key)) left.value.addNode(node);
            else right.value.addNode(node);
        } else {
            bucket.addNode(node);
        }
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
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            byte prefix = key.prefix(index);
            if (prefix == 0) {
                item = root.right;
            } else {
                item = root.left;
            }
            index++;
        }

        return item.value.nodes;
    }

    public NodeInfo getNode(NodeKey node) {
        for (NodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getKey().equals(node)) return nodeInfo;
        }
        return null;
    }


    /**
     * 前缀树
     * 只有叶子节点的value才不为空
     */
    private class TreeNode {

        public final byte key;

        public TreeNode(byte key) {
            this.key = key;
        }

        Bucket value;
        TreeNode left;//大
        TreeNode right;//小

        public void addNode(NodeInfo node) {
            value.addNode(node);
        }

    }

    private class Bucket {

        Instant localChange;

        int left;
        int right;

        /**
         * @param left  高字节
         * @param right 低字节
         */
        public Bucket(int left, int right) {
            this.left = left;
            this.right = right;
        }

        private Map<NodeKey, NodeInfo> infos = new HashMap<>();

        private List<NodeInfo> nodes = new ArrayList<>(8);

//        private Queue<NodeInfo> nodes = new ArrayDeque<>(8);

        private int size = 0;
        private NodeInfo replacement;//TODO opt

        /**
         * @return
         */
        public Tuple<Bucket, Bucket> spit() {
            int mid = left + 1;
            Bucket rightBucket = new Bucket(mid, right);
            for (NodeInfo nodeInfo : nodeInfos) {
                if (rightBucket.checkRange(nodeInfo.getKey())) rightBucket.addNode(nodeInfo);
            }
            Bucket leftBucket = new Bucket(right, mid);
            for (NodeInfo nodeInfo : nodeInfos) {
                if (leftBucket.checkRange(nodeInfo.getKey())) leftBucket.addNode(nodeInfo);
            }
            return new Tuple<>(leftBucket, rightBucket);
        }

        /**
         * 右子树包含
         *
         * @param key
         * @return
         */
        boolean checkRange(NodeKey key) {
            return checkRight(key) && checkLeft(key);
        }

        private boolean checkLeft(NodeKey key) {
            int len = key.getValue().length;
            for (int i = 0; i <= left && i < len; i++) {
                if (key.prefix(i) == 1) return false;
            }
            return true;
        }

        private boolean checkRight(NodeKey key) {
            for (int i = left + 1; i <= right; i++) {
                if (key.prefix(i) == 1) return true;
            }
            return false;
        }


        /**
         * TODO 分裂以后还是满的情况
         *
         * @param info
         */
        public void addNode(NodeInfo info) {
            if (size == 8) {
                return;
            }
            size++;
            nodes.add(info);
        }

        public int size() {
            return nodes.size();
        }

        boolean contains(NodeKey key) {
            return infos.containsKey(key);
        }

        NodeInfo getNode(NodeKey key) {
            for (NodeInfo node : nodes) {
                if (node.getKey().equals(key)) return node;
            }
            return null;
        }


        public void refresh() {
            localChange = Instant.now();
        }

        /**
         * 判断 当亲的bucket在15分钟内有没有节点有更新
         *
         * @return
         */
        public boolean isFresh() {
            return Instant.now().plus(15, ChronoUnit.MINUTES).isBefore(localChange);
        }
    }
}
