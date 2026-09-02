# Java Tech Review

> 面向 3～10 年 Java 后端工程师的技术复习仓库：先恢复面试表达，再进入 mental model、源码路径和可运行验证。

仓库按技术域组织。每个主题尽量形成：

```text
Interview Review → Deep Dive → Diagram → Runnable Example → Test
```

## 10 秒知道先看什么

- **今天突击**：从 [Java 核心](01-Java核心/README.md) 和 [集合框架](02-集合框架/README.md) 的 `01-面试速记/` 开始。
- **1 天路线**：先 Java 对象模型、参数传递、相等性，再集合契约、HashMap 和 LRU。
- **7 天路线**：Day 1 Java Core + Collections，Day 2 Concurrency，Day 3 JVM，Day 4 MySQL，Day 5 Redis + MQ，Day 6 Spring，Day 7 Distributed + System Design。尚未创建的模块会保持为待建设。
- **验证结论**：从主题文章进入同域 `03-图示/`、`04-示例代码/` 和测试。

## P0：面试高频 / 核心机制

| 主题 | Interview Review | Deep Dive |
| --- | --- | --- |
| Java 参数传递与不可变对象 | [Interview Review](01-Java核心/01-面试速记/参数传递与不可变对象.md) | [Deep Dive](01-Java核心/02-深度解析/参数传递机制.md) |
| 对象模型与多态 | [Interview Review](01-Java核心/01-面试速记/对象模型与多态.md) | [Deep Dive](01-Java核心/02-深度解析/多态与动态绑定.md) |
| equals/hashCode | [Interview Review](01-Java核心/01-面试速记/equals与hashCode对象契约.md) | [Deep Dive](01-Java核心/02-深度解析/equals与hashCode契约.md) |
| 集合契约与选型 | [Interview Review](02-集合框架/01-面试速记/集合选型.md) | [Deep Dive](02-集合框架/02-深度解析/集合框架体系与核心契约.md) |
| HashMap 与 Hash 集合 | [Interview Review](02-集合框架/01-面试速记/HashMap与Hash集合.md) | [HashMap Deep Dive](02-集合框架/02-深度解析/HashMap原理与源码分析.md) |
| LinkedHashMap 与 LRU | [Interview Review](02-集合框架/01-面试速记/有序Map与LRU缓存.md) | [Deep Dive](02-集合框架/02-深度解析/LinkedHashMap与LRU缓存.md) |

P0 是导航权重，不是内容价值评级。完整原理文章仍然保留，只是不让低频细节挡住核心机制。

## 仓库结构

```text
java-tech-review/
├── README.md
├── pom.xml
├── 01-Java核心/
│   ├── README.md
│   ├── 01-面试速记/
│   ├── 02-深度解析/
│   ├── 03-图示/
│   └── 04-示例代码/
│       ├── README.md
│       ├── pom.xml
│       └── src/main/java + src/test/java
├── 02-集合框架/
│   ├── README.md
│   ├── 01-面试速记/
│   ├── 02-深度解析/
│   ├── 03-图示/
│   └── 04-示例代码/
│       ├── README.md
│       ├── pom.xml
│       └── src/main/java + src/test/java
├── 项目文档/
├── 调研/
├── .github/
├── .gitignore
└── 笔记编写规范.md
```

技术域是第一组织维度；Interview Review、Deep Dive、Diagram 和 Runnable Example 是同一主题的不同表达层，资源优先留在所属模块内。根目录只保留跨模块地图、聚合构建和仓库级文档。

## 模块入口

| 模块 | 定位 | 状态 |
| --- | --- | --- |
| [01 Java 核心](01-Java核心/README.md) | 语言语义、对象模型、类型设计和运行边界 | 已整理面试速记、深度解析、图示和 5 个示例 |
| [02 集合框架](02-集合框架/README.md) | 集合契约、数据结构、哈希和顺序语义 | 已整理面试速记、深度解析、图示和 2 个示例 |
| Concurrency | JMM、锁、AQS、线程池和并发集合 | 待建设 |
| JVM | 类加载、内存、GC、诊断和调优 | 待建设 |
| Spring / Database / Redis / MQ | 生产后端核心栈 | 待建设 |
| Distributed / System Design | 分布式一致性、可靠性和设计题 | 待建设 |

## 统一验证

示例工程默认 Java 21、Maven 3.9+、JUnit 5。仓库根目录提供聚合 POM：

```bash
mvn test
mvn -pl '01-Java核心/04-示例代码' test
mvn -pl '02-集合框架/04-示例代码' test
```

模块 README 提供各自的示例索引；完整类和测试不嵌入正文。

## Java 版本策略

- 经典面试主线默认 `JDK 8+`。
- `Java 17+`、`Java 21+`、`Java 25+` 的语法和 API 会显式标记。
- 示例代码默认 Java 21；未来 Java 25 实验必须使用独立 profile/module，不破坏默认构建。
- 规范保证、OpenJDK 实现和版本相关观察要分开表述。

## 维护入口

- [仓库架构说明](项目文档/仓库架构说明.md)：目录、分层、P0/P1/P2、One Source of Truth 和新增内容规则。
- [Benchmark 研究报告](调研/Java技术复习仓库Benchmark.md)：公开项目比较和结构重构依据。
- [笔记编写规范](笔记编写规范.md)：结论优先、追问链、图示、版本和示例规范。

## 许可证状态

当前仓库没有检测到 `LICENSE` 文件。若要对外复用或分发，请先补充并确认明确的许可证，不从旧 README 的徽章推断授权范围。
