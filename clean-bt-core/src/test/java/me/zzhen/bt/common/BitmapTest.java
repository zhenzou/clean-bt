package me.zzhen.bt.common;

//import me.zzhen.bt.dht.base.NodeKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Project:CleanBT
 * Create Time: 16-12-25.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class BitmapTest {


    @Test
    public void get() throws Exception {
        Bitmap bitmap = new Bitmap(10);
        assertEquals(false, bitmap.get(9));
        assertEquals(false, bitmap.get(8));
        assertEquals(false, bitmap.get(7));
        assertEquals(false, bitmap.get(0));
    }

    @Test
    public void set() throws Exception {
        Bitmap bitmap = new Bitmap(10);
        bitmap.set(0);
        bitmap.set(3);
        bitmap.set(5);
        assertEquals(true, bitmap.get(0));
        assertEquals(true, bitmap.get(3));
        assertEquals(true, bitmap.get(5));
        assertEquals(false, bitmap.get(4));
        assertEquals(false, bitmap.get(1));
        bitmap.unset(0);
        assertEquals(false, bitmap.get(0));
        assertEquals(true, bitmap.get(5));
    }

    @Test
    public void clear() throws Exception {
        Bitmap bitmap = new Bitmap(10);
        bitmap.unset(0);
        bitmap.unset(3);
        bitmap.unset(5);

    }

    @Test
    public void or() {
        Bitmap bitmap = new Bitmap(10);
        bitmap.set(0);
        bitmap.set(3);
        bitmap.set(5);
        Bitmap bitmap1 = new Bitmap(11);
        bitmap1.or(bitmap);
        bitmap1.set(10);
        assertEquals(true, bitmap1.get(0));
        assertEquals(true, bitmap1.get(3));
        assertEquals(true, bitmap1.get(5));
        assertEquals(false, bitmap1.get(4));
        assertEquals(false, bitmap1.get(9));
        assertEquals(true, bitmap1.get(10));

        bitmap.or(bitmap1);
        assertEquals(true, bitmap.get(0));
        assertEquals(true, bitmap1.get(3));
        assertEquals(true, bitmap1.get(5));
        assertEquals(false, bitmap1.get(1));
        assertEquals(false, bitmap1.get(4));

    }

    @Test
    public void random() {
//        NodeKey key = NodeKey.genRandomKey();
//        NodeKey pre = NodeKey.genRandomKey();
//        Bitmap bits = key.getBits();
//        Bitmap prefix = pre.getBits();
//        for (int i = 0; i < 10; i++) {
//            bits.set(i, prefix.get(i));
//        }
//        for (int i = 0; i < 10; i++) {
//            assertEquals(prefix.get(i), bits.get(i));
//            //            System.out.println(bits.get(i) + ":" + prefix.get(i));
//        }
    }
}