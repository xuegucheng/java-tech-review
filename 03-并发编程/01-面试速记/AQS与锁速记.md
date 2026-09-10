# AQS 与锁速记

> 面试定位：ReentrantLock 和 Condition 的源码主线
> Java 版本：AQS 字段和节点描述按 Java 21
> P0/P1：P0
> 前置知识：CAS、volatile、线程阻塞与唤醒
> 本文不负责：完整复制 AbstractQueuedSynchronizer 源码

## 先说结论

AQS 是同步器骨架，不是某一把具体的锁。它把同步状态 state、FIFO 风格的双向等待队列、CAS 和 LockSupport.park/unpark 组合起来；子类通过 tryAcquire、tryRelease 或 shared 版本定义“状态代表什么”。

## 30 秒回答

> 线程尝试获取同步器失败时，AQS 把它包装成节点放入同步队列，并在没有获取资格时 park。持有者释放状态后，AQS 通过 unpark 让后继重新竞争。state 不直接等于锁，它可以表示重入次数、许可数、闸门计数或其他同步状态。ReentrantLock 用 state 表示持有次数，Condition 则维护独立的条件等待队列，signal 只是把节点转移回同步队列，线程还要重新获得锁才能继续。

## 必须避免的简化

- AQS 不是原始 CLH 自旋锁的直接实现；更准确地说，它受 CLH 思想影响，是为阻塞同步器设计的双向 FIFO 等待队列变体。
- Java 21 的 AQS 节点字段是 prev、next、waiter、status；不要把 JDK 8 的 waitStatus 字段不加版本说明地当作当前字段。
- head 是队列哨兵或已出队的头节点，不等于当前持锁线程。
- unpark 只是允许被阻塞线程重新调度和竞争，不等于立即运行，更不等于直接获得锁。

## 追问链

~~~text
state 表示什么？
↓
tryAcquire 为什么交给子类？
↓
获取失败为什么要入队？
↓
为什么 park 而不是一直自旋？
↓
释放时谁负责唤醒？
↓
取消节点为什么需要 prev/next？
↓
Condition 队列如何回到同步队列？
~~~

完整原理见 [AQS 核心原理](../02-深度解析/AQS核心原理.md) 和 [ReentrantLock 与 Condition](../02-深度解析/ReentrantLock与Condition.md)。

## 一句话复盘

AQS 的价值是把“同步状态如何定义”和“线程如何排队、阻塞、唤醒”分离，让 ReentrantLock、Semaphore、CountDownLatch 等同步器复用同一套等待框架。
