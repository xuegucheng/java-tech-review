package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证重载解析的转换阶段顺序。 */
class OverloadResolutionDemoTest {

    @Test
    /** 验证精确匹配和基本类型/引用转换都优先于可变参数。 */
    void strictAndLoosePhasesPrecedeVarargs() {
        assertEquals(
                List.of("int", "int", "Integer", "Object", "varargs:2"),
                OverloadResolutionDemo.demonstrate()
        );
    }
}
