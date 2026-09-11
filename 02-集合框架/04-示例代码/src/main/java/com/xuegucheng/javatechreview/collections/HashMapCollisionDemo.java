package com.xuegucheng.javatechreview.collections;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示 HashMap 如何处理哈希冲突。
 *
 * <p>这个示例验证：两个 Key 即使返回相同的 hashCode，也不会因此被当成同一个键；
 * HashMap 还要通过 equals 判断键是否相等。</p>
 */
public final class HashMapCollisionDemo {

    private HashMapCollisionDemo() {
    }

    /** 构造两个哈希值相同但逻辑上不同的键，观察它们能否同时存入 HashMap。 */
    public static Map<Key, String> build() {
        var values = new HashMap<Key, String>();
        values.put(new Key("a"), "value-a");
        values.put(new Key("b"), "value-b");
        return values;
    }

    /** 教学用键：故意制造哈希冲突，但保留 record 默认的 equals 语义。 */
    public record Key(String id) {
        /** 故意返回固定值，让不同 id 的键落到同一个哈希桶。 */
        @Override
        public int hashCode() {
            return 42;
        }
    }

    /** 运行哈希冲突演示，打印最终大小和两个键的查询结果。 */
    public static void main(String[] args) {
        var values = build();
        System.out.printf("size=%d, a=%s, b=%s%n", values.size(), values.get(new Key("a")), values.get(new Key("b")));
    }
}
