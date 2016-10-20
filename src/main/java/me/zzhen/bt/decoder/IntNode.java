package me.zzhen.bt.decoder;

import java.io.*;

/**
 * Created by zzhen on 2016/10/16.
 */
public class IntNode implements Node {

    public static final char INT_START = 'i';
    public static final char INT_END = 'e';

    String mValue;

    public IntNode(String value) {
        mValue = value;
    }


    @Override
    public byte[] encode() throws UnsupportedEncodingException {
//        return new StringBuilder().append(INT_START).append(mValue).append(INT_END).toString().getBytes();
        return new StringBuilder().append(INT_START).append(mValue).append(INT_END).toString().getBytes("UTF-8");
    }

    @Override
    public String decode() {
        return mValue;
    }

    @Override
    public String toString() {
        return mValue;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        IntNode intNode = new IntNode("123456789");
        try {
            OutputStream out = new FileOutputStream("d:/test.text");
            out.write(intNode.encode());
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
