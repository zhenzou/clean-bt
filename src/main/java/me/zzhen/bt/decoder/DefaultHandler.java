package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/17.
 */
public class DefaultHandler implements EventHandler {
    @Override
    public Node handleIntNode(IntNode value) {
//        System.out.println(value);
        return value;
    }

    @Override
    public Node handleStringNode(StringNode value) {
//        System.out.println(value);
        return value;
    }

    @Override
    public Node handleListNode(ListNode value) {
//        System.out.println(value);
        return value;
    }

    @Override
    public Node handleDictionaryNode(String key, Node value) {
        return value;
    }
}
