package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import me.zzhen.bt.dht.krpc.Krpc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 节点的最直接的容器
 */
public class Bucket {

    private Instant lastActive = Instant.now();

    /**
     * 这个Bucket的范围
     */
    public final Bitmap prefix;

    public Bucket(int size) {
        prefix = new Bitmap(size);
    }

    /**
     * bucket的节点
     * 新节点排在后面
     */
    public final List<NodeInfoWrapper> nodes = new ArrayList<>(8);

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

    private boolean reassignNode(NodeInfo node, Bucket left, Bucket right) {
        if (left.checkRange(node.getId())) {
            left.addNode(node);
            return true;
        }
        if (right.checkRange(node.getId())) {
            right.addNode(node);
            return true;
        }
        return false;
    }

    /**
     * 将当前Bucket分裂成两个
     * 左侧的是高位，右侧是低位
     *
     * @return
     */
    Tuple<Bucket, Bucket> split() {
        if (prefix.size == 160) return null;
        Bucket leftBucket = new Bucket(prefix, true);
        Bucket rightBucket = new Bucket(prefix, false);
        nodes.forEach(wrapper -> reassignNode(wrapper.node, leftBucket, rightBucket));
        return new Tuple<>(leftBucket, rightBucket);
    }

    /**
     * 生成一个在当前Bucket的范围内的id
     *
     * @return 当前的ID
     */
    public NodeId randomChildKey() {
        NodeId key = NodeId.randomId();
        Bitmap bits = key.getBits();
        int size = prefix.size;
        for (int i = 0; i < size; i++) {
            bits.set(i, prefix.get(i));
        }
        return key;
    }

    /**
     * 检查key是否在当前Bucket的范围内
     *
     * @param key
     * @return
     */
    boolean checkRange(NodeId key) {
        int len = key.getBits().size;
        for (int i = 0; i < prefix.size && i < len; i++) {
            if (key.prefix(i) != prefix.get(i)) return false;
        }
        return true;
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

    /**
     * 在前面已经处理已经存在的情况，在这里不需要再次处理
     * 保持有2个候选节点
     *
     * @param info node
     */
    public void addNode(NodeInfoWrapper info) {
        nodes.add(info);
        lastActive = Instant.now();
    }

    public int size() {
        return nodes.size();
    }

    boolean contains(NodeInfo info) {
        return nodes.contains(new NodeInfoWrapper(info, this));
    }

    /**
     * 找到key对应的DHT节点信息
     *
     * @param key
     * @return
     */
    public NodeInfo getNode(NodeId key) {
        Optional<NodeInfoWrapper> first = nodes.stream().filter(node -> node.node.getId().equals(key)).findFirst();
        return first.map(wrapper -> wrapper.node).orElse(null);
    }

    /**
     * 删除key对应的DHT节点
     * 如果没有就不进行任何动作
     *
     * @param key
     */
    public boolean remove(NodeId key) {
        return nodes.removeIf(wrapper -> wrapper.node.getId().equals(key));
    }

    /**
     * 判断当前的bucket在配置时间内是否有刷新
     *
     * @return
     */
    public boolean isActive() {
        return lastActive.plusSeconds(DhtConfig.BUCKET_FRESH).isAfter(Instant.now());
    }

    /**
     * 处理Bucket定时刷新问题
     * locked
     */
    public void refresh(Krpc krpc) {
        nodes.forEach(wrapper -> krpc.ping(wrapper.node));
//            List<NodeInfoWrapper> remove = new ArrayList<>();
//            for (NodeInfoWrapper node : nodes) {
//                if (node.isDead()) remove.put(node);
//                else if (!node.isActive()) krpc.ping(node.node);
//            }
//            nodes.removeAll(remove);
//            size -= remove.size();
//            lastActive = Instant.now();
    }

    /**
     * 刷新指定节点的活动时间,同时也刷新当前bucket的活动时间
     *
     * @param node
     */
    public void update(NodeInfo node) {
        nodes.stream().filter(wrapper -> wrapper.node.equals(node)).findFirst().ifPresent(NodeInfoWrapper::refresh);
        lastActive = Instant.now();
    }


    @Override
    public String toString() {
        return prefix.toString();
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
