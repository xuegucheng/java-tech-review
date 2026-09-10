# synchronized 原理与锁实现

> 面试定位：对象监视器、互斥、可重入和 Java 21 锁实现边界
> Java 版本：语言语义按 JDK 8+，HotSpot 实现观察按 Java 21
> P0/P1：P0
> 前置知识：JMM、volatile 和线程生命周期
> 本文不负责：HotSpot 每个版本的私有锁优化源码

## 先说结论

synchronized 的语义核心是对象 monitor：同一个 monitor 同时只允许一个线程进入受保护的临界区，并且 monitor 的释放与随后获取建立可见性边界。它具有可重入语义，但不承诺公平，进入 monitor 的等待不能像 lockInterruptibly 一样被 interrupt 取消。

Java 21 可以讨论 CAS、自旋、竞争和 monitor 膨胀等实现方向，但不能把“无锁 → 偏向锁 → 轻量级锁 → 重量级锁”当成当前运行时必然经历的完整升级流程。偏向锁属于历史实现背景，较新的 JDK 已禁用或移除相关路径。

## 30 秒回答

> synchronized 可以修饰实例方法、静态方法和代码块。实例方法锁 this，静态方法锁对应 Class 对象，代码块锁显式对象。线程进入 monitor 后执行临界区，退出时无论正常还是异常都会释放；同一线程再次进入同一 monitor 是可重入的。竞争时 HotSpot 可能先尝试快速路径或自旋，必要时让线程进入 monitor 等待，但这些是实现优化。synchronized 既保护互斥更新，又通过 monitor 释放/获取建立可见性；它不提供公平性，也不支持在等待获取 monitor 时由 interrupt 直接取消。

## 三种使用方式

~~~java
public synchronized void update() {
    // 锁 this
}

public static synchronized void updateGlobal() {
    // 锁 Type.class
}

public void updateBlock() {
    synchronized (lock) {
        // 锁 lock
    }
}
~~~

锁对象必须稳定且对参与者可见。不要把 synchronized(this) 暴露给外部调用者，也不要用会被重新赋值的字段作为锁。static synchronized 的锁是 Class 对象，所以同一个类加载器范围内的静态方法会竞争同一个 monitor；不同 ClassLoader 可能拥有不同 Class 对象。

## monitorenter 与 monitorexit

代码块形式在字节码层面可以抽象成 monitorenter 和 monitorexit。编译器会安排异常路径也执行 monitorexit，因此 synchronized 块不会因为异常而永久占用 monitor。

方法修饰符的实现表示不同，但面试重点是锁对象不同：

~~~text
实例 synchronized 方法 → this
静态 synchronized 方法 → 当前类的 Class 对象
synchronized (expression) → expression 结果对象
~~~

## 可重入为什么成立

monitor 需要记录当前持有者和重入深度。若持有者再次进入同一个 monitor，深度递增，不会把自己阻塞；退出时深度递减，归零后才真正释放。

可重入让分层调用成立：

~~~java
public synchronized void outer() {
    inner();
}

private synchronized void inner() {
    // 同一个 this，当前线程可以再次进入
}
~~~

可重入不代表任意锁都可重入，也不代表递归调用不会造成逻辑死循环；它只描述同一线程对同一 monitor 的再次获取。

## Java 21 的实现边界

HotSpot 会根据竞争情况选择不同的快速路径、CAS 尝试、自旋和 monitor 等待实现。实现细节会随 JDK 和平台变化：

- 不把对象头 Mark Word 的某种位模式当作 Java 规范；
- 不把旧版偏向锁升级图当作 Java 21 现状；
- 不以“轻量锁一定更快、重量锁一定更慢”回答工程问题；
- 真实性能要结合临界区、竞争、线程阻塞、调度和压测。

对象头和 monitor 是理解实现的入口，但工程代码依赖的是 synchronized 的语义，不应依赖 HotSpot 私有布局。

## synchronized 是否公平

没有公平性承诺。等待时间更长的线程不保证先获得 monitor，唤醒顺序也不等于获得顺序。若业务需要明确的队列策略，应评估 ReentrantLock 的公平模式或更高层排队设计，但公平也会付出吞吐和调度成本。

## synchronized 是否可 interrupt

线程在等待进入 synchronized monitor 时，调用 interrupt 通常只设置中断状态，不会让它抛出 InterruptedException 并退出等待。线程进入 monitor 后调用 Object.wait，则 wait 可以响应中断。需要可中断获取时，考虑 ReentrantLock.lockInterruptibly。

## 与 ReentrantLock 的选择

| 维度 | synchronized | ReentrantLock |
| --- | --- | --- |
| 释放 | 语言结构自动释放 | 必须 finally unlock |
| 可重入 | 支持 | 支持 |
| 公平模式 | 无显式公平配置 | 可选公平实现 |
| 可中断获取 | 不支持 | lockInterruptibly |
| 超时尝试 | 不提供 | tryLock(timeout) |
| 多条件队列 | 一个 monitor 等待集合 | 可创建多个 Condition |
| 复杂度 | 较低 | 更灵活但更容易误用 |

不能只用“性能更好”作为 ReentrantLock 的理由；先看是否需要它额外的控制能力。

## 高价值追问

- synchronized 锁的到底是什么？对象 monitor，具体取决于使用形式。
- synchronized 普通方法和 static synchronized 锁一样吗？不一样，分别是对象和 Class。
- 为什么可重入？当前持有者和重入深度被记录。
- 为什么不公平？语言语义没有提供公平顺序承诺。
- Java 21 还应该背偏向锁升级吗？只能作为历史背景，不能当当前必然事实。
- synchronized 和 ReentrantLock 怎么选？根据可中断、超时、多条件、公平和代码复杂度选择。

## 关键源码路径

- Java 编译结果：monitorenter、monitorexit 或方法级 synchronized 标志；
- Object monitor 语义：进入、退出、wait、notify；
- HotSpot 实现：锁快速路径、竞争、自旋和 monitor 等待；
- 对比入口：ReentrantLock 的 Sync、FairSync、NonfairSync 和 AQS。

## Runnable Example

- [AtomicityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java)
- [AqsLockDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AqsLockDemo.java)

## 一句话复盘

synchronized 的稳定答案是 monitor、可重入、互斥和可见性；锁升级细节属于 Java 21 HotSpot 实现观察，不能替代语言语义。
