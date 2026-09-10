package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class InterruptDemoTest {

    @Test
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
