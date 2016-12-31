package me.zzhen.bt.bencode;

import me.zzhen.bt.utils.Utils;

import java.io.*;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public class IntNode implements Node {

    public static IntNode decode(InputStream input) throws IOException {
        int c = -1;
        if ((c = input.read()) != -1 && c != INT_START) throw new IllegalArgumentException("IntNode must start with i");
        int pos = 1;
        StringBuilder sb = new StringBuilder();
        while ((c = input.read()) != -1 && c != IntNode.INT_END) {
            pos++;
            char cc = (char) c;
            if (Character.isDigit(cc)) {
                sb.append(cc);
            } else {
                throw new DecoderException("expect a digital in " + pos + " but found " + cc);
            }
        }
        return new IntNode(sb.toString());
    }


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
     *
     * @return
     */
    @Override
    public byte[] encode() {
        try {
            return (String.valueOf(INT_START) + value + INT_END).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return (String.valueOf(INT_START) + value + INT_END).getBytes();
    }

    @Override
    public byte[] decode() {
        return Utils.intToBytes(Integer.valueOf(value));
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
