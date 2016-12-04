package me.zzhen.bt.decoder;

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

    static final char INT_START = 'i';
    static final char INT_END = 'e';

    private String value;

    public IntNode(String value) {
        this.value = value;
    }

    public IntNode(long value) {
        this.value = value + "";
    }


    @Override
    public byte[] encode() throws UnsupportedEncodingException {
//        return new StringBuilder().append(INT_START).append(value).append(INT_END).toString().getBytes();
        return new StringBuilder().append(INT_START).append(value).append(INT_END).toString().getBytes("UTF-8");
    }

    @Override
    public String decode() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
