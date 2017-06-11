package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description：
 * TODO 重构,更加可读，性能更好
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
     * 容量
     */
    private int maxSize;

    /**
     * 前缀树，根节点,父节点为null
     */
    private TreeNode root = new TreeNode((byte) 0, null);

    /**
     * 便于操作
     */
    private Map<Bitmap, Bucket> buckets = new HashMap<>();

    private Map<String, NodeInfoWrapper> nodes = new HashMap<>();


    /**
     * 在添加节点以及刷新的时候需要加锁
     */
    private final ReentrantLock lock = new ReentrantLock();

    public RouteTable(int size) {
        Bucket init = new Bucket(0);
        maxSize = size;
        buckets.put(init.prefix, init);
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
        NodeId key = node.getId();
        if (key == null) return;
        try {
            lock.lock();
            //TODO
            NodeInfoWrapper wra = nodes.get(node.getFullAddress());
            if (wra != null) {
                wra.refresh();
                return;
            }
            if (size >= maxSize) return;
            TreeNode item = findTreeNode(key);
            if (item.value == null) return;
            if (addNodeToBucket(node, item)) {
                size++;
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
        NodeId key = node.getId();
        if (bucket.contains(node)) {
            repeat++;
            bucket.update(node);
            return false;
        } else if (bucket.size() < 8) {
            NodeInfoWrapper wra = new NodeInfoWrapper(node, bucket);
            nodes.put(node.getFullAddress(), wra);
            bucket.addNode(node);
            return true;
        } else if (bucket.checkRange(node.getId())) {
            Tuple<Bucket, Bucket> spit = bucket.split();
            if (spit == null) return false;
            TreeNode left = new TreeNode((byte) 1, item);
            left.value = spit._1;
            TreeNode right = new TreeNode((byte) 0, item);
            right.value = spit._2;
            item.left = left;
            item.right = right;
            buckets.remove(item.value.prefix);
            item.value = null;
            buckets.put(spit._1.prefix, spit._1);
            buckets.put(spit._2.prefix, spit._2);
            if (left.value.checkRange(key)) return addNodeToBucket(node, left);
            else return addNodeToBucket(node, right);
        }
        return false;
    }

    /**
     * 得到以Root节点为根节点的子树的总节点个数
     */
    public int size(TreeNode root) {
        if (root == null) return 0;
        int size = 0;
        if (root.value != null) size += root.value.size();
        size += size(root.left);
        size += size(root.right);
        return size;
    }

    /**
     * 刷新，删除过期的不活越节点
     */
    public void refresh() {
        try {
            if (lock.tryLock() || lock.tryLock(10, TimeUnit.SECONDS)) {
                nodes.entrySet().stream().forEach(entry -> {
//            if(entry.g)
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据ID删除对应的节点
     *
     * @param key
     */
    private void removeById(NodeId key) {
        TreeNode treeNode = findTreeNode(key);
        treeNode.value.remove(key);
    }

    /**
     * Test
     *
     * @return
     */
    public TreeNode getRoot() {
        return root;
    }

    public List<NodeInfo> closest8Nodes(NodeId key) {
        return closestKNodes(key, 8);
    }

    /**
     * 在添加节点的过程中已经保证分裂后的节点的Bucket是空的
     *
     * @param key
     * @return 离key最近的K个节点
     */
    public List<NodeInfo> closestKNodes(NodeId key, int k) {
        TreeNode node = findTreeNode(key);
        List<NodeInfo> infos = new ArrayList<>(8);
        closestKNodes(node, infos, k);
        return infos;
    }

    /**
     * 递归向上查找节点，直到找到的节点数为k，注意根节点的parent节点为null
     *
     * @param node
     * @param infos
     * @param k
     */
    private void closestKNodes(TreeNode node, List<NodeInfo> infos, int k) {
        int size = size(node);
        while (size < k && node.parent != null) {
            if (node == node.parent.left) size = size + size(node.parent.right) + 1;
            else size = size + size(node.parent.left) + 1;
            node = node.parent;
        }
        addClosestNode(node, infos, k);
    }

    /**
     * 将节点node或者node的子节点的Dht节点添加到返回的列表中
     *
     * @param node
     * @param infos
     * @param k
     */
    private void addClosestNode(TreeNode node, List<NodeInfo> infos, int k) {
        if (node == null || infos.size() == k) return;
        if (node.value == null) {
            addClosestNode(node.left, infos, k);
            addClosestNode(node.right, infos, k);
        } else {
            for (NodeInfoWrapper wrapper : node.value.nodes) {
                if (infos.size() >= k) break;
                infos.add(wrapper.node);
            }
        }
    }


    /**
     * 保存在刷新过程中访问的节点，将要删除或者调整，再次刷新的时候总是访问相同的节点
     */
    private List<NodeInfo> keys = new ArrayList<>(DhtConfig.AUTO_FIND_SIZE);


    /**
     * 找到key当前对应的节点
     *
     * @param key
     * @return
     */

    private TreeNode findTreeNode(NodeId key) {
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
     * @param node
     */
    public void remove(NodeInfo node) {
        String address = node.getFullAddress();
        if (nodes.containsKey(address)) {
            NodeInfoWrapper remove = nodes.remove(address);
            if (remove.delete()) {
                size--;
            }
        }
//        TreeNode item = findTreeNode(node.getId());
//        Bucket bucket = item.value;
////        buckets.remove(bucket.prefix);
//        if (bucket.remove(node.getId())) {
////            removeById(node.getId());
//            nodes.remove(node.getFullAddress());
//            size--;
//        }
//        buckets.put(bucket.prefix, bucket);
    }
}