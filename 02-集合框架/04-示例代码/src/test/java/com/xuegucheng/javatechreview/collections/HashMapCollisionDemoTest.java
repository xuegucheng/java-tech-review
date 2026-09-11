package com.xuegucheng.javatechreview.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 验证哈希冲突不会直接导致不同键被覆盖。 */
class HashMapCollisionDemoTest {

    @Test
    /** 验证相同 hashCode 的不同键仍由 equals 区分并可同时存储。 */
    void equalHashCodesDoNotMakeDifferentKeysEqual() {
        var values = HashMapCollisionDemo.build();

        assertEquals(2, values.size());
        assertEquals("value-a", values.get(new HashMapCollisionDemo.Key("a")));
        assertEquals("value-b", values.get(new HashMapCollisionDemo.Key("b")));
    }
}
