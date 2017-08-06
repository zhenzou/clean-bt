package me.zzhen.bt.dht;

import me.zzhen.bt.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Project:CleanBT
 * Create Time: 2016/10/29.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class NodeInfo {

    /**
     * 节点的域名或者点分IP地址
     */
    public final String address;

    /**
     * 节点的端口
     */
    public final int port;

    /**
     * 节点的ID
     */
    public NodeId id;

    /**
     * 暂时就是支持完整的NodeInfo
     * 包括ID，IP，Port
     * 没有检测长度
     *
     * @param bytes
     * @param offset
     */
    public static NodeInfo fromBytes(byte[] bytes, int offset) {
        return fromBytes(Utils.getSomeByte(bytes, offset, 26));
    }

    public static NodeInfo fromBytes(byte[] bytes) {
        byte[] keydata = new byte[20];
        System.arraycopy(bytes, 0, keydata, 0, 20);
        InetAddress address = Utils.getAddrFromBytes(bytes, 20);
        NodeId key = new NodeId(keydata);
        int port = Utils.bytes2Int(bytes, 24, 2);
        return new NodeInfo(address.getHostAddress(), port, key);
    }

    /**
     * @param host
     * @param port
     * @param id
     */
    public NodeInfo(String host, int port, NodeId id) {
        this.address = host;
        this.port = port;
        this.id = id;
    }


    public void setId(NodeId id) {
        this.id = id;
    }

    public NodeId getId() {
        return id;
    }

    public String fullAddress() {
        return address + ":" + port;
    }

    /**
     * @return node info的编码，ID IP/Port 一共26个字节
     */
    public byte[] compactNodeInfo() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(id.getValue());
            baos.write(Utils.ip2bytes(address));
            baos.write(Utils.int2Bytes(port), 2, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    /**
     * @return node info的编码 IP/Port 一共6个字节
     */
    public byte[] compactIpPort() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(Utils.ip2bytes(address));
            baos.write(Utils.int2Bytes(port), 2, 2);
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
        if (address != null ? !address.equals(nodeInfo.address) : nodeInfo.address != null) return false;
        return id.equals(nodeInfo.id) && address.equals(nodeInfo.address);
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + Objects.hash(id);
        result = 31 * result + address.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
            "address='" + address + '\'' +
            ", port=" + port +
            ", id=" + String.valueOf(id) +
            '}';
    }

}
