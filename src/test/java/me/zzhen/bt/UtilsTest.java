package me.zzhen.bt;

import me.zzhen.bt.utils.Utils;

import java.net.InetAddress;

import static org.junit.Assert.*;

/**
 * Project:CleanBT
 * Create Time: 16-12-13.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class UtilsTest {

    @org.junit.Test
    public void getExtName() throws Exception {
        assertEquals(Utils.getExtName("tt.ext"), "ext");
        assertEquals(Utils.getExtName("tt.img"), "img");
        assertEquals(Utils.getExtName("tt"), "tt");
        assertEquals(Utils.getExtName("tt.tt.img"), "img");
        assertEquals(Utils.getExtName(""), "");
    }


    @org.junit.Test
    public void toHex() throws Exception {
        assertEquals(Utils.toHex(new byte[]{1}), "01");
        assertEquals(Utils.toHex(new byte[]{2}), "02");
        assertEquals(Utils.toHex(new byte[]{-1}), "FF");
        assertEquals(Utils.toHex(new byte[]{0}), "00");
        assertEquals(Utils.toHex(new byte[]{127}), "7F");
        assertEquals(Utils.toHex(new byte[]{-128}), "80");
    }


    @org.junit.Test
    public void intToBytes() throws Exception {
        assertArrayEquals(Utils.intToBytes(1), new byte[]{0, 0, 0, 1});
    }

    @org.junit.Test
    public void hexToBytes() throws Exception {
        assertArrayEquals(Utils.hexToBytes("FF"), new byte[]{-1});
    }

    @org.junit.Test
    public void ipToBytes() throws Exception {
        String ip = "172.16.155.10";
        byte[] bytes = Utils.ipToBytes(ip);
        InetAddress addres = InetAddress.getByAddress(bytes);
        assertEquals(ip, addres.getHostAddress());
    }


}