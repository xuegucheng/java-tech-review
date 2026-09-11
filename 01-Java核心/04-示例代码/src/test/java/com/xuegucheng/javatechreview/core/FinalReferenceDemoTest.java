package com.xuegucheng.javatechreview.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证 final 引用与引用对象状态之间的区别。 */
class FinalReferenceDemoTest {

    @Test
    /** 验证 final 引用仍可修改对象，但已经生成的快照保持不变。 */
    void finalReferenceMayMutateObjectButSnapshotDoesNotChange() {
        var result = FinalReferenceDemo.demonstrate();
        assertEquals(List.of("A", "B", "C"), result.mutableReference());
        assertEquals(List.of("A", "B"), result.immutableSnapshot());
    }
}
