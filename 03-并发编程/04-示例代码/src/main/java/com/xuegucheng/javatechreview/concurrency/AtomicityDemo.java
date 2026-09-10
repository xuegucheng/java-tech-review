package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Compares a forced volatile read-modify-write race with atomic alternatives. */
public final class AtomicityDemo {

    private static final long ROUND_TIMEOUT_SECONDS = 2;

    private AtomicityDemo() {
    }

    public static Result compare(int participants) throws InterruptedException {
        if (participants < 2) {
            throw new IllegalArgumentException("participants must be at least 2");
        }

        VolatileCounter volatileCounter = new VolatileCounter();
        runRound(participants, (ready, go) -> {
            int snapshot = volatileCounter.value;
            ready.countDown();
            go.await();
            volatileCounter.value = snapshot + 1;
        });

        AtomicInteger atomicCounter = new AtomicInteger();
        runRound(participants, (ready, go) -> {
            ready.countDown();
            go.await();
            atomicCounter.incrementAndGet();
        });

        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        runRound(participants, (ready, go) -> {
            ready.countDown();
            go.await();
            synchronized (synchronizedCounter) {
                synchronizedCounter.value++;
            }
        });

        return new Result(
                participants,
                volatileCounter.value,
                atomicCounter.get(),
                synchronizedCounter.value);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(compare(8));
    }

    private static void runRound(int participants, RoundAction action) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(participants);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(participants);
        Thread[] threads = new Thread[participants];

        for (int i = 0; i < participants; i++) {
            threads[i] = new Thread(() -> {
                try {
                    action.run(ready, go);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "atomicity-demo-" + i);
            threads[i].start();
        }

        boolean allReady = ready.await(ROUND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        go.countDown();
        boolean allDone = done.await(ROUND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        for (Thread thread : threads) {
            thread.join(ROUND_TIMEOUT_SECONDS * 1_000);
        }
        if (!allReady || !allDone) {
            throw new IllegalStateException("round did not finish within the timeout");
        }
    }

    @FunctionalInterface
    private interface RoundAction {
        void run(CountDownLatch ready, CountDownLatch go) throws InterruptedException;
    }

    private static final class VolatileCounter {
        private volatile int value;
    }

    private static final class SynchronizedCounter {
        private int value;
    }

    public record Result(
            int participants,
            int volatileValue,
            int atomicValue,
            int synchronizedValue) {
    }
}
