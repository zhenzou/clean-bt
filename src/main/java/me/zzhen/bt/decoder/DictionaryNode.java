package me.zzhen.bt.decoder;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.VarHandle;
import java.util.*;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public class DictionaryNode implements Node {


    static final char DIC_START = 'd';
    static final char DIC_END = 'e';


    private Map<String, Node> mValue = new HashMap<>();


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
        mValue.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            try {
                baos.write(new StringNode(entry.getKey().getBytes()).encode());
                baos.write(entry.getValue().encode());
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
//        return new Gson().toJson(mValue.toString());
    }
}
