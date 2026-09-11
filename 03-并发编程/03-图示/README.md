# 并发编程图示

本目录存放独立的结构类图示（SVG）。SVG 使用 1600×900 画布，适合高清缩放、打印和后续维护；流程类 Mermaid 仍直接嵌入正文：

- AQS exclusive 获取路径 → [AQS 核心原理](../02-深度解析/AQS核心原理.md)；
- ThreadPoolExecutor 的 execute 决策与运行状态机 → [ThreadPoolExecutor 线程池](../02-深度解析/ThreadPoolExecutor线程池.md)；
- Condition 双队列与节点转移 → [ReentrantLock 与 Condition](../02-深度解析/ReentrantLock与Condition.md)；
- ThreadLocalMap 弱引用 key 与 stale entry → [ThreadLocal 原理与内存泄漏](../02-深度解析/ThreadLocal原理与内存泄漏.md)。

## 独立高清结构图

这些图围绕并发章节中最容易形成错误心智模型的部分制作，并从对应文章反向链接：

- [线程生命周期与中断协作](线程生命周期/线程生命周期与中断.svg) → [线程模型与生命周期](../02-深度解析/线程模型与生命周期.md)；
- [JMM 安全发布与可见性](JMM/JMM安全发布与可见性.svg) → [Java 内存模型与 happens-before](../02-深度解析/Java内存模型与happens-before.md)；
- [ConcurrentHashMap 并发写入与扩容](ConcurrentHashMap/ConcurrentHashMap并发写入与扩容.svg) → [ConcurrentHashMap 并发容器](../02-深度解析/ConcurrentHashMap并发容器.md)；
- [ThreadPoolExecutor 执行与状态](ThreadPoolExecutor/ThreadPoolExecutor执行与状态.svg) → [ThreadPoolExecutor 线程池](../02-深度解析/ThreadPoolExecutor线程池.md)；
- [AQS 是什么与职责分工](AQS/AQS是什么与职责分工.svg) → [AQS 核心原理](../02-深度解析/AQS核心原理.md)；
- [AQS 与 Condition 双队列](AQS/AQS与Condition双队列.svg) → [AQS 核心原理](../02-深度解析/AQS核心原理.md)；
- [ThreadLocalMap 弱引用链](ThreadLocal/ThreadLocalMap弱引用链.svg) → [ThreadLocal 原理与内存泄漏](../02-深度解析/ThreadLocal原理与内存泄漏.md)。

图示必须服务于一个明确的 mental model，并从对应文章反向链接；不为了填满目录生成低价值图。
