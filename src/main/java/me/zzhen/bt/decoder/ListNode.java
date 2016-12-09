package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

    static final char LIST_START = 'l';
    static final char LIST_END = 'e';

    List<Node> value = new ArrayList<>();

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
        return value.toString();//去掉Gson依赖
//        return new Gson().toJson(value.toString());
    }

    @Override
    public byte[] encode()  {
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
    public String decode() {
        return value.toString();
    }

}
