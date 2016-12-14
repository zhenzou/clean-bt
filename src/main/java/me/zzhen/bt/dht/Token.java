package me.zzhen.bt.dht;

import java.time.Instant;

/**
 * Project:CleanBT
 * Create Time: 16-12-14.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Token {

    public final NodeKey key;
    public final char token;
    public final Instant time;

    public Token(NodeKey key, char token, Instant time) {
        this.key = key;
        this.token = token;
        this.time = time;
    }
}
