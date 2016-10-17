package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/17.
 */
public interface EventHandler {
    int handleIntNode(IntNode value);

    String handleStringNode(StringNode value);

    String handleListNode(ListNode value);

    void handleDictionaryNode(String key, Node value);
}
