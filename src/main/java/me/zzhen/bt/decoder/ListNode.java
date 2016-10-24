package me.zzhen.bt.decoder;

import com.google.gson.Gson;

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

    public Node removeNode(int index) {
        return mValue.remove(index);
    }

    public Node get(int index) {
        return mValue.get(index);
    }

    public int size() {
        return mValue.size();
    }

    @Override
    public String toString() {
        return new Gson().toJson(mValue.toString());
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) LIST_START);
        mValue.forEach(node -> {
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
        return mValue.toString();
    }

}
