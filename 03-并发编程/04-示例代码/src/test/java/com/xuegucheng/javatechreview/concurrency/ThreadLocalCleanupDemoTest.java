package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** 验证线程池复用线程时 ThreadLocal 上下文不会跨任务泄漏。 */
class ThreadLocalCleanupDemoTest {

    @Test
    /** 验证 finally remove 后，后续任务读取不到上一个请求留下的值。 */
    void finallyRemovePreventsValueLeakAcrossReusedTasks() {
        ThreadLocalCleanupDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                ThreadLocalCleanupDemo::run);

        assertEquals("request-1", result.firstValue());
        assertNull(result.valueAfterCleanup());
    }
}
