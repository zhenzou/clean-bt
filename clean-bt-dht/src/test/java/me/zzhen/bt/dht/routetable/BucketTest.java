package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.bencode.Node;
import me.zzhen.bt.common.Tuple;
import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import me.zzhen.bt.util.Utils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class BucketTest {

    public Bucket init() {
        String ip = "10.0.0.";
        String id = "1234567890123456789";
        int start = 1;
        Bucket bucket = new Bucket(0, 8);
        for (int i = 0; i < 8; i++) {
            bucket.addNode(new NodeInfo(ip + start, 6881, new NodeId((id + start).getBytes())));
            start++;
        }
        return bucket;
    }

    @Test
    public void split() throws Exception {
        Bucket bucket = init();
        Tuple<Bucket, Bucket> split = bucket.split();
        assertEquals(bucket.length(), split._1.length() + split._2.length());
        assertEquals(1, split._2.prefix.size);
        assertEquals(1, split._1.prefix.size);

        bucket = new Bucket(0, 8);
        for (int i = 0; i < 8; i++) {
            bucket.addNode(new NodeInfo("10.0.0.0", 6881, NodeId.randomId()));
        }
        assertEquals(bucket.length(), split._1.length() + split._2.length());
        assertEquals(1, split._2.prefix.size);
        assertEquals(1, split._1.prefix.size);
    }

    @Test
    public void randomChildKey() throws Exception {
    }

    @Test
    public void checkRange() throws Exception {
        Bucket bucket = init();
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567890".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567891".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567892".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567893".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567894".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567895".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567896".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567897".getBytes())));
        Tuple<Bucket, Bucket> split = bucket.split();
        bucket = split._1;
        assertEquals(false, bucket.checkRange(new NodeId("02345678901234567897".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId((Utils.hex2Bytes("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")))));
        assertEquals(true, bucket.checkRange(new NodeId((Utils.hex2Bytes("FEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFF")))));
        assertEquals(true, bucket.checkRange(new NodeId((Utils.hex2Bytes("FCFFFFFFFFFFFFFFFFFFFCFFFFFFFFFFFFFFFFFF")))));
        bucket = split._2;
        assertEquals(true, bucket.checkRange(new NodeId("02345678901234567897".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567893".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567894".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567895".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567896".getBytes())));
        assertEquals(true, bucket.checkRange(new NodeId("12345678901234567897".getBytes())));
    }

    @Test
    public void addNode() throws Exception {
    }

    @Test
    public void addNode1() throws Exception {
    }

    @Test
    public void contains() throws Exception {
    }

    @Test
    public void getNode() throws Exception {
        Bucket bucket = init();
        assertEquals(true, bucket.getNode(new NodeId("12345678901234567891".getBytes())).isPresent());
        assertEquals(true, bucket.getNode(new NodeId("12345678901234567892".getBytes())).isPresent());
        assertEquals(true, bucket.getNode(new NodeId("12345678901234567894".getBytes())).isPresent());
        assertEquals(true, bucket.getNode(new NodeId("12345678901234567894".getBytes())).isPresent());
        assertEquals(8, bucket.length());
        assertEquals(false, bucket.getNode(new NodeId("02345678901234567894".getBytes())).isPresent());
    }

    @Test
    public void remove() throws Exception {
        Bucket bucket = init();
        assertEquals(true, bucket.remove(new NodeId("12345678901234567891".getBytes())));
        assertEquals(true, bucket.remove(new NodeId("12345678901234567892".getBytes())));
        assertEquals(true, bucket.remove(new NodeId("12345678901234567893".getBytes())));
        assertEquals(true, bucket.remove(new NodeId("12345678901234567895".getBytes())));
        assertEquals(4, bucket.length());
        assertEquals(false, bucket.remove(new NodeId("02345678901234567895".getBytes())));

    }

    @Test
    public void update() throws Exception {
    }

}