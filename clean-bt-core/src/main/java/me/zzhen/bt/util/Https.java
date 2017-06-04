package me.zzhen.bt.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Project:CleanBT
 * Create Time: 17-6-4.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface Https {
    static byte[] get(String url) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.connect();
        String encoding = conn.getContentEncoding();
        InputStream in = null;
        if ("gzip".equals(encoding)) {
            in = new GZIPInputStream(conn.getInputStream());
        } else {
            in = conn.getInputStream();
        }
        return IO.readAllBytes(in);
    }
}
