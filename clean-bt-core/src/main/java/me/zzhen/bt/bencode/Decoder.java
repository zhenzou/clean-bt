package me.zzhen.bt.bencode;

import me.zzhen.bt.util.Utils;

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
public final class Decoder {

    private InputStream input;

    /**
     * 事件回调
     */
    private DecodeEventHandler handler;

    /**
     * 解码的结果
     */
    private List<Node> nodes = new ArrayList<>();

    /**
     * 当前处理的输入字节流的位置
     */
    private int pos = -1;

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

    /**
     * 有时还是需要Handler的
     *
     * @param input
     */
    public Decoder(InputStream input) {
        this.input = new BufferedInputStream(input);
    }

    /**
     * 开始解析整个输入文件
     *
     * @throws IOException
     */
    public void decode() throws IOException {
        int c;
        while ((c = input.read()) != -1) {
            char cur = (char) c;
            Node node = decodeNext(cur);
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
    private Node decodeNext(char c) throws IOException {
        Node node = null;
        switch (c) {
            case IntNode.INT_START:
                node = decodeInt();
                break;
            case ListNode.LIST_START:
                node = decodeList();
                break;
            case DictionaryNode.DIC_START:
                node = decodeDic();
                break;
            default:
                if (Character.isDigit(c)) {
                    node = decodeString(c);
                } else {
                    throw new DecoderException("not a legal char in " + pos + " byte");
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
    private Node decodeString(int cur) throws IOException {
        int c;
        StringBuilder len = new StringBuilder();
        len.append((char) cur);
        pos++;
        while ((c = input.read()) != -1 && (char) c != StringNode.STRING_VALUE_START) {
            pos++;
            if (Character.isDigit(c)) {
                len.append((char) c);
            } else {
                throw new DecoderException("expect a digital in " + pos + " but found " + c);
            }
        }
        long length = Long.parseLong(len.toString().trim());
        long i = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (i < length && (c = input.read()) != -1) {
            pos++;
            baos.write(c & 0xFF);
            i++;
        }
        if (i < length) {
            throw new DecoderException("illegal string node , except " + length + " char but found " + i);
        }

        StringNode node = new StringNode(baos.toByteArray());
        //TODO 设计更好用的API 但是现在还是就是将这个东西做出来 能用吧
        if (handler != null) {
            handler.whenString(node);
        }
        return node;
    }

    /**
     * 解析字典结构，字典的的key值，应该是字符串类型
     *
     * @return
     * @throws IOException
     */
    private Node decodeDic() throws IOException {
        int c;
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        pos++;
        while ((c = input.read()) != -1 && (char) c != DictionaryNode.DIC_END) {
            pos++;
            Node node = null;
            char cur = (char) c;
            if (inKey) {
                if (Character.isDigit(cur)) {
                    key = decodeString(c).toString();
                    inKey = false;
                } else {
                    throw new DecoderException("key of dic must be string,found digital");
                }
            } else {
                node = decodeNext(cur);
                dic.addNode(key, node);
                inKey = true;
                if (handler != null) {
                    handler.whenDictionary(key, node);
                }
            }
        }
        return dic;
    }

    private Node decodeList() throws IOException {
        ListNode list = new ListNode();
        int c;
        pos++;
        while ((c = input.read()) != -1 && (char) c != ListNode.LIST_END) {
            char cc = (char) c;
            pos++;
            Node node = decodeNext(cc);
            list.addNode(node);
        }
        return list;
    }

    private Node decodeInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        pos++;
        while ((c = input.read()) != -1 && c != IntNode.INT_END) {
            pos++;
            char cc = (char) c;
            if (Character.isDigit(cc)) {
                sb.append(cc);
            } else {
                throw new DecoderException("expect a digital in " + pos + " but found " + cc);
            }
        }
        return new IntNode(sb.toString());
    }


    public List<Node> getValue() {
        return nodes;
    }

    public DecodeEventHandler getHandler() {
        return handler;
    }

    public void setHandler(DecodeEventHandler handler) {
        this.handler = handler;
    }


    public static void main(String[] args) {
        String s = "64313a65693165343a69707634343ab7693332343a6970763631363a2002b7693332000000000000b769333231323a636f6d706c6574655f61676f692d3165313a6d6431313a75706c6f61645f6f6e6c7969336531313a6c745f646f6e746861766569376531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f70657869316531303a75745f636f6d6d656e746936656531333a6d657461646174615f73697a6569313733373265313a7069333037363665343a726571716932353565313a7631353acebc546f7272656e7420332e342e39323a797069353133353665363a796f75726970343a2bf1e04865";
//        String s = "64313a65693165343a69707634343ab7693332343a6970763631363a2002b7693332000000000000b769333231323a636f6d706c6574655f61676f692d3165313a6d6431313a75706c6f61645f6f6e6c7969336531313a6c745f646f6e746861766569376531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f70657869316531303a75745f636f6d6d656e746936656531333a6d657461646174615f73697a6569313733373265313a7069333037363665343a726571716932353565313a7631353acebc546f7272656e7420332e342e39323a797069353133353665363a796f75726970343a2bf1e04865";
        byte[] bytes = Utils.hex2Bytes(s);

        try {
            Decoder decoder = new Decoder(new ByteArrayInputStream(bytes));
            decoder.decode();
            List<Node> decode = decoder.getValue();
            Node node = decode.get(0);
            System.out.println(node.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
