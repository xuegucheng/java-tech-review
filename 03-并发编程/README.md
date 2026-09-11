# 并发编程

> 面向 3～10 年 Java 后端工程师的并发复习模块。主线不是 JUC API 清单，而是从线程运行、内存语义到同步器、线程池和现代并发模型建立因果链。

返回 [仓库首页](../README.md)。Java 语言层的对象、final 和相等性契约由 [Java 核心](../01-Java核心/README.md)维护；HashMap 的基础结构由 [集合框架](../02-集合框架/README.md)维护。

## 模块定位

并发问题通常不是“记住哪个类”，而是回答下面几件事：

1. 多线程为什么会看到不同结果；
2. 哪个共享状态需要什么同步边界；
3. JDK 同步器如何把竞争、等待和唤醒组织起来；
4. 任务量、线程数量和上下游容量如何形成工程约束；
5. 何时应该选择锁、原子类、并发容器、异步编排或虚拟线程。

本模块统一采用：

~~~text
Interview Review → Mental Model → Deep Dive → Source Path → Runnable Example → Test
~~~

## 推荐学习顺序

### 10 分钟路线

只看 [并发编程面试主线](01-面试速记/并发编程面试主线.md)，重点记住：

~~~text
线程状态
→ 可见性 / 原子性 / 有序性
→ volatile 与 CAS 的边界
→ synchronized / AQS
→ 线程池的 execute 决策
→ ThreadLocal 清理责任
→ ConcurrentHashMap 的局部并发
~~~

### 1 小时路线

1. [并发编程面试主线](01-面试速记/并发编程面试主线.md)
2. [JMM 与 volatile 速记](01-面试速记/JMM与volatile速记.md)
3. [synchronized 与 CAS 速记](01-面试速记/synchronized与CAS速记.md)
4. [AQS 与锁速记](01-面试速记/AQS与锁速记.md)
5. [线程池速记](01-面试速记/线程池速记.md)
6. [ThreadLocal 与并发容器速记](01-面试速记/ThreadLocal与并发容器速记.md)

### 完整路线

~~~text
线程与生命周期
↓
Java Memory Model
↓
volatile
↓
synchronized
↓
CAS 与 Atomic
↓
AQS
↓
ReentrantLock 与 Condition
↓
ThreadPoolExecutor
↓
ThreadLocal
↓
ConcurrentHashMap
↓
CountDownLatch / CyclicBarrier / Semaphore
↓
CompletableFuture
↓
Java 21 Virtual Thread
~~~

## P0 主线

| 主题 | 面试入口 | 深度解析 | 关键验证 |
| --- | --- | --- | --- |
| 线程模型与生命周期 | [并发主线](01-面试速记/并发编程面试主线.md) | [线程模型与生命周期](02-深度解析/线程模型与生命周期.md) | [InterruptDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/InterruptDemo.java) |
| JMM 与 happens-before | [JMM 与 volatile](01-面试速记/JMM与volatile速记.md) | [Java 内存模型与 happens-before](02-深度解析/Java内存模型与happens-before.md) | [VisibilityDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemo.java) |
| volatile | [JMM 与 volatile](01-面试速记/JMM与volatile速记.md) | [volatile 原理与使用边界](02-深度解析/volatile原理与使用边界.md) | [AtomicityDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java) |
| synchronized | [synchronized 与 CAS](01-面试速记/synchronized与CAS速记.md) | [synchronized 原理与锁实现](02-深度解析/synchronized原理与锁实现.md) | [AtomicityDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java) |
| CAS 与 Atomic | [synchronized 与 CAS](01-面试速记/synchronized与CAS速记.md) | [CAS 与原子类](02-深度解析/CAS与原子类.md) | [AtomicityDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java) |
| AQS | [AQS 与锁](01-面试速记/AQS与锁速记.md) | [AQS 核心原理](02-深度解析/AQS核心原理.md) | [AqsLockDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AqsLockDemo.java) |
| ReentrantLock 与 Condition | [AQS 与锁](01-面试速记/AQS与锁速记.md) | [ReentrantLock 与 Condition](02-深度解析/ReentrantLock与Condition.md) | AQS 示例覆盖教学锁路径 |
| ThreadPoolExecutor | [线程池速记](01-面试速记/线程池速记.md) | [ThreadPoolExecutor 线程池](02-深度解析/ThreadPoolExecutor线程池.md) | [ThreadPoolSaturationDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/ThreadPoolSaturationDemo.java) |
| ThreadLocal | [ThreadLocal 与并发容器](01-面试速记/ThreadLocal与并发容器速记.md) | [ThreadLocal 原理与内存泄漏](02-深度解析/ThreadLocal原理与内存泄漏.md) | [ThreadLocalCleanupDemo](04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/ThreadLocalCleanupDemo.java) |

## P1 专题

| 主题 | Deep Dive | 复习边界 |
| --- | --- | --- |
| ConcurrentHashMap | [ConcurrentHashMap 并发容器](02-深度解析/ConcurrentHashMap并发容器.md) | Java 8+ / Java 21 的 Node、CAS、synchronized、协作扩容 |
| JUC 并发协作工具 | [JUC 并发协作工具](02-深度解析/JUC并发协作工具.md) | CountDownLatch、CyclicBarrier、Semaphore 的问题模型 |
| CompletableFuture | [CompletableFuture 异步编排](02-深度解析/CompletableFuture异步编排.md) | 依赖、聚合、异常、超时和线程池隔离 |
| Virtual Thread | [Java 21 虚拟线程](02-深度解析/Java21虚拟线程.md) | 阻塞 I/O 的线程资源成本，不承诺 CPU 加速 |

## P2 延伸

StampedLock、ReadWriteLock、Phaser、Exchanger、ForkJoinPool 深度源码、VarHandle 全 API、False Sharing、Structured Concurrency 和 ScopedValue 暂不作为第一版主线。它们只有在解释 P0/P1 必需时才提前引入。

## 文章索引

### Interview Review

- [并发编程面试主线](01-面试速记/并发编程面试主线.md)
- [JMM 与 volatile 速记](01-面试速记/JMM与volatile速记.md)
- [synchronized 与 CAS 速记](01-面试速记/synchronized与CAS速记.md)
- [AQS 与锁速记](01-面试速记/AQS与锁速记.md)
- [线程池速记](01-面试速记/线程池速记.md)
- [ThreadLocal 与并发容器速记](01-面试速记/ThreadLocal与并发容器速记.md)

### Deep Dive

- [线程模型与生命周期](02-深度解析/线程模型与生命周期.md)
- [Java 内存模型与 happens-before](02-深度解析/Java内存模型与happens-before.md)
- [volatile 原理与使用边界](02-深度解析/volatile原理与使用边界.md)
- [synchronized 原理与锁实现](02-深度解析/synchronized原理与锁实现.md)
- [CAS 与原子类](02-深度解析/CAS与原子类.md)
- [AQS 核心原理](02-深度解析/AQS核心原理.md)
- [ReentrantLock 与 Condition](02-深度解析/ReentrantLock与Condition.md)
- [ThreadPoolExecutor 线程池](02-深度解析/ThreadPoolExecutor线程池.md)
- [ThreadLocal 原理与内存泄漏](02-深度解析/ThreadLocal原理与内存泄漏.md)
- [ConcurrentHashMap 并发容器](02-深度解析/ConcurrentHashMap并发容器.md)
- [JUC 并发协作工具](02-深度解析/JUC并发协作工具.md)
- [CompletableFuture 异步编排](02-深度解析/CompletableFuture异步编排.md)
- [Java 21 虚拟线程](02-深度解析/Java21虚拟线程.md)

## One Source of Truth

- Java 对象、final、equals/hashCode 和安全构造边界仍由 [Java 核心](../01-Java核心/README.md)定义。
- HashMap 的数组、链表、树化、resize 仍由 [集合框架 HashMap 深度解析](../02-集合框架/02-深度解析/HashMap原理与源码分析.md)定义；ConcurrentHashMap 只解释并发读写、bin 锁和扩容协作。
- Object.wait/notify 在本模块解释线程协作、monitor 和条件等待语义；Java Core 只保留 API 边界。
- volatile、synchronized、CAS、AQS、线程池和 ThreadLocal 的完整并发语义归本模块维护，其他模块只链接消费关系。

## Java 版本边界

- 面试中的基础线程、JMM 和锁语义尽量按 JDK 8+ 表达。
- JDK 8+ 与 Java 21 的实现细节必须明确标注；AQS、ThreadLocalMap、ConcurrentHashMap 和 HotSpot 锁优化不混写。
- 本模块基准是 Java 21。Java 21 的虚拟线程是正式能力；后续 JDK 对 pinning 等实现的优化不倒灌成 Java 21 固定事实。
- 示例模块使用 Java 21、Maven Wrapper 和 JUnit 5。

## 图示状态

正文已按认知难点就近嵌入 Mermaid 流程图：AQS exclusive 获取路径（[AQS 核心原理](02-深度解析/AQS核心原理.md)）、execute 三步决策与二次检查（[ThreadPoolExecutor 线程池](02-深度解析/ThreadPoolExecutor线程池.md)）、Condition 双队列与节点转移（[ReentrantLock 与 Condition](02-深度解析/ReentrantLock与Condition.md)）、ThreadLocalMap 弱引用链（[ThreadLocal 原理与内存泄漏](02-深度解析/ThreadLocal原理与内存泄漏.md)）。目前已补充 7 张独立高清 SVG，新增 AQS 概念与职责分工图，完整索引见 [03-图示/README.md](03-图示/README.md)。

## 运行示例

~~~powershell
.\mvnw.cmd -pl '03-并发编程/04-示例代码' test
~~~

完整示例索引见 [04-示例代码/README.md](04-示例代码/README.md)。
