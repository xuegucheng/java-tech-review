# CAS 与原子类

> 面试定位：从 volatile 走向乐观原子更新
> Java 版本：原子类语义按 JDK 8+，实现入口优先 Java 21
> P0/P1：P0
> 前置知识：JMM、volatile 和基本位操作
> 本文不负责：CPU 指令集和 VarHandle 全 API

## 先说结论

CAS 是 Compare-And-Set：只有当前位置仍等于 expected，才把它更新为 update。它把一个变量的检查与更新绑定成原子动作，AtomicInteger 等类通常通过 CAS 循环实现 incrementAndGet。

CAS 不是“没有锁”，而是乐观并发控制原语。失败线程通常重试；低竞争时避免阻塞很有效，高竞争时会持续消耗 CPU。ABA、溢出、多字段一致性和重试上限都必须纳入设计。

## 30 秒回答

> CAS 读取当前值，与期望值比较，相等才更新，否则失败。AtomicInteger 能通过 CAS 循环实现单变量的原子加法；AtomicReference 能用不可变对象替换整体状态。CAS 不保证多个字段一起更新，也不自动解决 ABA 和高竞争自旋。LongAdder 通过多个 Cell 分散热点，适合高并发统计但 sum 不是强一致快照，不应拿来维护余额或严格序列号。CAS 的访问还需要合适的内存语义，通常与 volatile/VarHandle 访问结合。

## CAS 的最小模型

~~~text
old = value
if value == expected:
    value = update
    return success
else:
    return failure
~~~

关键是比较和更新不能被另一个线程插入。Java 代码中的伪代码不是普通 if + assignment；底层要依赖原子指令或 JVM 提供的原子访问能力。

## AtomicInteger 为什么能实现 count++

抽象为：

~~~java
for (;;) {
    int oldValue = value.get();
    int newValue = oldValue + 1;
    if (value.compareAndSet(oldValue, newValue)) {
        return newValue;
    }
}
~~~

失败意味着其他线程已经改变了值，当前线程重新读取并计算。这个循环保证单个计数器的更新不丢失，但不保证围绕计数器的其他对象状态同步更新。

Java 21 的 AtomicInteger 内部仍通过 Unsafe.getAndAddInt 等原子访问实现（其源码注释说明：本应迁移到 VarHandle，但存在未解决的循环启动依赖）；API 的内存语义以 VarHandle 访问模式文档化。应用层不应直接使用 Unsafe，新代码的原子访问入口是 Atomic 类或 VarHandle，具体实现随 JDK 版本演进，不能把“Atomic 一定只靠 Unsafe”当作版本无关事实。源码对照：[OpenJDK JDK 21 AtomicInteger.java](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/concurrent/atomic/AtomicInteger.java)。

## AtomicReference 与整体状态替换

多个字段要保持一致时，可以把它们封装成不可变快照，然后用 AtomicReference CAS 替换整个快照：

~~~java
record State(int version, boolean enabled) {}
~~~

这能把“两个字段一起更新”转化为“一个引用的原子替换”。若更新逻辑复杂、竞争高或失败代价大，显式锁可能更容易维护。

## ABA 问题

线程 A 读取值 A 后暂停，线程 B 把 A 改成 B，又改回 A；线程 A 的 CAS 只看到当前仍是 A，就会误以为期间没有变化。

解决方式包括：

- 使用 AtomicStampedReference 携带版本；
- 使用不可变对象引用，让每次状态版本拥有不同身份；
- 用领域版本号或单调序列号；
- 如果 ABA 不影响业务语义，则明确记录这个前提。

不是所有 CAS 都需要版本戳，关键是判断“中间变化后又回到原值”是否会改变更新合法性。

## 自旋与高竞争退化

CAS 失败后重试没有阻塞线程，但每次失败仍然消耗 CPU，并可能产生缓存一致性流量。竞争高、临界区复杂、失败概率高时：

- 自旋时间可能超过阻塞和唤醒成本；
- 多个线程反复修改同一个热点位置；
- 线程数和 CPU 核数不匹配时，吞吐反而下降；
- 业务层重试还可能放大请求压力。

工程上不能直接说 CAS 一定比锁快，应该结合竞争、临界区长度、失败率和压测。

## LongAdder 为什么适合统计

AtomicLong 的单一计数位置在高竞争下会成为热点。LongAdder 可以把更新分散到多个 Cell，读取时汇总 base 和各 Cell：

- 更新竞争较低时可能直接更新 base；
- 竞争出现时，不同线程尽量写不同 Cell；
- sum 是当时各分片的累加观察，不是一个冻结的线性化快照；
- reset 或 sumThenReset 与并发更新之间也要谨慎定义语义。

因此 LongAdder 适合吞吐统计、请求计数和监控指标，不适合强一致余额、库存扣减或必须每次读都对应严格顺序的业务状态。

## CAS 与 volatile 的关系

volatile 主要提供可见性和顺序，CAS 额外提供“只有期望值仍成立才更新”的条件原子性。二者都不自动保护多个变量：

~~~text
volatile：读取/写入的观察边界
CAS：对一个位置执行条件更新
锁：保护一段临界区和整体不变量
~~~

## 高频追问

- AtomicInteger 为什么能做 count++？因为 incrementAndGet 内部用 CAS 循环重试。
- CAS 能否保证多个字段一致？不能，除非把字段封装为一个可原子替换的状态，或使用锁。
- CAS 会不会阻塞？CAS 本身不是阻塞等待，但失败重试和调度可能让线程持续占用 CPU。
- LongAdder 是否比 AtomicLong 更准确？不是，它更适合高竞争统计；强一致性语义要看业务。
- ABA 一定是 bug 吗？只有中间变化会影响当前判断时才是问题。

## 关键源码路径

- AtomicInteger、AtomicLong、AtomicReference：单变量和引用状态的 CAS API；
- VarHandle：Java 21 应用层原子访问和内存语义入口（AtomicInteger 内部仍走 Unsafe，见上文版本说明）；
- Striped64 / LongAdder：分片计数和热点分散；
- [AtomicityDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AtomicityDemo.java)；
- [AqsLockDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/AqsLockDemo.java)。

## 一句话复盘

CAS 是对一个状态位置做乐观条件更新，不是通用事务；低竞争适合原子类，高竞争或多字段不变量要重新评估锁和状态建模。
