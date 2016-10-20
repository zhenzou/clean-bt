package me.zzhen.bt.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author zzhen
 *         Created by zzhen on 2016/10/16.
 */
public class StringNode implements Node {

    static final char STRING_VALUE_START = ':';

    byte[] mValue;

    public StringNode(byte[] value) {
        mValue = value;
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(String.valueOf(mValue.length).getBytes());
            baos.write((byte) STRING_VALUE_START);
            baos.write(mValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    @Override
    public String decode() {
        return new String(mValue);
    }

    @Override
    public String toString() {
        return new String(mValue);
    }

}
