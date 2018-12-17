package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author zzhen zzzhen1994@gmail.com
 * Create Time: 2016/10/16.
 */
public class StringNode implements Node {

    static final char STRING_VALUE_START = ':';

    private byte[] value;

    /**
     * 不用处理编码问题
     *
     * @param value
     */
    public StringNode(byte[] value) {
        this.value = value;
    }

    /**
     * 默认使用UTF-8
     *
     * @param value
     */
    public StringNode(String value) {
        try {
            this.value = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(String.valueOf(value.length).getBytes());
            baos.write((byte) STRING_VALUE_START);
            baos.write(value);
        } catch (IOException ignore) {
            // ignore
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] decode() {
        return value;
    }

    @Override
    public String toString() {
        return new String(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringNode that = (StringNode) o;

        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
