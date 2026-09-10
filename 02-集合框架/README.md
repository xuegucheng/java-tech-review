# 集合框架

> 从接口契约走到数据结构和工程选型。默认阅读顺序是面试速记 → 关键图示与实验 → 深度解析。

返回 [仓库首页](../README.md)，语言层的类型与相等性定义见 [Java核心](../01-Java核心/README.md)。

## 模块结构

```text
02-集合框架/
├── README.md
├── 01-面试速记/       # 5～15 分钟恢复面试表达
├── 02-深度解析/       # 契约、结构、源码路径和工程边界
├── 03-图示/           # HashMap、LinkedHashMap 等结构图
└── 04-示例代码/       # Java 21 + Maven Wrapper + JUnit 5
```

每个主题在本模块内按 Interview Review、Deep Dive、Diagram、Runnable Example 和 Test 定位，避免读者在资源类型之间来回猜目录。

## 先从哪里开始

### 1 天突击

1. [集合选型与契约](01-面试速记/集合选型.md)
2. [HashMap 与 Hash 集合](01-面试速记/HashMap与Hash集合.md)
3. [有序 Map 与 LRU](01-面试速记/有序Map与LRU缓存.md)

### 7 天复习中的集合框架

先走完上面的 P0，再补 [List 选型](01-面试速记/List选型.md)、[Collections 泛型 API 设计](02-深度解析/集合泛型API设计.md) 和各实现源码主线。Java 泛型语言规则统一见 [Java Core 泛型权威文档](../01-Java核心/02-深度解析/Java泛型与类型安全.md)。不要从 `ArrayList`、`LinkedList`、`HashMap` 的 API 清单开始背；先确定数据语义、顺序、并发边界和主要操作。

## 面试速记

| 优先级 | 主题 | 目标 |
| --- | --- | --- |
| P0 | [集合选型与契约](01-面试速记/集合选型.md) | 根据重复、顺序、复杂度、null 和并发做选择 |
| P0 | [HashMap 与 Hash 集合](01-面试速记/HashMap与Hash集合.md) | 讲清 bucket、put、resize、equals/hashCode 和 Set 去重 |
| P0 | [有序 Map 与 LRU](01-面试速记/有序Map与LRU缓存.md) | 讲清 insertion-order、access-order 和淘汰边界 |
| P1 | [List 选型](01-面试速记/List选型.md) | 解释 ArrayList/LinkedList/ArrayDeque 的真实取舍 |

## 深度解析资产

| 主题 | Deep Dive |
| --- | --- |
| 集合泛型 API 设计 | [集合泛型API设计.md](02-深度解析/集合泛型API设计.md) |
| 集合接口与核心契约 | [集合框架体系与核心契约.md](02-深度解析/集合框架体系与核心契约.md) |
| ArrayList | [ArrayList原理与实现.md](02-深度解析/ArrayList原理与实现.md) |
| LinkedList | [LinkedList原理与实现.md](02-深度解析/LinkedList原理与实现.md) |
| HashMap | [HashMap原理与源码分析.md](02-深度解析/HashMap原理与源码分析.md) |
| Hash 集合契约 | [Hash集合与equals-hashCode契约.md](02-深度解析/Hash集合与equals-hashCode契约.md) |
| HashSet 与 LinkedHashSet | [HashSet与LinkedHashSet.md](02-深度解析/HashSet与LinkedHashSet.md) |
| LinkedHashMap 与 LRU | [LinkedHashMap与LRU缓存.md](02-深度解析/LinkedHashMap与LRU缓存.md) |

## 集合冻结范围

当前集合模块先冻结三条高价值主线。新增文章或示例必须归属于下表中的职责，不能重新定义已有契约：

| 主线 | 唯一权威内容 | 本模块负责的关键机制 | 当前验证状态 |
| --- | --- | --- | --- |
| HashMap | [HashMap 原理与源码分析](02-深度解析/HashMap原理与源码分析.md) | hash 扰动、桶定位、put、treeify、resize | 冲突示例 + 测试 |
| HashSet / LinkedHashSet | [HashSet 与 LinkedHashSet](02-深度解析/HashSet与LinkedHashSet.md) | HashMap 复用、去重和 encounter order | 文章与 Mermaid 流程 |
| LinkedHashMap / LRU | [LinkedHashMap 与 LRU 缓存](02-深度解析/LinkedHashMap与LRU缓存.md) | before/after 链表、access-order、淘汰边界 | LRU 示例 + 测试 |

`equals/hashCode` 的语言契约继续由 Java 核心模块维护；`ConcurrentHashMap` 的并发语义归入 [03-并发编程](../03-并发编程/README.md)，本模块只维护 HashMap 基础结构及其集合消费关系。本轮不为每个 API 机械创建 Demo，优先验证会改变理解的机制。

## 主题资源地图

| 主题 | Interview Review | Deep Dive | Diagram | Runnable Example | Test |
| --- | --- | --- | --- | --- | --- |
| HashMap | [HashMap 与 Hash 集合](01-面试速记/HashMap与Hash集合.md) | [HashMap 原理与源码分析](02-深度解析/HashMap原理与源码分析.md) | [HashMap 扩容](03-图示/HashMap/HashMap扩容.svg) | [HashMapCollisionDemo.java](04-示例代码/src/main/java/com/xuegucheng/javatechreview/collections/HashMapCollisionDemo.java) | [HashMapCollisionDemoTest.java](04-示例代码/src/test/java/com/xuegucheng/javatechreview/collections/HashMapCollisionDemoTest.java) |
| LinkedHashMap / LRU | [有序 Map 与 LRU](01-面试速记/有序Map与LRU缓存.md) | [LinkedHashMap 与 LRU 缓存](02-深度解析/LinkedHashMap与LRU缓存.md) | [顺序与 LRU](03-图示/LinkedHashMap/LinkedHashMap与LRU.svg)、[数据结构](03-图示/LinkedHashMap/LinkedHashMap数据结构.png) | [LinkedHashMapLruDemo.java](04-示例代码/src/main/java/com/xuegucheng/javatechreview/collections/LinkedHashMapLruDemo.java) | [LinkedHashMapLruDemoTest.java](04-示例代码/src/test/java/com/xuegucheng/javatechreview/collections/LinkedHashMapLruDemoTest.java) |

Hash 集合与 List 等主题暂时只有文章层；未来新增实验时仍按本模块模板落入 `04-示例代码/`，并补齐测试与反向链接。

## P2 backlog

TreeMap、TreeSet、PriorityQueue、ArrayDeque、CopyOnWriteArrayList 和 BlockingQueue 还没有在仓库中伪装成“已完成”；ConcurrentHashMap 已在 [Concurrency 模块](../03-并发编程/02-深度解析/ConcurrentHashMap并发容器.md) 建立第一版。新增主题时先决定它属于 Collections 还是 Concurrency，并为每个主题指定唯一权威来源。

## One Source of Truth

- `equals/hashCode` 的语言契约唯一维护在 [Java Core 的对象契约](../01-Java核心/02-深度解析/equals与hashCode契约.md)。
- 本模块只解释哈希容器如何消费该契约，不重新完整定义相等性规则。
- HashMap 拥有 hash 扰动、桶定位、树化和 resize 的实现主线；HashSet、LinkedHashSet、LinkedHashMap 只解释各自的复用和增量结构。
- 并发集合属于 [Concurrency 模块](../03-并发编程/README.md)，避免在此处形成第二套并发语义。

## 运行示例

在仓库根目录执行本模块测试：

```powershell
.\mvnw.cmd -pl '02-集合框架/04-示例代码' test
```

macOS / Linux：

```bash
./mvnw -pl '02-集合框架/04-示例代码' test
```

完整索引见 [本模块的 `04-示例代码/`](04-示例代码/README.md)。
