package me.zzhen.bt.bencode;

import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 16-12-31.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DecoderTest {
    @Test
    public void decode() throws Exception {
        List<Node> decode = Decoder.decode(new File("/media/Software/Chicago.Med.torrent"));
        System.out.println(decode.toString());
    }

}