package me.zzhen.bt.bencode;

import java.io.InputStream;

/**
 * Project:CleanBT
 * Create Time: 16-12-27.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface ErrorHandler {
    int skip(int pos, char cur, InputStream input);

    default boolean toNext() {
        return false;
    }
}
