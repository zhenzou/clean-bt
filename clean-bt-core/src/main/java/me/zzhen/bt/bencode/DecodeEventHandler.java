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
public interface DecodeEventHandler {
    default void whenInteger(IntNode value) {
    }

    default void whenString(StringNode value) {
    }

    default void whenList(ListNode value) {
    }

    default void whenDictionary(String key, Node value) {
    }
}
