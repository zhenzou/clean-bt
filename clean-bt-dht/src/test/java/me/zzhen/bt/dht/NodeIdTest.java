package me.zzhen.bt.dht;

import me.zzhen.bt.common.Bitmap;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeIdTest {

    @Test
    public void randomId() {
        NodeId key = NodeId.randomId();
        NodeId pre = NodeId.randomId();
    }

    @Test
    public void prefix() throws Exception {
        NodeId key = NodeId.randomId();
        NodeId pre = NodeId.randomId();
        Bitmap bits = key.getBits();
        Bitmap prefix = pre.getBits();
        for (int i = 0; i < 10; i++) {
            bits.set(i, prefix.get(i));
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(pre.prefix(i), key.prefix(i));
        }
    }

    @Test
    public void compareTo() throws Exception {
    }

}