package me.zzhen.bt.dht.base;

import me.zzhen.bt.dht.DhtConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Project:CleanBT
 * Create Time: 16-12-12.
 * Description:
 * 管理请求的t字段和get_peers的token字段
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class TokenManager {

    /**
     * token自增ID
     */
    private static volatile AtomicLong autoIncId = new AtomicLong();
    private static final ConcurrentHashMap<Long, Token> tokens = new ConcurrentHashMap<>();


    /**
     * 删除过期的token
     */
    public static void clearTokens() {
        tokens.entrySet().removeIf(entry -> !entry.getValue().isLive());
    }

    /**
     * @param key    请求的目标ID
     * @param method 请求的方法
     * @return id is a long
     */
    public static Token newToken(NodeKey key, String method) {
        long id = autoIncId.addAndGet(1);
        Token token = new Token(key, id, method);
        tokens.put(id, token);
        return token;
    }

    /**
     * 获取的同时删除对应的token
     *
     * @param id
     * @return
     */
    public static Optional<Token> getToken(long id) {
        return Optional.of(tokens.remove(id));
    }
}
