package me.zzhen.bt.dht.base;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.TokenManager;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Request {

    public static DictionaryNode makeRequest(NodeKey key, String method) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(TokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("q"));
        node.addNode("q", new StringNode(method));
        return node;
    }


    public final String method;
    public final DictionaryNode arg;
    public final Token token;

    public Request(String method, DictionaryNode arg, Token token) {
        this.method = method;
        this.arg = arg;
        this.token = token;
    }
}
