package me.zzhen.bt.decoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zzhen on 2016/10/16.
 */
public class Decoder {

    private Reader mReader;

    private static final char INT_START = 'i';
    private static final char INT_END = 'e';
    private static final char LIST_START = 'l';
    private static final char LIST_END = 'e';
    private static final char LIST_VALUE_START = ':';
    private static final char DIC_START = 'd';
    private static final char DIC_END = 'e';

    private List<Node> mValues = new ArrayList<>();


    public Decoder(String file) throws FileNotFoundException {
        mReader = new FileReader(file);
    }

    public Decoder(InputStream input) throws FileNotFoundException {
        mReader = new InputStreamReader(input);
    }

    public void parse() throws IOException {
        int c;
        while ((c = mReader.read()) != -1) {
            Node node = null;
            char cur = (char) c;
            switch (c) {
                case INT_START:
                    node = parseInt();
                    break;
                case LIST_START:
                    node = parseList();
                    break;
                case DIC_START:
                    node = parseDic();
                    break;
                default:
                    if (Character.isDigit(c)) {
                        node = parseString(c);
                    } else {
                        throw new DecoderExecption("not a legal char");
                    }
                    break;
            }
            mValues.add(node);
        }
    }

    private Node parseString(int cur) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        StringBuilder len = new StringBuilder();
        len.append((char) cur);
        while ((c = mReader.read()) != -1 && (char) c != LIST_VALUE_START) {
            len.append((char) c);
        }
        int length = Integer.parseInt(len.toString().trim());
        int i = 1;
        while ((c = mReader.read()) != -1 && i < length) {
            sb.append((char) c);
            i++;
        }
        sb.append((char) c);
        if (i < length) {
            throw new DecoderExecption("illegal string node , except " + length + " char but found" + i);
        }
        System.out.println(sb.toString());
        return new StringNode(sb.toString());
    }

    private Node parseDic() throws IOException {
        int c;
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        while ((c = mReader.read()) != -1 && (char) c != DIC_END) {
            Node node = null;
            char cur = (char) c;
            if (inKey) {
                if (Character.isDigit(c)) {
                    key = parseString(c).toString();
                    inKey = false;
                } else {
                    throw new DecoderExecption("key of dic must be string,except digital");
                }
            } else {
                switch (cur) {
                    case INT_START:
                        node = parseInt();
                        break;
                    case LIST_START:
                        node = parseList();
                        break;
                    case DIC_START:
                        node = parseDic();
                        break;
                    default:
                        if (Character.isDigit(c)) {
                            node = parseString(c);
                        } else {
                            throw new DecoderExecption("not a legal char");
                        }
                        break;
                }
                inKey = true;
            }
            dic.addNode(key, node);
        }
        System.out.println(dic.toString());
        return dic;
    }

    private Node parseList() throws IOException {
        ListNode list = new ListNode();
        int c;
        while ((c = mReader.read()) != -1 && (char) c != LIST_END) {
            Node node = null;
            char cur = (char) c;
            switch (cur) {
                case INT_START:
                    node = parseInt();
                    break;
                case LIST_START:
                    node = parseList();
                    break;
                case DIC_START:
                    node = parseDic();
                    break;
                default:
                    if (Character.isDigit(c)) {
                        node = parseString(c);
                    } else {
                        throw new DecoderExecption("not a legal char");
                    }
                    break;
            }
            list.addNode(node);
        }
        System.out.println(list.toString());
        return list;
    }

    private Node parseInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        while ((c = mReader.read()) != -1 && c != INT_END) {
            sb.append((char) c);
        }
        System.out.println(sb.toString());
        return new IntNode(sb.toString());
    }

    public List<Node> getValue() {
        return mValues;
    }

    public static void main(String[] args) {
        try {
            Decoder decoder = new Decoder("D:/Chicago.Med.torrent");
            decoder.parse();
            List<Node> value = decoder.getValue();
            value.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
