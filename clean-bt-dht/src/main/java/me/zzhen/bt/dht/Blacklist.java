package me.zzhen.bt.dht;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Project:CleanBT
 * Create Time: 17-6-4.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface Blacklist {

    static Blacklist defaultBlacklist(int size, long expired) {
        return new DefaultBlacklist(size, expired);
    }

    boolean is(String ip, int port);

    void put(String ip, int port);

    void remove(String item, int port);

    int length();
}

class DefaultBlacklist implements Blacklist {
    private Map<String, Long> list;
    private final int size;
    private final long expired;

    public DefaultBlacklist(int size, long expired) {
        list = new HashMap<>();
        this.size = size;
        this.expired = expired;
    }

    @Override
    public boolean is(String ip, int port) {
        String key = key(ip, port);
        Long ts = list.get(key);
        if (ts == null) {
            return false;
        } else {
            if (ts - Instant.now().getEpochSecond() > expired) {
                list.remove(key);
                return false;
            }
        }
        return true;
    }

    @Override
    public void put(String ip, int port) {
        list.put(key(ip, port), Instant.now().getEpochSecond());
    }

    @Override
    public void remove(String item, int port) {
        list.remove(key(item, port));
    }

    @Override
    public int length() {
        return list.size();
    }

    private String key(String ip, int port) {
        return ip + ":" + port;
    }
}
