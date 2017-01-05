package me.zzhen.bt.bencode;

import me.zzhen.bt.dht.base.NodeInfo;
import me.zzhen.bt.utils.Utils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Project:CleanBT
 * Create Time: 16-12-29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DictionaryNodeTest {
    @Test
    public void decode() throws Exception {
//        InputStream input = new FileInputStream("/media/Software/The.Big.Bang.Theory.torrent");
//        InputStream input = new FileInputStream("/media/Software/Chicago.Med.torrent");
//
//        DictionaryNode decode = DictionaryNode.decode(input);
//        DictionaryNode info = (DictionaryNode) decode.getNode("info");
//        System.out.println(info.getNode("files"));

//        byte[] bytes = Utils.hex2Bytes("64323A69000000000000000000000000000003AC0FBAAB");
        byte[] bytes = Utils.hex2Bytes("64323A696432303A719F31B67615092F1E346EB262113171F8FC98F9353A6E6F6465733230383A7304C6B939BA2840401311E280289A330A500AD6B7D223B9079873F543A654C367EF7D9CA2B46550DC30B382D807DF490BA29A037388CBD2A5F4CC2D14F88291E2144126B107E60C6551108E2B5D73F7C82E1A71205E9B77E6093A94001058E77A926D9F5481C49173E8CE886382E657315E99625F512F002AF53D442BF1E0481AE170658CD6AE529049F1F1BBE9EBB3A6DB3C870CE1DDA5940959EC70628C10EF2763989D40A3F0C53DF2522981FBC3DF66AE112814702761ADC9671E08057A74DF7C1B67427E03FE001804F1D6C491313A7069363838316565");
//        System.out.println(new String(bytes));

        DictionaryNode dict = DictionaryNode.decode(new ByteArrayInputStream(bytes));
        Node nodes = dict.getNode("nodes");
        for (int i = 0; i < nodes.decode().length; i += 26) {
            NodeInfo nodeInfo = NodeInfo.fromBytes(bytes, i);
            System.out.println(nodeInfo);
        }
        //        System.out.println(dict.toString());
//        System.out.println(decode.toString());

//        String s = "228feb442808e84f6d370008004500010498cf4000e506807c23a54bc92bf1e0481fb49396d883211a29268b0e801800e535e500000101080a0033b85b00717993000000c514006431323a636f6d706c6574655f61676f69313465313a6d6431313a6c745f646f6e746861766569376531303a73686172655f6d6f646569386531313a75706c6f61645f6f6e6c7969336531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f7065786931656531333a6d657461646174615f73697a6569313833353265343a726571716935303065313a7631393a636c69656e745f746573742f312e302e362e30363a796f75726970343a2bf1e0486500000003091fb4";
//        String s1 = "piece=0, total_size=18835, msg_type=1";
//        String[] split = s1.split(",");
//        DictionaryNode node = new DictionaryNode();
//        for (String s2 : split) {
//            String[] split1 = s2.split("=");
//            node.addNode(split1[0], new IntNode(split1[1]));
//        }
//        System.out.println(node.toString());
//        System.out.println(node.encode().length);

    }
}