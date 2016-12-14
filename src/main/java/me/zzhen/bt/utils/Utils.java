package me.zzhen.bt.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    static byte[] intToBytes(int num) {
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
    static int bytesToInt(byte[] bytes) {
        return bytesToInt(bytes, 0, bytes.length);
    }

    static int bytesToInt(byte[] bytes, int offset, int length) {
        int len = bytes.length;
        length = offset + length;
        int ret = 0;
        for (int i = offset; i < len && i < length; i++) {
            ret |= (bytes[i] & 0xFF);//暂时
            System.out.println(ret);
            if (i < length - 1) {
                ret <<= 8;
            }
            System.out.println(ret);
        }
        return ret;
    }

    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char[] chars = hex.toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            int i1 = getHexInt(chars[i]);
            int i2 = getHexInt(chars[i + 1]);
            baos.write((byte) (i1 + i2));
        }
        return baos.toByteArray();
    }

    static int getHexInt(char c) {
        int i = -1;
        if (c > 15) return -1;
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
                i = c;
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
}