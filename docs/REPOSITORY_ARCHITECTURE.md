# Repository Architecture

> 本文是后续维护者的架构契约。它解释仓库为什么这样组织，以及新增笔记、图和实验时应把内容放在哪里。

## 1. 产品定位

`java-tech-review` 面向 **3～10 年 Java 后端工程师**，服务跳槽前系统复习、日常回顾、面试突击、源码理解和疑难知识重新建模。

它不是 Java 初学教程、API 大全或百科式目录。每个主题都应尽量形成下面的闭环：

```text
Interview Answer → Mental Model → Deep Dive → Source Path → Runnable Example
```

## 2. 为什么从旧结构重建

旧结构以 `1-Java核心基础/1.0-Java语言基础` 和 `1.1-集合框架` 为主，19 篇语言长文和 8 篇集合长文连续编号。它保留了大量高质量技术内容，但没有把“快速恢复记忆”和“完整理解”分开；集合后续章节也没有同步进入根 README。

本次选择 `REBUILD_STRUCTURE`，重建的是信息架构，不是知识资产：旧主题文章整体迁移到对应模块的 `deep-dive/`，新的 `interview/` 只提供面试主线和导航。旧文件名改为语义化英文 slug，便于长期扩展和跨平台链接；文章标题、正文和 Git 重命名历史保留。

## 3. 目录分工

```text
README.md
├── 01-java-core/
│   ├── README.md
│   ├── interview/       # 5～15 分钟复习，按 P0/P1 进入
│   └── deep-dive/       # 原理长文、规范边界、源码阅读清单
├── 02-collections/
│   ├── README.md
│   ├── interview/
│   └── deep-dive/
├── diagrams/
│   ├── README.md
│   ├── java/
│   ├── collections/
│   └── legacy/          # 已有位图，仅作资产保留，不作为新入口
├── examples/
│   ├── README.md
│   ├── pom.xml
│   └── src/main/java + src/test/java
├── docs/REPOSITORY_ARCHITECTURE.md
├── research/repository-benchmark.md
└── NOTE_STYLE_GUIDE.md
```

采用“技术域内部按层、示例与图示共享”的混合结构，而不是全局 `interview-review/` 和 `deep-dive/` 两个大目录。这样既能保持 Java、Collections、未来 Concurrency/JVM/Spring/Database 的边界，又能让一个模块内的复习顺序清晰；共享 examples 和 diagrams 则避免同一个实验被复制到多个领域。

## 4. 内容层级

### Interview Review

目标是 5～15 分钟恢复一个明确主题的面试记忆。文件应优先放：

- 一句话面试结论；
- 30 秒回答或 mental model；
- 3～8 个因果式追问；
- 高频工程坑；
- 指向唯一 Deep Dive、图和实验的链接。

它不是完整教材，也不是“面试题 1～40”的平铺题库。

### Deep Dive

只有值得理解实现、规范边界、性能/并发取舍或源码路径的主题进入这里。长文可以保留，但一个文件要围绕一个可命名的认知主题；不能把 language、DI、ORM、JVM 和业务架构为了篇幅塞到一起。

Deep Dive 允许保留完整推理、关键源码片段、图和工程讨论，但源码只追关键路径，不复制整个 JDK 文件。

### Examples

完整的 `main` 类、多个协作类、可重复测试和验证输出进入 `examples/`。正文只保留解释机制所需的短片段。每个示例必须有测试，且从笔记链接回来；示例失败时不应被文章描述成已验证结论。

### Diagrams

- Mermaid：流程、决策、生命周期、状态机和简单时序，靠近解释文字。
- SVG：桶、链表、对象布局、引用、指针和需要精确空间关系的结构图。
- 位图：只保留已有且不可替代的资产，新结构图默认创建 SVG。

图的职责是建立 mental model，不能把正文逐字重复一遍。

## 5. P0 / P1 / P2

- **P0**：面试高频、核心机制、能够区分工程判断的主题。当前包括参数传递、对象模型与多态、equals/hashCode、集合契约、HashMap、Hash 集合和 LinkedHashMap/LRU。
- **P1**：常见且工程重要，或是理解 P0 的必要前置。当前包括运行链路、类型转换、方法重载、封装、继承/接口/组合、ArrayList、LinkedList 和泛型。
- **P2**：低频边缘 API、完整语法表或版本补充。内容可以准确保留，但不应遮住 P0/P1。

优先级是导航权重，不是正确性评级。一个 P2 Deep Dive 仍然可以有很高的技术价值，只是默认不出现在 1 天突击路线。

## 6. Markdown 笔记模板

下面是推荐骨架，不是每篇文章的强制标题清单。知识点不同就裁剪，不要为了统一而制造空章节。

```markdown
# Topic

> P0/P1/P2 · JDK 8+ / Java 17+ / Java 21+ / Java 25+ · 预计时间

## 面试结论

## 30 秒回答

## 核心结构或 mental model

## 核心流程

## 为什么这样设计

## 面试追问链

### 追问：为什么？

## 工程坑

## Deep Dive / 关键源码

## Runnable Example

## 一句话复盘
```

“面试追问链”必须表达因果顺序，例如 HashMap 的 `是什么 → 如何定位桶 → 冲突 → 树化 → resize → equals/hashCode → 并发边界`，不能只是罗列问题。

## 7. One Source of Truth

| 概念 | 唯一权威 | 其他模块允许写什么 |
| --- | --- | --- |
| 变量、引用、参数传递 | `01-java-core` | 只引用；集合可用一个短例子说明消费方式 |
| `equals/hashCode` 契约 | `01-java-core/deep-dive/equals-and-hashcode-contract.md` | Collections 只解释 HashMap/HashSet 如何消费 |
| HashMap 的 hash、bucket、树化、resize | `02-collections/deep-dive/hashmap.md` | HashSet/LinkedHashMap 只写复用关系和增量结构 |
| 集合接口、视图、fail-fast | `02-collections/deep-dive/collection-contracts.md` | 实现专题只引用契约，不重写全表 |
| `wait/notify`、中断、LockSupport、条件队列 | 未来 `concurrency` | Object 主题只说明历史归属和调用边界 |
| 泛型语言规则 | `01-java-core` 的类型专题 | Collections 只说明 `extends/super` 在 API 中的用法 |

当一个概念出现第二次时，先问它是在定义契约，还是在说明另一个组件如何消费契约；如果是后者，使用链接而不是复制完整解释。

## 8. Java 版本策略

- 默认面试主线是 `JDK 8+`，不把现代语法强行写进经典答案。
- `Java 17+`、`Java 21+`、`Java 25+` 必须在标题、段落或代码块前显式标记。
- 当前 examples 默认 Java 21；如果未来必须验证 Java 25，新增独立 profile/module，不破坏默认 `mvn -f examples/pom.xml test`。
- 实现细节必须绑定 JDK 版本；规范保证、OpenJDK 实现和某个版本的观察结果分开写。

## 9. 模块 README 契约

每个主要模块 README 至少回答：

1. 模块解决什么问题；
2. 1 天突击从哪里开始；
3. P0/P1/P2 如何分层；
4. 推荐阅读顺序；
5. Deep Dive 在哪里；
6. 图示和 Runnable Example 在哪里；
7. 本模块不负责哪些概念，以及权威链接在哪里。

根 README 只负责跨模块地图、时间路线和入口，不复制每篇文章的正文。

## 10. 新增内容的检查清单

- 能否给文件命名为一个明确的认知主题？
- 它属于哪个技术域和优先级？
- 是否应该先写 Interview Review，再链接 Deep Dive？
- 是否已经有同概念的 authoritative source？
- 完整代码是否应移入 examples，并配测试？
- 图是在解释流程还是空间结构，应该用 Mermaid 还是 SVG？
- 所有内部链接、图片、标题锚点和代码围栏是否可验证？
- 是否把未来计划误写成已完成？
