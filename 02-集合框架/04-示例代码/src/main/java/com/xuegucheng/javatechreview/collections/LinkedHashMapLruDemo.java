package com.xuegucheng.javatechreview.collections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Demonstrates access-order and eldest-entry eviction for a small teaching LRU. */
public final class LinkedHashMapLruDemo {

    private LinkedHashMapLruDemo() {
    }

    public static List<Integer> sequenceAfterReadAndInsert() {
        var cache = new LruCache<Integer, String>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(2);
        cache.put(4, "four");
        return new ArrayList<>(cache.keySet());
    }

    public static final class LruCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxEntries;

        public LruCache(int maxEntries) {
            super(16, 0.75f, true);
            if (maxEntries < 1) {
                throw new IllegalArgumentException("maxEntries must be positive");
            }
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }

    public static void main(String[] args) {
        System.out.println(sequenceAfterReadAndInsert());
    }
}
