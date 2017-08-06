package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.bencode.*;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.TokenManager;

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


    public final String address;
    public final int port;
    public final DictNode arg;

    /**
     * TODO 将现在的请求参数改为使用Message，增加类型
     *
     * @param method
     * @param arg
     * @param token
     */
    Message(String address, int port, DictNode arg) {
        this.address = address;
        this.port = port;
        this.arg = arg;
    }


    /**
     * 判断参数内容是不是Krpc的响应
     *
     * @param dict
     * @return 如果为空, 则返回false, 不为空, 则判断y值是不是r
     */
    public static boolean isResp(DictNode dict) {
        return dict != null && is(dict, "y", "r");
    }

    /**
     * 判断参数内容是不是Krpc的请求
     *
     * @param dict
     * @return 如果为空, 则返回false, 不为空, 则判断 y值是不是q
     */
    public static boolean isReq(DictNode dict) {
        return dict != null && is(dict, "y", "q");
    }

    /**
     * 判断给予的内容是不是Krpc的错误响应
     *
     * @param dict
     * @return 如果为空, 则返回false, 不为空, 则判断 y值是不是e
     */
    public static boolean isErr(DictNode dict) {
        return dict != null && is(dict, "y", "e");
    }

    private static boolean is(DictNode dict, String key, String value) {
        return value.equals(dict.getNode(key).toString());
    }

    public static DictNode makeReq(NodeId target, String method) {
        DictNode node = new DictNode();
        node.addNode("t", new StringNode(TokenManager.newToken(target, method).id + ""));
        node.addNode("y", new StringNode("q"));
        node.addNode("q", new StringNode(method));
        return node;
    }


    public static DictNode makeResp(Node t) {
        DictNode node = new DictNode();
        node.addNode("t", t);
        node.addNode("y", new StringNode("r"));
        return node;
    }

    public static DictNode makeErr(Node t, int errno, String msg) {
        DictNode node = new DictNode();
        node.addNode("t", t);
        node.addNode("y", new StringNode("e"));
        ListNode e = new ListNode();
        e.addNode(new IntNode(errno));
        e.addNode(new StringNode(msg));
        node.addNode("e", e);
        return node;
    }
}
