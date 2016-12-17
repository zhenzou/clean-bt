package me.zzhen.bt.bencode;

import me.zzhen.bt.dht.NodeInfo;
import me.zzhen.bt.utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Project:CleanBT
 * TODO 重构成工具类 使用代码模式
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

    public Decoder(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public Decoder(InputStream input) {
        this.input = new BufferedInputStream(input);
    }

    public static List<Node> parse(byte[] input) throws IOException {
        return parse(new ByteArrayInputStream(input));
    }

    public static List<Node> parse(byte[] input, int offset, int length) throws IOException {
        return parse(new ByteArrayInputStream(input, offset, length));
    }

    public static List<Node> parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    public static List<Node> parse(InputStream input) throws IOException {
        Decoder decoder = new Decoder(input);
        decoder.parse();
        return decoder.getValue();
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
     * TODO 增加错误位置
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (i < length && (c = input.read()) != -1) {
            baos.write(c & 0xFF);
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
            String info = "7494dc47bec535581b208170d98d3f16ddaa22a5";
            String x = "64323a6970363a71738432457d313a7264323a696432303aebff36697351ff4aec29cdbaabf2fbe3467cc267353a6e6f6465733431363a86d481f1d385d269d90ca72f5dad2c37ada985575c37fded51855cb3ec86d3f1ed8dd97898cb5dd913d3adddbab36921fd6546185cb3ec87f0a6b93768ac30422e2891c936d89560bf428df417845cb3ec0021456877aa5d9873033d1c0e079be65fb38d5e8b79cb5cb3ec018bcc1d862a4705762eee65f253eac3087b202364460c5cb3ec025d6494f7d9c51fef29c1b68fadbcb2298e7395a7620d5cb3ec03ecb03c7eaa349df5b2c4995cd240e4583d532a42687a5cb3ec04501fe8ea25596c4bae419c4f07211832da9978a974025cb3ec0506a5573e8fd011ba2e5b094a2af269cebffdd6a8452c5cb3ec0653f1ed8d597898cbddd913d32dddbab3020c33696fb45cb3ec0770a6b937e8ac3042ae2891c9b6d895605a6e9b4c74b55cb3ec4061456877ea5d9873433d1c0e479be65f31b6435c12645cb3ec41cbcc1d866a4705766eee65f213eac30861d093ba38585cb3ec421d6494f799c51fef69c1b68fedbcb229bb70c3092c015cb3ec43acb03c7eea349df5f2c4995c9240e458d90bc2788c1c5cb3ec44101fe8ea65596c4bee419c4f47211832592a7de6a24865313a74313a01313a76343a4c540011313a79313a7265";
            String x1 = "64323a6970363a7781ff5c1ae1313a7264323a696432303aebff36697351ff4aec29cdbaabf2fbe3467cc267353a6e6f6465733431363a02b3afe2cb7829e37b9cd259faefcbb3c6cb53beb87982d0690d6a8a433912f38a14c49f678a43f236e226f1a5f92ecc3080107a6a8a433912f38a14c49f678a43f236e352fd3c175b4f3c7995116a8a433912f38a14c49f678a43f236e42d8b328ea84290568a346a8a433912f38a14c49f678a43f236e508f2468cb5796da958c06a8a433912f38a14c49f678a43f236e61ed131f8a71ef2236d5a6a8a433912f38a14c49f678a43f236e7bcc514835c8cec91732d6a8a433912f38a14c49f678a43f236f0a18a96f2af2735fc7e576a8a433912f38a14c49f678a43f236f138664f20507148c24bd96a8a433912f38a14c49f678a43f236f236f1a5f925c3f5403f456a8a433912f38a14c49f678a43f236f342fd3c17b0af6d3b1adf6a8a433912f38a14c49f678a43f236f43d8b328ebc8194ce53d56a8a433912f38a14c49f678a43f236f518f2468c3e8b8b9986e16a8a433912f38a14c49f678a43f236f60ed131f8df3598b4161e6a8a433912f38a14c49f678a43f236f7acc51483978867790cf16a8a433912f38a14c49f678a43f236f8a98a96f2abcb8f9423ad65313a74313a01313a76343a4c540011313a79313a7265";
            String x2 = "64313a74313a01313a79313a71313a71393a66696e645f6e6f6465313a6164363a74617267657432303a6a8a433912f38a14c49f678a43f236ffae1276c7323a696432303a6a8a433912f38a14c49f678a43f236ffae1276c76565";
            byte[] bytes = Utils.hex2Bytes(x2);
            Decoder decoder = new Decoder(bytes);
            decoder.parse();
            List<Node> value = Decoder.parse(bytes);
            value.forEach((Node item) -> {
                DictionaryNode node = (DictionaryNode) item;
                Map<String, Node> map = node.getValue();
                for (Map.Entry<String, Node> entry : map.entrySet()) {
                    System.out.println(entry.getValue().decode().length);
                    System.out.println(entry.getKey() + ":" + Utils.toHex(entry.getValue().encode()));
                }

                DictionaryNode a = (DictionaryNode) node.getNode("a");
                Node id = a.getNode("id");
                System.out.println(id.encode().length);
                Node info_hash = a.getNode("target");
                System.out.println(info_hash.encode().length);
//                DictionaryNode resp = (DictionaryNode) ((DictionaryNode) item).getNode("r");
//                Node arg = resp.getNode("nodes");
//                byte[] decode = arg.decode();
//                System.out.println(decode.length);
//                for (int i = 0; i < decode.length; i += 26) {
//                    NodeInfo nodeInfo = new NodeInfo(decode, i);
//                    System.out.println(nodeInfo.getAddress().getHostAddress());
//                    System.out.println(nodeInfo.getPort());
//                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
