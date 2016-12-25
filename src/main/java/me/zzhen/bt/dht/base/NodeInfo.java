package me.zzhen.bt.dht.base;

import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeInfo {
    public String hostName;
    public int port;
    public NodeKey key;
    private InetAddress address;

    /**
     * 一般用于
     *
     * @param address
     * @param port
     * @param key
     */
    public NodeInfo(InetAddress address, int port, NodeKey key) {
        this.port = port;
        this.key = key;
        this.address = address;
    }

    /**
     *
     */
    public NodeInfo(byte[] bytes) {
        byte[] keydata = new byte[20];
        System.arraycopy(bytes, 0, keydata, 0, 20);
        address = IO.getAddrFromBytes(bytes, 20);
        key = new NodeKey(keydata);
        port = Utils.bytesToInt(bytes, 24, 2);
    }

    /**
     * TODO factory
     * 暂时就是支持完整的NodeInfo
     * 包括ID，IP，Port
     *
     * @param bytes
     * @param offset
     */
    public NodeInfo(byte[] bytes, int offset) {
        this(Utils.getSomeByte(bytes, offset, 26));
    }

    /**
     * 一般Bootstrap RouteTreeNode 初始化使用
     *
     * @param host
     * @param port
     */
    public NodeInfo(String host, int port) {
        this(host, port, null);
    }

    public NodeInfo(String host, int port, NodeKey key) {
        this.hostName = host;
        this.port = port;
        this.key = key;
        try {
            initAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void initAddress() throws UnknownHostException {
        address = InetAddress.getByName(hostName);
    }

    public void setKey(NodeKey key) {
        this.key = key;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public NodeKey getKey() {
        return key;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    /**
     * @return nodeinfo的编码，ID IP/Port 一共26个字节
     */
    public byte[] compactNodeInfo() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(key.getValue());
            baos.write(Utils.ipToBytes(address.getHostAddress()));
            baos.write(Utils.intToBytes(port), 2, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    /**
     * @return nodeinfo的编码 IP/Port 一共6个字节
     */
    public byte[] compactIpPort() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(Utils.ipToBytes(address.getHostAddress()));
            baos.write(Utils.intToBytes(port), 2, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeInfo nodeInfo = (NodeInfo) o;

        if (port != nodeInfo.port) return false;
        if (hostName != null ? !hostName.equals(nodeInfo.hostName) : nodeInfo.hostName != null) return false;
        if (!key.equals(nodeInfo.key)) return false;
        return address.equals(nodeInfo.address);
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + Objects.hash(key);
        result = 31 * result + address.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "hostName='" + hostName + '\'' +
                ", port=" + port +
                ", key=" + String.valueOf(key) +
                ", address=" + address +
                '}';
    }

}
