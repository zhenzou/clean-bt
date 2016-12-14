package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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

}
