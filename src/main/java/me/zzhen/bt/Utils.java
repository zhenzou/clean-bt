package me.zzhen.bt;

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
public class Utils {

    public static String randomDigitalName() {
        return UUID.randomUUID().toString();
    }

    /**
     * 取扩展名
     *
     * @param file
     * @return 没有扩展名则返回全部
     */
    public static String getExtName(String file) {
        return file.substring(file.lastIndexOf('.') + 1);
    }

    public static byte[] SHA_1(byte[] input) {
        try {
            MessageDigest sha_1 = MessageDigest.getInstance("SHA-1");
            sha_1.update(input);
            byte[] digest = sha_1.digest();
            return digest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toHex(byte[] input) {
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

}
