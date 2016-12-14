package me.zzhen.bt.dht;

import com.sun.corba.se.impl.oa.toa.TOA;

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

    private final Random random = new Random();
    private final Map<NodeKey, List<Token>> tokens = new HashMap<>();
    private final Timer timer;
    private boolean isRunning = true;

    public TokenManager() {
        timer = new Timer();
        //每隔五分钟清理一次
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
    private void clearTokens() {
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
    public Token newToken(NodeKey key) {
        char c = (char) random.nextInt();
        Token token = new Token(key, c, Instant.now());
        List<Token> keyTokens = this.tokens.get(key);
        if (keyTokens != null) {
            keyTokens.add(token);
        } else {
            List<Token> list = new ArrayList<>();
            list.add(token);
            this.tokens.put(key, list);
        }
        return token;
    }

    /**
     * token 五分钟后失效
     *
     * @param token
     * @return
     */
    public boolean isEffective(char token) {
        return tokens.get(token) != null;
    }

    public synchronized void clear() {
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
}
