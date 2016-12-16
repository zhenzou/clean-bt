package me.zzhen.bt.dht;

import me.zzhen.bt.utils.IO;
import me.zzhen.bt.utils.Utils;

import java.io.*;
import java.net.*;
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
        port = Utils.bytes2Int(bytes, 24, 2);
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
        this(IO.getSomeByte(bytes, offset, 26));
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

    public boolean prefix(int i) {
        if (i >= 160 || i < 1) throw new RuntimeException("prefix of node should smaller than 160 and bigger than 1");

        return key.getValue()[(i - 1) / 8] >>> i % 8 == 1;
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
            baos.write(address.getAddress());
            baos.write((char) port);
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

    public static void main(String[] args) {
        try {
            URL url = new URL("http://www.baidu.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream read = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(read));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            read.close();
            System.out.println(sb.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
