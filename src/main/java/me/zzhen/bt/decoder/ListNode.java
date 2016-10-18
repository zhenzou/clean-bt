package me.zzhen.bt.decoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zzhen on 2016/10/16.
 */
public class ListNode implements Node {

    static final char LIST_START = 'l';
    static final char LIST_END = 'e';

    List<Node> mValue = new ArrayList<>();

    public ListNode() {

    }

    public ListNode(List<Node> value) {
        mValue = value;
    }

    public List<Node> getValue() {
        return mValue;
    }

    public void addNode(Node node) {
        mValue.add(node);
    }

    public Node remove(int index) {
        return mValue.remove(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node node : mValue) {
            sb.append(node.toString() + ',');
        }
        sb.deleteCharAt(sb.length() - 1);
        return mValue.toString();
    }

    @Override
    public String encode() {
        return null;
    }

    @Override
    public String decode() {
        return mValue.toString();
    }
}
