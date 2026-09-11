package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** 验证 volatile 停止信号能够在限定时间内被工作线程观察到。 */
class VisibilityDemoTest {

    @Test
    /** 验证写线程更新 volatile 后，轮询线程能够退出而不是永久自旋。 */
    void volatileStopSignalBecomesVisibleWithinBound() {
        boolean stopped = assertTimeoutPreemptively(
                Duration.ofSeconds(3),
                () -> VisibilityDemo.stopsAfterVolatileWrite(1_000));

        assertTrue(stopped);
    }
}
