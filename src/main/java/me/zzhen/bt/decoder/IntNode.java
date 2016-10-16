package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/16.
 */
public class IntNode extends Node {

    String mValue;

    public IntNode(String value) {
        mValue = value;
    }

    @Override
    public String toString() {
        return mValue;
    }
}
