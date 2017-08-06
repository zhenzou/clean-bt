package me.zzhen.bt.dht;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 * 所有时间单位为秒
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DhtConfig {

    /**
     * 本地IP地址
     */
//    public static final String SERVER_IP = "127.0.0.1";
    public static final String SERVER_IP = "119.129.82.197";
    /**
     * DHT Server监听端口
     */
    public static final int SERVER_PORT = 6881;

    public static final int RETRY_TIME = 2;


    public static final int CONN_TIMEOUT = 15;

    public static final int TOKEN_TIMEOUT = 15 * 1000;

    public static final int BLACKLIST_SIZE = 5 * 1000;

    /**
     * 黑名单失效时间，默认一个小时
     */
    private static final int BLACKLIST_EXPIRED = 60 * 60;

    public static final int ROUTER_TABLE_SIZE = 5 * 1000;

    public static final int BUCKET_FRESH = 0;
    /**
     * 每个节点的有效时间，单位为秒
     */
    public static final int NODE_FRESH = 0;

    public static final int AUTO_FIND = 30;

    public static final int AUTO_FIND_SIZE = 256;

    /**
     * DHT Server的IP地址
     */
    public final String serverIp;

    final int serverPort;

    /**
     * Token失效间隔
     */
    int tokenTimeout;

    /**
     * 请求过时间隔，请求失败，将加入黑名单
     */
    int connTimeout;

    /**
     * 黑名单大小，超出将自动移除前面的
     */
    int blacklistSize;

    /**
     * 黑名单大小，超出将自动移除前面的
     */
    int blacklistExpired;


    /**
     * 路由表大小
     */
    int routeTableSize;

    /**
     * bucket 刷新间隔，单位为秒
     */
    int bucketFresh;

    /**
     * 节点刷新间隔，单位为秒
     */
    int nodeFresh;

    /**
     * 自动查找间隔,时间为秒
     */
    int refireshInterval;

    /**
     * 每次自动查找的节点数
     */
    int autoFindSize;

    private DhtConfig(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public static DhtConfig config(String serverIp, int serverPort) {
        DhtConfig config = new DhtConfig(serverIp, serverPort);
        config.refireshInterval = AUTO_FIND;
        config.autoFindSize = AUTO_FIND_SIZE;
        config.blacklistSize = BLACKLIST_SIZE;
        config.blacklistExpired = BLACKLIST_EXPIRED;
        config.tokenTimeout = TOKEN_TIMEOUT;
        config.connTimeout = CONN_TIMEOUT;
        config.routeTableSize = ROUTER_TABLE_SIZE;
        config.bucketFresh = BUCKET_FRESH;
        config.nodeFresh = NODE_FRESH;
        return config;
    }

    public static DhtConfig defaultConfig() {
        return config(SERVER_IP, SERVER_PORT);
    }
}
