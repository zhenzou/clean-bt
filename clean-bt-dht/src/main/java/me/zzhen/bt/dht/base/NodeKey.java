package me.zzhen.bt.dht.base;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static me.zzhen.bt.util.Utils.bytesToInt;
import static me.zzhen.bt.util.Utils.intToBytes;

/**
 * Project:CleanBT
 * Create Time: 2016/10/28.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeKey implements Comparable<NodeKey> {


    public static NodeKey genRandomKey() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 5; i++) {
            byte[] bytes = intToBytes((int) (Math.random() * Integer.MAX_VALUE));
            try {
                baos.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new NodeKey(baos.toByteArray());
    }

    public static NodeKey distance(NodeKey lhs, NodeKey rhs) {
        byte[] val = new byte[20];
        for (int i = 0; i < 20; i++) {
            val[i] = (byte) (lhs.value.getData()[i] ^ rhs.value.getData()[i]);
        }
        return new NodeKey(val);
    }

    private Bitmap value;

    public NodeKey(byte[] val) {
        if (val.length != 20) {
            throw new IllegalArgumentException("the key require 20 byte");
        } else {
            value = new Bitmap(val);
        }
    }

    public NodeKey(Bitmap val) {
        if (val.size != 160) {
            throw new IllegalArgumentException("the key require 20 byte");
        } else {
            value = val;
        }
    }

    public NodeKey distance(NodeKey other) {
        return distance(this, other);
    }

    /**
     * @param i 0～159
     * @return 第 i 个 bit的值 0 or 1
     */
    public boolean prefix(int i) {
        return value.get(i);
    }

    @Override
    public String toString() {
        return Utils.toHex(value.getData());
    }

    /**
     * 为了兼容以前的代码,以后有时间重构吧
     * TODO 删除
     *
     * @return
     */
    public byte[] getValue() {
        return value.getData();
    }

    public Bitmap getBits() {
        return value;
    }

    @Override
    public int compareTo(NodeKey o) {
        byte[] val1 = value.getData();
        byte[] val2 = o.value.getData();
        for (int i = 0; i < 20; i++) {
            if (val1[i] > val2[i]) {
                return 1;
            } else if (val1[i] < val2[i]) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeKey nodeKey = (NodeKey) o;
        return Objects.equals(value, nodeKey.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static void main(String[] args) {

        byte[] bytes = intToBytes(100);
        for (byte aByte : bytes) {
            System.out.println(aByte);
        }
        System.out.println(bytesToInt(bytes));
    }
}
