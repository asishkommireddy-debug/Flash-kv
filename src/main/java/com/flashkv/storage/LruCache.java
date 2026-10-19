package com.flashkv.storage;

import java.util.concurrent.ConcurrentHashMap;

public class LruCache {

    // Internal Doubly Linked List Node
    private static class Node {
        String key;
        String value;
        Node prev;
        Node next;

        Node(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final ConcurrentHashMap<String, Node> map;
    private final Node head;
    private final Node tail;

    public LruCache(int capacity) {
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>();

        // Dummy head and tail nodes eliminate null pointer edge cases!
        this.head = new Node("", "");
        this.tail = new Node("", "");
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    // O(1) GET: Fetches value and marks key as Most Recently Used
    public synchronized String get(String key) {
        Node node = map.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    // O(1) PUT: Inserts/Updates key. Evicts LRU key if capacity is exceeded.
    public synchronized String put(String key, String value) {
        Node node = map.get(key);

        if (node != null) {
            // Key exists: update value and move to front
            node.value = value;
            moveToHead(node);
            return null;
        }

        // New Key
        Node newNode = new Node(key, value);
        map.put(key, newNode);
        addToHead(newNode);

        // Check Capacity: Evict Least Recently Used (Tail)
        if (map.size() > capacity) {
            Node lru = removeTail();
            map.remove(lru.key);
            return lru.key; // Return evicted key name for logging/WAL tracking
        }

        return null;
    }

    // O(1) REMOVE: Deletes a key explicitly
    public synchronized boolean remove(String key) {
        Node node = map.remove(key);
        if (node != null) {
            removeNode(node);
            return true;
        }
        return false;
    }

    public synchronized boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public int getCapacity() {
        return capacity;
    }

    // --- Private Doubly Linked List Pointer Helpers ---

    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private Node removeTail() {
        Node lru = tail.prev;
        removeNode(lru);
        return lru;
    }
}