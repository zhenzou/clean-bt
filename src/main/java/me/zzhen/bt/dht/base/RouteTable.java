package me.zzhen.bt.dht.base;

import me.zzhen.bt.common.Tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description： 每次重启都会重新开始
 * TODO 完整的测试,修复问题,优化性能
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTable {

    private int size;
    private TreeNode root = new TreeNode((byte) 0);//前缀树，根节点，不使用
    private final NodeInfo self;

    public RouteTable(NodeInfo self) {
        this.self = self;
        root = new TreeNode((byte) 0);
        root.value = new Bucket(0, 160);
    }

    public int size() {
        return size;
    }

    public synchronized void addNode(NodeInfo node) {
        NodeKey key = node.getKey();
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            int prefix = key.prefix(index);
            if (prefix == 0) {
                item = item.right;
            } else {
                item = item.left;
            }
            index++;
        }
        if (addNodeToBucket(node, item)) {
            size++;
        }
    }

    /**
     * 添加Node信息到响应的Bucket中
     * 如果Bucket满了，则分裂
     * item的Bucket不可能是null
     *
     * @param node
     * @param item
     */
    private boolean addNodeToBucket(NodeInfo node, TreeNode item) {
        Bucket bucket = item.value;
        if (bucket.contains(node)) return false;
//        System.out.println(bucket.checkRange(key));
//        System.out.println(bucket.left + ":" + bucket.right);
//        System.out.println("self:" + Utils.bytesToBin(self.getKey().getValue()));
//        System.out.println("node:" + Utils.bytesToBin(key.getValue()));
        if (bucket.size() == 8) {
            if (!bucket.checkRange(self.getKey())) return false;
            NodeKey key = node.getKey();
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
        return true;
    }

    //Test
    public void preOrderTra(RouteTable.TreeNode root) {
        if (root == null) return;
        System.out.println(root.key);
        if (root.value != null) System.out.println("size:" + root.value.size());
        preOrderTra(root.left);
        preOrderTra(root.right);
    }

    //Test
    public int size(RouteTable.TreeNode root) {
        int size = 0;
        if (root == null) return 0;
        if (root.value != null) {
            size += root.value.size();
        }
        size += size(root.left);
        size += size(root.right);
        return size;
    }


    /**
     * Test
     *
     * @return
     */
    public TreeNode getRoot() {
        return root;
    }

    public List<NodeInfo> closest8Nodes(NodeKey key) {
        List<NodeInfo> nodes = closestNodes(key);
        return nodes.size() == 8 ? nodes.subList(0, 7) : nodes;
    }

    public List<NodeInfo> closestNodes(NodeInfo node, int k) {
        return closestNodes(node.getKey());
    }

    /**
     * 在添加节点的过程中已经保证分裂后的节点的Bucket是空的
     *
     * @param key
     * @return 离key最近的K个节点
     */
    public List<NodeInfo> closestNodes(NodeKey key) {
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            int prefix = key.prefix(index);
            if (prefix == 0) {
                item = item.right;
            } else {
                item = item.left;
            }
            index++;
        }
        return item.value.nodes;
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
    }

    private class Bucket {

        Instant localChange = Instant.now();

        int left;//在[0,left)出现1则不在范围内
        int right;//[left,right)之间，至少有一个为1的位置

        /**
         * @param left  高字节
         * @param right 低字节
         */
        public Bucket(int left, int right) {
            this.left = left;
            this.right = right;
        }

        private List<NodeInfo> nodes = new ArrayList<>(8);

        private NodeInfo replacement;//TODO optimize

        /**
         * package-private
         *
         * @return
         */
        Tuple<Bucket, Bucket> spit() {
            int mid = left + 1;
            Bucket rightBucket = new Bucket(mid, right);
            for (NodeInfo nodeInfo : nodes) {
                if (rightBucket.checkRange(nodeInfo.getKey())) rightBucket.addNode(nodeInfo);
            }
            Bucket leftBucket = new Bucket(left, mid);
            for (NodeInfo nodeInfo : nodes) {
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
            for (int i = 0; i < left && i < len; i++) {
                if (key.prefix(i) == 1) return false;
            }
            return true;
        }

        private boolean checkRight(NodeKey key) {
            int len = key.getValue().length;
            for (int i = left; i < right && i < len; i++) {
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
            if (size() == 8) {
                return;
            }
            nodes.add(info);
        }

        public int size() {
            return nodes.size();
        }

        boolean contains(NodeInfo info) {
            return nodes.contains(info);
        }

        NodeInfo getNode(NodeKey key) {
            for (NodeInfo node : nodes) {
                if (node.getKey().equals(key)) return node;
            }
            return null;
        }


        /**
         * 处理Bucket定时刷新问题
         */
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
