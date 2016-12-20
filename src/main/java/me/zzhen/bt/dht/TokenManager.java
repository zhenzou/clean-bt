package me.zzhen.bt.dht;

import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-12.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class TokenManager {

    //TODO 不使用Timer，而且使用ConcurrentMap
    private static final Random random = new Random();
    private static final Map<NodeKey, List<Token>> tokens = new HashMap<>();
    private static final Timer timer = new Timer();
    private static boolean isRunning = true;

    static {
        long seconds = 5L * 60 * 1000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearTokens();
            }
        }, seconds, seconds);
    }

    /**
     * 没有严格检查，随便吧，反正token过期时间就是节点自己定义的
     */
    private static void clearTokens() {
        System.out.println("clear");
        Instant now = Instant.now();
        for (Map.Entry<NodeKey, List<Token>> entry : tokens.entrySet()) {
            List<Token> tokens = entry.getValue();
            for (int i = 0; i < tokens.size(); i++) {
                boolean after = tokens.get(i).time.plus(5, ChronoUnit.MINUTES).isAfter(now);
                if (!after) {
                    tokens.remove(i);//iter and removce
                }
            }
        }
    }

    /**
     * @return token is a two byte char
     */
    public static Token newToken(NodeKey key) {
        char c = (char) random.nextInt();
        Token token = new Token(key, c, Instant.now());
        List<Token> keyTokens = tokens.get(key);
        if (keyTokens != null) {
            keyTokens.add(token);
        } else {
            List<Token> list = new ArrayList<>();
            list.add(token);
            tokens.put(key, list);
        }
        return token;
    }

    /**
     * token 五分钟后失效
     *
     * @param token
     * @return
     */
    public static boolean isEffective(char token) {
        return tokens.get(token) != null;
    }

    public static synchronized void clear() {
        System.out.println("clear");
        if (isRunning) {
            tokens.clear();
            timer.cancel();
            isRunning = false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        tokens.clear();
        timer.cancel();
    }

    public static void remove(int token) {

    }
}
