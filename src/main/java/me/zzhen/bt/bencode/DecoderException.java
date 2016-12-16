package me.zzhen.bt.bencode;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */

public class DecoderException extends RuntimeException {

    public DecoderException() {
        super("not a legal ben coding input");
    }

    public DecoderException(String msg) {
        super(msg);
    }
}
