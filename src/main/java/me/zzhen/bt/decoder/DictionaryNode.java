package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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


    private Map<String, Node> value = new HashMap<>();


    public DictionaryNode() {
    }

    public DictionaryNode(Map<String, Node> value) {
        this.value = value;
    }

    public Map<String, Node> getValue() {
        return value;
    }

    public void addNode(String key, Node value) {
        this.value.put(key, value);
    }

    public Node removeNode(String key) {
        return value.remove(key);
    }


    public Node getNode(String key) {
        return value.get(key);
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) DIC_START);
        value.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
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
    public byte[] decode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write('{');
        try {
            for (Map.Entry<String, Node> entry : value.entrySet()) {
                baos.write(entry.getKey().getBytes());
                baos.write(':');
                baos.write(entry.getValue().decode());
                baos.write(',');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        baos.write('}');
        return baos.toByteArray();
    }

    /**
     * 都没有检查null
     *
     * @return
     */
    @Override
    public String toString() {
        return value.toString();
    }
}
