package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AqsLockDemoTest {

    @Test
    void teachingMutexAllowsOnlyOneCriticalSectionAtATime() {
        AqsLockDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> AqsLockDemo.run(8));

        assertEquals(result.participants(), result.completed());
        assertEquals(1, result.maximumInside());
    }
}
