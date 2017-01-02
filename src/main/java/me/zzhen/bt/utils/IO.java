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


    /**
     * 用BufferedReader读取所有行，并且在行尾添加换行符
     *
     * @param input
     * @return
     */
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

    /**
     * 读取输入中的前k个字节数组
     *
     * @param input 输入流
     * @param k     第几个数据
     * @return
     */
    static byte[] readKBytes(InputStream input, int k) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(k);
        int c = -1;
        int count = 0;
        try {
            while (count < k && (c = input.read()) != -1) {
                baos.write(c);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    /**
     * 用Buffered读取全部数据
     *
     * @param input
     * @return
     */
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


}
