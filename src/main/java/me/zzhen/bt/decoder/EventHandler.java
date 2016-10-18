package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/17.
 */
public interface EventHandler {
    Node handleIntNode(IntNode value);

    Node handleStringNode(StringNode value);

    Node handleListNode(ListNode value);

    Node handleDictionaryNode(String key, Node value);
}
