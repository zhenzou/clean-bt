package me.zzhen.bt.common;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

/**
 * not thread safe
 *
 * @param <T>
 */
class SimpleChannel<T> implements Channel<T> {
    private BlockingQueue<T> channel;
    private boolean closed;
    private int bufSize;

    SimpleChannel(int bufSize) {
        this.channel = new LinkedBlockingDeque<>(bufSize);
        this.bufSize = bufSize;
    }

    private synchronized boolean hasNext() {
        return !closed || !channel.isEmpty();
    }

    /**
     * NOTE push not thread safe
     *
     * @param value
     */
    @Override
    public void push(T value) {
        if (closed) {
            throw new RuntimeException("closed");
        }
        try {
            channel.put(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return null if do not has next
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> next() {
        try {
            if (hasNext()) {
                return Optional.of(channel.take());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public int size() {
        return bufSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void close() throws InterruptedException {
        if (!closed) {
            closed = true;
        } else {
            throw new RuntimeException("closed");
        }
    }

    @Override
    public void foreach(Consumer<T> func) {
        while (hasNext()) {
            next().ifPresent(func);
        }
    }
}
