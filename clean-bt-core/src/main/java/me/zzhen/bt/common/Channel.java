package me.zzhen.bt.common;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Description:
 * A simple channel like chan in golang
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public interface Channel<T> {


    static <T> Channel<T> simpleChannel(int size) {
        return new SimpleChannel<>(size);
    }

    void push(T value);

    Optional<T> next();

    int size();

    void close() throws InterruptedException;

    default void foreach(Consumer<T> func) {
        Optional<T> next;
        while (((next = next()).isPresent())) {
            func.accept(next.get());
        }
    }
}

