package me.zzhen.bt.bencode;

import java.io.UnsupportedEncodingException;

/**
 * @author zzhen zzzhen1994@gmail.com
 * Create Time: 2016/10/16.
 */
public class IntNode implements Node {

    public static final char INT_START = 'i';
    public static final char INT_END = 'e';

    private String value;

    public IntNode(String value) {
        this.value = value;
    }

    public IntNode(long value) {
        this.value = value + "";
    }


    /**
     * 返回UTF-8编码的字节数组，如果异常则返回默认编码数组
     */
    @Override
    public byte[] encode() {
        try {
            return (INT_START + value + INT_END).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return (INT_START + value + INT_END).getBytes();
    }

    @Override
    public byte[] decode() {
        return value.getBytes();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() && o.getClass() != Integer.class) return false;
        if (o instanceof Integer) return Integer.valueOf(value).equals(o);
        IntNode intNode = (IntNode) o;
        return value != null ? value.equals(intNode.value) : intNode.value == null;
    }
}
