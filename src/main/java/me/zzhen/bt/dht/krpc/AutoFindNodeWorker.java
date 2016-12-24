package me.zzhen.bt.dht.krpc;

import me.zzhen.bt.dht.DhtApp;
import me.zzhen.bt.dht.base.NodeInfo;

import java.util.List;

/**
 * Project:CleanBT
 * Create Time: 16-12-25.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class AutoFindNodeWorker implements Runnable {

    private Krpc krpc;

    public AutoFindNodeWorker(Krpc krpc) {
        this.krpc = krpc;
    }

    @Override
    public void run() {
        List<NodeInfo> infos = DhtApp.NODE.routes.closest8Nodes(DhtApp.NODE.getSelf().getKey());
        for (NodeInfo info : infos) {
            krpc.findNode(info, DhtApp.NODE.getSelfKey());
        }
    }
}
