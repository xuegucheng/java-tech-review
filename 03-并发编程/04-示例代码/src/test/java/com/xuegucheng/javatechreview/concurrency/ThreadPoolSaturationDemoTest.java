package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ThreadPoolSaturationDemoTest {

    @Test
    void callerRunsPolicyExecutesTheOverflowTaskInTheSubmitter() {
        ThreadPoolSaturationDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                ThreadPoolSaturationDemo::run);

        assertTrue(result.callerRan());
        assertEquals(4, result.completed());
        assertEquals(2, result.largestPoolSize());
    }
}
