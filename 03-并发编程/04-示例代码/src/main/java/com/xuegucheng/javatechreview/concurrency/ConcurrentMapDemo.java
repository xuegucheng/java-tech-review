package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Demonstrates atomic per-key initialization and merge on ConcurrentHashMap. */
public final class ConcurrentMapDemo {

    private ConcurrentMapDemo() {
    }

    public static Result run(int workers, int incrementsPerWorker)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (workers < 2 || incrementsPerWorker < 1) {
            throw new IllegalArgumentException("workers and increments must be positive");
        }

        ConcurrentHashMap<String, Integer> values = new ConcurrentHashMap<>();
        AtomicInteger mappingCalls = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        Future<?>[] futures = new Future<?>[workers];

        try {
            for (int i = 0; i < workers; i++) {
                futures[i] = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    values.computeIfAbsent("answer", key -> {
                        mappingCalls.incrementAndGet();
                        return 42;
                    });
                    for (int increment = 0; increment < incrementsPerWorker; increment++) {
                        values.merge("count", 1, Integer::sum);
                    }
                    return null;
                });
            }

            if (!ready.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("workers did not become ready");
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(2, TimeUnit.SECONDS);
            }
            return new Result(
                    values.get("answer"),
                    mappingCalls.get(),
                    values.getOrDefault("count", 0));
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println(run(8, 100));
    }

    public record Result(int initializedValue, int mappingCalls, int mergedCount) {
    }
}
