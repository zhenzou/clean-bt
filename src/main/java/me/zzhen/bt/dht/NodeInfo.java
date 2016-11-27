package me.zzhen.bt.dht;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeInfo {
    public final String ip;
    public final int port;
    public final NodeKey key;

    public NodeInfo(String ip, int port, NodeKey key) {
        this.ip = ip;
        this.port = port;
        this.key = key;
    }
}
