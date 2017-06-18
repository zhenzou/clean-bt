package me.zzhen.bt.dht;

import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeInfoTest {
    @Test
    public void fromBytes() throws Exception {

    }

    @Test
    public void getFullAddress() throws Exception {
        NodeInfo node = new NodeInfo("101.236.3.198", 6881, NodeId.randomId());
        assertEquals("101.236.3.198:6881", node.getFullAddress());
        InetAddress localHost = InetAddress.getLoopbackAddress();
        node = new NodeInfo(localHost, 6881, NodeId.randomId());
        assertEquals("127.0.0.1:6881", node.getFullAddress());
    }

    @Test
    public void compactNodeInfo() throws Exception {
        InetAddress localHost = InetAddress.getLoopbackAddress();
        NodeInfo node = new NodeInfo(localHost, 6881, NodeId.defaultId());
        byte[] bytes = node.compactNodeInfo();
    }

    @Test
    public void compactIpPort() throws Exception {
    }

}