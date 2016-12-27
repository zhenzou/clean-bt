package me.zzhen.bt.dht.base;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.krpc.Krpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description： 每次重启都会重新开始
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTable {

    private static final Logger logger = LoggerFactory.getLogger(RouteTable.class.getName());
    /**
     * 路由表的节点数
     */
    private int size;

    /**
     * 前缀树，根节点，不使用
     */
    private TreeNode root = new TreeNode((byte) 0);

    /**
     * 本地DHT节点的信息
     */
    private final NodeInfo self;

    /**
     * 便于更新,不然会重复请求相同的节点
     */
    private List<Bucket> buckets = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public RouteTable(NodeInfo self) {
        this.self = self;
        root = new TreeNode((byte) 0);
        Bucket init = new Bucket(0);
        buckets.add(init);
        root.value = init;
    }

    public int size() {
        return size;
    }

    /**
     * 将node添加到对应的Bucket中
     *
     * @param node
     */
    public void addNode(NodeInfo node) {
        NodeKey key = node.getKey();
        if (key == null) return;
        try {
            lock.lock();
            if (size < DhtConfig.ROUTETABLE_SIZE) {
                TreeNode item = findTreeNode(key);
                if (item.value == null) return;
                if (addNodeToBucket(node, item)) {
                    size++;
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public int repeat = 0;

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
        NodeKey key = node.getKey();
        if (bucket.contains(node)) {
            repeat++;
            bucket.refresh(node);
            return false;
        }
        if (bucket.size() == 8) {
            if (!bucket.checkRange(self.getKey())) {
                return false;
            }
            Tuple<Bucket, Bucket> spit = bucket.split();
            if (spit == null) return false;
            TreeNode left = new TreeNode((byte) 1);
            left.value = spit._1;
            TreeNode right = new TreeNode((byte) 0);
            right.value = spit._2;
            item.left = left;
            item.right = right;
            buckets.remove(item.value);
            item.value = null;
            buckets.add(spit._1);
            buckets.add(spit._2);
            if (left.value.checkRange(key)) addNodeToBucket(node, left);
            else addNodeToBucket(node, right);
        } else {
            bucket.addNode(node);
        }
        return true;
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

    /**
     * 在添加节点的过程中已经保证分裂后的节点的Bucket是空的
     *
     * @param key
     * @return 离key最近的K个节点
     */
    public List<NodeInfo> closestNodes(NodeKey key) {
        TreeNode node = findTreeNode(key);
        return node.value.nodes.stream().map(wrapper -> wrapper.node).collect(Collectors.toList());
    }

    /**
     * 保存在刷新过程中访问的节点，将要删除或者调整，再次刷新的时候总是访问相同的节点
     */
    private List<NodeKey> keys = new ArrayList<>(DhtConfig.AUTO_FIND_SIZE);

    /**
     * 刷新路由表的不获取的节点
     *
     * @param krpc
     */
    public void refresh(Krpc krpc) {
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                buckets.stream()
                        .flatMap(bucket -> bucket.nodes.stream())
                        .limit(DhtConfig.AUTO_FIND_SIZE)
                        .forEach(wrapper -> {
                            krpc.findNode(wrapper.node, wrapper.bucket.randomChildKey());
                            keys.add(wrapper.node.getKey());
                        });
//                buckets.forEach(bucket -> bucket.refresh(krpc));
                keys.forEach(this::remove);
                keys.clear();
                lock.unlock();
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        logger.info("repeat:" + repeat + "routes:" + size);
    }

    /**
     * 找到key在当前对应的节点
     *
     * @param key
     * @return
     */

    private TreeNode findTreeNode(NodeKey key) {
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            boolean prefix = key.prefix(index);
            if (prefix) {
                item = item.left;
            } else {
                item = item.right;
            }
            index++;
        }
        return item;
    }

    /**
     * 在整个路由表中删除key对应的DHT节点信息
     *
     * @param key
     */
    public void remove(NodeKey key) {
        TreeNode item = findTreeNode(key);
        Bucket bucket = item.value;
//        if (!bucket.isActive()) {
            buckets.remove(bucket);
            bucket.replace(key);
            buckets.add(item.value);
//        }
    }

    /**
     * 前缀树
     * 只有叶子节点的value才不为空
     */
    private class TreeNode {

        /**
         * 现在没什么用，以后扩展的时候有用
         */
        public final byte key;

        public TreeNode(byte key) {
            this.key = key;
        }

        Bucket value;
        TreeNode left;//大
        TreeNode right;//小
    }


    /**
     * DHT节点包装，主要增加活动时间记录，便于处理
     */
    private class NodeInfoWrapper implements Comparable<NodeInfoWrapper> {

        public final NodeInfo node;
        public final Bucket bucket;

        private Instant lastActive = Instant.now();

        private NodeInfoWrapper(NodeInfo node, Bucket bucket) {
            this.node = node;
            this.bucket = bucket;
        }

        /**
         * 将节点的活动时间改为现在
         */
        public void refresh() {
            lastActive = Instant.now();
        }

        /**
         * 判断节点是否处于活动状态，如果不是则需要ping一下刷新状态
         *
         * @return
         */
        public boolean isActive() {
            return lastActive.plus(DhtConfig.NODE_FRESH, ChronoUnit.MINUTES).isAfter(Instant.now());
        }

        /**
         * 当节点在三个刷新间隔后还是没有回应将会删除
         *
         * @return
         */
        public boolean isDead() {
            return lastActive.plus(3 * DhtConfig.NODE_FRESH, ChronoUnit.MINUTES).isBefore(Instant.now());
        }

        /**
         * 就比较最后活跃时间
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(NodeInfoWrapper o) {
            return lastActive.compareTo(o.lastActive);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeInfoWrapper that = (NodeInfoWrapper) o;

            return node.equals(that.node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

    }

    /**
     * 节点的最直接的容器
     */
    private class Bucket {

        private Instant lastActive = Instant.now();

        private final Bitmap prefix;

        public Bucket(int size) {
            prefix = new Bitmap(size);
        }

        /**
         * bucket的节点
         * 新节点排在后面
         */
        private List<NodeInfoWrapper> nodes = new ArrayList<>(8);

        private Deque<NodeInfo> candidates = new ArrayDeque<>(2);//TODO optimize


        /**
         * 根据给予的Bitmap和值构造新的Bitmap
         * 新的Bitmap前面的值与老的一直，最后的值为val
         *
         * @param old 原来的bitmap
         * @param val 新的bitmap最后一位的值
         */
        public Bucket(Bitmap old, boolean val) {
            prefix = new Bitmap(old.size + 1);
            prefix.or(old);
            prefix.set(prefix.size - 1, val);
        }

        /**
         * 将当前Bucket分裂成两个
         * 左侧的是高位，右侧是低位
         *
         * @return
         */
        Tuple<Bucket, Bucket> split() {
            if (prefix.size == 160) return null;
            Bucket rightBucket = new Bucket(prefix, false);
            for (NodeInfoWrapper node : nodes) {
                if (rightBucket.checkRange(node.node.getKey())) rightBucket.addNode(node.node);
            }
            Bucket leftBucket = new Bucket(prefix, true);
            for (NodeInfoWrapper node : nodes) {
                if (leftBucket.checkRange(node.node.getKey())) leftBucket.addNode(node.node);
            }
            return new Tuple<>(leftBucket, rightBucket);
        }

        /**
         * 生成一个在当前Bucket的范围内的id
         *
         * @return
         */
        public NodeKey randomChildKey() {
            NodeKey key = NodeKey.genRandomKey();
            Bitmap bits = key.getBits();
            int size = prefix.size;
            for (int i = 0; i < size; i++) {
                bits.set(i, prefix.get(i));
            }
            return key;
        }

        /**
         * @param key
         * @return
         */
        boolean checkRange(NodeKey key) {
            int len = key.getBits().size;
            for (int i = 0; i < prefix.size && i < len; i++) {
                if (key.prefix(i) != prefix.get(i)) return false;
            }
            return true;
        }

        /**
         * 将info节点删除，用候选节点替换
         *
         * @param info
         */
        public void replace(NodeKey info) {
            NodeInfoWrapper node = remove(info);
            if (node != null) {
                if (!candidates.isEmpty()) {
                    NodeInfoWrapper wrapper = new NodeInfoWrapper(candidates.getLast(), this);
                    int len = nodes.size();
                    for (int i = 0; i < len; i++) {
                        if (nodes.get(i).compareTo(wrapper) < 0) {
                            nodes.add(i, wrapper);
                            return;
                        }
                    }
                    lastActive = Instant.now();
                }
                addNode(node.node);
            }
        }

        public void addCadidate(NodeInfo info) {
            if (candidates.size() == 2) {
                candidates.poll();
            }
            candidates.add(info);
        }

        /**
         * 在前面已经处理已经存在的情况，在这里不需要再次处理
         * 保持有2个候选节点
         *
         * @param info
         */
        public void addNode(NodeInfo info) {
            nodes.add(new NodeInfoWrapper(info, this));
            lastActive = Instant.now();
        }

        public int size() {
            return nodes.size();
        }

        boolean contains(NodeInfo info) {
            return nodes.contains(new NodeInfoWrapper(info, this));
        }

        /**
         * 可到key对应的DHT节点信息
         *
         * @param key
         * @return
         */
        NodeInfo getNode(NodeKey key) {
//            nodes.stream().filter(node -> node.getKey().equals(key)).findFirst();
            for (NodeInfoWrapper node : nodes) {
                if (node.node.getKey().equals(key)) return node.node;
            }
            return null;
        }

        /**
         * 删除key对应的DHT节点
         * 如果没有就不进行任何动作
         *
         * @param key
         */
        public NodeInfoWrapper remove(NodeKey key) {
            int size = nodes.size();
            for (int i = 0; i < size; i++) {
                if (nodes.get(i).node.getKey().equals(key)) {
                    return nodes.remove(i);
                }
            }
//            return nodes.removeIf(node -> node.node.getKey().equals(key));
            return null;
        }

        /**
         * 判断当前的bucket在配置时间内是否有刷新
         *
         * @return
         */
        public boolean isActive() {
            return Instant.now().plus(DhtConfig.BUCKET_FRESH, ChronoUnit.MINUTES).isAfter(lastActive);
        }

        /**
         * 处理Bucket定时刷新问题
         * locked
         */
        public void refresh(Krpc krpc) {
//            //TODO 提高性能，修改设计，代码好看
//            nodes.stream().filter(NodeInfoWrapper::isActive).findFirst().ifPresent(wrapper -> krpc.ping(wrapper.node));
            int len = nodes.size();
            List<NodeInfoWrapper> remove = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                NodeInfoWrapper wrapper = nodes.get(i);
                if (wrapper.isDead()) {
                    remove.add(wrapper);
                } else if (!wrapper.isActive()) {
                    krpc.ping(wrapper.node);
                }
            }
            nodes.removeAll(remove);
            size = size - remove.size();
            lastActive = Instant.now();
        }

        /**
         * 刷新指定节点的活动时间,同时也刷新当前bucket的活动时间
         *
         * @param node
         */
        public void refresh(NodeInfo node) {
            nodes.stream().filter(wrapper -> wrapper.node.equals(node)).findFirst().ifPresent(NodeInfoWrapper::refresh);
            lastActive = Instant.now();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bucket bucket = (Bucket) o;

            return prefix.equals(bucket.prefix);
        }

        @Override
        public int hashCode() {
            return prefix.hashCode();
        }

    }
}