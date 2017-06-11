package me.zzhen.bt.dht;

import java.util.Optional;
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
    /**
     * 保存ID与Token 的键值对
     */
    private static final ConcurrentHashMap<Long, Token> tokens = new ConcurrentHashMap<>();

    /**
     * 新建一个请求中的t参数类型的Token实例
     *
     * @param key    请求的目标ID
     * @param method 请求的方法
     * @return id is a long
     */
    public static Token newToken(NodeId key, String method) {
        long id = autoIncId.addAndGet(1);
        Token token = new Token(key, id, method, false);
        tokens.put(id, token);
        return token;
    }

    /**
     * 新建一个get_peers响应中的token参数类型的Token实例
     *
     * @param key    请求的目标ID
     * @param method 请求的方法
     * @return id is a long
     */
    public static Token newTokenToken(NodeId key, String method) {
        long id = autoIncId.addAndGet(1);
        Token token = new Token(key, id, method, true);
        tokens.put(id, token);
        return token;
    }

    /**
     * 删除过期的token
     */
    public static void clearTokens() {
//        tokens.entrySet().removeIf(entry -> !entry.getValue().isLive());
    }

    /**
     * 获取的同时删除对应的token
     *
     * @param id
     * @return
     */
    public static Optional<Token> getToken(long id) {
        return Optional.ofNullable(tokens.remove(id));
    }

    public static int size() {
        return tokens.size();
    }
}
