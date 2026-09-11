package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 演示线程池复用工作线程时的 ThreadLocal 清理。
 *
 * <p>这个示例验证：请求结束时调用 {@code remove}，后续复用同一工作线程的任务就不会读到
 * 上一个请求遗留的数据。</p>
 */
public final class ThreadLocalCleanupDemo {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private ThreadLocalCleanupDemo() {
    }

    /** 在同一线程池工作线程上连续执行两个任务，验证第一个任务的上下文已清理。 */
    public static Result run()
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = executor.submit(() -> {
                try {
                    CONTEXT.set("request-1");
                    // 第一个任务可以读到自己写入的请求上下文。
                    return CONTEXT.get();
                } finally {
                    // 请求结束必须清理，避免线程池复用导致上下文串线或内存滞留。
                    CONTEXT.remove();
                }
            });
            String firstValue = first.get(2, TimeUnit.SECONDS);

            // 单线程执行器保证第二个任务复用同一个工作线程。
            Future<String> second = executor.submit(CONTEXT::get);
            String valueAfterCleanup = second.get(2, TimeUnit.SECONDS);
            return new Result(firstValue, valueAfterCleanup);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    /** 运行 ThreadLocal 清理演示。 */
    public static void main(String[] args)
            throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println(run());
    }

    /** 保存第一次任务读取到的值和清理后第二次任务读取到的值。 */
    public record Result(String firstValue, String valueAfterCleanup) {
    }
}
