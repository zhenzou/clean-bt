package me.zzhen.bt.decoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zzhen on 2016/10/16.
 */
public class Decoder {

    private InputStream mInput;
    private EventHandler mHandler;
    private List<Node> mValues = new ArrayList<>();

    private Decoder(byte[] input) {
        mInput = new ByteArrayInputStream(input);
    }

    public Decoder(String file) throws FileNotFoundException {
        mInput = new BufferedInputStream(new FileInputStream(file));
    }

    public Decoder(InputStream input) throws FileNotFoundException {
        mInput = new BufferedInputStream(input);
    }

    public void parse() throws IOException {
        int c;
        while ((c = mInput.read()) != -1) {
            Node node = null;
            char cur = (char) c;
            switch (c) {
                case IntNode.INT_START:
                    node = parseInt();
                    break;
                case ListNode.LIST_START:
                    node = parseList();
                    break;
                case DictionaryNode.DIC_START:
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
        while ((c = mInput.read()) != -1 && (char) c != StringNode.STRING_VALUE_START) {
            len.append((char) c);
        }
        int length = Integer.parseInt(len.toString().trim());
        int i = 1;
        while ((c = mInput.read()) != -1 && i < length) {
            sb.append((char) c);
            i++;
        }
        sb.append((char) c);
        i++;
        if (i < length) {
            throw new DecoderExecption("illegal string node , except " + length + " char but found " + i);
        }
        //TODO 设计更好用的API 但是现在还是就是将这个东西做出来 能用吧
//        if (mHandler != null) {
//            mHandler.handleStringNode(sb.toString());
//        }
        return new StringNode(sb.toString());
    }

    private Node parseDic() throws IOException {
        int c;
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        while ((c = mInput.read()) != -1 && (char) c != DictionaryNode.DIC_END) {
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
                    case IntNode.INT_START:
                        node = parseInt();
                        break;
                    case ListNode.LIST_START:
                        node = parseList();
                        break;
                    case DictionaryNode.DIC_START:
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
        return dic;
    }

    private Node parseList() throws IOException {
        ListNode list = new ListNode();
        int c;
        while ((c = mInput.read()) != -1 && (char) c != ListNode.LIST_END) {
            Node node = null;
            char cur = (char) c;
            switch (cur) {
                case IntNode.INT_START:
                    node = parseInt();
                    break;
                case ListNode.LIST_START:
                    node = parseList();
                    break;
                case DictionaryNode.DIC_START:
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
        return list;
    }

    private Node parseInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        while ((c = mInput.read()) != -1 && c != IntNode.INT_END) {
            sb.append((char) c);
        }
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
            System.out.println(value.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
