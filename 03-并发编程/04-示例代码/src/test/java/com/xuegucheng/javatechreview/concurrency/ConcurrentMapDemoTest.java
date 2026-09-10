package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ConcurrentMapDemoTest {

    @Test
    void perKeyInitializationAndMergeAreAtomicMapOperations() {
        int workers = 8;
        int incrementsPerWorker = 100;
        ConcurrentMapDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> ConcurrentMapDemo.run(workers, incrementsPerWorker));

        assertEquals(42, result.initializedValue());
        assertEquals(1, result.mappingCalls());
        assertEquals(workers * incrementsPerWorker, result.mergedCount());
    }
}
