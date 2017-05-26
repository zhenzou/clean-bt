package me.zzhen.bt.bencode;

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
        while (next()) {
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
            case DictNode.DIC_START:
                node = decodeDic();
                break;
            default:
                if (Character.isDigit(current)) {
                    node = decodeString(current);
                } else {
                    throw new DecoderException("not a legal char in " + pos + " byte");
                }
        }
        return node;
    }


    /**
     * 解析字符串节点，current应该为数字
     *
     * @return
     * @throws IOException
     */
    public StringNode decodeString(char cur) throws IOException {
        StringBuilder len = new StringBuilder();
        len.append(cur);
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
    public DictNode decodeDic() throws IOException {
        String key = "";
        DictNode dic = new DictNode();
        boolean inKey = true;
        while (next() && current != DictNode.DIC_END) {
            Node node = null;
            if (inKey) {
                if (Character.isDigit(current)) {
                    key = decodeString(current).toString();
                    inKey = false;
                } else {
                    throw new DecoderException("key of dic must be string,found " + current);
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
}
