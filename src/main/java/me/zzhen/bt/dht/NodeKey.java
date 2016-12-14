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


    public static NodeKey generateKey() {
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

        byte[] bytes = intToBytes(100);
        for (byte aByte : bytes) {
            System.out.println(aByte);
        }
        System.out.println(bytesToInt(bytes));
    }
}
