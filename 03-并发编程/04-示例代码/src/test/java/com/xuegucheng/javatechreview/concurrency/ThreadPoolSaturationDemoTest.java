package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/** 验证有界线程池饱和时 CallerRunsPolicy 的反压行为。 */
class ThreadPoolSaturationDemoTest {

    @Test
    /** 验证溢出任务由提交者线程执行，且线程池最多创建两个工作线程。 */
    void callerRunsPolicyExecutesTheOverflowTaskInTheSubmitter() {
        ThreadPoolSaturationDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                ThreadPoolSaturationDemo::run);

        assertTrue(result.callerRan());
        assertEquals(4, result.completed());
        assertEquals(2, result.largestPoolSize());
    }
}
