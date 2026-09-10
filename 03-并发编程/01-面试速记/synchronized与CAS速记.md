# synchronized 与 CAS 速记

> 面试定位：互斥更新与乐观更新的选择
> Java 版本：基础语义按 JDK 8+，锁实现重点按 Java 21
> P0/P1：P0
> 前置知识：JMM 与 happens-before
> 本文不负责：具体 CPU 汇编和所有 HotSpot 私有优化

## 先说结论

synchronized 以对象 monitor 为互斥边界，适合保护一段需要保持不变量的临界区；CAS 以某个内存位置的 expected/update 比较交换为原子原语，适合低冲突、单变量或可拆分状态的乐观更新。两者不是简单的性能高低关系。

## synchronized 的面试回答

> synchronized 可以修饰实例方法、静态方法或代码块。实例方法通常锁当前对象，static synchronized 锁对应 Class 对象，代码块锁显式指定的对象。进入 monitor 后同一时刻只有一个线程执行受保护临界区，并且锁具有可重入语义；退出时自动释放，即使发生异常也能释放。Java 21 的实现可能结合 CAS、自旋和 monitor 膨胀等优化，但不能把旧版偏向锁升级口诀当成当前必然流程。它不承诺公平，进入锁的等待线程也不能靠 interrupt 强制退出 monitor 获取。

## CAS 的面试回答

> CAS 把位置当前值与 expected 比较，相等才更新为 update，否则失败。AtomicInteger 的 incrementAndGet 可以用 CAS 循环把 read-modify-write 合成为可重试的原子更新。CAS 仍然需要等待和重试，高竞争会消耗 CPU；还要注意 ABA、溢出和多字段一致性。因此 CAS 不是“没有锁”，而是另一种并发控制原语。

## 选择边界

| 问题 | 更适合的起点 | 原因 |
| --- | --- | --- |
| 多字段必须保持一个不变量 | synchronized 或显式锁 | 临界区容易表达整体协议 |
| 单个计数器、引用或状态 | Atomic / CAS | 语义集中，避免不必要的互斥 |
| 高竞争统计，只关心最终汇总 | LongAdder | 分散热点，牺牲即时精确读的成本 |
| 需要等待条件、可中断或多个等待队列 | ReentrantLock + Condition | 能表达更丰富的等待协议 |
| 访问外部资源或阻塞很久 | 先重新设计边界 | 锁和 CAS 都不能消除下游容量问题 |

## Java 21 版本提醒

偏向锁是历史实现背景，不能写成 Java 21 当前运行时一定经历的“无锁 → 偏向 → 轻量 → 重量”完整升级口诀。面试可以解释 monitor、竞争、自旋、膨胀和重量级等待，但要明确 HotSpot 内部优化不等于 Java 语言契约。

## 追问链

~~~text
synchronized 锁的是什么？
↓
为什么可以重入？
↓
为什么不保证公平？
↓
monitor 竞争时线程在哪里等待？
↓
CAS 失败后怎么办？
↓
为什么 CAS 会有 ABA 和自旋退化？
↓
多个字段如何保持一致？
~~~

完整原理见 [synchronized 原理与锁实现](../02-深度解析/synchronized原理与锁实现.md) 和 [CAS 与原子类](../02-深度解析/CAS与原子类.md)。

## 一句话复盘

锁解决的是临界区和不变量，CAS 解决的是可重试的原子状态更新；选择依据是状态形状、竞争强度、等待方式和工程可观测性。
