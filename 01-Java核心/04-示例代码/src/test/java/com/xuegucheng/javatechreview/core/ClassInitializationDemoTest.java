package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证静态内部类的初始化时机。 */
class ClassInitializationDemoTest {

    @Test
    /** 验证首次主动读取 Holder.VALUE 时才执行初始化方法。 */
    void holderInitializesOnFirstActiveUse() {
        assertEquals("ready", ClassInitializationDemo.value());
        assertEquals(List.of("initialized"), ClassInitializationDemo.events());
    }
}
