package me.zzhen.bt.decoder;

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
    public String encode() {
        return new StringBuilder().append(INT_START).append(mValue).append(INT_END).toString();
    }

    @Override
    public String decode() {
        return mValue;
    }

    @Override
    public String toString() {
        return mValue;
    }

}
