package com.xuegucheng.javatechreview.collections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 演示 LinkedHashMap 的访问顺序和最老条目淘汰机制。
 *
 * <p>这个示例验证：读取会改变访问顺序，插入新条目超过容量后，
 * {@code removeEldestEntry} 可以淘汰最老的条目。</p>
 */
public final class LinkedHashMapLruDemo {

    private LinkedHashMapLruDemo() {
    }

    /** 先读取键 2，再插入键 4，观察访问顺序和容量淘汰结果。 */
    public static List<Integer> sequenceAfterReadAndInsert() {
        var cache = new LruCache<Integer, String>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.get(2);
        cache.put(4, "four");
        return new ArrayList<>(cache.keySet());
    }

    /** 基于 LinkedHashMap 的最小 LRU 教学实现。 */
    public static final class LruCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxEntries;

        /** 创建按访问顺序维护、且具有最大条目数的缓存。 */
        public LruCache(int maxEntries) {
            super(16, 0.75f, true);
            if (maxEntries < 1) {
                throw new IllegalArgumentException("maxEntries must be positive");
            }
            this.maxEntries = maxEntries;
        }

        /** 当容量超限时淘汰最老条目，表达 LRU 的淘汰边界。 */
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }

    /** 运行访问顺序与 LRU 淘汰演示。 */
    public static void main(String[] args) {
        System.out.println(sequenceAfterReadAndInsert());
    }
}
