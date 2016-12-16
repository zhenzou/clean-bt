package me.zzhen.bt.bencode;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/17.
 *         Version :
 *         Description:
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
