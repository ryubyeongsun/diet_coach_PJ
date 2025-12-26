package com.dietcoach.project.common.cache;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleTtlCache<V> {
    private static class Entry<V> {
        V value; long expireAt;
        Entry(V v, long e) { value=v; expireAt=e; }
    }
    private final ConcurrentHashMap<String, Entry<V>> map = new ConcurrentHashMap<>();

    public Optional<V> get(String key) {
        Entry<V> e = map.get(key);
        if (e == null) return Optional.empty();
        if (Instant.now().toEpochMilli() > e.expireAt) {
            map.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(e.value);
    }

    public void put(String key, V value, long ttlMillis) {
        map.put(key, new Entry<>(value, Instant.now().toEpochMilli() + ttlMillis));
    }
}
