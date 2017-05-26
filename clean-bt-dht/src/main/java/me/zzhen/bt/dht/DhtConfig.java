package me.zzhen.bt.dht;

import lombok.Data;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
@Data
public class DhtConfig {

    /**
     * DHT Server的IP地址
     */
    public final String serverIp;

    public final int serverPort;

    /**
     * Token失效间隔 单位为分钟
     */
    private int tokenTimeout;
    /**
     * t 失效间隔 单位为分钟
     */
    private int tTimeout;

    /**
     * 请求过时间隔，请求失败，将加入黑名单
     */
    private int timeout;

    /**
     * 黑名单大小，超出将自动移除前面的
     */
    private int blacklistSize;

    /**
     * 路由表大小
     */
    private int routeTableSize;

    /**
     * bucket 刷新间隔，单位为秒
     */
    private int bucketFresh;

    /**
     * 节点刷新间隔，单位为秒
     */
    private int nodeFresh;

    /**
     * 自动查找间隔,时间为秒
     */
    private int autoFind;

    /**
     * 每次自动查找的节点数
     */
    private int autoFindSize;


    private int mode;
    /**
     * 标准DHT节点模式
     */
    public static final int STANDARD_MODE = 1111;

    /**
     * DHT爬虫模式
     */
    public static final int CRAWL_MODE = 2222;


    public DhtConfig(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public static DhtConfig defaultConfig() {
        DhtConfig config = new DhtConfig("127.0.0.1", 6881);
        config.timeout = 15 * 1000;
        return config;
    }

    //    public static final String SERVER_IP = "127.0.0.1";
    public static final String SERVER_IP = "43.241.226.21";
    /**
     * DHT Server监听端口
     */
    public static final int SERVER_PORT = 6881;

    public static final int RETRY_TIME = 2;


    public static final int CONN_TIMEOUT = 15 * 1000;

    public static final int TOKEN_TIMEOUT = 15;

    /**
     * t 失效时间，单位为分钟
     */
    public static final int T_TIMEOUT = 1;

    public static final int BLACKLIST_SIZE = 5 * 1000;
    /**
     *
     */
    public static final int ROUTETABLE_SIZE = 5 * 1000;


    public static final int BUCKET_FRESH = 0;
    /**
     * 每个节点的有效时间，单位为秒
     */
    public static final int NODE_FRESH = 0;

    public static final int AUTO_FIND = 30;

    public static final int AUTO_FIND_SIZE = 256;
}
