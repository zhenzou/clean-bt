package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/16.
 */
public class StringNode extends Node {
    String mValue;

    public StringNode(String value) {
        mValue = value;
    }

    @Override
    public String toString() {

        return mValue;
    }
}
