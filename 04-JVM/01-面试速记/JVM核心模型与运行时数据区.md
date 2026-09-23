# JVM 核心模型与运行时数据区

> **P0 · Java 21 / JVMS 21 · 面试速记**
>
> 权威解释见 [JVM 执行模型与运行时数据区](../02-深度解析/JVM执行模型与运行时数据区.md)。

## 一句话结论

先按 JVMS 解释运行时区域的抽象职责和线程生命周期，再把 Metaspace、Direct Memory 等放回 HotSpot / 进程实现视角；Runtime Data Areas 与 JMM 不是同一个模型。

## 30 秒回答

> JVMS 的运行时数据区中，Heap 和 Method Area 由 JVM 线程共享；每个线程有自己的 pc Register 和 JVM Stack，方法调用时创建 Frame。每个类或接口有自己的 Run-Time Constant Pool，它在规范模型中属于 Method Area。Java 21 HotSpot 的 Metaspace 是类元数据的本地内存管理实现，不等于规范里的 Method Area；DirectByteBuffer 的内容可能位于普通 GC 堆外，但 Direct Memory 不是 JVMS 单列的运行时区域。运行时区域描述结构与生命周期，JMM 描述并发读写语义。

## 口述展开顺序

1. **执行入口**：常见路径是 .java 经 javac 生成 .class，再由 java 启动 JVM 并装入入口类。
2. **区域分组**：Heap、Method Area 共享；pc Register、JVM Stack 按线程创建。Native Method Stack 由实现决定是否提供。
3. **方法现场**：一次方法调用创建一个 Frame；Frame 有 Local Variables、Operand Stack 和当前类型 Run-Time Constant Pool 的引用。
4. **引用与对象**：局部变量保存 reference 值；对象按 JVMS 抽象模型归属 Heap。HotSpot 优化可能消除某次实际分配，但不代表对象搬到了 JVM Stack。
5. **实现边界**：Method Area 是规范逻辑区域；Metaspace 是 HotSpot 机制。DirectByteBuffer 内容可能堆外，Direct Memory 不属于 JVMS Runtime Data Areas。
6. **并发边界**：区域共享/私有不推出线程安全结论；JMM 的唯一 Owner 在并发模块。

## 高频追问链

哪些区域共享？ → 一次方法调用怎样创建 Frame？ → 局部变量保存对象还是引用？ → Method Area 与 Metaspace 什么关系？ → Runtime Constant Pool 是全局还是每类一份？ → Direct Memory 算规范运行时区域吗？ → Runtime Data Areas 和 JMM 分别解决什么问题？

## 易错词

- 不说“方法区就是元空间”或“Java 8 后方法区改名”。
- 不说“局部变量在栈上，所以对象就在栈上”。
- 不把 Direct Memory 画进 JVMS 区域清单。
- 不把 Class File constant_pool、Run-Time Constant Pool 和字符串驻留机制混成一个池。
- 不从线程共享/私有推导并发安全或对象不可共享。

## 配套资源

- [完整 Deep Dive：执行模型与运行时数据区](../02-深度解析/JVM执行模型与运行时数据区.md)
- [Runtime Data Areas 高清结构图](../03-图示/JVM/JVM运行时数据区.svg)
- [JVM Knowledge Map 与后续 Owner](../README.md)

本文保留口述结论和追问路线；规范解释、实现边界与追问答案由 Deep Dive 唯一维护。

\n