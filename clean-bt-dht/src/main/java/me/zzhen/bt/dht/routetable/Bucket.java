package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 节点的最直接的容器
 */
public class Bucket {

    /**
     * 这个Bucket的范围
     */
    public final Bitmap prefix;

    /**
     * 最大容量
     */
    public final int capacity;

    /**
     * bucket的节点
     * 新节点排在后面
     */
    public final List<NodeInfoWrapper> nodes;

    /**
     * 最后活跃时间
     */
    private long lastActive = Instant.now().getEpochSecond();

    public Bucket(int bitSize, int capacity) {
        prefix = new Bitmap(bitSize);
        this.capacity = capacity;
        nodes = new ArrayList<>(capacity);
    }

    /**
     * 根据给予的Bitmap和值构造新的Bitmap
     * 新的Bitmap前面的值与老的一直，最后的值为val
     *
     * @param old      原来的bitmap
     * @param val      新的bitmap最后一位的值
     * @param capacity
     */
    public Bucket(Bitmap old, boolean val, int capacity) {
        prefix = new Bitmap(old.size + 1);
        this.capacity = capacity;
        prefix.or(old);
        prefix.set(prefix.size - 1, val);
        nodes = new ArrayList<>(capacity);
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
     * @return 分裂后的buckets
     */
    Tuple<Bucket, Bucket> split() {
        if (prefix.size == 160) return null;
        Bucket leftBucket = new Bucket(prefix, true, 8);
        Bucket rightBucket = new Bucket(prefix, false, 8);
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
            bits.set(i, prefix.at(i));
        }
        return key;
    }

    /**
     * 检查id是否在当前Bucket的范围内
     *
     * @param id
     */
    public boolean checkRange(NodeId id) {
        int len = id.getBits().size;
        for (int i = 0; i < prefix.size && i < len; i++) {
            if (id.prefix(i) != prefix.at(i)) return false;
        }
        return true;
    }

    /**
     * 在前面已经处理已经存在的情况，在这里不需要再次处理
     *
     * @param info
     */
    public void addNode(NodeInfo info) {
        addNode(new NodeInfoWrapper(info, this));
    }

    /**
     * 在前面已经处理已经存在的情况，在这里不需要再次处理
     * 保持有2个候选节点
     *
     * @param info node
     */
    public void addNode(NodeInfoWrapper info) {
        if (nodes.size() >= capacity) {
            throw new RuntimeException("capacity out of range max " + capacity);
        }
        nodes.add(info);
        lastActive = Instant.now().getEpochSecond();
    }

    /**
     * 已有NodeInfo的数量
     *
     * @return
     */
    public int length() {
        return nodes.size();
    }

    boolean contains(NodeInfo info) {
        return nodes.contains(new NodeInfoWrapper(info, this));
    }

    /**
     * 找到key对应的DHT节点信息
     *
     * @param id
     * @return
     */
    public Optional<NodeInfo> getNode(NodeId id) {
        Optional<NodeInfoWrapper> first = nodes.stream().filter(node -> node.node.getId().equals(id)).findFirst();
        return first.map(wrapper -> wrapper.node);
    }

    /**
     * 删除key对应的DHT节点
     * 如果没有就不进行任何动作
     *
     * @param id
     */
    public boolean remove(NodeId id) {
        return nodes.removeIf(wrapper -> wrapper.node.getId().equals(id));
    }

    /**
     * 判断当前的bucket在配置时间内是否有刷新
     *
     * @return
     */
    public boolean isActive() {
        return lastActive + 30 > (Instant.now().getEpochSecond());
    }

    /**
     * 刷新指定节点的活动时间,同时也刷新当前bucket的活动时间
     *
     * @param node
     */
    public void update(NodeInfo node) {
        nodes.forEach(n -> { if (n.node.fullAddress().equals(node.fullAddress())) n.refresh(); });
        refresh();
    }


    /**
     * 刷新当前bucket的活动时间
     *
     * @param node
     */
    public void refresh() {
        lastActive = Instant.now().getEpochSecond();
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
