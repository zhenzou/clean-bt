package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/16.
 */
public class StringNode implements Node {

    static final char STRING_VALUE_START = ':';

    String mValue;

    public StringNode(String value) {
        mValue = value;
    }


    @Override
    public String encode() {
        return new StringBuilder().append(mValue.length()).append(STRING_VALUE_START).append(mValue).toString();
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
