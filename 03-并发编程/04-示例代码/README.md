# 并发编程示例代码

这里放可运行的 Java 21 + JUnit 5 小实验。正文负责原理、面试表达和源码路径；完整并发控制代码集中在本目录，并为每个示例提供稳定测试。

## 运行

仓库根目录执行：

~~~powershell
.\mvnw.cmd -pl '03-并发编程/04-示例代码' test
~~~

也可以运行全部示例模块：

~~~powershell
.\mvnw.cmd test
~~~

## 示例索引

| 示例 | 观察目标 | 主类 | 测试 |
| --- | --- | --- | --- |
| VisibilityDemo | volatile 标志的可见性与有界停止 | [VisibilityDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemo.java) | [VisibilityDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemoTest.java) |
| AtomicityDemo | volatile 复合更新、AtomicInteger 与 synchronized | [AtomicityDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java) | [AtomicityDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemoTest.java) |
| InterruptDemo | interrupt、InterruptedException 和状态恢复 | [InterruptDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/InterruptDemo.java) | [InterruptDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/InterruptDemoTest.java) |
| AqsLockDemo | AQS 的 state、tryAcquire 和 tryRelease | [AqsLockDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/AqsLockDemo.java) | [AqsLockDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/AqsLockDemoTest.java) |
| ThreadPoolSaturationDemo | 有界队列、maximumPoolSize 和 CallerRunsPolicy | [ThreadPoolSaturationDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/ThreadPoolSaturationDemo.java) | [ThreadPoolSaturationDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/ThreadPoolSaturationDemoTest.java) |
| ThreadLocalCleanupDemo | 线程池复用下的 set/remove 责任 | [ThreadLocalCleanupDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/ThreadLocalCleanupDemo.java) | [ThreadLocalCleanupDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/ThreadLocalCleanupDemoTest.java) |
| ConcurrentMapDemo | computeIfAbsent 与 merge 的并发复合操作 | [ConcurrentMapDemo.java](src/main/java/com/xuegucheng/javatechreview/concurrency/ConcurrentMapDemo.java) | [ConcurrentMapDemoTest.java](src/test/java/com/xuegucheng/javatechreview/concurrency/ConcurrentMapDemoTest.java) |

示例是教学边界验证，不是生产锁、线程池或缓存实现。并发测试使用 CountDownLatch、Future 超时或有界等待，不依赖长时间 sleep 和特定线程调度顺序。
