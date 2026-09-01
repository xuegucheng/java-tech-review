# Java Tech Review

> 面向 3～10 年 Java 后端工程师的技术复习仓库：先恢复面试表达，再进入 mental model、源码路径和可运行验证。

它不是 Java 初学教程、API 大全或不断追加 Markdown 的文件清单。每个主题尽量形成：

```text
Interview Answer → Mental Model → Deep Dive → Source Path → Runnable Example
```

## 10 秒知道先看什么

- **今天突击**：从 [Java Core](01-java-core/README.md) 和 [Collections](02-collections/README.md) 的 `Interview Review` 开始。
- **1 天路线**：只看下方 P0；先 Java 对象模型/参数传递/相等性，再集合契约/HashMap/LRU。
- **7 天路线**：Day 1 Java Core + Collections，Day 2 Concurrency，Day 3 JVM，Day 4 MySQL，Day 5 Redis + MQ，Day 6 Spring，Day 7 Distributed + System Design。未创建的模块会明确保持为待建设，不伪装成已完成。
- **怀疑一个结论**：从 Review 页面跳到 Deep Dive，再运行 [examples](examples/README.md)。

## P0：面试高频 / 核心机制

| 主题 | 先看 | 再深入 |
| --- | --- | --- |
| Java 参数传递与不可变对象 | [Interview Review](01-java-core/interview/parameter-passing-and-immutability.md) | [Deep Dive](01-java-core/deep-dive/parameter-passing.md) |
| 对象模型与多态 | [Interview Review](01-java-core/interview/object-model-and-polymorphism.md) | [Deep Dive](01-java-core/deep-dive/polymorphism-and-dynamic-dispatch.md) |
| equals/hashCode | [Interview Review](01-java-core/interview/equality-and-object-contract.md) | [Deep Dive](01-java-core/deep-dive/equals-and-hashcode-contract.md) |
| 集合契约与选型 | [Interview Review](02-collections/interview/collection-selection.md) | [Deep Dive](02-collections/deep-dive/collection-contracts.md) |
| HashMap 与 Hash 集合 | [Interview Review](02-collections/interview/hashmap-and-hash-collections.md) | [HashMap Deep Dive](02-collections/deep-dive/hashmap.md) |
| LinkedHashMap 与 LRU | [Interview Review](02-collections/interview/ordered-map-and-lru.md) | [Deep Dive](02-collections/deep-dive/linkedhashmap-and-lru.md) |

P0 是导航权重，不是“其他内容不重要”。完整原理文章仍然保留，只是不让低频细节挡住核心机制。

## 仓库结构

```text
java-tech-review/
├── 01-java-core/
│   ├── README.md
│   ├── interview/       # 5～15 分钟复习
│   └── deep-dive/       # 原有长文、规范边界、关键源码
├── 02-collections/
│   ├── README.md
│   ├── interview/
│   └── deep-dive/
├── diagrams/
│   ├── java/            # SVG mental model
│   ├── collections/     # SVG 数据结构图
│   └── legacy/          # 原有 PNG 资产，仅作保留
├── examples/            # Java 21 + Maven + JUnit 5
├── docs/                # 仓库架构契约
├── research/            # Benchmark 与重构依据
└── NOTE_STYLE_GUIDE.md
```

### 模块入口

| 模块 | 定位 | 状态 |
| --- | --- | --- |
| [01 Java Core](01-java-core/README.md) | 语言语义、对象模型、类型设计和运行边界 | 已整理 Interview Review + Deep Dive |
| [02 Collections](02-collections/README.md) | 集合契约、数据结构、哈希和顺序语义 | 已整理 Interview Review + Deep Dive |
| Concurrency | JMM、锁、AQS、线程池和并发集合 | 待建设 |
| JVM | 类加载、内存、GC、诊断和调优 | 待建设 |
| Spring / Database / Redis / MQ | 生产后端核心栈 | 待建设 |
| Distributed / System Design | 分布式一致性、可靠性和设计题 | 待建设 |

## 如何阅读一篇主题

1. 先读 `面试结论` 和 `30 秒回答`，确认自己能否说清。
2. 顺着 `面试追问链` 继续问“为什么”，不要平铺背 40 道题。
3. 需要细节时进入对应的 `Deep Dive`，只追关键源码路径。
4. 需要证据时运行 `examples/` 中的示例和测试。

## Runnable Examples

示例工程默认 Java 21，完整类和测试不嵌入正文。执行：

```bash
mvn -f examples/pom.xml test
```

当前可验证：参数传递、HashMap 冲突、LinkedHashMap LRU、Holder 类初始化、重载解析、多态分派和 `final` 引用边界。详见 [examples/README.md](examples/README.md)。

## 图示策略

- Mermaid 用于流程、决策、生命周期和简单时序。
- SVG 用于 bucket、链表、引用、队列和对象布局等空间结构。
- 一张图只解决一个真正难理解的问题，不重复正文。

入口：[图示规则](diagrams/README.md)、[Java 复习闭环](diagrams/java/review-loop.svg)、[重载解析](diagrams/java/overload-resolution.svg)、[参数传递](diagrams/java/java-pass-by-value.svg)、[多态分派](diagrams/java/polymorphism-dispatch.svg)、[类初始化](diagrams/java/class-initialization.svg)、[final 引用](diagrams/java/final-reference-vs-immutable.svg)、[HashMap resize](diagrams/collections/hashmap-resize.svg)、[LinkedHashMap/LRU](diagrams/collections/linkedhashmap-lru.svg)。

## Java 版本策略

- 经典面试主线默认 `JDK 8+`。
- `Java 17+`、`Java 21+`、`Java 25+` 的语法和 API 会显式标记。
- examples 默认 Java 21；未来 Java 25 实验必须使用独立 profile/module，不破坏默认构建。
- 规范保证、OpenJDK 实现和版本相关观察要分开表述。

## 维护入口

- [仓库架构说明](docs/REPOSITORY_ARCHITECTURE.md)：目录、分层、P0/P1/P2、One Source of Truth 和新增内容规则。
- [Benchmark 研究报告](research/repository-benchmark.md)：7 个公开项目的比较和本次 `REBUILD_STRUCTURE` 依据。
- [Note Style Guide](NOTE_STYLE_GUIDE.md)：结论优先、追问链、图示、版本和示例规范。

## 许可证状态

当前仓库没有检测到 `LICENSE` 文件。若要对外复用或分发，请先补充并确认明确的许可证，不从旧 README 的徽章推断授权范围。
