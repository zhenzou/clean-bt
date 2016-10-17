package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/17.
 */
public class DefaultHandler implements EventHandler {
    @Override
    public int handleIntNode(IntNode value) {
        System.out.println(value);
        return Integer.parseInt(value.mValue);
    }

    @Override
    public String handleStringNode(StringNode value) {
        System.out.println(value);
        return value.mValue;
    }

    @Override
    public String handleListNode(ListNode value) {
        System.out.println(value);
        return value.toString();
    }

    @Override
    public void handleDictionaryNode(String key, Node value) {
        System.out.println(value);
    }
}
