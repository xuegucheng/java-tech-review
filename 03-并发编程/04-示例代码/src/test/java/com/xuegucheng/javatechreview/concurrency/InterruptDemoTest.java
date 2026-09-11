package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** 验证中断是协作式取消信号，以及阻塞方法对中断状态的处理约定。 */
class InterruptDemoTest {

    @Test
    /** 验证中断能唤醒阻塞线程，异常中清除状态后又被业务代码恢复。 */
    void interruptIsAHandledCooperativeCancellationSignal() {
        InterruptDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(3),
                InterruptDemo::run);

        assertAll(
                () -> assertTrue(result.interruptSent()),
                () -> assertTrue(result.interruptedExceptionCaught()),
                () -> assertTrue(result.statusClearedInCatch()),
                () -> assertTrue(result.statusRestored()),
                () -> assertTrue(result.terminated()));
    }
}
