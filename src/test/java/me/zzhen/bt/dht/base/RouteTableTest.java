package me.zzhen.bt.dht.base;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
            table.addNode(new NodeInfo(id.getBytes()));
        }
        assertEquals(4, table.size());
        for (String id : ids) {
            table.addNode(new NodeInfo(id.getBytes()));
        }
        assertEquals(4, table.size());
    }


    @Test
    public void addNode() throws Exception {
        NodeKey local = NodeKey.genRandomKey();
        RouteTable table = new RouteTable(new NodeInfo(InetAddress.getLocalHost(), 100, local));
        for (int i = 0; i < 10000; i++) {
            NodeKey key = NodeKey.genRandomKey();
            NodeInfo info = new NodeInfo(InetAddress.getLocalHost(), 200, key);
            table.addNode(info);
        }

//        table.preOrderPrint(table.getRoot(), "");
//        System.out.println(table.ttSize);
        assertEquals(table.size(table.getRoot()), table.size());
    }

    @Test
    public void closestKNodes() throws Exception {

    }

}