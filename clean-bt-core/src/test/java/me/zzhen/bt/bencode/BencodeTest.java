package me.zzhen.bt.bencode;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-5-26.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class BencodeTest {
    @Test
    public void decode() throws Exception {
    }

    @Test
    public void decodeDic() throws Exception {
        String dic1 = "d3:keyi10ee";
        DictNode dict = Bencode.decodeDict(new ByteArrayInputStream(dic1.getBytes()));
        assertEquals(dict.getNode("key").toString(), "10");
        InputStream file = new FileInputStream("/media/Software/Chicago.Med.torrent");
        DictNode bt = Bencode.decodeDict(file);
        System.out.println(bt.getNode("announce").toString());
    }

    @Test
    public void decodeList() throws Exception {
        String list1 = "l3:keyi10ee";
        ListNode list = Bencode.decodeList(new ByteArrayInputStream(list1.getBytes()));
        assertEquals(list.get(0).toString(), "key");
    }

    @Test
    public void decodeInt() throws Exception {
        String i1 = "i10e";
        IntNode i = Bencode.decodeInt(new ByteArrayInputStream(i1.getBytes()));
        assertEquals(i.toString(), "10");
    }

    @Test
    public void decodeString() throws Exception {
        String i1 = "10:0123456789";
        StringNode string = Bencode.decodeString(new ByteArrayInputStream(i1.getBytes()));
        assertEquals(string.toString(), "0123456789");
    }

}