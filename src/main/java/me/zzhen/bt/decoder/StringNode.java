package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    public String decode() {
        return new String(value);
    }

    @Override
    public String toString() {
        return new String(value);
    }

}
