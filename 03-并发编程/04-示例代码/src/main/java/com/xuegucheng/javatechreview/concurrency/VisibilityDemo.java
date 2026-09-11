package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 演示通过 volatile 字段发布停止信号。
 *
 * <p>这个示例验证：工作线程持续读取 volatile 标志位时，另一个线程写入 false 后，
 * 工作线程可以在限定时间内观察到变化并退出。</p>
 */
public final class VisibilityDemo {

    private VisibilityDemo() {
    }

    /** 启动轮询线程并写入 volatile 停止信号，验证可见性和有界退出。 */
    public static boolean stopsAfterVolatileWrite(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }

        StopSignal signal = new StopSignal();
        Thread worker = new Thread(() -> {
            signal.started.countDown();
            // running 是 volatile，写线程修改后这里可以观察到最新值。
            while (signal.running) {
                Thread.onSpinWait();
            }
            signal.stopped.countDown();
        }, "visibility-demo-worker");

        worker.start();
        boolean started = signal.started.await(timeoutMillis, TimeUnit.MILLISECONDS);
            // 发布停止信号；本示例关注可见性，不使用锁保护这个字段。
        signal.running = false;

        boolean stopped = signal.stopped.await(timeoutMillis, TimeUnit.MILLISECONDS);
        worker.join(timeoutMillis);
        return started && stopped && !worker.isAlive();
    }

    /** 运行 volatile 可见性演示。 */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("stopped=" + stopsAfterVolatileWrite(1_000));
    }

    /** 保存 volatile 停止标志以及两个用于协调测试时序的闩锁。 */
    private static final class StopSignal {
        private volatile boolean running = true;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
    }
}
