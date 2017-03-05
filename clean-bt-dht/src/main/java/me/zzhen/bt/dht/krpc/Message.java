package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.dht.base.Token;
import me.zzhen.bt.dht.base.TokenManager;

/**
 * Project:CleanBT
 * Create Time: 16-12-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Message {


    public static final int ERRNO_NORMAL = 201;
    public static final int ERRNO_SERVICE = 202;
    public static final int ERRNO_PROTOCOL = 203;
    public static final int ERRNO_UNKNOWN = 204;


    public final String method;
    public final DictionaryNode arg;
    public final Token token;

    /**
     * TODO 将现在的请求参数改为使用Message，增加类型
     *
     * @param method
     * @param arg
     * @param token
     */
    public Message(String method, DictionaryNode arg, Token token) {
        this.method = method;
        this.arg = arg;
        this.token = token;
    }


    /**
     * 判断给予的内容是不是Krpc的响应
     *
     * @param resp
     * @return 如果为空, 则返回false, 不为空, 则判断 y值时不时r
     */
    public static boolean isResponse(DictionaryNode resp) {
        return resp != null && "r".equals(resp.getNode("y").toString());
    }

    /**
     * 判断给予的内容是不是Krpc的请求
     *
     * @param request
     * @return 如果为空, 则返回false, 不为空, 则判断 y值时不时r
     */
    public static boolean isRequest(DictionaryNode request) {
        return request != null && "q".equals(request.getNode("y").toString());
    }

    /**
     * 判断给予的内容是不是Krpc的错误响应
     *
     * @param resp
     * @return 如果为空, 则返回false, 不为空, 则判断 y值时不时r
     */
    public static boolean isError(DictionaryNode resp) {
        return resp != null && "e".equals(resp.getNode("y").toString());
    }

    public static DictionaryNode makeRequest(NodeKey target, String method) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", new StringNode(TokenManager.newToken(target, method).id + ""));
        node.addNode("y", new StringNode("q"));
        node.addNode("q", new StringNode(method));
        return node;
    }


    public static DictionaryNode makeResponse(Node t) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", t);
        node.addNode("y", new StringNode("r"));
        return node;
    }

    public static DictionaryNode makeError(Node t, int errno, String msg) {
        DictionaryNode node = new DictionaryNode();
        node.addNode("t", t);
        node.addNode("y", new StringNode("e"));
        ListNode e = new ListNode();
        e.addNode(new IntNode(errno));
        e.addNode(new StringNode(msg));
        node.addNode("e", e);
        return node;
    }

    public static DictionaryNode makeArg() {
        DictionaryNode node = new DictionaryNode();
        node.addNode("id", new StringNode(DhtApp.NODE.getSelfKey().getValue()));
        return node;
    }
}
