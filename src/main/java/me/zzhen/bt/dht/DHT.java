package me.zzhen.bt.dht;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class DHT {
    private NodeKey mKey;
    private RouteTable mRouteTable;


    public NodeKey getKey() {
        return mKey;
    }

    public void setKey(NodeKey key) {
        mKey = key;
    }

    public RouteTable getRouteTable() {
        return mRouteTable;
    }

    public void setRouteTable(RouteTable routeTable) {
        mRouteTable = routeTable;
    }
}
