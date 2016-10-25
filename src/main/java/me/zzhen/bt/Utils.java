package me.zzhen.bt;

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
        return file.substring(file.lastIndexOf(".") + 1);
    }
}
