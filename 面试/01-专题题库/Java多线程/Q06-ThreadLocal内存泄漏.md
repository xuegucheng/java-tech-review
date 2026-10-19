# Q06. ThreadLocal 为什么会内存泄漏？JDK 做了什么补救？

> Java多线程 · P0 · 高频

**30 秒回答**：`ThreadLocalMap` 的 key 是弱引用、value 是强引用，key 被回收后 value 仍被 `Entry` 强引用着、而 `Entry` 又被线程持有，于是 value 随线程一起存活——JDK 的补救是 `get`/`set` 时顺带清理 key 为 null 的 `Entry`，但不保证清到，所以必须自己 `remove`。

**深入理解**：结构是 `Thread → ThreadLocalMap → Entry(ThreadLocal 弱引用, value 强引用)`。key 用弱引用是为了让 `ThreadLocal` 对象本身能被回收，避免 key 永远存活；但 value 若也用弱引用，正常使用时就会被回收掉，所以只能强引用。代价就是「key 为 null 但 value 还在」的滞留条目。线程池场景是重灾区：线程长期存活，不 `remove` 则 value 一直在，而且下一个任务可能读到上一个任务的残留数据——这比内存泄漏更危险。

**为什么这样设计**：这是「弱引用只能保护 key、保护不了 value」的结构性结果，不是可以简单修掉的缺陷——真正的解法只有「用完显式 remove」，所以规范写法总是把 `try { ... } finally { tl.remove(); }` 写在一起。JDK 的惰性清理只是减慢泄漏速度，不能当解决方案。

**面试官可能继续追问**：
- **ThreadLocalMap 怎么解决哈希冲突，和 key 弱引用有关系吗？** —— 用开放地址法（线性探测）而非链表；key 过期后探测链可能断，所以 `expungeStaleEntry` 会主动重排后续条目——这是它不用链表的代价之一。
- **`InheritableThreadLocal` 是什么，有什么问题？** —— 创建线程时把父线程的值复制给子线程，适合传递 traceId 这类上下文；但用线程池时「父子」关系不确定（任务提交线程 ≠ 执行线程），传递会失效或串味，需要装饰线程池（如 TTL 的 TransmittableThreadLocal）才行。
- **除了内存泄漏，ThreadLocal 更大的风险是什么？** —— 线程池下的数据串味：上一个请求的 userId、租户 ID 残留在线程变量里，下一个请求读到别人的数据——这是安全 bug，不是性能问题。面试里主动讲这个，层次立刻不一样。

**实际工程场景**：WMS 里常见用法是传递当前操作人、仓库编码、traceId。规范是「入口 set、出口 finally remove」；请求级上下文优先考虑随任务参数显式传递，能不用 ThreadLocal 就不用——它在过滤器拦截器里最自然，在异步线程池里最容易出事。监控上可以定期 dump 线程的 ThreadLocalMap 大小做趋势观察。

**容易答错的地方**：说 value 也是弱引用；以为 JDK 会在 GC 时自动清掉 value；只答内存泄漏不答数据串味。还有「每个线程一个副本所以线程安全」是常见误读——ThreadLocal 保证的是隔离，不是共享变量的正确性，该同步的还是要同步。

> 权威内容：[ThreadLocal 原理与内存泄漏](../../../03-并发编程/02-深度解析/ThreadLocal原理与内存泄漏.md) · [ThreadLocal 与并发容器速记](../../../03-并发编程/01-面试速记/ThreadLocal与并发容器速记.md)

---

返回 [Java多线程 专题](../Java多线程.md) · [专题题库首页](../README.md)
