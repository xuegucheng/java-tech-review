package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对比 volatile 读改写竞态与原子类、synchronized 的结果。
 *
 * <p>这个示例验证：volatile 只能保证可见性，不能把“读取 + 加一 + 写回”变成原子操作；
 * AtomicInteger 和 synchronized 可以避免并发更新丢失。</p>
 */
public final class AtomicityDemo {

    private static final long ROUND_TIMEOUT_SECONDS = 2;

    private AtomicityDemo() {
    }

    /** 用相同的并发起点分别执行三种计数方式，比较最终结果。 */
    public static Result compare(int participants) throws InterruptedException {
        if (participants < 2) {
            throw new IllegalArgumentException("participants must be at least 2");
        }

        VolatileCounter volatileCounter = new VolatileCounter();
        // 故意拆开读和写，并让所有线程使用同一个快照，稳定制造覆盖写。
        runRound(participants, (ready, go) -> {
            int snapshot = volatileCounter.value;
            ready.countDown();
            go.await();
            volatileCounter.value = snapshot + 1;
        });

        AtomicInteger atomicCounter = new AtomicInteger();
        // incrementAndGet 把读改写封装成原子操作。
        runRound(participants, (ready, go) -> {
            ready.countDown();
            go.await();
            atomicCounter.incrementAndGet();
        });

        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        // synchronized 让同一时刻只有一个线程执行计数更新。
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

    /** 运行三种计数方式的对比示例。 */
    public static void main(String[] args) throws InterruptedException {
        System.out.println(compare(8));
    }

    /** 让一轮参与线程先全部就绪，再同时执行动作，保证对比具有相同起点。 */
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

        // ready 保证所有线程都进入动作，go 再统一放行，放大读改写冲突。
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

    /** 表示一轮并发计数中每个线程要执行的动作。 */
    @FunctionalInterface
    private interface RoundAction {
        void run(CountDownLatch ready, CountDownLatch go) throws InterruptedException;
    }

    /** 只具备可见性，不保证复合读改写操作的原子性。 */
    private static final class VolatileCounter {
        private volatile int value;
    }

    /** 通过外部 synchronized 保护普通计数值。 */
    private static final class SynchronizedCounter {
        private int value;
    }

    /** 保存三种实现的最终计数，供测试比较是否发生丢失更新。 */
    public record Result(
            int participants,
            int volatileValue,
            int atomicValue,
            int synchronizedValue) {
    }
}
