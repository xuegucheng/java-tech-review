# Java Tech Review

> 面向有经验 Java 后端工程师的通用技术复习仓库，重点服务 5 年以上工程师的面试复盘、原理深挖和源码理解：先恢复面试表达，再进入 mental model、源码路径和可运行验证。

仓库按技术域组织。每个主题尽量形成：

```text
Interview Review → Deep Dive → Diagram → Runnable Example → Test
```

## 10 秒知道先看什么

- **今天突击**：从 [Java 核心](01-Java核心/README.md) 和 [集合框架](02-集合框架/README.md) 的 `01-面试速记/` 开始。
- **1 天路线**：先 Java 对象模型、参数传递、相等性，再集合契约、HashMap 和 LRU。
- **7 天路线**：Day 1 Java Core + Collections，Day 2 Concurrency，Day 3 JVM，Day 4 MySQL，Day 5 Redis + MQ，Day 6 Spring，Day 7 Distributed + System Design。当前并发模块已完成第一版，进入人工内容审核阶段；尚未创建的模块仍保持为待建设。
- **验证结论**：从主题文章进入同域 `03-图示/`、`04-示例代码/` 和测试。
- **阅读边界**：正文优先使用通用领域模型；订单、缓存、资源分配等工程案例用于解释取舍，特定行业案例只作为可选补充，不要求读者了解作者的工作背景。

## P0：面试高频 / 核心机制

| 主题 | Interview Review | Deep Dive |
| --- | --- | --- |
| Java 参数传递与不可变对象 | [Interview Review](01-Java核心/01-面试速记/参数传递与不可变对象.md) | [Deep Dive](01-Java核心/02-深度解析/参数传递机制.md) |
| 对象模型与多态 | [Interview Review](01-Java核心/01-面试速记/对象模型与多态.md) | [Deep Dive](01-Java核心/02-深度解析/多态与动态绑定.md) |
| equals/hashCode | [Interview Review](01-Java核心/01-面试速记/equals与hashCode对象契约.md) | [Deep Dive](01-Java核心/02-深度解析/equals与hashCode契约.md) |
| 集合契约与选型 | [Interview Review](02-集合框架/01-面试速记/集合选型.md) | [Deep Dive](02-集合框架/02-深度解析/集合框架体系与核心契约.md) |
| HashMap 与 Hash 集合 | [Interview Review](02-集合框架/01-面试速记/HashMap与Hash集合.md) | [HashMap Deep Dive](02-集合框架/02-深度解析/HashMap原理与源码分析.md) |
| LinkedHashMap 与 LRU | [Interview Review](02-集合框架/01-面试速记/有序Map与LRU缓存.md) | [Deep Dive](02-集合框架/02-深度解析/LinkedHashMap与LRU缓存.md) |
| 并发主线：JMM、锁、AQS、线程池 | [Concurrency Review](03-并发编程/01-面试速记/并发编程面试主线.md) | [Concurrency Deep Dive](03-并发编程/README.md) |

P0 是导航权重，不是内容价值评级。完整原理文章仍然保留，只是不让低频细节挡住核心机制。

## 仓库结构

```text
java-tech-review/
├── README.md
├── pom.xml
├── LICENSE
├── mvnw / mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── .editorconfig
├── .gitattributes
├── CONTRIBUTING.md
├── scripts/
│   ├── check_markdown.py
│   └── check_text_sanity.py
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
├── 03-并发编程/
│   ├── README.md
│   ├── 01-面试速记/
│   ├── 02-深度解析/
│   ├── 03-图示/           # 第一版只保留待补图说明
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
| [03 并发编程](03-并发编程/README.md) | JMM、锁、AQS、线程池、ThreadLocal、并发集合和现代并发模型 | 第一版完成，待人工内容审核 |
| JVM | 类加载、内存、GC、诊断和调优 | 待建设 |
| Spring / Database / Redis / MQ | 生产后端核心栈 | 待建设 |
| Distributed / System Design | 分布式一致性、可靠性和设计题 | 待建设 |

## 统一验证

示例工程默认 Java 21、Maven Wrapper（固定 Maven 3.9.16）和 JUnit 5。仓库根目录提供 Wrapper 和聚合 POM；首次运行会自动准备固定版本的 Maven：

```powershell
# Windows PowerShell
.\mvnw.cmd test
.\mvnw.cmd -pl '01-Java核心/04-示例代码' test
.\mvnw.cmd -pl '02-集合框架/04-示例代码' test
.\mvnw.cmd -pl '03-并发编程/04-示例代码' test
```

```bash
# macOS / Linux
./mvnw test
./mvnw -pl '01-Java核心/04-示例代码' test
./mvnw -pl '02-集合框架/04-示例代码' test
./mvnw -pl '03-并发编程/04-示例代码' test
```

如果首次在 macOS / Linux 使用时 Git 没有保留脚本可执行位，先执行 `chmod +x mvnw`。

模块 README 提供各自的示例索引；完整类和测试不嵌入正文。

## Java 版本策略

- 经典面试主线默认 `JDK 8+`。
- `Java 17+`、`Java 21+`、`Java 25+`、`Java 26+` 的语法和 API 会显式标记。
- 示例代码默认 Java 21；Java 25/26 的语法、API 或预览能力属于版本专题，不纳入默认构建。
- 版本专题必须说明所需 JDK、是否为预览能力，以及能否通过默认 `mvnw test` 验证。
- 规范保证、OpenJDK 实现和版本相关观察要分开表述。

## 维护入口

- [仓库架构说明](项目文档/仓库架构说明.md)：目录、分层、P0/P1/P2、One Source of Truth 和新增内容规则。
- [Benchmark 研究报告](调研/Java技术复习仓库Benchmark.md)：公开项目比较和结构重构依据。
- [笔记编写规范](笔记编写规范.md)：结论优先、追问链、图示、版本和示例规范。
- [贡献指南](CONTRIBUTING.md)：本地验证、内容边界和 Pull Request 检查清单。
- [历史审计快照](项目文档/审计/2026-09-09-仓库重构审计.md)：记录上一轮仓库接管与重构基线，不作为动态状态页。

## 许可证

本仓库采用 [MIT License](LICENSE)。仓库作者创作的笔记、示例代码和图示按 MIT License 发布；外部链接指向的资料及另有说明的第三方内容仍受其各自许可证约束。
