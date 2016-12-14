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

    private InputStream input;
    private EventHandler handler;
    private List<Node> nodes = new ArrayList<>();

    public Decoder(byte[] input) {
        this(new ByteArrayInputStream(input));
    }

    public Decoder(byte[] input, int offset, int length) {
        this(new ByteArrayInputStream(input, offset, length));
    }

    public Decoder(String file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public Decoder(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public Decoder(InputStream input) {
        this.input = new BufferedInputStream(input);
    }

    /**
     * 开始解析整个输入文件
     *
     * @throws IOException
     */
    public void parse() throws IOException {
        int c;
        while ((c = input.read()) != -1) {
            char cur = (char) c;
            Node node = parseNext(cur);
            nodes.add(node);
        }
        input.close();
    }

    /**
     * 解析下一个Node
     *
     * @param c 当前输入的字符 应该是 i,d,l或者数字
     * @return 当前节点的Node结构
     * @throws IOException
     */
    private Node parseNext(char c) throws IOException {
        Node node = null;
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
                    throw new DecoderException("not a legal char");
                }
                break;
        }
        return node;
    }


    /**
     * 解析字符串节点，cur应该为数字
     *
     * @param cur
     * @return
     * @throws IOException
     */
    private Node parseString(int cur) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        StringBuilder len = new StringBuilder();
        len.append((char) cur);
        while ((c = input.read()) != -1 && (char) c != StringNode.STRING_VALUE_START) {
            if (Character.isDigit(c)) {
                len.append((char) c);
            } else {
                throw new DecoderException("expect a digital but found " + c);
            }
        }
        long length = Long.parseLong(len.toString().trim());
        long i = 0;
        while (i < length && (c = input.read()) != -1) {
            baos.write((byte) c);
            i++;
        }
        if (i < length) {
            throw new DecoderException("illegal string node , except " + length + " char but found " + i);
        }

        StringNode node = new StringNode(baos.toByteArray());
        //TODO 设计更好用的API 但是现在还是就是将这个东西做出来 能用吧
        if (handler != null) {
            handler.handleStringNode(node);
        }
        return node;
    }

    /**
     * 解析字典结构，字典的的key值，应该是字符串类型
     *
     * @return
     * @throws IOException
     */
    private Node parseDic() throws IOException {
        int c;
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        while ((c = input.read()) != -1 && (char) c != DictionaryNode.DIC_END) {
            Node node = null;
            char cur = (char) c;
            if (inKey) {
                if (Character.isDigit(c)) {
                    key = parseString(c).toString();
                    inKey = false;
                } else {
                    System.out.println(cur);
                    throw new DecoderException("key of dic must be string,found digital");
                }
            } else {
                node = parseNext(cur);
                dic.addNode(key, node);
                inKey = true;
                if (handler != null) {
                    handler.handleDictionaryNode(key, node);
                }
            }
        }
        return dic;
    }

    private Node parseList() throws IOException {
        ListNode list = new ListNode();
        int c;
        while ((c = input.read()) != -1 && (char) c != ListNode.LIST_END) {
            char cc = (char) c;
            Node node = parseNext(cc);
            list.addNode(node);
        }
        return list;
    }

    private Node parseInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        while ((c = input.read()) != -1 && c != IntNode.INT_END) {
            char cc = (char) c;
            if (Character.isDigit(cc)) {
                sb.append(cc);
            } else {
                throw new DecoderException("expect a digital but found " + cc);
            }
        }
        return new IntNode(sb.toString());
    }

    public List<Node> getValue() {
        return nodes;
    }

    public EventHandler getHandler() {
        return handler;
    }

    public void setHandler(EventHandler handler) {
        this.handler = handler;
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
