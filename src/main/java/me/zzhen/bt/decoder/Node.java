package me.zzhen.bt.decoder;

import java.io.UnsupportedEncodingException;

/**
 * Created by zzhen on 2016/10/16.
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
