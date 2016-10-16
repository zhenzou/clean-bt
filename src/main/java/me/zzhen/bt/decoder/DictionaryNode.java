package me.zzhen.bt.decoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zzhen on 2016/10/16.
 */
public class DictionaryNode extends Node {

    Map<String, Node> mValue = new HashMap<>();


    public DictionaryNode() {
    }

    public DictionaryNode(Map<String, Node> map) {
        mValue = map;
    }

    public void addNode(String key, Node value) {
        mValue.put(key, value);
    }

    public Node removeNode(String key) {
        return mValue.remove(key);
    }

    @Override
    public String toString() {
        return mValue.toString();
    }
}
