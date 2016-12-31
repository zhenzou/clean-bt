package me.zzhen.bt.bencode;

import me.zzhen.bt.utils.Utils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

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
//        DictionaryNode decode = DictionaryNode.decode(input);
//        System.out.println(decode.toString());

//        String s = "228feb442808e84f6d370008004500010498cf4000e506807c23a54bc92bf1e0481fb49396d883211a29268b0e801800e535e500000101080a0033b85b00717993000000c514006431323a636f6d706c6574655f61676f69313465313a6d6431313a6c745f646f6e746861766569376531303a73686172655f6d6f646569386531313a75706c6f61645f6f6e6c7969336531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f7065786931656531333a6d657461646174615f73697a6569313833353265343a726571716935303065313a7631393a636c69656e745f746573742f312e302e362e30363a796f75726970343a2bf1e0486500000003091fb4";
        String s = "64313a65693165343a69707634343ab7693332343a6970763631363a2002b7693332000000000000b769333231323a636f6d706c6574655f61676f69313165313a6d6431313a75706c6f61645f6f6e6c7969336531313a6c745f646f6e746861766569376531323a75745f686f6c6570756e636869346531313a75745f6d65746164617461693265363a75745f70657869316531303a75745f636f6d6d656e746936656531333a6d657461646174615f73697a6569313733373265313a7069333037363665343a726571716932353565313a7631353acebc546f7272656e7420332e342e39323a797069353133353665363a796f75726970343a2bf1e04865";
        String s1 = "piece=0, total_size=18835, msg_type=1";
        String[] split = s1.split(",");
        DictionaryNode node = new DictionaryNode();
        for (String s2 : split) {
            String[] split1 = s2.split("=");
            node.addNode(split1[0], new IntNode(split1[1]));
        }
        System.out.println(node.toString());
        System.out.println(node.encode().length);

        byte[] bytes = Utils.hex2Bytes(s);
        DictionaryNode decode1 = DictionaryNode.decode(new ByteArrayInputStream(bytes));
        System.out.println(decode1);
    }
}