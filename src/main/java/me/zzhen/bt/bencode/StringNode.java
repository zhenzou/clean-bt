package me.zzhen.bt.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public class StringNode implements Node {

    public static StringNode decode(InputStream input) throws IOException {
        int pos = 0;
        int c;
        StringBuilder len = new StringBuilder();

        while ((c = input.read()) != -1 && (char) c != StringNode.STRING_VALUE_START) {
            pos++;
            if (Character.isDigit(c)) {
                len.append((char) c);
            } else {
                throw new DecoderException("expect a digital in " + pos + " but found " + (char) c);
            }
        }
        long length = Long.parseLong(len.toString().trim());
        long i = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (i < length && (c = input.read()) != -1) {
            pos++;
            baos.write(c & 0xFF);
            i++;
        }
        if (i < length) {
            throw new DecoderException("illegal string node , except " + length + " char but found " + i);
        }

        StringNode node = new StringNode(baos.toByteArray());
        return node;
    }

    static final char STRING_VALUE_START = ':';

    byte[] value;

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
        } catch (IOException e) {
            e.printStackTrace();
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
