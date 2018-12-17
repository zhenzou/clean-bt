package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zzhen zzzhen1994@gmail.com
 * Create Time: 2016/10/16.
 */
public class ListNode implements Node {

    static final char LIST_START = 'l';
    static final char LIST_END = 'e';

    private List<Node> value = new ArrayList<>();

    public ListNode() {
    }

    public ListNode(List<Node> value) {
        this.value = value;
    }

    public List<Node> getValue() {
        return value;
    }

    public void addNode(Node node) {
        value.add(node);
    }

    public Node removeNode(int index) {
        return value.remove(index);
    }

    public Node get(int index) {
        return value.get(index);
    }

    public int size() {
        return value.size();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) LIST_START);
        value.forEach(node -> {
            try {
                baos.write(node.encode());
            } catch (IOException ignore) {
                // ignore
            }
        });
        baos.write((byte) LIST_END);
        return baos.toByteArray();
    }

    @Override
    public byte[] decode() {
        String format = "[%s]";
        String content = value.stream()
                .map((item -> new String(item.decode()))).collect(Collectors.joining(","));
        return String.format(format, content).getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListNode listNode = (ListNode) o;

        return value.equals(listNode.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
