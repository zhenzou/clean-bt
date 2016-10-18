package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/17.
 */
public interface EventHandler {
    default Node handleIntNode(IntNode value) {
        return value;
    }

    default Node handleStringNode(StringNode value) {
        return value;
    }

    default Node handleListNode(ListNode value) {
        return value;
    }

    default Node handleDictionaryNode(String key, Node value) {
        return value;
    }
}
