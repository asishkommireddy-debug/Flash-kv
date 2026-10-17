package com.flashkv.storage;

import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore {
    // Value Store: Key -> Value
    private final ConcurrentHashMap<String, String> dataStore;
    // Expiration Store: Key -> Absolute Timestamp in Milliseconds
    private final ConcurrentHashMap<String, Long> ttlStore;

    public MemoryStore() {
        this.dataStore = new ConcurrentHashMap<>();
        this.ttlStore = new ConcurrentHashMap<>();
    }

    // SET key value
    public void set(String key, String value) {
        this.dataStore.put(key, value);
        this.ttlStore.remove(key); // New SET clears any previous expiration
    }

    // GET key (with Lazy Expiration check!)
    public String get(String key) {
        if (!this.dataStore.containsKey(key)) {
            return null;
        }

        // Lazy Expiration Check
        Long expireAt = this.ttlStore.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            delete(key); // Key is expired! Evict it immediately
            return null;
        }

        return this.dataStore.get(key);
    }

    // DEL key
    public boolean delete(String key) {
        this.ttlStore.remove(key);
        return this.dataStore.remove(key) != null;
    }

    // EXPIRE key seconds (returns true if timeout was set)
    public boolean expire(String key, long seconds) {
        if (!this.dataStore.containsKey(key)) {
            return false;
        }
        long expireAt = System.currentTimeMillis() + (seconds * 1000);
        this.ttlStore.put(key, expireAt);
        return true;
    }

    // TTL key (returns seconds remaining, -1 if persistent, -2 if key doesn't exist)
    public long ttl(String key) {
        if (!this.dataStore.containsKey(key)) {
            return -2;
        }

        Long expireAt = this.ttlStore.get(key);
        if (expireAt == null) {
            return -1; // Key exists, but has no expiration (persistent)
        }

        long remainingMillis = expireAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            delete(key);
            return -2;
        }

        return remainingMillis / 1000;
    }

    public boolean exists(String key) {
        return get(key) != null; // Leverages lazy check
    }

    public int size() {
        return this.dataStore.size();
    }
}