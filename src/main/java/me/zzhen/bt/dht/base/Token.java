package me.zzhen.bt.dht.base;

import me.zzhen.bt.dht.DhtConfig;

import java.time.Instant;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Token {

    /**
     * 请求的目标节点ID或者资源hash
     */
    public final NodeInfo target;
    /**
     * id
     */
    public final long id;
    /**
     * token生成的时间
     */
    public final Instant time = Instant.now();

    /**
     * id 对应的请求的方法
     */
    public final String method;

    public Token(NodeInfo target, long id, String method) {
        this.target = target;
        this.id = id;
        this.method = method;
    }

    /**
     * @return 当前token是否有效
     */
    public boolean isLive() {
        return time.plusSeconds(DhtConfig.TOKEN_TIMEOUT).isAfter(Instant.now());
    }

    @Override
    public String toString() {
        return "Token{" +
                "target=" + target +
                ", id=" + id +
                ", time=" + time +
                ", method='" + method + '\'' +
                '}';
    }
}
