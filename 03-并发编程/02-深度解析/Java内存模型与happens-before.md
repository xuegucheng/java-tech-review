# Java 内存模型与 happens-before

> 面试定位：可见性、原子性、有序性和安全发布的理论地基
> Java 版本：JMM 语义按 JDK 8+，实现示例优先 Java 21
> P0/P1：P0
> 前置知识：线程生命周期和 Java 字段访问
> 本文不负责：特定 CPU 汇编、缓存一致性协议和 JVM 堆布局

## 先说结论

JMM 是 Java 规范对跨线程读写、同步关系和允许重排序的抽象模型，不是“堆、栈、方法区”的另一种叫法。它通过 happens-before 规定哪些动作的效果必须对哪些后续读取可见，并允许实现使用屏障、原子指令或其他机制完成这个承诺。

没有 happens-before 的普通共享变量访问可能产生数据竞争；即使某次运行在某台机器上看起来正确，也不能把偶然观察当成 Java 语义保证。

## 30 秒回答

> 并发问题要区分可见性、原子性和有序性。JMM 的主内存、工作内存是规范抽象，不能直接等同 JVM 的运行时内存区域。happens-before 是可观察结果的偏序关系，不代表物理时间上一定先执行。程序次序、monitor 解锁到后续加锁、volatile 写到后续读、线程 start、线程终止到 join 返回都能建立 happens-before，并且具有传递性。synchronized 通过互斥和 monitor 的释放/获取同时保护临界区和可见性；安全发布还要使用 final、静态初始化、volatile 引用、锁或并发容器等明确边界。

## JMM 解决什么问题

一个线程写入字段，另一个线程读取字段，中间至少有三个问题：

1. 写入结果何时对另一个线程可见；
2. 多步更新是否能被另一个线程观察到中间状态；
3. 编译器、JIT 和 CPU 是否可以重排不相关操作。

JMM 不承诺每一个普通字段访问都立即对其他线程可见，也不承诺没有数据竞争的程序会按源码的物理时间顺序执行；它定义的是在正确同步关系下必须成立的行为。

## 主内存与工作内存不是 JVM 内存结构

规范里的主内存可以理解为共享变量的抽象归属，工作内存可以理解为线程进行读写和缓存的抽象区域。它们用于解释可见性和同步，不是要求 JVM 必须有一个叫“工作内存”的物理区。

不要把下面两组概念画成同一张结构图：

~~~text
JMM：共享变量、线程本地读写、同步关系
JVM 实现：堆、线程栈、元空间、代码缓存、GC 区域
~~~

## happens-before 规则

### 程序次序规则

同一线程中，按程序顺序靠前的动作 happens-before 靠后的动作。它描述的是单线程语义约束，不是让所有跨线程读取自动可见。

### monitor 规则

对同一个 monitor，解锁 happens-before 随后的加锁。一个线程退出 synchronized 临界区，之后成功进入同一 monitor 的线程可以观察到前者在临界区内发布的写入。

### volatile 规则

对同一个 volatile 变量，写入 happens-before 随后的读取。它是单变量的可见性与顺序边界，不会把围绕其他普通字段的任意业务不变量自动变成原子。

### start 规则

调用线程在 Thread.start 之前的动作 happens-before 新线程中的动作。它解释了为什么可以先构造并初始化任务对象，再 start 新线程。

### termination / join 规则

线程中的动作 happens-before 另一个线程从该线程的 join 成功返回。Future.get、CountDownLatch.await 等高层同步工具也会建立各自的结果可见性协议。

### 传递性

如果 A happens-before B，B happens-before C，则 A happens-before C。工程中常见的安全发布经常是通过多段同步关系传递出来的。

## happens-before 不是时间线

问“happens-before 是否表示 A 一定在时间上先执行”，正确回答是：

> 它表示 A 的效果必须对 B 的合法观察可见，并约束允许的重排序；它不等价于某个线程已经完成后另一个线程才获得 CPU，也不要求实现暴露物理执行时间。

两个动作可能在不同 CPU 上并发执行，仍然通过同步关系建立可观察顺序；没有同步关系时，即使源码行看起来先后明确，跨线程观察也可能不满足直觉。

## 数据竞争

同一变量被多个线程访问，至少一个是写，并且没有建立足够的 happens-before 关系，就可能形成数据竞争。数据竞争不仅是“结果加错一次”，也可能让线程长期看不到更新、观察到不完整发布或依赖不可靠的重排结果。

解决方式不是给所有字段加 volatile，而是明确：

- 哪些状态共享；
- 哪个线程负责写；
- 哪个动作发布；
- 读者通过什么同步协议观察；
- 多字段不变量由哪个临界区保护。

## 安全发布

常见安全发布边界包括：

- 类初始化：静态字段由 JVM 的类初始化协议发布；
- 正确构造的 final 字段：有特殊初始化可见性，但不等于深度不可变；
- volatile 引用：发布引用写入前已经完成的对象状态；
- 锁：解锁与后续加锁建立可见性；
- 并发容器和 Future：由库实现提供读取结果的同步协议。

构造器中把 this 传给其他线程、把可变对象暴露出去或只保护部分字段，都可能破坏安全发布。

## DCL 与 final 之间的边界

双重检查锁示例：

~~~java
private static volatile Singleton instance;

static Singleton getInstance() {
    if (instance == null) {
        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
        }
    }
    return instance;
}
~~~

这里需要 volatile 的是引用发布边界。Singleton 内部的 final 字段有额外初始化保证，但可变字段仍然要通过构造完成、锁或其他协议发布。若单例构造复杂，静态初始化通常更容易证明。

## 屏障、编译器和硬件

JMM 说“必须满足什么”，实现决定“如何做到”。可能涉及：

- 编译器和 JIT 对普通访问的重排限制；
- volatile 或锁操作对应的屏障；
- CAS 等原子指令；
- CPU 缓存一致性和内存序。

不能从某次 x86 运行中推导出所有架构都提供同样的弱内存行为，也不能把某条屏障指令直接当成 JMM 的全部定义。

## 关键源码与验证

- Java 语义入口：volatile、synchronized、Thread.start 和 join 的同步规则；
- 实现入口：VarHandle 访问模式、Atomic 类和 AQS 的 volatile state；
- [VisibilityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemo.java) 验证有界可见性；
- [AtomicityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java) 验证复合更新边界。

## 工程边界

- 先定义状态机和不变量，再选同步工具；
- 不用“加了 volatile”作为线程安全总证明；
- 不用“在我的机器上没复现”替代 happens-before 分析；
- 发布不可变快照时，先构造完整对象，再发布引用；
- 对取消、超时和异常路径也建立可见性和清理协议。

## 一句话复盘

happens-before 是跨线程可观察性的合同，不是物理执行时间表；所有安全发布和并发不变量都应该能指出具体同步边界。
