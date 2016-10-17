package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/16.
 */
public class IntNode implements Node {

    static final char INT_START = 'i';
    static final char INT_END = 'e';

    String mValue;

    public IntNode(String value) {
        mValue = value;
    }


    @Override
    public String encode() {
        return "" + INT_START + mValue + INT_END;
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
