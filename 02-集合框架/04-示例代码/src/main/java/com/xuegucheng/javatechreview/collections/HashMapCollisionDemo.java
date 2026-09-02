package com.xuegucheng.javatechreview.collections;

import java.util.HashMap;
import java.util.Map;

/** Demonstrates that a hash collision still needs equals to decide key identity. */
public final class HashMapCollisionDemo {

    private HashMapCollisionDemo() {
    }

    public static Map<Key, String> build() {
        var values = new HashMap<Key, String>();
        values.put(new Key("a"), "value-a");
        values.put(new Key("b"), "value-b");
        return values;
    }

    public record Key(String id) {
        @Override
        public int hashCode() {
            return 42;
        }
    }

    public static void main(String[] args) {
        var values = build();
        System.out.printf("size=%d, a=%s, b=%s%n", values.size(), values.get(new Key("a")), values.get(new Key("b")));
    }
}
