package me.zzhen.bt.dht;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.PriorityQueue;

/**
 * Project:CleanBT
 * Create Time: 17-6-4.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface Blacklist {

    static Blacklist defaultBlacklist(int size) {
        return new DefaultBlacklist(size);
    }

    boolean is(String ip, int port);

    void put(String item, int port);

    boolean remove(String item, int port);

    int length();
}

/**
 * 特殊情况，equals 和 compareTo的结果不一定一样
 */
@EqualsAndHashCode(exclude = "ts")
class BlacklistItem implements Comparable<BlacklistItem> {
    public final String ip;
    public final int port;
    private final long ts = Instant.now().getEpochSecond();

    public BlacklistItem(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public int compareTo(@NotNull BlacklistItem o) {
        if (ts > o.ts) {
            return 1;
        } else if (ts < o.ts) {
            return -1;
        } else {
            return 0;
        }
    }
}

class DefaultBlacklist extends PriorityQueue<BlacklistItem> implements Blacklist {

    private int size;

    public DefaultBlacklist(int size) {
        super();
        this.size = size;
    }

    @Override
    public boolean is(String item, int port) {
        return contains(new BlacklistItem(item, port));
    }

    @Override
    public void put(String item, int port) {
        if (size() >= size) {
            poll();
        }
        add(new BlacklistItem(item, port));
    }

    @Override
    public boolean remove(String item, int port) {
        return super.remove(new BlacklistItem(item, port));
    }

    @Override
    public int length() {
        return size;
    }
}
