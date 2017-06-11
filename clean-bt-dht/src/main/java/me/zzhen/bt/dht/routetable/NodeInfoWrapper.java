package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.dht.DhtConfig;
import me.zzhen.bt.dht.NodeInfo;

import java.time.Instant;

/**
 * DHT节点包装，主要增加活动时间记录，便于处理
 */
public class NodeInfoWrapper implements Comparable<NodeInfoWrapper> {

    public final NodeInfo node;
    public final Bucket bucket;

    private long lastActive = Instant.now().getEpochSecond();

    NodeInfoWrapper(NodeInfo node, Bucket bucket) {
        this.node = node;
        this.bucket = bucket;
    }

    /**
     * 将节点的活动时间改为现在
     */
    void refresh() {
        lastActive = Instant.now().getEpochSecond();
    }

    /**
     * 判断节点是否处于活动状态，如果不是则需要ping一下刷新状态
     *
     * @return
     */
    boolean isActive() {
        return lastActive + (15 * 60) > Instant.now().getEpochSecond();
    }

    /**
     * 当节点在三个刷新间隔后还是没有回应将会删除
     *
     * @return
     */
    boolean delete() {
        return bucket.remove(node.getId());
    }

    /**
     * 当节点在三个刷新间隔后还是没有回应将会删除
     *
     * @return
     */
    boolean isDead() {
        return lastActive + (3 * 30) > Instant.now().getEpochSecond();
    }

    /**
     * 只比较最后活跃时间
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(NodeInfoWrapper o) {
        if (lastActive > o.lastActive) {
            return 1;
        } else if (lastActive < o.lastActive) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 只比较 node
     *
     * @param o
     * @return
     */
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
