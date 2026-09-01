# Collections

> 从接口契约走到数据结构和工程选型。默认阅读顺序是 Interview Review → 关键图示/实验 → Deep Dive。

返回 [仓库首页](../README.md)，语言层的类型与相等性定义见 [Java Core](../01-java-core/README.md)。

## 先从哪里开始

### 1 天突击

1. [集合选型与契约](interview/collection-selection.md)
2. [HashMap 与 Hash 集合](interview/hashmap-and-hash-collections.md)
3. [有序 Map 与 LRU](interview/ordered-map-and-lru.md)

### 7 天复习中的 Collections

先走完上面的 P0，再补 [List 选型](interview/list-choice.md)、[Collections 泛型 API 设计](deep-dive/collection-generic-api-design.md) 和各实现源码主线。Java 泛型语言规则统一见 [Java Core 泛型权威文档](../01-java-core/deep-dive/generics.md)。不要从 `ArrayList`、`LinkedList`、`HashMap` 的 API 清单开始背；先确定数据语义、顺序、并发边界和主要操作。

## Interview Review

| 优先级 | 主题 | 目标 |
| --- | --- | --- |
| P0 | [集合选型与契约](interview/collection-selection.md) | 根据重复、顺序、复杂度、null 和并发做选择 |
| P0 | [HashMap 与 Hash 集合](interview/hashmap-and-hash-collections.md) | 讲清 bucket、put、resize、equals/hashCode 和 Set 去重 |
| P0 | [有序 Map 与 LRU](interview/ordered-map-and-lru.md) | 讲清 insertion-order、access-order 和淘汰边界 |
| P1 | [List 选型](interview/list-choice.md) | 解释 ArrayList/LinkedList/ArrayDeque 的真实取舍 |

## Deep Dive 资产

| 主题 | Deep Dive |
| --- | --- |
| 集合泛型 API 设计 | [collection-generic-api-design.md](deep-dive/collection-generic-api-design.md) |
| 集合接口与核心契约 | [collection-contracts.md](deep-dive/collection-contracts.md) |
| ArrayList | [arraylist.md](deep-dive/arraylist.md) |
| LinkedList | [linkedlist.md](deep-dive/linkedlist.md) |
| HashMap | [hashmap.md](deep-dive/hashmap.md) |
| Hash 集合契约 | [hash-collections-and-set-contract.md](deep-dive/hash-collections-and-set-contract.md) |
| HashSet 与 LinkedHashSet | [hashset-and-linkedhashset.md](deep-dive/hashset-and-linkedhashset.md) |
| LinkedHashMap 与 LRU | [linkedhashmap-and-lru.md](deep-dive/linkedhashmap-and-lru.md) |

## P2 backlog

TreeMap、TreeSet、PriorityQueue、ArrayDeque、ConcurrentHashMap、CopyOnWriteArrayList 和 BlockingQueue 还没有在仓库中伪装成“已完成”。新增时先决定它属于 Collections 还是 Concurrency，并为每个主题指定唯一权威来源。

## One Source of Truth

- `equals/hashCode` 的语言契约唯一维护在 [Java Core 的对象契约](../01-java-core/deep-dive/equals-and-hashcode-contract.md)。
- 本模块只解释哈希容器如何消费该契约，不重新完整定义相等性规则。
- HashMap 拥有 hash 扰动、桶定位、树化和 resize 的实现主线；HashSet、LinkedHashSet、LinkedHashMap 只解释各自的复用和增量结构。
- 并发集合将在未来 `concurrency` 模块落地，避免在此处形成第二套并发语义。

## 图示与实验

- [HashMap resize 图](../diagrams/collections/hashmap-resize.svg)
- [LinkedHashMap/LRU 图](../diagrams/collections/linkedhashmap-lru.svg)
- [examples/](../examples/README.md)
- [图示规则](../diagrams/README.md)
