package com.xuegucheng.javatechreview.concurrency;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证自定义 AQS 独占同步器的互斥性和持有者校验。 */
class AqsLockDemoTest {

    @Test
    /** 验证多个线程并发竞争时，临界区最大并发数始终为 1。 */
    void teachingMutexAllowsOnlyOneCriticalSectionAtATime() {
        AqsLockDemo.Result result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> AqsLockDemo.run(8));

        assertEquals(result.participants(), result.completed());
        assertEquals(1, result.maximumInside());
    }

    @Test
    /** 验证未持有锁、重复释放锁都会抛出非法监视器状态异常。 */
    void unlockWithoutOwnershipThrowsIllegalMonitorStateException() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            AqsLockDemo.Mutex mutex = new AqsLockDemo.Mutex();

            // 未持有锁的线程直接 unlock：越权释放，抛 IllegalMonitorStateException
            assertThrows(IllegalMonitorStateException.class, mutex::unlock);

            // 持有者正常配对 unlock 后 state 归零；再次 unlock 同样是越权释放
            mutex.lock();
            mutex.unlock();
            assertThrows(IllegalMonitorStateException.class, mutex::unlock);
        });
    }

    @Test
    /** 验证其他线程不能越权释放锁，原持有者随后仍能正常解锁和再次加锁。 */
    void unlockFromAnotherThreadWhileHeldThrowsIllegalMonitorStateException() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            AqsLockDemo.Mutex mutex = new AqsLockDemo.Mutex();
            mutex.lock();

            // 线程 A 持有锁，线程 B 越权 unlock：owner 校验失败
            AtomicReference<Throwable> thrownByOther = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            Thread other = new Thread(() -> {
                try {
                    mutex.unlock();
                } catch (Throwable t) {
                    thrownByOther.set(t);
                } finally {
                    done.countDown();
                }
            }, "aqs-lock-foreign-unlocker");
            other.start();
            assertTrue(done.await(2, TimeUnit.SECONDS), "foreign unlock thread did not finish");
            other.join(2_000);

            assertTrue(thrownByOther.get() instanceof IllegalMonitorStateException,
                    "expected IllegalMonitorStateException but got: " + thrownByOther.get());

            // 持有者仍可正常释放，锁未被越权解锁破坏
            mutex.unlock();
            mutex.lock();
            mutex.unlock();
        });
    }
}
