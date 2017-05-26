package me.zzhen.bt.dht.base;

import me.zzhen.bt.dht.NodeInfo;
import me.zzhen.bt.dht.NodeKey;
import me.zzhen.bt.dht.RouteTable;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Project:CleanBT
 * Create Time: 16-12-20.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTableTest {


    String[] ids = {"F70A32BB776DFD0C1C8FB16B02FBC8CA26CB970B",
            "FDF32EAA2C8A85E4AA4A18D452D93CD30B8743F4",
            "B0EC3F01AFD128A504C4A2E2B199569516A05C08",
            "D7430A41E64C8A2E1FE536083012CC2C5434FF72", "F70A32BB776DFD0C1C8FB16B02FBC8CA26CB970B",
            "FDF32EAA2C8A85E4AA4A18D452D93CD30B8743F4",
            "B0EC3F01AFD128A504C4A2E2B199569516A05C08",
            "D7430A41E64C8A2E1FE536083012CC2C5434FF72"};
    String[] ids1 = {"F70A32BB776DFD0C1C8FB16B02FBC8CA26CB970B",
            "FDF32EAA2C8A85E4AA4A18D452D93CD30B8743F4",
            "B0EC3F01AFD128A504C4A2E2B199569516A05C08",
            "D7430A41E64C8A2E1FE536083012CC2C5434FF72"};

    @Test
    public void size() throws UnknownHostException {
        NodeKey local = NodeKey.genRandomKey();
        RouteTable table = new RouteTable(new NodeInfo(InetAddress.getLocalHost(), 100, local));
        for (String id : ids) {
            table.addNode(NodeInfo.fromBytes(id.getBytes()));
        }
        assertEquals(4, table.size());
        for (String id : ids) {
            table.addNode(NodeInfo.fromBytes(id.getBytes()));
        }
        assertEquals(4, table.size());
    }


    @Test
    public void addNode() throws Exception {
        NodeKey local = NodeKey.genRandomKey();
        System.out.println("local:" + local);
        RouteTable table = new RouteTable(new NodeInfo(InetAddress.getLocalHost(), 100, local));
        for (int i = 0; i < 1000000; i++) {
            NodeKey key = NodeKey.genRandomKey();
//            Bitmap bits = key.getBits();
//            for (int i1 = 0; i1 < 15; i1++) {
//                bits.set(i1, local.prefix(i1));
//            }
            NodeInfo info = new NodeInfo(InetAddress.getLocalHost(), 200, key);
            table.addNode(info);
        }
        System.out.println(table.size());
        for (int i = 0; i < 1000; i++) {
            NodeKey randomKey = NodeKey.genRandomKey();
//            System.out.println(randomKey);
            List<NodeInfo> infos = table.closest8Nodes(randomKey);
            System.out.println(infos.size());
//            for (NodeInfo info : infos) {
//                System.out.println(info);
//            }
        }
        assertEquals(table.size(table.getRoot()), table.size());

        table = new RouteTable(new NodeInfo(InetAddress.getLocalHost(), 100, local));
        for (int i = 0; i < 6; i++) {
            NodeKey key = NodeKey.genRandomKey();
            NodeInfo info = new NodeInfo(InetAddress.getLocalHost(), 200, key);
            table.addNode(info);
        }
        List<NodeInfo> infos = table.closest8Nodes(local);
        System.out.println(infos.size());
        for (NodeInfo info : infos) {
            System.out.println(info);
        }
        assertEquals(table.size(table.getRoot()), table.size());

    }

    @Test
    public void closestKNodes() throws Exception {

    }

}