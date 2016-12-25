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

    public void set(int index, boolean val) {
        int pos = index / 8;
        int mod = index % 8;
        byte tmp = data[pos];
        byte i = tmp;
//        byte i = (byte) (tmp << 7 - mod);
        if (val) {
            i |= (1 << (7 - mod));
            data[pos] = (byte) (i | tmp);
        } else {
            i = (byte) ((~i) | (1 << (7 - mod)));
            data[pos] = (byte) ~i;
        }
    }

    public void or(Bitmap other) {
        int minsize = size > other.size ? other.size : size;
        int len = minsize / 8;
        if (minsize % 8 != 0) {
            len++;
        }
        for (int i = 0; i < len; i++) {
            data[i] |= other.data[i];
        }
    }

    public void and(Bitmap other) {
        int minsize = size > other.size ? other.size : size;
        int len = minsize / 8;
        if (minsize % 8 != 0) {
            len++;
        }
        for (int i = 0; i < len; i++) {
            data[i] &= other.data[i];
        }
    }

    /**
     * @param index
     */
    public void clear(int index) {
        set(index, false);
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
