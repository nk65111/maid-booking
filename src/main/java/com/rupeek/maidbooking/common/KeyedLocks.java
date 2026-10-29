package com.rupeek.maidbooking.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * One lock per key (e.g. per maid, per booking) so unrelated requests never block each other.
 */
@Component
public class KeyedLocks {

    // shortcut: locks are never evicted. Fine for an in-memory store; with a real DB this
    // would become a row lock / optimistic version check instead.
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
