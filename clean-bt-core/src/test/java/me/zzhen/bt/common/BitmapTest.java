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
        assertEquals(false, bitmap.at(9));
        assertEquals(false, bitmap.at(8));
        assertEquals(false, bitmap.at(7));
        assertEquals(false, bitmap.at(0));
    }

    @Test
    public void set() throws Exception {
        Bitmap bitmap = new Bitmap(10);
        bitmap.set(0);
        bitmap.set(3);
        bitmap.set(5);
        assertEquals(true, bitmap.at(0));
        assertEquals(true, bitmap.at(3));
        assertEquals(true, bitmap.at(5));
        assertEquals(false, bitmap.at(4));
        assertEquals(false, bitmap.at(1));
        bitmap.unset(0);
        assertEquals(false, bitmap.at(0));
        assertEquals(true, bitmap.at(5));
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
        assertEquals(true, bitmap1.at(0));
        assertEquals(true, bitmap1.at(3));
        assertEquals(true, bitmap1.at(5));
        assertEquals(false, bitmap1.at(4));
        assertEquals(false, bitmap1.at(9));
        assertEquals(true, bitmap1.at(10));

        bitmap.or(bitmap1);
        assertEquals(true, bitmap.at(0));
        assertEquals(true, bitmap1.at(3));
        assertEquals(true, bitmap1.at(5));
        assertEquals(false, bitmap1.at(1));
        assertEquals(false, bitmap1.at(4));

    }
}