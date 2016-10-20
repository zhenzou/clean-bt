package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zzhen on 2016/10/16.
 */
public class DictionaryNode implements Node {


    static final char DIC_START = 'd';
    static final char DIC_END = 'e';


    Map<String, Node> mValue = new HashMap<>();


    public DictionaryNode() {
    }

    public DictionaryNode(Map<String, Node> map) {
        mValue = map;
    }

    public Map<String, Node> getValue() {
        return mValue;
    }

    public void addNode(String key, Node value) {
        mValue.put(key, value);
    }

    public Node removeNode(String key) {
        return mValue.remove(key);
    }


    public Node getNode(String key) {
        return mValue.get(key);
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) DIC_START);
        mValue.forEach((key, node) -> {
            try {
                baos.write(new StringNode(key.getBytes()).encode());
                baos.write(node.encode());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        baos.write((byte) DIC_END);
        return baos.toByteArray();
    }

    @Override
    public String decode() {
        return mValue.toString();
    }

    @Override
    public String toString() {
        return mValue.toString();
    }


}
