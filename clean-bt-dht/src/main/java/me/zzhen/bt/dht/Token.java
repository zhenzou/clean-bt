package me.zzhen.bt.dht;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Token {

    /**
     * id
     */
    public final long id;

    /**
     * 请求的目标节点ID或者资源hash或者announce_peer对应的额资源hash
     */
    public final NodeId target;

    /**
     * token生成的时间
     */
    public final long lastUpdate = Instant.now().toEpochMilli();

    /**
     * id 对应的请求的方法
     */
    public final String method;

    /**
     * 是否是响应get_peers请求的token参数
     */
    public final boolean isToken;

    Token(NodeId target, long id, String method, boolean isToken) {
        this.target = target;
        this.id = id;
        this.method = method;
        this.isToken = isToken;
    }
}
