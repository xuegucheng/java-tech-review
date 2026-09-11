package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 演示中断信号的传递、InterruptedException 和中断状态恢复。
 *
 * <p>这个示例验证：中断是一种协作式取消信号；阻塞方法抛出 InterruptedException 后，
 * 当前线程的中断状态通常已被清除，业务代码需要根据约定恢复它。</p>
 */
public final class InterruptDemo {

    private InterruptDemo() {
    }

    /** 启动阻塞线程并发送中断，观察异常、状态清除和状态恢复过程。 */
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
                // 进入 catch 时，sleep 已响应中断，线程的中断状态已经被清除。
                caught.set(true);
                statusWasClearedInCatch.set(!Thread.currentThread().isInterrupted());
                // 将取消信号继续向上层传播，避免吞掉调用方的中断意图。
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

    /** 运行中断协作演示并打印最终观测结果。 */
    public static void main(String[] args) throws InterruptedException {
        System.out.println(run());
    }

    /** 保存中断发送、异常处理和线程结束状态，便于测试逐项验证。 */
    public record Result(
            boolean interruptSent,
            boolean interruptedExceptionCaught,
            boolean statusClearedInCatch,
            boolean statusRestored,
            boolean terminated) {
    }
}
