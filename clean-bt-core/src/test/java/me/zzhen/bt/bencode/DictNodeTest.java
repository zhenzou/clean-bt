package me.zzhen.bt.bencode;


import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Project:CleanBT
 * Create Time: 16-12-29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DictNodeTest {

    private DictNode intDict;
    private DictNode stringDict;
    private DictNode dictDict;
    private DictNode listDict;
    private String dictInt = "d3:keyi10ee";
    private String dictString = "d3:key5:valuee";
    private String dictDicS = "d3:keyd3:keyi10eee";
    private String dictList = "d3:keyli10eee";

    @Before
    public void init() throws IOException {
        intDict = Bencode.decodeDict(new ByteArrayInputStream(dictInt.getBytes()));
        stringDict = Bencode.decodeDict(new ByteArrayInputStream(dictString.getBytes()));
        dictDict = Bencode.decodeDict(new ByteArrayInputStream(dictDicS.getBytes()));
        listDict = Bencode.decodeDict(new ByteArrayInputStream(dictList.getBytes()));
    }

    @Test
    public void encode() throws Exception {
        assertEquals(dictInt, new String(intDict.encode()));
        assertEquals(dictDicS, new String(dictDict.encode()));
        assertEquals(dictString, new String(stringDict.encode()));
        assertEquals(dictList, new String(listDict.encode()));
    }

    @Test
    public void decode() throws Exception {
        assertEquals("{key:10}", new String(intDict.decode()));
        assertEquals("{key:value}", new String(stringDict.decode()));
        assertEquals("{key:{key:10}}", new String(dictDict.decode()));
        assertEquals("{key:[10]}", new String(listDict.decode()));
    }
}