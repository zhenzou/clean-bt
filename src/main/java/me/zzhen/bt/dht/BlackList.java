package me.zzhen.bt.dht;

import sun.java2d.pipe.AAShapePipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class BlackList {

    private List<BlackListItem> blackList = new ArrayList<>();

    public void add(String ip, int port) {
        blackList.add(new BlackListItem(ip, port));
    }

    public boolean contains(String ip, int port) {
        return blackList.contains(new BlackListItem(ip, port));
    }

    public boolean remove(String ip,int port){
        return blackList.remove(new BlackListItem(ip,port));
    }

    final class BlackListItem {
        final String ip;
        final int port;

        public BlackListItem(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlackListItem that = (BlackListItem) o;

            if (port != that.port) return false;
            return ip.equals(that.ip);
        }

        @Override
        public int hashCode() {
            int result = ip.hashCode();
            result = 31 * result + port;
            return result;
        }
    }


}
