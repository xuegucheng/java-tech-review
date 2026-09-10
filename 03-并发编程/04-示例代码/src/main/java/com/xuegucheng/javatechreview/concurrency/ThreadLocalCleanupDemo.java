package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Demonstrates request-scoped ThreadLocal cleanup on a reused worker. */
public final class ThreadLocalCleanupDemo {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private ThreadLocalCleanupDemo() {
    }

    public static Result run()
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = executor.submit(() -> {
                try {
                    CONTEXT.set("request-1");
                    return CONTEXT.get();
                } finally {
                    CONTEXT.remove();
                }
            });
            String firstValue = first.get(2, TimeUnit.SECONDS);

            Future<String> second = executor.submit(CONTEXT::get);
            String valueAfterCleanup = second.get(2, TimeUnit.SECONDS);
            return new Result(firstValue, valueAfterCleanup);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args)
            throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println(run());
    }

    public record Result(String firstValue, String valueAfterCleanup) {
    }
}
