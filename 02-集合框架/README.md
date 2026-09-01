# 集合框架

> 从接口契约走到数据结构和工程选型。默认阅读顺序是面试速记 → 关键图示/实验 → 深度解析。

返回 [仓库首页](../README.md)，语言层的类型与相等性定义见 [Java核心](../01-Java核心/README.md)。

## 先从哪里开始

### 1 天突击

1. [集合选型与契约](面试速记/集合选型.md)
2. [HashMap 与 Hash 集合](面试速记/HashMap与Hash集合.md)
3. [有序 Map 与 LRU](面试速记/有序Map与LRU缓存.md)

### 7 天复习中的集合框架

先走完上面的 P0，再补 [List 选型](面试速记/List选型.md)、[Collections 泛型 API 设计](深度解析/集合泛型API设计.md) 和各实现源码主线。Java 泛型语言规则统一见 [Java Core 泛型权威文档](../01-Java核心/深度解析/Java泛型与类型安全.md)。不要从 `ArrayList`、`LinkedList`、`HashMap` 的 API 清单开始背；先确定数据语义、顺序、并发边界和主要操作。

## 面试速记

| 优先级 | 主题 | 目标 |
| --- | --- | --- |
| P0 | [集合选型与契约](面试速记/集合选型.md) | 根据重复、顺序、复杂度、null 和并发做选择 |
| P0 | [HashMap 与 Hash 集合](面试速记/HashMap与Hash集合.md) | 讲清 bucket、put、resize、equals/hashCode 和 Set 去重 |
| P0 | [有序 Map 与 LRU](面试速记/有序Map与LRU缓存.md) | 讲清 insertion-order、access-order 和淘汰边界 |
| P1 | [List 选型](面试速记/List选型.md) | 解释 ArrayList/LinkedList/ArrayDeque 的真实取舍 |

## 深度解析资产

| 主题 | Deep Dive |
| --- | --- |
| 集合泛型 API 设计 | [集合泛型API设计.md](深度解析/集合泛型API设计.md) |
| 集合接口与核心契约 | [集合框架体系与核心契约.md](深度解析/集合框架体系与核心契约.md) |
| ArrayList | [ArrayList原理与实现.md](深度解析/ArrayList原理与实现.md) |
| LinkedList | [LinkedList原理与实现.md](深度解析/LinkedList原理与实现.md) |
| HashMap | [HashMap原理与源码分析.md](深度解析/HashMap原理与源码分析.md) |
| Hash 集合契约 | [Hash集合与equals-hashCode契约.md](深度解析/Hash集合与equals-hashCode契约.md) |
| HashSet 与 LinkedHashSet | [HashSet与LinkedHashSet.md](深度解析/HashSet与LinkedHashSet.md) |
| LinkedHashMap 与 LRU | [LinkedHashMap与LRU缓存.md](深度解析/LinkedHashMap与LRU缓存.md) |

## P2 backlog

TreeMap、TreeSet、PriorityQueue、ArrayDeque、ConcurrentHashMap、CopyOnWriteArrayList 和 BlockingQueue 还没有在仓库中伪装成“已完成”。新增时先决定它属于 Collections 还是 Concurrency，并为每个主题指定唯一权威来源。

## One Source of Truth

- `equals/hashCode` 的语言契约唯一维护在 [Java Core 的对象契约](../01-Java核心/深度解析/equals与hashCode契约.md)。
- 本模块只解释哈希容器如何消费该契约，不重新完整定义相等性规则。
- HashMap 拥有 hash 扰动、桶定位、树化和 resize 的实现主线；HashSet、LinkedHashSet、LinkedHashMap 只解释各自的复用和增量结构。
- 并发集合将在未来 `concurrency` 模块落地，避免在此处形成第二套并发语义。

## 图示与实验

- [HashMap resize 图](../图示/集合框架/HashMap扩容.svg)
- [LinkedHashMap/LRU 图](../图示/集合框架/LinkedHashMap与LRU.svg)
- [示例代码/](../示例代码/README.md)
- [图示规则](../图示/README.md)
