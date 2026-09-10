package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ThreadLocalCleanupDemoTest {

    @Test
    void finallyRemovePreventsValueLeakAcrossReusedTasks() {
        ThreadLocalCleanupDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                ThreadLocalCleanupDemo::run);

        assertEquals("request-1", result.firstValue());
        assertNull(result.valueAfterCleanup());
    }
}
