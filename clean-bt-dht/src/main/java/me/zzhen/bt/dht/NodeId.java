package me.zzhen.bt.dht;

import me.zzhen.bt.common.Bitmap;
import me.zzhen.bt.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static me.zzhen.bt.util.Utils.int2Bytes;

/**
 * Project:CleanBT
 * Create Time: 2016/10/28.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */

public class NodeId implements Comparable<NodeId> {


    public static NodeId randomId() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 5; i++) {
            byte[] bytes = int2Bytes((int) (Math.random() * Integer.MAX_VALUE));
            try {
                baos.write(bytes);
            } catch (IOException ignored) {
            }
        }
        return new NodeId(baos.toByteArray());
    }

    public static NodeId defaultId() {
        return new NodeId("ThisIsaDefaultNodeId".getBytes());
    }

    public static NodeId distance(NodeId lhs, NodeId rhs) {
        byte[] val = new byte[20];
        for (int i = 0; i < 20; i++) {
            val[i] = (byte) (lhs.value.getData()[i] ^ rhs.value.getData()[i]);
        }
        return new NodeId(val);
    }

    private Bitmap value;

    public NodeId(byte[] val) {
        if (val.length != 20) {
            throw new IllegalArgumentException("the id require 20 byte");
        } else {
            value = new Bitmap(val);
        }
    }

    public NodeId(Bitmap val) {
        if (val.size != 160) {
            throw new IllegalArgumentException("the id require 20 byte");
        } else {
            value = val;
        }
    }

    public NodeId distance(NodeId other) {
        return distance(this, other);
    }

    /**
     * @param i 0～159
     * @return 第 i 个 bit的值 0 or 1
     */
    public boolean prefix(int i) {
        return value.at(i);
    }

    @Override
    public String toString() {
//        return Utils.toHex(value.getData());
        return new String(value.getData());
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
    public int compareTo(NodeId o) {
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
        NodeId nodeId = (NodeId) o;
        return Objects.equals(value, nodeId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static void main(String[] args) {

        byte[] bytes = int2Bytes(100);
        for (byte aByte : bytes) {
            System.out.println(aByte);
        }
        System.out.println(Utils.bytes2Int(bytes));
    }
}
