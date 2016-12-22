package me.zzhen.bt.dht;

import me.zzhen.bt.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class BlackList {

    private Set<BlackListItem> items = new HashSet<>();

    public void add(String ip, int port) {
        items.add(new BlackListItem(ip, port));
    }

    public boolean contains(String ip, int port) {
        return items.contains(new BlackListItem(ip, port));
    }

    public boolean remove(String ip, int port) {
        return items.remove(new BlackListItem(ip, port));
    }

    final class BlackListItem {
        final int ip;//32位 IPV4
        final int port;

        /**
         * @param ip   分隔符形式的IPV4地址
         * @param port
         */
        public BlackListItem(String ip, int port) {
            byte[] bytes = Utils.ipToBytes(ip);
            this.ip = Utils.bytesToInt(bytes);
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlackListItem that = (BlackListItem) o;

            if (ip != that.ip) return false;
            return port == that.port;
        }

        @Override
        public int hashCode() {
            int result = ip;
            result = 31 * result + port;
            return result;
        }
    }
}
