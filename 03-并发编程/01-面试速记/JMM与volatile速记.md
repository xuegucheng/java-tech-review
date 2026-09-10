# JMM 与 volatile 速记

> 面试定位：可见性、顺序和安全发布的第一轮回答
> Java 版本：JDK 8+ 语义，Java 21 实现边界
> P0/P1：P0
> 前置知识：线程生命周期和 Java 对象引用
> 本文不负责：HotSpot 汇编和所有 CPU 内存屏障指令

## 先说结论

JMM 是语言和虚拟机规范层面的内存语义，不是 JVM 内存结构图。它回答的是：一个线程的写入在什么同步关系下，对另一个线程的读取可见，以及哪些重排序不能改变合法程序的观察结果。

volatile 变量的写对后续读建立 happens-before，可用于停止标志、配置快照引用和安全发布的一部分；但 volatile 不把读、改、写的复合操作变成一个原子动作。

## 30 秒回答

> 并发错误通常分成可见性、原子性和有序性。JMM 用 happens-before 描述同步关系，而不是描述物理时间。程序次序、monitor 解锁与加锁、volatile 写与读、线程 start 和线程终止都可以建立 happens-before，并且具有传递性。volatile 主要提供单次读写的可见性和有序性，不能保护 count++；synchronized 通过互斥和 monitor 的释放/获取同时解决临界区原子性与可见性。安全发布还可以使用 final 字段语义、静态初始化、锁、volatile 引用或并发容器，但 final 不等于任意发布都安全。

## 三个概念不要混淆

| 概念 | 关注点 | 典型手段 |
| --- | --- | --- |
| 可见性 | 一个线程的写入何时能被另一个线程观察 | volatile、锁、线程启动/终止、并发容器 |
| 原子性 | 一个操作是否不可被拆开观察或打断 | synchronized、CAS、Atomic、数据库原子操作 |
| 有序性 | 允许的重排是否改变跨线程观察 | happens-before、volatile、锁、正确发布 |

主内存和工作内存是规范抽象，用来描述线程之间的读写语义；不要把它们直接画成 JVM 的堆、栈、方法区。

## happens-before 规则

高频规则只保留这些：

1. 同一线程内，程序前面的动作 happens-before 后面的动作；
2. 对同一个 monitor，解锁 happens-before 随后的加锁；
3. 对同一个 volatile 变量，写 happens-before 随后的读；
4. 对线程调用 start 之前的动作，happens-before 新线程中的动作；
5. 线程中的所有动作 happens-before 另一个线程成功从 join 返回；
6. happens-before 具有传递性。

happens-before 不等于 A 在物理时间上先完成，也不等于所有 CPU 指令都停止重排。它是语言层面可观察结果的约束。

## DCL 为什么需要 volatile

~~~java
class Singleton {
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
}
~~~

创建对象可以抽象成：

~~~text
分配内存
→ 执行构造初始化
→ 把引用写入 instance
~~~

没有正确的发布边界时，引用写入可能在其他线程观察中先于初始化效果出现，第二次检查线程可能拿到尚未完整构造的对象。volatile 让引用写入与后续读取形成所需的可见性和有序性边界。更简单的单例优先使用静态初始化或枚举。

## synchronized 为什么同时解决两类问题

synchronized 临界区内同一时刻只能有一个持有者，解决了受保护状态的互斥更新；线程退出临界区时释放 monitor，后续线程进入时获取 monitor，这条同步关系又建立了可见性。

但这个结论有前提：所有访问该共享状态的路径都必须遵守同一个同步协议。只在写入处加锁、读取处绕过锁，不能自动获得完整保护。

## final 的安全发布边界

正确构造并通过合适方式发布的对象，其 final 字段具有特殊的初始化可见性语义；这不是说构造器可以把 this 泄漏出去，也不是说 final 引用指向的可变对象自动不可变。构造期间泄漏 this、通过数据竞争访问可变字段，仍然可能产生问题。

## CPU 屏障与 JMM 的关系

编译器和运行时可能使用内存屏障、原子指令或其他实现手段满足 JMM 要求。JMM 的 happens-before 是可移植语义；具体屏障数量、指令和优化属于实现与硬件细节，不能把某个 x86 观察当成 Java 规范保证。

## 高价值追问

- happens-before 是否意味着时间上 A 一定先执行？不是，它是可观察性和顺序约束。
- JMM 是不是 JVM 内存结构？不是，JMM 规定并发内存语义，堆/栈/元空间是运行时布局。
- volatile 能不能保护 count++？不能，复合操作仍是 read、modify、write。
- synchronized 为什么有可见性？monitor 的释放和后续获取形成同步边界。
- DCL 为什么要 volatile？防止引用发布与初始化观察顺序不满足安全发布要求。
- final 是否等于深度不可变？不是，final 引用指向的对象仍可能可变。

## 关键源码与验证

- 规范入口：happens-before、volatile 和 monitor 的语言语义；
- 实现入口：volatile 字段访问、VarHandle 的访问模式和 monitor 操作；
- 实验入口：[VisibilityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/VisibilityDemo.java)；
- 复合更新对比：[AtomicityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java)。

## 一句话复盘

JMM 先定义跨线程能看到什么，volatile 只提供单变量读写边界，复合不变量仍需要锁、CAS 或更高层同步协议。
