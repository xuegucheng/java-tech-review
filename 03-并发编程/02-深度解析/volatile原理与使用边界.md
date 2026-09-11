# volatile 原理与使用边界

> 面试定位：可见性、有序性和单变量原子访问
> Java 版本：语义按 JDK 8+，实现观察优先 Java 21
> P0/P1：P0
> 前置知识：Java 内存模型与 happens-before
> 本文不负责：所有平台的汇编指令和完整 VarHandle API

## 先说结论

volatile 修饰的字段具有特殊读写语义：写入对随后读取可见，并建立相应的顺序边界。它不提供互斥，也不把多个普通字段绑定为一个事务，更不能把 count++ 这种复合更新变成原子操作。

## 30 秒回答

> volatile 适合表示状态标志、配置快照引用、单写多读的发布状态和双重检查单例引用。它主要解决可见性和有序性。volatile int count 的 count++ 仍然拆成 read、modify、write；两个线程可以读取同一个旧值，再分别写回相同的新值，所以会丢更新。需要保护复合不变量时使用 synchronized、Lock、Atomic 的 CAS 方案或并发容器的原子复合方法。

## volatile 读写建立什么关系

对同一个 volatile 变量：

~~~text
线程 A：普通字段初始化 → volatile 写 ready=true
                              ↓ happens-before
线程 B：volatile 读 ready=true → 读取已发布普通字段
~~~

这个模式的前提是：普通字段初始化发生在发布写之前，读线程通过同一个 volatile 变量观察到发布结果。若发布后又继续修改普通字段，后续修改需要新的同步协议。

## count++ 为什么不安全

~~~java
volatile int count;
count++;
~~~

展开后是：

~~~text
read count
modify oldValue + 1
write count
~~~

两个线程可能同时读到 10：

~~~text
线程 A 读 10
线程 B 读 10
线程 A 写 11
线程 B 写 11
最终结果 11，而不是 12
~~~

volatile 让每次读写都更容易观察到最新的 volatile 值，但它没有把三步组合成一个不可分割的临界区。

## 适合的工程场景

### 状态标志

停止标志、配置是否已加载、服务是否进入关闭阶段等，通常由一个线程写、多个线程读。写入方仍要定义状态转换，不能让多个线程无协议地竞争写状态。

### 不可变配置快照引用

先构造完整的不可变配置对象，再通过 volatile 引用发布。读线程可以拿到旧快照或新快照，但不会因为快照内部正在被修改而观察到半成品。

### DCL 引用

双重检查锁的实例引用需要 volatile，原因是安全发布和初始化顺序，不是因为 volatile 让构造器本身变成原子。

### 单写多读状态

单写者更新一个版本号、指针或标志，多读者观察。若存在多个写者或多个字段联动，就必须重新评估是否需要锁、CAS 或消息串行化。

## volatile 不能替代锁的情况

- 多个字段需要一起保持不变量；
- 需要检查后更新且不能接受并发竞争；
- 需要条件等待和唤醒；
- 需要保护一段包含外部副作用的临界区；
- 需要限制并发数量或公平等待；
- 需要在多个 key 之间建立一致性。

把 volatile 叫成“轻量级 synchronized”会掩盖这些语义差异。二者的共同点是内存可见性，核心能力并不相同。

## volatile 与 CAS 的关系

CAS 通常需要对同一个内存位置进行具有明确内存语义的读写。AtomicInteger 的 CAS 循环可以抽象成：

~~~text
读取当前值
→ 根据当前值计算新值
→ 如果位置仍是期望值则 CAS 写入
→ 失败则重新读取和重试
~~~

volatile 负责观察和顺序边界，CAS 负责条件更新；CAS 也不能自动把多个字段合并成一个原子事务。

## 高频追问

- volatile 写对后续哪个读建立 happens-before？对同一个 volatile 变量的后续读。
- volatile 是否保证复合操作原子？不保证。
- volatile 能否保护一个 List 的 add？不能，List 内部状态和容量更新是复合过程。
- volatile 引用指向的对象是否不可变？不一定，引用不可见性和对象内部可变性是两件事。
- 单写多读是否一定不用锁？还要看发布前后是否存在多字段不变量和其他写者。

## 关键源码路径

- 语义入口：volatile 字段读写与 happens-before；
- 实现入口：Java 21 的 VarHandle 访问模式、Atomic 类 CAS 循环；
- [VisibilityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemo.java)；
- [AtomicityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java)。

## 一句话复盘

volatile 适合发布一个状态或快照，不适合保护“先检查、再修改”的复合不变量；看到 count++、多字段或条件等待，就要换成更强的同步协议。
