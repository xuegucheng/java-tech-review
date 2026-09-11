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

/**
 * 演示 ConcurrentHashMap 的按键原子初始化和合并更新。
 *
 * <p>这个示例验证：多个线程对同一个键执行 {@code computeIfAbsent} 时，映射函数不会被
 * 并发初始化出多个最终值；{@code merge} 可以安全累加共享计数。</p>
 */
public final class ConcurrentMapDemo {

    private ConcurrentMapDemo() {
    }

    /** 并发执行按键初始化和计数合并，返回可观察结果供测试断言。 */
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
                    // 只允许按键 answer 的首次缺失计算进入映射函数。
                    values.computeIfAbsent("answer", key -> {
                        mappingCalls.incrementAndGet();
                        return 42;
                    });
                    // merge 将每次增量和当前值合并，验证复合更新的并发安全性。
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

    /** 运行 ConcurrentHashMap 原子初始化与合并更新演示。 */
    public static void main(String[] args)
            throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println(run(8, 100));
    }

    /** 保存初始化结果、映射函数调用次数和合并后的计数。 */
    public record Result(int initializedValue, int mappingCalls, int mergedCount) {
    }
}
