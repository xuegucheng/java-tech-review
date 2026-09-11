# 并发编程图示

本目录存放独立的结构类图示（SVG）。流程类 Mermaid 已直接嵌入正文：

- AQS exclusive 获取路径 → [AQS 核心原理](../02-深度解析/AQS核心原理.md)；
- ThreadPoolExecutor 的 execute 决策与运行状态机 → [ThreadPoolExecutor 线程池](../02-深度解析/ThreadPoolExecutor线程池.md)；
- Condition 双队列与节点转移 → [ReentrantLock 与 Condition](../02-深度解析/ReentrantLock与Condition.md)；
- ThreadLocalMap 弱引用 key 与 stale entry → [ThreadLocal 原理与内存泄漏](../02-深度解析/ThreadLocal原理与内存泄漏.md)。

后续优先考虑的独立结构图：

- 线程生命周期与中断协作；
- JMM 的可见性、happens-before 与安全发布；
- ConcurrentHashMap 的 bin、树化和协作扩容。

图示必须服务于一个明确的 mental model，并从对应文章反向链接；不为了填满目录生成低价值图。
