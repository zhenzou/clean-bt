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
public class Response {

    public final String method;
    public final Node value;
    public final int token;

    private boolean isError;

    public Response(String method, Node value, int token) {
        this.method = method;
        this.value = value;
        this.token = token;
    }

    public boolean isError() {
        return isError;
    }

//    public byte[] token() {
//        return value.getNode("t").decode();
//    }

    public static boolean isError(Node node) {
        if (node == null) return true;
        try {
            DictionaryNode arg = (DictionaryNode) node;
            return arg.getNode("e") != null;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public static DictionaryNode makeResponse(NodeKey self) {
        DictionaryNode node = new DictionaryNode();
        DictionaryNode r = new DictionaryNode();
        r.addNode("id", new StringNode(self.getValue()));
        node.addNode("r", r);
        node.addNode("y", new StringNode("r"));
        return node;
    }

    public static DictionaryNode makeError(NodeKey key, int errno, String msg) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(TokenManager.newToken(key).token + ""));
        node.addNode("y", new StringNode("e"));
        ListNode e = new ListNode();
        e.addNode(new IntNode(errno));
        e.addNode(new StringNode(msg));
        node.addNode("e", e);
        return node;
    }
}
