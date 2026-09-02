package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PolymorphismDispatchDemoTest {

    @Test
    void parentReferenceSelectsParentVisibleOverloadThenDogOverride() {
        assertEquals("dog-object", PolymorphismDispatchDemo.throughParentType());
        assertEquals("dog-string", PolymorphismDispatchDemo.throughChildType());
    }
}
