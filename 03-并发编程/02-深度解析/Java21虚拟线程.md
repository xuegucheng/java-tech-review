# Java 21 虚拟线程

> 面试定位：大量阻塞 I/O 场景的线程资源模型
> Java 版本：Java 21 正式虚拟线程；后续 JDK 行为单独标记
> P0/P1：P1
> 前置知识：线程池、阻塞调用、取消和资源容量
> 本文不负责：Structured Concurrency、ScopedValue 和后续预览特性

## 先说结论

Virtual Thread 不是更快的 Platform Thread，也不会让 CPU 密集计算自动变快。它主要降低大量阻塞任务使用线程的资源成本：虚拟线程阻塞时，在支持的阻塞点可以卸载，让 carrier platform thread 去运行其他虚拟线程。

Java 21 中应关注 platform thread、virtual thread、carrier thread 和阻塞行为的关系；不要把后续 JDK 对 pinning 等实现的优化倒灌成 Java 21 的固定事实。

版本入口：[Java SE 21 Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html)；`ofVirtual`、`startVirtualThread` 和 `isVirtual` 均以 Java 21 API 为准。

## 30 秒回答

> 平台线程通常直接映射到操作系统线程，创建和阻塞成本较高；虚拟线程由 JVM 调度到少量 carrier platform threads 上。对大量阻塞 I/O、每请求一个轻量执行单元的服务，虚拟线程可以减少线程资源成本，让同步写法保留较好的可读性。它不增加 CPU 核心，也不让 CPU 密集任务更快；数据库连接、远程服务、文件描述符和下游限流仍然是瓶颈。Java 21 中长时间在 synchronized 或 native 调用内阻塞可能 pin carrier，因此要关注临界区和阻塞点；虚拟线程不应像平台线程一样放进固定大小线程池，但资源并发仍需要 semaphore、连接池和业务限流。

## 三种线程角色

~~~text
Virtual Thread：大量、轻量、由 JVM 管理
        ↓ 调度
Carrier Thread：少量平台线程，实际运行虚拟线程
        ↓
OS Thread：操作系统调度实体
~~~

平台线程池的思路是“限制线程数量”；虚拟线程的思路更接近“每个任务一个虚拟线程，另外限制真正稀缺的资源”。

## 创建方式

Java 21 提供：

~~~java
Thread.startVirtualThread(() -> handle());

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handle());
}
~~~

newVirtualThreadPerTaskExecutor 是每个任务创建一个虚拟线程的执行器，不是固定复用少量虚拟线程的池。不要把虚拟线程当作需要池化的稀缺线程对象。

## 为什么阻塞 I/O 受益

传统平台线程执行阻塞 I/O 时，OS 线程通常也被占用，线程数多会增加栈内存、调度和上下文切换成本。虚拟线程在支持的阻塞点可以挂起自身，把 carrier 让给其他虚拟线程：

~~~text
虚拟线程 A → 阻塞 I/O → 让出 carrier
虚拟线程 B → 获得 carrier → 继续运行
I/O 完成 → A 重新调度
~~~

这不代表外部服务可以无限并发。连接池、服务端线程、限流、带宽和 CPU 仍然需要显式上限。

## synchronized 与 pinning

Java 21 需要注意：虚拟线程在 synchronized 保护的代码中执行阻塞操作时，可能 pin 住 carrier，使 carrier 不能高效承载其他虚拟线程；native 调用也可能有类似影响。

工程建议：

- 不要在长时间阻塞调用外层持有 synchronized；
- 缩短 synchronized 临界区；
- 需要虚拟线程友好的等待协议时评估 ReentrantLock；
- 通过压测确认具体 JDK、平台和库的行为；
- 不把“ReentrantLock 永远解决 pinning”写成无条件保证。

后续版本确有变化：从 JDK 24 起，[JEP 491：Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491) 调整了 monitor 实现，使虚拟线程在 `synchronized` 中阻塞时也能释放 carrier，消除了几乎所有由 `synchronized` 引起的 pinning；native 或 foreign 调用仍可能 pin。Java 21 仍保留上文所述行为。本模块按 Java 21 基线描述，不把后续变化倒灌成 Java 21 的固定事实。

## Virtual Thread 能否替代线程池

它可以减少“复用平台线程来承载阻塞任务”的必要性，但不等于所有 Executor 都没用了：

- 虚拟线程执行器负责为任务提供执行单元；
- CPU 密集任务仍需要限制并发，避免超过 CPU 能力；
- 外部资源仍需要连接池和 semaphore；
- 定时、调度、批量队列和生命周期管理仍需要执行器；
- 线程池也可能用于隔离不同任务模型。

正确说法是：不要把虚拟线程当作平台线程池里的有限 worker 去池化；要把并发上限放在真正稀缺的 CPU、连接和下游资源上。

## CPU 密集任务不会变快

虚拟线程不会增加 CPU 核心，也不会消除算法复杂度。CPU 密集任务创建更多虚拟线程通常只会增加竞争和调度压力。它最有价值的场景是大量并发、单任务阻塞比例高、调用链希望保持同步写法的服务。

## 取消和结构化边界

虚拟线程仍然需要：

- interrupt 和超时；
- try/finally 释放连接、文件和上下文；
- 任务失败传播；
- 关闭执行器；
- 业务级限流和幂等。

Virtual Thread 不自动提供结构化并发；Structured Concurrency 在本仓库列为 P2/后续主题，不在本版展开。

## 关键源码路径

- Thread.Builder.OfVirtual；
- Executors.newVirtualThreadPerTaskExecutor；
- JVM 虚拟线程调度器和 carrier 关系；
- 阻塞 API 的挂起与恢复路径；
- synchronized/native pinning 的 Java 21 行为。

本轮暂不增加虚拟线程示例，避免为了填满示例目录制造对调度细节过度敏感的测试。后续内容审核确定真实难点后，再补有边界的实验。

## 高频追问

- Virtual Thread 是更快的线程吗？不是，主要降低大量阻塞任务的线程资源成本。
- CPU 密集任务会更快吗？不会增加 CPU 能力。
- 出现虚拟线程后线程池没用了吗？仍需要资源隔离、调度、限流和生命周期管理。
- synchronized 会影响虚拟线程吗？Java 21 中长阻塞可能 pin carrier，缩短临界区并验证具体实现。
- 虚拟线程能否无限创建？创建成本较低不等于下游资源无限，仍要限制真实并发。

## 一句话复盘

虚拟线程扩展的是“可承载的阻塞任务数量”，不是 CPU 能力；使用它时仍然要围绕资源容量、取消、超时和 pinning 设计。
