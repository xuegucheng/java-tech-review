package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Demonstrates a bounded stop signal published through a volatile field. */
public final class VisibilityDemo {

    private VisibilityDemo() {
    }

    public static boolean stopsAfterVolatileWrite(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }

        StopSignal signal = new StopSignal();
        Thread worker = new Thread(() -> {
            signal.started.countDown();
            while (signal.running) {
                Thread.onSpinWait();
            }
            signal.stopped.countDown();
        }, "visibility-demo-worker");

        worker.start();
        boolean started = signal.started.await(timeoutMillis, TimeUnit.MILLISECONDS);
        signal.running = false;

        boolean stopped = signal.stopped.await(timeoutMillis, TimeUnit.MILLISECONDS);
        worker.join(timeoutMillis);
        return started && stopped && !worker.isAlive();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("stopped=" + stopsAfterVolatileWrite(1_000));
    }

    private static final class StopSignal {
        private volatile boolean running = true;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
    }
}
