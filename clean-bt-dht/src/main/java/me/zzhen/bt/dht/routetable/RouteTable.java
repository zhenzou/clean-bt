package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private int length;

    /**
     * 容量
     */
    private int capacity;

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

    public RouteTable(int capacity) {
        Bucket init = new Bucket(0, 8);
        this.capacity = capacity;
        buckets.put(init.prefix, init);
        root.value = init;
    }

    public int length() {
        return length;
    }

    /**
     * 将node添加到对应的Bucket中
     *
     * @param node
     */
    public void addNode(NodeInfo node) {
        NodeId id = node.getId();
        if (id == null) return;
        try {
            lock.lock();
            //TODO
            NodeInfoWrapper wra = nodes.get(node.getFullAddress());
            if (wra != null) {
                wra.refresh();
                return;
            }
            if (length >= capacity) return;
            TreeNode item = findTreeNode(id);
            if (item.value == null) return;
            if (addNodeToBucket(node, item)) {
                length++;
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
        } else if (bucket.length() < 8) {
            NodeInfoWrapper wra = new NodeInfoWrapper(node, bucket);
            nodes.put(node.getFullAddress(), wra);
            bucket.addNode(wra);
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
     * 找到id 当前对应的Bucket
     *
     * @param id
     * @return
     */

    private TreeNode findTreeNode(NodeId id) {
        TreeNode item = root;
        int index = 0;
        while (item.value == null && index < 160) {
            boolean prefix = id.prefix(index);
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
     * 根据ID删除对应的节点
     *
     * @param id
     */
    private void removeById(NodeId id) {
        TreeNode treeNode = findTreeNode(id);
        treeNode.value.remove(id);
    }

    /**
     * Test
     * <p>
     * 得到以Root节点为根节点的子树的总节点个数
     */
    public int length(TreeNode root) {
        if (root == null) return 0;
        int size = 0;
        if (root.value != null) size += root.value.length();
        size += length(root.left);
        size += length(root.right);
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

    public List<NodeInfo> closest8Nodes(NodeId key) {
        return closestKNodes(key, 8);
    }

    /**
     * 在添加节点的过程中已经保证分裂后的节点的Bucket是空的
     *
     * @param id
     * @return 离key最近的K个节点
     */
    public List<NodeInfo> closestKNodes(NodeId id, int k) {
        TreeNode node = findTreeNode(id);
        List<NodeInfo> infos = new ArrayList<>(k);
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
        fillNode(node, infos, k);
        int size = infos.size();
        while (size < k && node.parent != null) {
            if (node == node.parent.left) {
                fillNode(node.parent.right, infos, k);
            } else {
                fillNode(node.parent.left, infos, k);
            }
            node = node.parent;
        }
    }

    /**
     * 将节点node或者node的子节点的Dht节点添加到返回的列表中
     *
     * @param node
     * @param infos
     * @param k
     */
    private void fillNode(TreeNode node, List<NodeInfo> infos, int k) {
        if (node == null || infos.size() == k) return;
        if (node.value == null) {
            fillNode(node.left, infos, k);
            fillNode(node.right, infos, k);
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
     * 在整个路由表中删除key对应的DHT节点信息
     *
     * @param node
     */
    public void remove(NodeInfo node) {
        String address = node.getFullAddress();
        if (nodes.containsKey(address)) {
            NodeInfoWrapper remove = nodes.remove(address);
            if (remove.delete()) {
                length--;
            }
        }
    }
}