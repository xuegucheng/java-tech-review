package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示有界线程池达到饱和后的执行策略。
 *
 * <p>这个示例验证：核心线程和最大线程都被占用、队列也已满时，
 * {@code CallerRunsPolicy} 会让提交任务的线程直接执行溢出任务，形成反压。</p>
 */
public final class ThreadPoolSaturationDemo {

    private static final long TIMEOUT_SECONDS = 2;

    private ThreadPoolSaturationDemo() {
    }

    /** 按“核心线程、队列、最大线程、溢出任务”的顺序填满线程池并观察 CallerRunsPolicy。 */
    public static Result run() throws InterruptedException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch thirdStarted = new CountDownLatch(1);
        CountDownLatch releaseHolders = new CountDownLatch(1);
        AtomicBoolean callerRan = new AtomicBoolean();
        AtomicInteger completed = new AtomicInteger();
        ThreadFactory factory = new NamedThreadFactory();

        // 容量为：1 个核心线程 + 1 个队列槽位 + 1 个非核心线程。
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
                // 第一个任务占住核心线程。
                firstStarted.countDown();
                awaitRelease(releaseHolders);
                completed.incrementAndGet();
            });
            if (!firstStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("core worker did not start");
            }

            executor.execute(completed::incrementAndGet);

            executor.execute(() -> {
                // 第三个任务触发创建最大线程，填满线程池的工作能力。
                thirdStarted.countDown();
                awaitRelease(releaseHolders);
                completed.incrementAndGet();
            });
            if (!thirdStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("maximum worker did not start");
            }

            executor.execute(() -> {
                // 此时线程和队列都满，任务由提交者线程直接执行。
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

    /** 让占用线程等待统一释放信号，用于稳定制造线程池饱和状态。 */
    private static void awaitRelease(CountDownLatch releaseHolders) {
        try {
            if (!releaseHolders.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("release timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 运行线程池饱和与 CallerRunsPolicy 反压演示。 */
    public static void main(String[] args) throws InterruptedException {
        System.out.println(run());
    }

    /** 为工作线程生成稳定、可读的名称，便于观察任务到底由谁执行。 */
    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        /** 创建带有递增编号的工作线程。 */
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "saturation-demo-" + sequence.incrementAndGet());
        }
    }

    /** 保存 CallerRunsPolicy 是否生效、完成任务数和最大线程数。 */
    public record Result(boolean callerRan, int completed, int largestPoolSize) {
    }
}
