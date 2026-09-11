package com.xuegucheng.javatechreview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 演示最小可用的独占式 AQS 同步器。
 *
 * <p>这个示例验证：多个线程同时竞争同一个 Mutex 时，临界区内最多只有一个线程；
 * 同时也展示子类只实现获取/释放条件，排队、阻塞和唤醒由 AQS 负责。</p>
 */
public final class AqsLockDemo {

    private static final long TIMEOUT_SECONDS = 2;

    private AqsLockDemo() {
    }

    /** 启动多个竞争线程，验证独占锁不会允许多个线程同时进入临界区。 */
    public static Result run(int participants) throws InterruptedException {
        if (participants < 2) {
            throw new IllegalArgumentException("participants must be at least 2");
        }

        Mutex mutex = new Mutex();
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maximumInside = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        // 先让所有线程就绪，再同时放行，尽量制造真实的锁竞争。
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
                    // 统计临界区并发数量；正确结果应始终为 1。
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

    /** 运行 AQS 独占锁示例，输出参与线程数、完成数和临界区最大并发数。 */
    public static void main(String[] args) throws InterruptedException {
        System.out.println(run(8));
    }

    /** 只实现独占获取/释放条件的最小 AQS 同步器。 */
    public static final class Mutex extends AbstractQueuedSynchronizer {

        /** 进入 AQS 的独占获取流程；失败时由 AQS 负责排队和阻塞。 */
        public void lock() {
            acquire(1);
        }

        /** 只尝试一次独占获取，不排队，返回是否立即成功。 */
        public boolean tryLock() {
            return tryAcquire(1);
        }

        /** 进入 AQS 的独占释放流程，并尝试唤醒后继线程。 */
        public void unlock() {
            release(1);
        }

        /** 定义 Mutex 的获取条件：state 从 0 原子切换到 1。 */
        @Override
        protected boolean tryAcquire(int acquires) {
            if (acquires != 1) {
                throw new IllegalArgumentException("Mutex accepts exactly one permit");
            }
            Thread current = Thread.currentThread();
            if (compareAndSetState(0, 1)) {
                // 获取成功后记录独占持有者，供释放时校验线程身份。
                setExclusiveOwnerThread(current);
                return true;
            }
            // 获取失败只报告结果，排队和阻塞不在子类中实现。
            return false;
        }

        /** 定义 Mutex 的释放条件：只有当前持有者才能把 state 归零。 */
        @Override
        protected boolean tryRelease(int releases) {
            if (releases != 1 || getState() != 1
                    || getExclusiveOwnerThread() != Thread.currentThread()) {
                throw new IllegalMonitorStateException("current thread does not own the Mutex");
            }
            // 先清除持有者，再释放同步状态，让 AQS 可以唤醒后继线程。
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        /** 供 Condition 等能力判断当前线程是否独占持有 Mutex。 */
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1
                    && getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    /** 保存示例的可观察结果，便于测试直接验证并发互斥性。 */
    public record Result(int participants, int completed, int maximumInside) {
    }
}
