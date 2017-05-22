package me.zzhen.bt.bencode;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-5-22.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DecoderTest {
    @Test
    public void decode() throws Exception {
    }

    @Test
    public void decodeString() throws Exception {
        String s = "10:1234567890";
//        OutputStreamWriter
        Decoder decoder = new Decoder(new ByteArrayInputStream(s.getBytes()));
        String res1 = decoder.decodeString().toString();

        assertEquals("1234567890", res1);
    }

    @Test
    public void decodeDic() throws Exception {
    }

    @Test
    public void decodeList() throws Exception {
    }

    @Test
    public void decodeInt() throws Exception {
    }

}