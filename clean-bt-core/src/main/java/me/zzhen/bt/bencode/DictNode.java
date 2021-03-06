package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzhen zzzhen1994@gmail.com
 * Create Time: 2016/10/16.
 */
public class DictNode implements Node {

    static final char DIC_START = 'd';
    static final char DIC_END = 'e';

    private Map<String, Node> value = new HashMap<>();

    public DictNode() {
    }

    public DictNode(Map<String, Node> value) {
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
        String format = "{%s}";
        String content = value.entrySet()
                .stream()
                .map((entry -> entry.getKey() + ":" + new String(entry.getValue().decode()))).collect(Collectors.joining(","));
        return String.format(format, content).getBytes();
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DictNode node = (DictNode) o;

        return value.equals(node.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
