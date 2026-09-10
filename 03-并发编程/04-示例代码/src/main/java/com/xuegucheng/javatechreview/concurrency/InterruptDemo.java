package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shows interrupt delivery, InterruptedException, and restoring the status. */
public final class InterruptDemo {

    private InterruptDemo() {
    }

    public static Result run() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean caught = new AtomicBoolean();
        AtomicBoolean statusWasClearedInCatch = new AtomicBoolean();
        AtomicBoolean statusWasRestored = new AtomicBoolean();

        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                caught.set(true);
                statusWasClearedInCatch.set(!Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                statusWasRestored.set(Thread.currentThread().isInterrupted());
            } finally {
                finished.countDown();
            }
        }, "interrupt-demo-worker");

        worker.start();
        if (!started.await(1, TimeUnit.SECONDS)) {
            worker.interrupt();
            worker.join(1_000);
            throw new IllegalStateException("worker did not start");
        }

        worker.interrupt();
        boolean finishedInTime = finished.await(2, TimeUnit.SECONDS);
        worker.join(2_000);
        return new Result(
                true,
                caught.get(),
                statusWasClearedInCatch.get(),
                statusWasRestored.get(),
                finishedInTime && !worker.isAlive());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(run());
    }

    public record Result(
            boolean interruptSent,
            boolean interruptedExceptionCaught,
            boolean statusClearedInCatch,
            boolean statusRestored,
            boolean terminated) {
    }
}
