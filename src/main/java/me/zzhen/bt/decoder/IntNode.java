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
    public String decode() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
