package me.zzhen.bt.decoder;

/**
 * Created by zzhen on 2016/10/16.
 */
public class DecoderExecption extends RuntimeException {
    public DecoderExecption() {
        super("not a legal ben coding input");
    }

    public DecoderExecption(String msg) {
        super(msg);
    }
}
