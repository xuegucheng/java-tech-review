package com.xuegucheng.javatechreview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashMapCollisionDemoTest {

    @Test
    void equalHashCodesDoNotMakeDifferentKeysEqual() {
        var values = HashMapCollisionDemo.build();

        assertEquals(2, values.size());
        assertEquals("value-a", values.get(new HashMapCollisionDemo.Key("a")));
        assertEquals("value-b", values.get(new HashMapCollisionDemo.Key("b")));
    }
}
