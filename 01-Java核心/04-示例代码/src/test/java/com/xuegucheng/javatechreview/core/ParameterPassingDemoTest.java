package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 Java 传递的是引用值副本，而不是变量别名。 */
class ParameterPassingDemoTest {

    @Test
    /** 验证修改共享对象可见，但重新绑定形参不会影响调用方变量。 */
    void mutatingSharedObjectIsVisibleButRebindingParameterIsNot() {
        assertEquals(List.of("before", "changed"), ParameterPassingDemo.demonstrate());
    }
}
