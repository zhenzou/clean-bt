package me.zzhen.bt.utils;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Project:CleanBT
 * Create Time: 16-12-13.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface IO {


    static String readAllLine(InputStream input) {
        String line = null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input));) {
            while ((line = reader.readLine()) != null) {
                sb.append(line + Character.LINE_SEPARATOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    static byte[] readKBytes(InputStream input, int k) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(k);
        int c = -1;
        int count = 0;
        BufferedInputStream buffer = new BufferedInputStream(input);
        try {
            while (count < k && (c = buffer.read()) != -1) {
                baos.write(c);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    static byte[] readAllBytes(InputStream input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c = -1;
        BufferedInputStream buffer = new BufferedInputStream(input);
        try {
            while ((c = buffer.read()) != -1) {
                baos.write(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    static InetAddress getAddrFromBytes(byte[] bytes) {
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 暂时只用IPV4地址
     *
     * @param bytes
     * @param offset
     * @return IPV4 地址
     */
    static InetAddress getAddrFromBytes(byte[] bytes, int offset) {
        byte[] addr = Utils.getSomeByte(bytes, offset, 4);
        return getAddrFromBytes(addr);
    }


}
