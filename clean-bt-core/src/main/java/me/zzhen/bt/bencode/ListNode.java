package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public class ListNode implements Node {

    public static ListNode decode(InputStream input) throws IOException {
        int c;
        if ((c = input.read()) == -1 || c != LIST_START)
            throw new IllegalArgumentException("ListNode must start with " + LIST_START);
        ListNode list = new ListNode();
        while ((c = input.read()) != -1 && (char) c != ListNode.LIST_END) {
            char cc = (char) c;
            Node node = DictionaryNode.decodeNext(new PushbackInputStream(input), cc);
            list.addNode(node);
        }
        return list;
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        baos.write((byte) LIST_END);
        return baos.toByteArray();
    }

    @Override
    public byte[] decode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        value.forEach(node -> {
            try {
                baos.write(node.decode());
                baos.write(',');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return baos.toByteArray();
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
