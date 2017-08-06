package me.zzhen.bt.dht.meta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class MetadataHolder {
    public MetadataState state = MetadataState.START;
    public ByteArrayOutputStream data = new ByteArrayOutputStream();
    public int totalPiece;
    public int currentPiece;
    public int size;
    public int ut;
    public byte[] hash;
    public InetSocketAddress address;

    public MetadataHolder(String address, int port, byte[] hash) {
        this.address = new InetSocketAddress(address, port);
        this.hash = hash;
    }

    public void next() {
        state = state.next();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataHolder holder = (MetadataHolder) o;

        return Arrays.equals(hash, holder.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }
}
