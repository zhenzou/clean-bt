package me.zzhen.bt.dht.base;

import me.zzhen.bt.dht.DhtConfig;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Project:CleanBT
 * Create Time: 16-12-12.
 * Description:
 * TODO 实现
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class TokenManager {

    private static AtomicLong autoIncId = new AtomicLong();
    private static final Map<Long, Token> tokens = new HashMap<>();
    private static final Timer timer = new Timer();
    private static boolean isRunning = true;

    static {
        long seconds = DhtConfig.TOKEN_TIMEOUT;
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                clearTokens();
//            }
//        }, seconds, seconds);
    }

    /**
     * 没有严格检查，随便吧，反正token过期时间就是节点自己定义的
     */
    private static void clearTokens() {
        Instant now = Instant.now();
        for (Map.Entry<Long, Token> entry : tokens.entrySet()) {
            if (entry.getValue().isLive()) {
                //remove
            }
        }
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

    public static synchronized void clear() {
        if (isRunning) {
            tokens.clear();
            timer.cancel();
            isRunning = false;
        }
    }

    public static Token getToken(long id) {
        return tokens.get(id);
    }

    @Override
    protected void finalize() throws Throwable {
        tokens.clear();
        timer.cancel();
        super.finalize();
    }

    public static void remove(long id) {
        tokens.remove(id);
    }
}
