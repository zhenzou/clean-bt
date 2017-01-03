package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
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

    public static DictionaryNode decode(InputStream input) throws IOException {
        PushbackInputStream push = new PushbackInputStream(input);
        int c = push.read();
        if (c == -1 || c != DIC_START) throw new DecoderException("dic should start with " + DIC_START);
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        while ((c = push.read()) != -1 && (char) c != DictionaryNode.DIC_END) {
            Node node = null;
            char cur = (char) c;
            if (inKey) {
                if (Character.isDigit(cur)) {
                    push.unread(c);
                    key = StringNode.decode(push).toString();
                    inKey = false;
                } else {
                    throw new DecoderException("key of dic must be string,found digital");
                }
            } else {
                node = decodeNext(push, cur);
                dic.addNode(key, node);
                inKey = true;
            }
        }
        return dic;
    }

    /**
     * 解析下一个Node
     * TODO 增加错误位置
     *
     * @param c 当前输入的字符 应该是 i,d,l或者数字
     * @return 当前节点的Node结构
     * @throws IOException
     */
    public static Node decodeNext(PushbackInputStream input, char c) throws IOException {
        Node node = null;
        input.unread(c);
        switch (c) {
            case IntNode.INT_START:
                node = IntNode.decode(input);
                break;
            case ListNode.LIST_START:
                node = ListNode.decode(input);
                break;
            case DictionaryNode.DIC_START:
                node = decode(input);
                break;
            default:
                if (Character.isDigit(c)) {
                    node = StringNode.decode(input);
                } else {
                    throw new DecoderException("not a legal char");
                }
                break;
        }
        return node;
    }


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

        DictionaryNode node = (DictionaryNode) o;

        return value.equals(node.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
