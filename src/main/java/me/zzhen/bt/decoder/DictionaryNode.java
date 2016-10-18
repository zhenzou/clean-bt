package me.zzhen.bt.decoder;

import javax.management.MBeanAttributeInfo;
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
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(DIC_START);
        mValue.forEach((key, node) -> sb.append(new StringNode(key).encode() + node.encode()));
        sb.append(DIC_END);
        return sb.toString();
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
