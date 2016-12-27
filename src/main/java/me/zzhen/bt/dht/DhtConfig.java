package me.zzhen.bt.dht;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtConfig {
    /**
     * DHT Server的IP地址
     */
//    public static final String SERVER_IP = "127.0.0.1";
    public static final String SERVER_IP = "43.241.224.72";
    /**
     * DHT Server监听端口
     */
    public static final int SERVER_PORT = 6881;

    public static final int RETRY_TIME = 2;
    /**
     * 请求过时间隔，请求失败，将加入黑名单
     */
    public static final int CONN_TIMEOUT = 10 * 1000;
    /**
     * Token失效间隔 单位为秒
     */
    public static final int TOKEN_TIMEOUT = 15 * 60;
    /**
     * 黑名单大小，超出将自动移除前面的
     */
    public static final int BLACKLIST_SIZE = 5 * 1000;
    /**
     *
     */
    public static final int ROUTETABLE_SIZE = 5 * 1000;

    /**
     * 获取metadata时的block大小
     */
    public static final int BLOCK_SIZE = 16 * 1024;
    /**
     * bucket 刷新间隔，单位为秒
     */
    public static final int BUCKET_FRESH = 10;
    /**
     * 每个节点的有效时间，单位为秒
     */
    public static final int NODE_FRESH = 10;
    /**
     * 自动查找间隔,时间为秒
     */
    public static final int AUTO_FIND = 30;
    /**
     * 每次自动查找的节点数
     */
    public static final int AUTO_FIND_SIZE = 256;
}
