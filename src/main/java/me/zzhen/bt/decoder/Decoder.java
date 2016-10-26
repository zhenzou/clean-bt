package me.zzhen.bt.decoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public class Decoder {

    private InputStream mInput;
    private EventHandler mHandler;
    private List<Node> mValues = new ArrayList<>();

    public Decoder(byte[] input) {
        mInput = new ByteArrayInputStream(input);
    }

    public Decoder(String file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public Decoder(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public Decoder(InputStream input) throws FileNotFoundException {
        mInput = new BufferedInputStream(input);
    }

    /**
     * TODO 整理代码
     *
     * @throws IOException
     */
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
        mInput.close();
    }

    /**
     * @param cur
     * @return
     * @throws IOException
     */
    private Node parseString(int cur) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        StringBuilder len = new StringBuilder();
        len.append((char) cur);
        while ((c = mInput.read()) != -1 && (char) c != StringNode.STRING_VALUE_START) {
            if (Character.isDigit(c)) {
                len.append((char) c);
            } else {
                throw new DecoderExecption("expect a digital but found " + c);
            }
        }
        long length = Long.parseLong(len.toString().trim());
        long i = 0;
        while (i < length && (c = mInput.read()) != -1) {
            baos.write((byte) c);
            i++;
        }
        if (i < length) {
            throw new DecoderExecption("illegal string node , except " + length + " char but found " + i);
        }

        StringNode node = new StringNode(baos.toByteArray());
        //TODO 设计更好用的API 但是现在还是就是将这个东西做出来 能用吧
        if (mHandler != null) {
            mHandler.handleStringNode(node);
        }
        return node;
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
                    System.out.println(cur);
                    throw new DecoderExecption("key of dic must be string,found digital");
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
                dic.addNode(key, node);
                inKey = true;
                if (mHandler != null) {
                    mHandler.handleDictionaryNode(key, node);
                }
            }
        }
        return dic;
    }

    private Node parseList() throws IOException {
        ListNode list = new ListNode();
        int c;
        while ((c = mInput.read()) != -1 && (char) c != ListNode.LIST_END) {
            Node node = null;
            char cc = (char) c;
            switch (cc) {
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
            char cc = (char) c;
            if (Character.isDigit(cc)) {
                sb.append(cc);
            } else {
                throw new DecoderExecption("expect a digital but found " + cc);
            }
        }
        return new IntNode(sb.toString());
    }

    public List<Node> getValue() {
        return mValues;
    }

    public EventHandler getHandler() {
        return mHandler;
    }

    public void setHandler(EventHandler handler) {
        mHandler = handler;
    }

    public static void main(String[] args) {
        try {
//            Decoder decoder = new Decoder("D:/Chicago.Med.torrent");
            Decoder decoder = new Decoder("d4:test0:e".getBytes());
            decoder.parse();
            List<Node> value = decoder.getValue();
            value.forEach(item -> System.out.println(item.decode()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
