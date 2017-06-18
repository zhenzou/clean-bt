package me.zzhen.bt.util;

//import me.zzhen.bt.dht.base.NodeKey;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.InetAddress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Project:CleanBT
 * Create Time: 16-12-13.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class UtilsTest {


    @Rule
    public ExpectedException expect = ExpectedException.none();

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
        assertArrayEquals(Utils.int2Bytes(1), new byte[]{0, 0, 0, 1});
        InetAddress addr = InetAddress.getByName("82.221.103.244");
        byte[] address = addr.getAddress();

        assertEquals(addr, InetAddress.getByAddress(address));
    }

    @org.junit.Test
    public void bytes2Int() throws Exception {
        assertArrayEquals(Utils.int2Bytes(1), new byte[]{0, 0, 0, 1});
        InetAddress addr = InetAddress.getByName("82.221.103.244");
        byte[] address = addr.getAddress();

        assertEquals(addr, InetAddress.getByAddress(address));
    }

    @org.junit.Test
    public void hexToBytes() throws Exception {
        assertArrayEquals(Utils.hex2Bytes("FF"), new byte[]{-1});
        byte[] bytes = Utils.hex2Bytes("498b50");
        assertEquals(3, bytes.length);
        expect.expect(ArrayIndexOutOfBoundsException.class);
        InetAddress addr = Utils.getAddrFromBytes(bytes, 0);
        bytes = Utils.ip2bytes("172.16.155.10");
        addr = Utils.getAddrFromBytes(bytes, 0);
        assertEquals("172.16.155.10", addr.getAddress());
    }


    @org.junit.Test
    public void ip2Bytes() throws Exception {
        String[] correct = {"172.16.155.10", "172.16.155.12", "127.0.0.1", "127.0.0.1", "127.22.33.44"};
        for (String s : correct) {
            byte[] bytes = Utils.ip2bytes(s);
            InetAddress addres = InetAddress.getByAddress(bytes);
            assertEquals(s, addres.getHostAddress());
        }
        String tooBig = "1275.22.33.11";
        expect.expect(IllegalArgumentException.class);
        Utils.ip2bytes(tooBig);
        String tooLong = "127.22.33.25.26";
        expect.expect(IllegalArgumentException.class);
        Utils.ip2bytes(tooLong);
    }

    @Test
    public void bytes2Bin() {
        byte[] value = Utils.ip2bytes("172.16.155.10");
        String s = Utils.bytes2Bin(value);
        assertEquals("10101100000100001001101100001010", s);
    }

    @Test
    public void bitAt() {
        byte b = (byte) 0xEF;
        assertEquals(1, Utils.bitAt(b, 0));
        assertEquals(1, Utils.bitAt(b, 1));
        assertEquals(1, Utils.bitAt(b, 2));
        assertEquals(0, Utils.bitAt(b, 3));
        assertEquals(1, Utils.bitAt(b, 4));
        assertEquals(1, Utils.bitAt(b, 5));
        assertEquals(1, Utils.bitAt(b, 6));
        assertEquals(1, Utils.bitAt(b, 7));
        b = (byte) 0xFF;
        assertEquals(1, Utils.bitAt(b, 0));
        assertEquals(1, Utils.bitAt(b, 1));
        assertEquals(1, Utils.bitAt(b, 2));
        assertEquals(1, Utils.bitAt(b, 3));
        assertEquals(1, Utils.bitAt(b, 4));
        assertEquals(1, Utils.bitAt(b, 5));
        assertEquals(1, Utils.bitAt(b, 6));
        assertEquals(1, Utils.bitAt(b, 7));
        b = (byte) 0x00;
        assertEquals(0, Utils.bitAt(b, 0));
        assertEquals(0, Utils.bitAt(b, 1));
        assertEquals(0, Utils.bitAt(b, 2));
        assertEquals(0, Utils.bitAt(b, 3));
        assertEquals(0, Utils.bitAt(b, 4));
        assertEquals(0, Utils.bitAt(b, 5));
        assertEquals(0, Utils.bitAt(b, 6));
        assertEquals(0, Utils.bitAt(b, 7));
    }
}