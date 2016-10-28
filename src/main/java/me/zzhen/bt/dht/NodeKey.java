package me.zzhen.bt.dht;

import java.util.Arrays;

/**
 * Project:CleanBT
 * Create Time: 2016/10/28.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeKey implements Comparable<NodeKey> {

    private byte[] mValue = new byte[20];

    public NodeKey(byte[] val) {
        if (val.length != 20) {
            throw new IllegalArgumentException("the key require 20 byte");
        } else {
            mValue = val;
        }
    }

    public NodeKey xor(NodeKey other) {
        byte[] val = new byte[20];
        for (int i = 0; i < 20; i++) {
            val[i] = (byte) (mValue[i] ^ other.mValue[i]);
        }
        return new NodeKey(val);
    }

    public byte[] getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeKey nodeKey = (NodeKey) o;
        return Arrays.equals(mValue, nodeKey.mValue);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mValue);
    }

    @Override
    public int compareTo(NodeKey o) {
        byte[] val1 = mValue;
        byte[] val2 = o.mValue;
        for (int i = 0; i < 20; i++) {
            if (val1[i] > val2[i]) {
                return 1;
            } else if (val1[i] < val2[i]) {
                return -1;
            }
        }
        return 0;
    }
}
