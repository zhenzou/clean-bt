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

    /**
     * 当前位置的字符，
     */
    private char current = 0;

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

        while (next()) {
            Node node = decodeNext();
            nodes.add(node);
        }
        while ((c = input.read()) != -1) {
            char cur = (char) c;
            Node node = decodeNext();
            nodes.add(node);
        }
        input.close();
    }

    /**
     * 解析下一个Node
     * <p>
     * current 当前输入的字符 应该是 i,d,l或者数字
     *
     * @return 当前节点的Node结构
     * @throws IOException
     */
    private Node decodeNext() throws IOException {
        Node node = null;
        switch (current) {
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
                if (Character.isDigit(current)) {
                    node = decodeString();
                } else {
                    throw new DecoderException("not a legal char in " + pos + " byte");
                }
                break;
        }
        return node;
    }


    /**
     * 解析字符串节点，current应该为数字
     *
     * @return
     * @throws IOException
     */
    public StringNode decodeString() throws IOException {
        StringBuilder len = new StringBuilder();
        len.append(current);
        while (next() && current != StringNode.STRING_VALUE_START) {
            if (Character.isDigit(current)) {
                len.append(current);
            } else {
                throw new DecoderException(String.format("expect a digital in %d but found %c", pos, current));
            }
        }
        long length = Long.parseLong(len.toString().trim());
        long i = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (i < length && next()) {
            baos.write(current & 0xFF);
            i++;
        }
        if (i < length) {
            throw new DecoderException(String.format("illegal string node , except %d char but found %d", length, i));
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
    public DictionaryNode decodeDic() throws IOException {
        String key = "";
        DictionaryNode dic = new DictionaryNode();
        boolean inKey = true;
        while (next() && current != DictionaryNode.DIC_END) {
            Node node = null;
            char cur = current;
            if (inKey) {
                if (Character.isDigit(cur)) {
                    key = decodeString().toString();
                    inKey = false;
                } else {
                    throw new DecoderException("key of dic must be string,found digital");
                }
            } else {
                node = decodeNext();
                dic.addNode(key, node);
                inKey = true;
                if (handler != null) {
                    handler.whenDictionary(key, node);
                }
            }
        }
        return dic;
    }

    public ListNode decodeList() throws IOException {
        ListNode list = new ListNode();
        while (next() && current != ListNode.LIST_END) {
            Node node = decodeNext();
            list.addNode(node);
        }
        return list;
    }

    public IntNode decodeInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (next() && current != IntNode.INT_END) {
            if (Character.isDigit(current)) {
                sb.append(current);
            } else {
                throw new DecoderException(String.format("expect a digital in %d but found %c", pos, current));
            }
        }
        return new IntNode(sb.toString());
    }

    /**
     * <p>
     * 读取下一个字节，并且将值赋值给current
     * </p>
     *
     * @return 如果没有到达流终点则返回true，到达则返回false
     * @throws IOException io
     */
    private boolean next() throws IOException {
        int c = -1;
        if ((c = input.read()) != -1) {
            current = (char) c;
            pos++;
            return true;
        }
        return false;
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
//        String s = "64313a65693165343a69707634343ab7693332343a6970763631363a2002b7693332000000000000b769333231323a636f6d706c6574655f61676f692d3165313a6d6431313a75706c6f61645f6f6e6c7969336531313a6c745f646f6e746861766569376531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f70657869316531303a75745f636f6d6d656e746936656531333a6d657461646174615f73697a6569313733373265313a7069333037363665343a726571716932353565313a7631353acebc546f7272656e7420332e342e39323a797069353133353665363a796f75726970343a2bf1e04865";
        String s = "64313a65693165343a69707634343ab7693332343a6970763631363a2002b7693332000000000000b769333231323a636f6d706c6574655f61676f692d3165313a6d6431313a75706c6f61645f6f6e6c7969336531313a6c745f646f6e746861766569376531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f70657869316531303a75745f636f6d6d656e746936656531333a6d657461646174615f73697a6569313733373265313a7069333037363665343a726571716932353565313a7631353acebc546f7272656e7420332e342e39323a797069353133353665363a796f75726970343a2bf1e04865";
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
