package me.zzhen.bt.dht;

import me.zzhen.bt.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project:CleanBT
 * Create Time: 16-12-19.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class PeerManager {


    public static PeerManager PM;

    public static PeerManager init() {
        PM = new PeerManager();
        return PM;
    }

    private Map<NodeId, List<InetSocketAddress>> peers = new HashMap<>();

    public void addPeer(NodeId info, InetSocketAddress peer) {
        List<InetSocketAddress> addrs = peers.get(info);
        if (addrs == null) {
            List<InetSocketAddress> a = new ArrayList<>();
            a.add(peer);
            peers.put(info, a);
        } else {
            addrs.add(peer);
        }
    }

    public void addAllPeer(NodeId info, List<InetSocketAddress> peers) {
        List<InetSocketAddress> addrs = this.peers.get(info);
        if (addrs == null) {
            this.peers.put(info, peers);
        } else {
            addrs.addAll(peers);
        }
    }

    /**
     * @param key
     * @return 返回null, 表示对应的peers不存在
     */
    public List<InetSocketAddress> getPeers(NodeId key) {
        return peers.get(key);
    }

    /**
     * 查看是否包含对应peer
     *
     * @param key
     * @return
     */
    public boolean contains(NodeId key) {
        return peers.containsKey(key);
    }

    public byte[] compact(NodeId key) {
        List<InetSocketAddress> nodes = peers.get(key);
        if (nodes == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (InetSocketAddress node : nodes) {
            try {
                baos.write(compact(node));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return baos.toByteArray();
    }

    public static byte[] compact(InetSocketAddress address) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(address.getAddress().getAddress());
            baos.write(Utils.getSomeByte(Utils.int2Bytes(address.getPort()), 2, 2));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

}
