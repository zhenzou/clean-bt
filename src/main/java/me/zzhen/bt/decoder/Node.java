package me.zzhen.bt.decoder;

import java.io.UnsupportedEncodingException;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public interface Node {

    /**
     * @return 返回最基本的byte[] 不用受编码影响
     * @throws UnsupportedEncodingException
     */
    byte[] encode() throws UnsupportedEncodingException;

    /**
     * @return 返回JSON数据
     */
    String decode();
}
