package me.zzhen.bt.dht.routetable;

import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.NodeInfo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class RouteTableTest {

    public RouteTable init() {
        String ip = "10.0.0.";
        String id = "1234567890123456";
        int start = 1;
        RouteTable routes = new RouteTable(1024);
        for (int i = 0; i < 1024; i++) {
            routes.addNode(new NodeInfo(ip + start, 6881, new NodeId((id + String.format("%04d", start)).getBytes())));
            start++;
        }
        return routes;
    }

    @Test
    public void addNode() throws Exception {
        RouteTable routes = init();
        assertEquals(1024, routes.length());
        String ip = "10.0.0.";
        String id = "1234567890123456";
        int start = 1;
        NodeInfo info = new NodeInfo(ip + start, 6881, new NodeId((id + String.format("%04d", start)).getBytes()));
        routes.addNode(info);
        assertEquals(1024, routes.length());
    }

    @Test
    public void closest8Nodes() throws Exception {
        RouteTable routes = init();
        assertEquals(1024, routes.length());
        String ip = "10.0.0.";
        String id = "1234567890123456";
        int start = 1;
        NodeInfo info = new NodeInfo(ip + start, 6881, new NodeId((id + String.format("%04d", start)).getBytes()));
        List<NodeInfo> infos = routes.closest8Nodes(info.getId());
        assertEquals(8, infos.size());
        for (NodeInfo node : infos) {
            for (int j = 0; j < info.getId().getBits().size - 8; j++) {
                assertEquals(info.getId().prefix(j), node.getId().prefix(j));
            }
        }
    }

    @Test
    public void closestKNodes() throws Exception {
        RouteTable routes = init();
        assertEquals(1024, routes.length());
        String ip = "10.0.0.";
        String id = "1234567890123456";
        int start = 1;
        NodeInfo info = new NodeInfo(ip + start, 6881, new NodeId((id + String.format("%04d", start)).getBytes()));
        List<NodeInfo> infos = routes.closestKNodes(info.getId(), 16);
        assertEquals(16, infos.size());
        for (int i = 0; i < infos.size(); i++) {
            NodeInfo node = infos.get(i);
//            System.out.println(node.getId().toString());
            for (int j = 0; j < info.getId().getBits().size - 16; j++) {
//                System.out.printf("%d,%d,%b:%b\n", i, j, info.getId().prefix(j), node.getId().prefix(j));
                assertEquals(info.getId().prefix(j), node.getId().prefix(j));
            }
        }
    }

    @Test
    public void remove() throws Exception {
        RouteTable routes = init();
        assertEquals(1024, routes.length());
        String ip = "10.0.0.";
        String id = "1234567890123456";
        routes.remove(new NodeInfo(ip + 1, 6881, new NodeId((id + String.format("%04d", 1)).getBytes())));
        routes.remove(new NodeInfo(ip + 2, 6881, new NodeId((id + String.format("%04d", 2)).getBytes())));
        routes.remove(new NodeInfo(ip + 3, 6881, new NodeId((id + String.format("%04d", 3)).getBytes())));
        assertEquals(1021, routes.length());
    }

}