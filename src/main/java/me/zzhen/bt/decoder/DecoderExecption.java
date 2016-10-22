package me.zzhen.bt.decoder;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 * Create Time: 2016/10/16.
 * Version :
 * Description:
 */

public class DecoderExecption extends RuntimeException {
    public DecoderExecption() {
        super("not a legal ben coding input");
    }

    public DecoderExecption(String msg) {
        super(msg);
    }
}
