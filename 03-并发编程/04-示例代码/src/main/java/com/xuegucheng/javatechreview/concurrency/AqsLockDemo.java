package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/** Demonstrates the smallest useful exclusive AQS synchronizer. */
public final class AqsLockDemo {

    private static final long TIMEOUT_SECONDS = 2;

    private AqsLockDemo() {
    }

    public static Result run(int participants) throws InterruptedException {
        if (participants < 2) {
            throw new IllegalArgumentException("participants must be at least 2");
        }

        Mutex mutex = new Mutex();
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maximumInside = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(participants);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(participants);
        Thread[] threads = new Thread[participants];

        for (int i = 0; i < participants; i++) {
            threads[i] = new Thread(() -> {
                boolean locked = false;
                try {
                    ready.countDown();
                    start.await();
                    mutex.lock();
                    locked = true;
                    int current = inside.incrementAndGet();
                    maximumInside.accumulateAndGet(current, Math::max);
                    firstEntered.countDown();
                    if (!releaseHolder.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("holder release timed out");
                    }
                    completed.incrementAndGet();
                    inside.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (locked) {
                        mutex.unlock();
                    }
                    done.countDown();
                }
            }, "aqs-lock-demo-" + i);
            threads[i].start();
        }

        if (!ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            releaseHolder.countDown();
            throw new IllegalStateException("workers did not become ready");
        }
        start.countDown();
        if (!firstEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            releaseHolder.countDown();
            throw new IllegalStateException("no worker acquired the mutex");
        }
        releaseHolder.countDown();
        boolean finished = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        for (Thread thread : threads) {
            thread.join(TIMEOUT_SECONDS * 1_000);
        }
        if (!finished) {
            throw new IllegalStateException("workers did not finish");
        }
        return new Result(participants, completed.get(), maximumInside.get());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(run(8));
    }

    public static final class Mutex extends AbstractQueuedSynchronizer {

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        @Override
        protected boolean tryAcquire(int acquires) {
            if (acquires != 1) {
                throw new IllegalArgumentException("Mutex accepts exactly one permit");
            }
            Thread current = Thread.currentThread();
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(current);
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (releases != 1 || getState() != 1
                    || getExclusiveOwnerThread() != Thread.currentThread()) {
                throw new IllegalMonitorStateException("current thread does not own the Mutex");
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1
                    && getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    public record Result(int participants, int completed, int maximumInside) {
    }
}
