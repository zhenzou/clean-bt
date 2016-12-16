package me.zzhen.bt.utils;

import me.zzhen.bt.decoder.Decoder;
import me.zzhen.bt.decoder.Node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Project:CleanBT
 * Create Time: 2016/10/25.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface Utils {

    static String randomDigitalName() {
        return UUID.randomUUID().toString();
    }

    /**
     * 取扩展名
     *
     * @param file
     * @return 没有扩展名则返回全部
     */
    static String getExtName(String file) {
        int index = file.lastIndexOf(".");
        if (index > 0) {
            return file.substring(index + 1);
        } else {
            return file;
        }
    }

    static byte[] SHA_1(byte[] input) {
        try {
            MessageDigest sha_1 = MessageDigest.getInstance("SHA-1");
            sha_1.update(input);
            return sha_1.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String toHex(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            int i = (b >> 4) & 0x0f;
            if (i > 9) {
                sb.append((char) (i + 55));
            } else {
                sb.append((char) (i + 48));
            }
            i = b & 0x0f;
            if (i > 9) {
                sb.append((char) (i + 55));
            } else {
                sb.append((char) (i + 48));
            }
        }
        return sb.toString();
    }

    /**
     * if (len(input)<offset+length) return toHex(input[offset,input.length])
     *
     * @param input
     * @param offset
     * @param length max length
     * @return
     */
    static String toHex(byte[] input, int offset, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        length = offset + length;
        int len = input.length;
        for (int i = offset; i < len && i < length; i++) {
            baos.write(input[i]);
        }
        return toHex(baos.toByteArray());
    }


    /**
     * Big Endian
     *
     * @param num
     * @return
     */
    static byte[] int2Bytes(int num) {
        byte[] bytes = new byte[4];
        int mask = 0xFF;
        for (int i = 0; i < 4; i++) {
            bytes[3 - i] = (byte) (num >>> 8 * i);
        }
        return bytes;
    }


    /**
     * @param bytes Big Endian
     * @return
     */
    static int bytes2Int(byte[] bytes) {
        return bytes2Int(bytes, 0, bytes.length);
    }

    /**
     * @param bytes   小端，需要reverse
     * @param reverse 标志是否reverse
     * @return
     */
    static int bytes2Int(byte[] bytes, boolean reverse) {
        if (reverse) {
            reverseArray(bytes);
        }
        return bytes2Int(bytes, 0, bytes.length);
    }

    static int bytes2Int(byte[] bytes, int offset, int length) {
        int len = bytes.length;
        length = offset + length;
        int ret = 0;
        for (int i = offset; i < len && i < length; i++) {
            ret |= (bytes[i] & 0xFF);//暂时
            if (i < length - 1) {
                ret <<= 8;
            }
        }
        return ret;
    }

    static byte[] hex2Bytes(String hex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char[] chars = hex.toCharArray();
        int len = chars.length;
        for (int i = 0; i < len; i += 2) {
            int i1 = getHexInt(chars[i]) * 16;
            int i2 = getHexInt(chars[i + 1]);
            baos.write((byte) (i1 + i2));
        }
        return baos.toByteArray();
    }

    static int getHexInt(char c) {
        int i = -1;
        if (c > 'f') return -1;
        switch (Character.toUpperCase(c)) {
            case 'A':
                i = 10;
                break;
            case 'B':
                i = 11;
                break;
            case 'C':
                i = 12;
                break;
            case 'D':
                i = 13;
                break;
            case 'E':
                i = 14;
                break;
            case 'F':
                i = 15;
                break;
            default:
                i = Integer.parseInt(c + "");
        }
        return i;
    }

    static byte[] ipToBytes(String ip) {
        String[] addrs = ip.split("\\.");
        assert addrs.length == 4;
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int a = Integer.parseInt(addrs[i]);
            bytes[i] = (byte) a;
        }
        return bytes;
    }

    static <T> void reverseArray(T[] ts) {
        int len = ts.length;
        int halfLen = len / 2;
        len--;
        for (int i = 0; i < halfLen; i++) {
            swap(ts, i, len - i);
        }
    }

    static void reverseArray(int[] ts) {
        int len = ts.length;
        int halfLen = len / 2;
        len--;
        for (int i = 0; i < halfLen; i++) {
            swap(ts, i, len - i);
        }
    }

    static void reverseArray(byte[] ts) {
        int len = ts.length;
        int halfLen = len / 2;
        len--;
        for (int i = 0; i < halfLen; i++) {
            swap(ts, i, len - i);
        }
    }

    static <T> void swap(T[] ts, int i, int j) {
        T tmp = ts[i];
        ts[i] = ts[j];
        ts[j] = tmp;
    }

    static void swap(int[] ts, int i, int j) {
        int tmp = ts[i];
        ts[i] = ts[j];
        ts[j] = tmp;
    }

    static void swap(byte[] ts, int i, int j) {
        byte tmp = ts[i];
        ts[i] = ts[j];
        ts[j] = tmp;
    }
}