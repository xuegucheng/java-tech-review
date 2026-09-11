package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 验证重载按静态类型选择、重写按运行时类型分派。 */
class PolymorphismDispatchDemoTest {

    @Test
    /** 验证父类型引用和子类型引用得到不同的重载结果。 */
    void parentReferenceSelectsParentVisibleOverloadThenDogOverride() {
        assertEquals("dog-object", PolymorphismDispatchDemo.throughParentType());
        assertEquals("dog-string", PolymorphismDispatchDemo.throughChildType());
    }
}
