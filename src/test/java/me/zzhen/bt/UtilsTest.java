package me.zzhen.bt;

import me.zzhen.bt.dht.base.NodeKey;
import me.zzhen.bt.util.Utils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        InetAddress addr = InetAddress.getByName("82.221.103.244");
        byte[] address = addr.getAddress();

        assertEquals(addr, InetAddress.getByAddress(address));
    }

    @org.junit.Test
    public void bytesToInt() throws Exception {
        assertArrayEquals(Utils.intToBytes(1), new byte[]{0, 0, 0, 1});
        InetAddress addr = InetAddress.getByName("82.221.103.244");
        byte[] address = addr.getAddress();

        assertEquals(addr, InetAddress.getByAddress(address));
    }

    @org.junit.Test
    public void hexToBytes() throws Exception {
//        assertArrayEquals(Utils.hex2Bytes("FF"), new byte[]{-1});
//        byte[] bytes = Utils.hex2Bytes("498b50");
//        System.out.println(bytes.length);
//        InetAddress addr = IO.getAddrFromBytes(bytes, 0);
//        System.out.println(addr.getHostAddress());
//        System.out.println(Utils.bytesToInt(bytes, 4, 2));
        byte[] bytes = Utils.hex2Bytes("64313a65693165343a6970763434");
        System.out.println(new String(bytes));
    }


    @Rule
    public ExpectedException expect = ExpectedException.none();

    @org.junit.Test
    public void ipToBytes() throws Exception {
        String[] correct = {"172.16.155.10", "172.16.155.12", "127.0.0.1", "127.0.0.1", "127.22.33.44"};
        for (String s : correct) {
            byte[] bytes = Utils.ipToBytes(s);
            InetAddress addres = InetAddress.getByAddress(bytes);
            assertEquals(s, addres.getHostAddress());
        }
        String tooBig = "1275.22.33.11";
        expect.expect(IllegalArgumentException.class);
        Utils.ipToBytes(tooBig);
        String tooLong = "127.22.33.25.26";
        expect.expect(IllegalArgumentException.class);
        Utils.ipToBytes(tooLong);
    }

    @Test
    public void bytesToBin() {
        byte[] value = NodeKey.genRandomKey().getValue();
        String s = Utils.bytesToBin(value);
        System.out.println(s);
        System.out.println(s.length());
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