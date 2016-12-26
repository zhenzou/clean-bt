package me.zzhen.bt.common;

import me.zzhen.bt.utils.Utils;

import java.util.Arrays;

/**
 * Project:CleanBT
 * Create Time: 16-12-25.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Bitmap {
    public final int size;
    private byte[] data;

    public Bitmap(int size) {
        this.size = size;
        int len = size / 8;
        if (size % 8 != 0) {
            data = new byte[len + 1];
        } else {
            data = new byte[len];
        }
    }

    public Bitmap(byte[] data) {
        this.size = data.length * 8;
        this.data = data;
    }

    public boolean get(int index) {
        if (index >= size || index < 0) throw new IllegalArgumentException("too long for this bitmap ");
        return Utils.bitAt(data[index / 8], index % 8) == 1;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * 将index对应的bit设为1
     *
     * @param index
     */
    public void set(int index) {
        set(index, true);
    }

    /**
     * 将指定的bit值设为0
     *
     * @param index
     */
    public void clear(int index) {
        set(index, false);
    }


    /**
     * 将指定位置的bit设为指定的值
     *
     * @param index
     * @param val
     */
    public void set(int index, boolean val) {
        int pos = index / 8;
        int mod = index % 8;
        byte tmp = data[pos];
        byte i = tmp;
        if (val) {
            i |= (1 << (7 - mod));
            data[pos] = (byte) (i | tmp);
        } else {
            i = (byte) ((~i) | (1 << (7 - mod)));
            data[pos] = (byte) ~i;
        }
    }

    /**
     * 按位与，如果长度不一样则按照短的
     *
     * @param other
     */
    public void or(Bitmap other) {
        op(other, OP_OR);

    }

    public void and(Bitmap other) {
        op(other, OP_AND);

    }

    public void xor(Bitmap other) {
        op(other, OP_XOR);
    }

    public static final int OP_OR = 11;
    public static final int OP_AND = 22;
    public static final int OP_XOR = 33;

    /**
     * @param other
     * @param op    操作类型 11 or,22,and,33 xor
     */
    private void op(Bitmap other, int op) {
        int min = size > other.size ? other.size : size;
        int len = min / 8;
        if (min % 8 != 0) len++;
        //optimize
        switch (op) {
            case OP_OR:
                for (int i = 0; i < len; i++) {
                    data[i] |= other.data[i];
                }
                break;
            case OP_AND:
                for (int i = 0; i < len; i++) {
                    data[i] &= other.data[i];
                }
                break;
            case OP_XOR:
                for (int i = 0; i < len; i++) {
                    data[i] ^= other.data[i];
                }
                break;
            default:
                break;
        }

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bitmap bitmap = (Bitmap) o;

        if (size != bitmap.size) return false;
        return Arrays.equals(data, bitmap.data);
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
