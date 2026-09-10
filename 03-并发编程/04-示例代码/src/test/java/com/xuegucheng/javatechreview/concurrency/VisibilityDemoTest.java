package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class VisibilityDemoTest {

    @Test
    void volatileStopSignalBecomesVisibleWithinBound() {
        boolean stopped = assertTimeoutPreemptively(
                Duration.ofSeconds(3),
                () -> VisibilityDemo.stopsAfterVolatileWrite(1_000));

        assertTrue(stopped);
    }
}
