package com.flashkv.storage;

import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore {
    // 1. The Core In-Memory Engine is now bounded by our O(1) LRU Cache!
    private final LruCache cache;
    // 2. Expiration Store: Key -> Absolute Timestamp (Millis)
    private final ConcurrentHashMap<String, Long> ttlStore;

    // Default constructor: 10,000 keys limit
    public MemoryStore() {
        this(10000);
    }

    // Configurable capacity constructor
    public MemoryStore(int capacity) {
        this.cache = new LruCache(capacity);
        this.ttlStore = new ConcurrentHashMap<>();
    }

    // SET key value (with automatic LRU eviction)
    public void set(String key, String value) {
        String evictedKey = this.cache.put(key, value);
        this.ttlStore.remove(key); // Clear any old TTL on overwrite

        // If capacity was exceeded, an old key was kicked out!
        if (evictedKey != null) {
            this.ttlStore.remove(evictedKey);
            System.out.println("♻️ [LRU Eviction] Memory limit reached. Evicted oldest key: [" + evictedKey + "]");
        }
    }

    // GET key (moves key to head of LRU, with lazy expiration check)
    public String get(String key) {
        if (!this.cache.containsKey(key)) {
            return null;
        }

        // Lazy Expiration Check
        Long expireAt = this.ttlStore.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            delete(key);
            return null;
        }

        // O(1) Fetch + marks as Most Recently Used
        return this.cache.get(key);
    }

    // DEL key
    public boolean delete(String key) {
        this.ttlStore.remove(key);
        return this.cache.remove(key);
    }

    // EXPIRE key seconds
    public boolean expire(String key, long seconds) {
        if (!this.cache.containsKey(key)) {
            return false;
        }
        long expireAt = System.currentTimeMillis() + (seconds * 1000);
        this.ttlStore.put(key, expireAt);
        return true;
    }

    // TTL key
    public long ttl(String key) {
        if (!this.cache.containsKey(key)) {
            return -2;
        }

        Long expireAt = this.ttlStore.get(key);
        if (expireAt == null) {
            return -1;
        }

        long remainingMillis = expireAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            delete(key);
            return -2;
        }

        return remainingMillis / 1000;
    }

    public boolean exists(String key) {
        return get(key) != null;
    }

    public int size() {
        return this.cache.size();
    }

    public int getCapacity() {
        return this.cache.getCapacity();
    }
}