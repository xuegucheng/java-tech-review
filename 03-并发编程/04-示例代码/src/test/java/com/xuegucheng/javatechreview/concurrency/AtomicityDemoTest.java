package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AtomicityDemoTest {

    @Test
    void atomicAndSynchronizedUpdatesDoNotLoseIncrements() {
        AtomicityDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> AtomicityDemo.compare(8));

        assertEquals(1, result.volatileValue());
        assertEquals(result.participants(), result.atomicValue());
        assertEquals(result.participants(), result.synchronizedValue());
    }
}
