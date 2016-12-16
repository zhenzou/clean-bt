package me.zzhen.bt.dht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static me.zzhen.bt.utils.Utils.*;

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
            byte[] bytes = int2Bytes((int) (Math.random() * Integer.MAX_VALUE));
            try {
                baos.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new NodeKey(baos.toByteArray());
    }

    /**
     * TODO 加载以前的ID，保持自己的节点ID一致
     *
     * @return
     */
    public static NodeKey loadOldKey() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 5; i++) {
            byte[] bytes = int2Bytes((int) (Math.random() * Integer.MAX_VALUE));
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
            val[i] = (byte) (lhs.value[i] ^ rhs.value[i]);
        }
        return new NodeKey(val);
    }

    private byte[] value = new byte[20];

    public NodeKey(byte[] val) {
        if (val.length != 20) {
            throw new IllegalArgumentException("the key require 20 byte");
        } else {
            value = val;
        }
    }

    public NodeKey xor(NodeKey other) {
        byte[] val = new byte[20];
        for (int i = 0; i < 20; i++) {
            val[i] = (byte) (value[i] ^ other.value[i]);
        }
        return new NodeKey(val);
    }

    public boolean prefix(int i) {
        if (i >= 160 || i < 1) throw new RuntimeException("prefix of node should smaller than 160 and bigger than 1");

        return value[(i - 1) / 8] >>> i % 8 == 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(20);
        byte[] value = this.value;
        for (byte b : value) {
            sb.append(b);
        }
        return sb.toString();
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public int compareTo(NodeKey o) {
        byte[] val1 = value;
        byte[] val2 = o.value;
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
        return Arrays.equals(value, nodeKey.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    public static void main(String[] args) {

        byte[] bytes = int2Bytes(100);
        for (byte aByte : bytes) {
            System.out.println(aByte);
        }
        System.out.println(bytes2Int(bytes));
    }
}
