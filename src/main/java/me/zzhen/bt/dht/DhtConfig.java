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
    public static final int SERVER_PORT = 6882;

    public static final int RETRY_TIME = 2;
    /**
     * 请求过时间隔，请求失败，将加入黑名单
     */
    public static final int CONN_TIMEOUT = 10 * 1000;
    /**
     * Token失效间隔
     */
    public static final int TOKEN_TIMEOUT = 15 * 60 * 1000;
    /**
     * 黑名单大小，超出将自动移除前面的
     */
    public static final int BLACKLIST_SIZE = 15 * 1000;
}
