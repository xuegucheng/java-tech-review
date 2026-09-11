package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** 验证 volatile、AtomicInteger 和 synchronized 在复合更新上的差异。 */
class AtomicityDemoTest {

    @Test
    /** 验证 volatile 读改写会丢失更新，而原子类和 synchronized 不会。 */
    void atomicAndSynchronizedUpdatesDoNotLoseIncrements() {
        AtomicityDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> AtomicityDemo.compare(8));

        assertEquals(1, result.volatileValue());
        assertEquals(result.participants(), result.atomicValue());
        assertEquals(result.participants(), result.synchronizedValue());
    }
}
