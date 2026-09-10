# ThreadLocal 与并发容器速记

> 面试定位：线程隔离、共享容器和复合操作边界
> Java 版本：ThreadLocalMap 与 ConcurrentHashMap 实现重点按 Java 21
> P0/P1：P0 总结 + P1 入口
> 前置知识：HashMap、volatile、CAS 和线程池
> 本文不负责：复写 HashMap 的基础结构和所有并发集合 API

## 先说结论

ThreadLocal 解决的是“同一份逻辑数据不想在线程之间共享”，数据实际位于 Thread 的 ThreadLocalMap；在线程池里线程会长期复用，所以 set 后必须在 finally 中 remove。

ConcurrentHashMap 解决的是多个线程共享 Map 时的局部并发访问，不提供跨多个 key 的事务一致性。Java 8+ / Java 21 主要围绕 Node 数组、CAS、bin 级 synchronized、树化和协作扩容组织。

## ThreadLocal

~~~text
Thread
  ↓
ThreadLocalMap
  ↓
Entry[]：弱引用 key + 强引用 value
~~~

key 弱引用只降低 ThreadLocal 对象本身的存活约束；key 被回收后，Entry 的 value 仍可能由线程强引用。ThreadLocalMap 会在后续访问、设置或显式清理时发现 stale entry，但不能把它当成及时、全局的泄漏回收机制。

推荐边界：

~~~java
try {
    context.set(requestContext);
    handle();
} finally {
    context.remove();
}
~~~

## ConcurrentHashMap

- get 大多数路径不需要锁，依赖 volatile 访问和节点链路可见性；
- 空 bin 的首次写入可以用 CAS；
- 非空 bin 的更新可能在 bin 头节点上 synchronized；
- 冲突严重时可以使用 TreeBin；
- resize 期间其他线程可以通过 ForwardingNode 协助迁移；
- 不允许 null，因为并发 get 无法区分“没有映射”和“映射值是 null”，也会破坏 computeIfAbsent 等原子 API 的语义。

完整解释分别见 [ThreadLocal 原理与内存泄漏](../02-深度解析/ThreadLocal原理与内存泄漏.md) 和 [ConcurrentHashMap 并发容器](../02-深度解析/ConcurrentHashMap并发容器.md)。

## 其他 P1 入口

- [JUC 并发协作工具](../02-深度解析/JUC并发协作工具.md)：按问题模型理解 CountDownLatch、CyclicBarrier、Semaphore；
- [CompletableFuture 异步编排](../02-深度解析/CompletableFuture异步编排.md)：重点看依赖、聚合、异常和执行器隔离；
- [Java 21 虚拟线程](../02-深度解析/Java21虚拟线程.md)：重点看阻塞 I/O 的资源成本，不把它当 CPU 加速器。

## 一句话复盘

ThreadLocal 减少共享但增加清理责任，ConcurrentHashMap 降低共享 Map 的竞争但不提供业务事务；二者都不能代替完整的生命周期和一致性设计。
