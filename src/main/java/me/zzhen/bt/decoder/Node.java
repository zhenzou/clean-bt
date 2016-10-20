package me.zzhen.bt.decoder;

import java.io.UnsupportedEncodingException;

/**
 * Created by zzhen on 2016/10/16.
 */
public interface Node {

    byte[] encode() throws UnsupportedEncodingException;

    String decode();
}
