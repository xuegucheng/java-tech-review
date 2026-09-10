package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Demonstrates bounded saturation and CallerRunsPolicy back pressure. */
public final class ThreadPoolSaturationDemo {

    private static final long TIMEOUT_SECONDS = 2;

    private ThreadPoolSaturationDemo() {
    }

    public static Result run() throws InterruptedException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch thirdStarted = new CountDownLatch(1);
        CountDownLatch releaseHolders = new CountDownLatch(1);
        AtomicBoolean callerRan = new AtomicBoolean();
        AtomicInteger completed = new AtomicInteger();
        ThreadFactory factory = new NamedThreadFactory();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                2,
                1,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());

        try {
            executor.execute(() -> {
                firstStarted.countDown();
                awaitRelease(releaseHolders);
                completed.incrementAndGet();
            });
            if (!firstStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("core worker did not start");
            }

            executor.execute(completed::incrementAndGet);

            executor.execute(() -> {
                thirdStarted.countDown();
                awaitRelease(releaseHolders);
                completed.incrementAndGet();
            });
            if (!thirdStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("maximum worker did not start");
            }

            executor.execute(() -> {
                callerRan.set(true);
                completed.incrementAndGet();
            });

            releaseHolders.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("executor did not terminate");
            }
            return new Result(callerRan.get(), completed.get(), executor.getLargestPoolSize());
        } finally {
            releaseHolders.countDown();
            executor.shutdownNow();
            executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void awaitRelease(CountDownLatch releaseHolders) {
        try {
            if (!releaseHolders.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("release timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(run());
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "saturation-demo-" + sequence.incrementAndGet());
        }
    }

    public record Result(boolean callerRan, int completed, int largestPoolSize) {
    }
}
