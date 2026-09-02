package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClassInitializationDemoTest {

    @Test
    void holderInitializesOnFirstActiveUse() {
        assertEquals("ready", ClassInitializationDemo.value());
        assertEquals(List.of("initialized"), ClassInitializationDemo.events());
    }
}
