package me.zzhen.bt.bencode;

import java.io.*;
import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 17-5-22.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class Bencode {
    public static List<Node> decode(byte[] input) throws IOException {
        return decode(new ByteArrayInputStream(input));
    }

    public static List<Node> decode(byte[] input, int offset, int length) throws IOException {
        return decode(new ByteArrayInputStream(input, offset, length));
    }

    public static List<Node> decode(File file) throws IOException {
        return decode(new FileInputStream(file));
    }

    public static List<Node> decode(InputStream input) throws IOException {
        Decoder decoder = new Decoder(input);
        decoder.decode();
        return decoder.getValue();
    }

    public static DictNode decodeDict(InputStream input) throws IOException {
        if (firstNotSpace(input) != DictNode.DIC_START)
            throw new IllegalArgumentException("first char should be " + DictNode.DIC_START);
        return new Decoder(input).decodeDic();
    }

    public static ListNode decodeList(InputStream input) throws IOException {
        if (firstNotSpace(input) != ListNode.LIST_START)
            throw new IllegalArgumentException("first char should be " + ListNode.LIST_START);

        return new Decoder(input).decodeList();
    }

    public static IntNode decodeInt(InputStream input) throws IOException {
        if (firstNotSpace(input) != IntNode.INT_START)
            throw new IllegalArgumentException("first char should be " + IntNode.INT_START);
        return new Decoder(input).decodeInt();
    }

    public static StringNode decodeString(InputStream input) throws IOException {
        char c = firstNotSpace(input);
        if (!Character.isDigit(c))
            throw new IllegalArgumentException("first char should be digital");
        return new Decoder(input).decodeString(c);
    }

    private static char firstNotSpace(InputStream input) throws IOException {
        int c = -1;
        while ((c = input.read()) != -1 && Character.isWhitespace(c)) {
            //skip
        }
        if (c == -1) {
            throw new IllegalArgumentException("empty input");
        }
        return (char) c;
    }

}
