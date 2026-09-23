# 04-JVM

> 状态：架构规划已建立，正文与图示按阶段建设。本轮不把规划条目伪装成已完成资产。

## 模块定位

面向有 Java 后端经验的读者，帮助中高级面试复习，并把 JVM Mental Model 用于线上问题定位。每个核心主题最终回答：面试怎么说、机制怎样工作、故障如何取证。

本模块不做 JVM 百科、参数清单、规范翻译或 HotSpot C++ 逐行注释。按“先模型、再机制、后诊断与实现细节”组织；规范保证、HotSpot 实现和特定 JDK 行为必须明确标记。

## Knowledge Map 与推荐顺序

主线按概念依赖组织，而非按名词罗列：

1. **执行模型与运行时数据区**：认识字节码执行、线程私有/共享区域及其生命周期，并分清 JVMS 抽象和 HotSpot 内存实现。
2. **类文件与类加载**：理解类从二进制表示到可执行状态的加载、连接、初始化，再看 ClassLoader 委派策略和类身份。
3. **栈帧与对象创建**：理解方法调用、局部变量和操作数栈，再追踪对象分配；将规范层面的对象创建与 HotSpot 布局、TLAB 分开。
4. **可达性与回收**：从 GC Roots 和引用链开始，学习引用类型、回收算法及收集器选择，随后深入 G1 周期。
5. **生产诊断**：先获取并关联线程、GC、堆和进程证据，再按 CPU/线程或 GC/内存故障分流，最后形成带版本、配置和证据的结论。
6. **运行时优化与现代工具**：在前述模型上学习 JIT、内存配置、GC 日志、JFR 和现代低延迟收集器。

诊断复用前面的执行、对象、回收模型，不孤立成“调优参数”章节。面试突击可以跳过 P1/P2，沿 P0 入口复习。

### P0：先掌握的主线

| Deep Dive Owner（规划） | 核心问题 |
| --- | --- |
| JVM执行模型与运行时数据区.md | JVM 抽象执行模型有哪些运行时区域，各自何时创建、由谁共享或释放？ |
| Java虚拟机栈与栈帧.md | 方法调用如何形成栈帧；局部变量、操作数栈、动态链接和返回如何协作？ |
| Java对象创建与分配路径.md | 从创建请求到初始化对象，分配与构造分别发生什么？ |
| 类文件与类加载生命周期.md | 类如何经历加载、验证、准备、解析和初始化？ |
| ClassLoader双亲委派与类身份.md | 委派如何工作，为什么同名类仍可能类型不兼容？ |
| GCRoots与对象可达性.md | GC Roots 与引用链如何决定对象是否可达？ |
| 垃圾收集算法与收集器选择.md | 标记、复制、整理和分代假设解决什么取舍；如何理解常见收集器？ |
| G1垃圾收集器与回收周期.md | Region、暂停目标、并发标记、Mixed 回收之间如何关联？ |
| JVM诊断工具与证据链.md | 何时选 jcmd、线程转储、堆信息、GC 日志或 JFR；各自能证明什么？ |
| CPU与线程故障排查.md | CPU 飙高、Load 高、线程阻塞或死锁如何从进程证据缩小到线程与代码？ |
| GC与内存故障排查.md | Young/Full GC、内存上涨、各类 OOM、GC Pause 和响应时间抖动如何分流？ |

P0 共 11 个 Deep Dive Owner。它同时是面试主干和诊断前置；不意味着所有 HotSpot 细节都必须 P0。

### P1：补齐实现与分析能力

| Deep Dive Owner（规划） | 范围与边界 |
| --- | --- |
| HotSpot对象布局与TLAB.md | 对象头、对齐、Compressed Oops 和 TLAB；实现与观测结果要注明 Java 版本、架构、参数。 |
| 引用类型与可达性处理.md | Strong/Soft/Weak/Phantom 与 Reference/ReferenceQueue 的用途和处理，不承诺某次 GC 的时机。 |
| Metaspace与类卸载.md | HotSpot 元数据存储、类加载器生命周期和类卸载线索；不把 Metaspace 等同于 JVMS Method Area。 |
| DirectMemory与NativeMemoryTracking.md | 堆外直接内存、分配来源与 NMT 证据；Direct Memory 不是 JVMS 列出的运行时数据区。 |
| JIT编译与运行时优化.md | 解释执行、分层编译、C1/C2、内联、逃逸分析、标量替换和锁消除的适用条件。 |
| JVM内存配置与GC日志.md | 堆 sizing、常用配置和统一日志读取；只覆盖有诊断上下文的参数，不做全量旗标目录。 |
| ZGC与现代低延迟收集器.md | 以 Java 21 和明确版本变化讲 ZGC；Shenandoah 仅做受发行版支持情况约束的对比入口。 |
| JFR与持续性能分析.md | 记录配置、事件和时间线如何支持分析；JMC 是分析工具，不能替代证据解释。 |

P1 共 8 个 Deep Dive Owner。对象布局和 TLAB 虽常被问到，但布局细节依赖 HotSpot、架构和版本，故作为 P1；P0 的对象创建文章只说明必要分配路径并链接此 Owner。

### P2：有边界的扩展

以下四个主题组不建立独立主线文章，必要时只在相邻 Owner 加简短说明或权威外链：

1. **Shenandoah 深入实现**：发行版与构建支持不同；不作为 Java 21 通用主线收集器。
2. **CMS 历史细节**：在收集器选择中说明其解决的问题和淘汰背景，不展开完整参数史。
3. **全部字节码指令与 Class File 属性**：类加载 Owner 只讲理解生命周期所需内容，不制作指令百科。
4. **穷举 JVM 参数与 HotSpot C++ 内部结构**：围绕具体机制和诊断只追关键路径，不维护无上下文的参数或源码清单。

P2_COUNT 指这 4 个受限扩展主题组，不计入 19 个计划 Deep Dive Owner。

## Ownership Matrix

下表规定规划中的唯一主要 Owner。标为规划的文章尚未创建；在建设之前，本 README 是该知识域的 Owner 地图。标题与路径落地时应保持一对一，不因面试入口再复制完整机制解释。

| 候选知识 | 唯一 Owner / 处理方式 | 合并、拆分及边界决定 |
| --- | --- | --- |
| JVM 执行模型、Runtime Data Areas、Heap | P0：JVM执行模型与运行时数据区.md | Heap 作为抽象运行时区域在此定义；对象分配由对象创建 Owner 解释，回收与生产症状分别由 GC、诊断 Owner 解释。 |
| Java Stack Frame | P0：Java虚拟机栈与栈帧.md | 与总览分开，避免总览过载；不为局部变量表、操作数栈等各建碎片文章。 |
| 对象创建过程 | P0：Java对象创建与分配路径.md | 讲创建/初始化和规范可保证的行为；不把具体对象头布局说成 JVMS 要求。 |
| 对象布局、对象头、Compressed Oops、TLAB | P1：HotSpot对象布局与TLAB.md | 合并为一个 HotSpot 分配与布局模型，标明版本、架构和观测限制。 |
| Class File 与加载/连接/初始化生命周期 | P0：类文件与类加载生命周期.md | 生命周期合并为完整模型；Java 语言层面的主动使用和初始化语义链接 Java Core Owner。 |
| ClassLoader、双亲委派、自定义加载器、类身份 | P0：ClassLoader双亲委派与类身份.md | 合并为加载器可见性与类型身份模型；双亲委派是常见默认策略，不是 JVMS 对所有加载器的强制规则。 |
| GC Roots、Reachability Analysis | P0：GCRoots与对象可达性.md | 合并为可达性 Mental Model；堆对象布局不在此重复定义。 |
| Reference Types | P1：引用类型与可达性处理.md | 从 Roots/可达性机制中拆出独立增量；清理、入队与回收时机不作确定性承诺。 |
| Mark-Sweep、Copying、Mark-Compact、Generational Collection | P0：垃圾收集算法与收集器选择.md | 算法和分代假设合讲，不按 Eden、Survivor、S0/S1 拆碎。 |
| Serial、Parallel、CMS、G1、ZGC、Shenandoah | P0 收集器选择 Owner；G1、ZGC 各有深入 Owner | Serial/Parallel 做选择地图；CMS 只讲历史；G1/ZGC 深入各自 Owner；Shenandoah 限 P2 对比。 |
| Minor/Young/Mixed/Full GC | P0：G1垃圾收集器与回收周期.md、GC与内存故障排查.md | 按事件语境解释，不假设所有术语跨收集器含义完全相同，不为事件标签单独建文。 |
| C1/C2、Tiered Compilation、Inlining、Escape Analysis、Scalar Replacement、Lock Elimination | P1：JIT编译与运行时优化.md | 按运行时优化因果链合并，不承诺某段代码必定触发某项优化。 |
| Heap/Stack/Metaspace/Direct Memory OOM、StackOverflowError、Memory Leak | P0：CPU/线程与 GC/内存排障 Owner；机制链接对应 P0/P1 Owner | 按证据与症状分流；危险复现只进入受限子进程手工实验规划。 |
| JVM Options、Heap Sizing、GC Logging | P1：JVM内存配置与GC日志.md | 和诊断上下文绑定；不扩展成全量 JVM Flag 手册。 |
| jps、jstack、jmap、jstat、jcmd、JFR、JMC、Arthas、async-profiler | P0：JVM诊断工具与证据链.md；JFR 深入由 P1：JFR与持续性能分析.md 负责 | 按问题、证据和能力限制组织；优先解释 JDK 自带工具，外部工具单列适用前提。 |
| CPU 100%、Load 高、BLOCKED、死锁 | P0：CPU与线程故障排查.md | 从 OS/进程证据追到线程状态、线程栈和代码热点。 |
| 频繁 GC、内存持续上涨、Direct Memory、OOM、Pause 与延迟抖动 | P0：GC与内存故障排查.md | 以堆、Metaspace、Direct/Native 和 GC 证据分类，避免把所有内存归为 Heap。 |

### 跨模块 Owner 边界

- **Runtime Data Areas 不等于 JMM。** JVMS 运行时数据区描述抽象运行时结构及其生命周期；JMM 规定并发读写的可见性、顺序和 happens-before 关系。
- JMM 的唯一 Owner 是 [03-并发编程的 Java Memory Model 文章](../03-并发编程/02-深度解析/Java内存模型与happens-before.md)。本模块只链接 JMM 来解释并发语义，不复制 happens-before、安全发布或 volatile 规则。
- 本模块 Runtime Data Areas 的唯一规划 Owner 是 JVM执行模型与运行时数据区.md。JVMS Method Area 是规范抽象；Metaspace 是 HotSpot 实现选择之一，两者不能互换。Direct Memory 不属于 JVMS 规定的运行时数据区清单。
- Java 语言级对象不变量、不可变设计由 [Java Core 对象设计 Owner](../01-Java核心/02-深度解析/对象创建与不可变设计.md) 负责。static、主动使用与 Java 类初始化语义由 [static 与类初始化 Owner](../01-Java核心/02-深度解析/static与类初始化.md) 负责；JVM 类加载 Owner 解释与之衔接的生命周期和实现边界。
- HashMap、集合契约、线程池、ThreadLocal 和并发容器仍由现有模块负责；JVM 文中只保留必要场景说明和回链。

## 面试速记入口规划

只规划 6 个综合入口，不生成碎片题库。每篇最终应有结论、30 秒回答、2 分钟展开、流程/原因、高频追问、易混边界，并链接到唯一 Deep Dive、图或实验。

| 入口（规划） | 优先级 | 聚合范围 |
| --- | --- | --- |
| JVM核心模型与运行时数据区.md | P0 | 执行、运行时区域、JVMS 与 HotSpot 区分、JMM 边界 |
| 对象创建与对象内存.md | P0 | 创建主线；对象布局/TLAB 作为 P1 追问 |
| 类加载与双亲委派.md | P0 | 生命周期、委派、自定义加载器和类身份 |
| GC可达性、算法与G1.md | P0 | Roots、可达性、算法、收集器地图、G1 |
| JVM故障排查与诊断工具.md | P0 | 证据链、工具选择、CPU/线程、GC/内存症状 |
| JIT、现代GC与性能分析.md | P1 | JIT、版本化的 ZGC、JFR/JMC 和 P2 扩展边界 |

## 图示规划

共规划 13 张图，图应解释真实难点；未创建之前不把它们列成已有资源。

| 图（规划名称） | 形式 | 解答的理解难点 |
| --- | --- | --- |
| JVM 整体执行模型 | Mermaid | 类文件、加载、执行、运行时区域之间的关系。 |
| Runtime Data Areas | SVG | 线程私有与共享区域、抽象生命周期及与 JMM 的边界。 |
| Java Stack Frame | SVG | 局部变量、操作数栈、动态链接和返回信息的空间关系。 |
| 对象创建流程 | Mermaid | 分配、默认状态、初始化与构造调用的先后关系。 |
| HotSpot 对象布局 | SVG | 对象头、实例数据、填充；标记压缩指针和版本差异。 |
| Class Loading Lifecycle | Mermaid | 加载、验证、准备、解析、初始化的状态/流程关系。 |
| Parent Delegation | Mermaid | 委派调用顺序、父加载器返回和本地查找分支。 |
| GC Roots Reachability | SVG | Roots 到对象的引用图及不可达对象判定。 |
| G1 Region 结构 | SVG | Region、Humongous 区域和逻辑代际视图；不画成连续物理分代。 |
| G1 回收周期 | Mermaid | Young、并发标记、Mixed 的关系和路径分支。 |
| JIT 编译路径 | Mermaid | 解释执行、计数/编译、代码缓存与优化入口。 |
| OOM / Full GC 诊断决策树 | Mermaid | 先识别内存类型与 GC 证据，再分配对象、类或堆外方向。 |
| CPU / 线程诊断流程 | Mermaid | 从主机负载和进程定位到线程状态、栈与热点。 |

算法比较更适合表格，不另画重复图。对象头细节放在对象布局 SVG 中，不单独再画一张对象头图。类加载器层级在委派流程中表达。

## Runnable Example 规划

规划 8 组真实实验，不为凑目录创建 Hello World。前两组适合自动测试；其余明确限制为受控手工实验，不放入默认 Maven 测试。

| 示例主题（规划） | 安全级别 | 设计约束 |
| --- | --- | --- |
| 自定义 ClassLoader 与同名类身份 | 可自动测试 | 稳定夹具验证不同定义加载器产生不同运行时类型。 |
| Reference 与 ReferenceQueue | 可自动测试 | 显式 clear/enqueue 验证 API 协作；不等待或断言 GC 时机。 |
| StackOverflowError | 手工隔离 | 子 JVM、有限栈和超时；绝不在测试进程无限递归。 |
| Heap/Metaspace/Direct Memory OOM | 手工隔离 | 每类单独子 JVM，硬性最大内存、超时和可中断退出；不在 Maven 自动测试里耗尽资源。 |
| G1 与 GC 日志观察 | 手工隔离 | 有界、可重复分配负载；保存 JDK、参数和日志，不将机器阈值当断言。 |
| TLAB/对象布局观测 | 手工隔离 | 标记 JDK、CPU 架构、压缩指针和诊断选项；工具输出不等于规范保证。 |
| JIT Warmup、逃逸分析与 JFR | 手工隔离 | 预热与测量分开，用 JFR/编译日志作证据；不以单次耗时断言优化发生。 |
| Thread Dump / CPU 与有界内存泄漏场景 | 手工隔离 | 可控线程数、明确停止条件、超时；不运行无限线程或无限增长。 |

以后有实际源代码和测试后再创建 04-示例代码/ 并评估是否加入根 Maven reactor；当前不建空 POM、不建空目录，也不把危险实验混进默认构建。

## 生产诊断路线

所有排障按同一条证据链写作：

**现象 → 获取基线和时间窗口 → 收集线程/GC/堆/进程证据 → 缩小到线程、对象、类元数据或堆外内存 → 验证假设 → 结论与限制**

- **CPU 100% / Load 高**：先区分主机负载与目标进程 CPU，再定位高 CPU 线程、线程栈和热点；线程 ID 转换、采样间隔和容器 CPU 限额要写清。
- **BLOCKED / 死锁**：线程转储用于识别状态、锁持有者和等待链；一次快照不能证明长期趋势或业务因果。
- **Young GC / Full GC / Pause / 延迟抖动**：从 GC 日志时间窗口、频率、停顿和回收前后占用变化入手；事件名称不能跨收集器机械类比。
- **Heap 持续上涨 / Heap OOM / Leak**：先看堆占用与对象保留证据，再用堆转储/直方图等定位保留关系；分配热点不等同于泄漏根因。
- **Metaspace**：关联类加载器、类卸载与元数据趋势；不要将其误判为 Java Heap。
- **Direct / Native Memory**：结合 NMT 是否已启用、进程内存和分配路径；NMT 能力有范围，不覆盖所有本地分配。
- **StackOverflowError**：识别递归/栈深度与线程栈配置；不要用无限递归作在线诊断。
- **工具选择**：JDK 自带工具按问题选用并记录目标进程、JDK 和采集方式；JFR/JMC、Arthas、async-profiler 是补充视角，结论要注明采样、授权、部署和版本限制。

工具文章以“能回答什么、不能回答什么、下一步如何交叉验证”为主，命令示例随 JDK 版本校验，不维护孤立 API 目录。

## 学习路线

### 完整路线

按 Knowledge Map 六步学习：执行模型与数据区 → 类文件/加载/委派 → 栈帧与对象创建 → 可达性/算法/G1 → 故障诊断 → JIT/配置/JFR/ZGC。

### 面试前快速复习

1. JVM 执行模型、Runtime Data Areas 与 JMM 边界。
2. 对象创建、栈帧、类加载和双亲委派。
3. GC Roots、算法和 G1 回收周期。
4. CPU/线程、Full GC/OOM 诊断入口和工具证据链。
5. 有余量再复习对象布局、JIT 与 Java 21/23/24 的 ZGC 差异。

### 建设阶段

| 阶段 | 范围 | 完成原则 |
| --- | --- | --- |
| Phase 1：Mental Model | 执行模型/数据区、栈帧、对象创建、类文件/加载/委派 | 建立可复用术语、版本标签和跨模块链接。 |
| Phase 2：GC 主线 | Roots、引用、算法与收集器地图、G1 | 算法和收集器有因果关系，G1 图准确表达 Region 与周期。 |
| Phase 3：诊断闭环 | 工具证据链、CPU/线程、GC/内存故障 | 每种症状从证据走到判断，并写清工具盲区。 |
| Phase 4：运行时优化 | 对象布局/TLAB、JIT、配置和日志 | 只写可解释或实测的机制，注明 JDK/架构。 |
| Phase 5：现代 JVM | ZGC、JFR/持续分析、受限扩展 | 将版本差异、发行版可用性与当前 Java 21 基线分开。 |

阶段是后续建设次序，不表示本轮已写正文。

## Java 21 与实现边界

示例及默认说明以仓库 Java 21 为基线；经典概念可引用较早版本，但须显式标注适用范围。

| 层次 | 写作规则 |
| --- | --- |
| Java Language / JLS | 语言行为由 JLS 约束；类初始化触发条件属于语言/平台契约，引用 Java Core 对应 Owner。 |
| JVM Specification / JVMS | 说明抽象机器、Class File 和 Runtime Data Areas 等规范要求；规范不指定所有数据结构的物理布局或 GC 算法。 |
| HotSpot Implementation | G1、对象头、TLAB、Metaspace、Compressed Oops、C1/C2 等作为特定实现讨论，不写成所有 JVM 必须如此。 |
| JDK Version Specific | 收集器默认值、可用选项和行为绑定发行版/JDK 版本，并给出官方来源或可复现命令。 |

需要保持的版本锚点：

- Java 21 HotSpot 文档以 G1 作为常见默认收集器说明；仍按目标运行环境的 ergonomics 和实际启动参数确认。
- Generational ZGC 在 JDK 21 是可选模式；JDK 23 将其设为 ZGC 默认模式；JDK 24 移除了非分代 ZGC。写 ZGC 时必须标明版本语境。
- CMS 已从 JDK 14 移除，仅保留历史定位。
- Shenandoah 是否可用取决于具体 JDK 发行版和构建，不能无条件列为所有 Java 21 HotSpot 的可选项。

规划阶段所依据的规范和官方资料：

- [JVMS 21，Chapter 2：运行时数据区与栈帧](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-2.html)
- [JLS 21，Chapter 12：执行与初始化](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html)；[JLS 21 §17.4：内存模型](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4)
- [Java 21 GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)；[Java 21 Troubleshooting Guide：诊断工具](https://docs.oracle.com/en/java/javase/21/troubleshoot/diagnostic-tools.html)
- [JEP 439：Generational ZGC（JDK 21）](https://openjdk.org/jeps/439)、[JEP 474：Generational ZGC 默认模式（JDK 23）](https://openjdk.org/jeps/474)、[JEP 490：移除非分代 ZGC（JDK 24）](https://openjdk.org/jeps/490)、[JEP 363：移除 CMS（JDK 14）](https://openjdk.org/jeps/363)

这些链接是规划基线。撰写每篇文章时仍需核对对应版本的官方文档和可执行环境，不能只凭本 README 推导实现结论。

## 资产状态与本轮计数

本轮只建立规划 Owner 与地图，没有创建文章、图或示例代码。

| 规划项 | 数量 | 口径 |
| --- | ---: | --- |
| P0_COUNT | 11 | P0 Deep Dive Owner |
| P1_COUNT | 8 | P1 Deep Dive Owner |
| P2_COUNT | 4 | 限定扩展主题组，不生成独立 Owner 文章 |
| INTERVIEW_ENTRY_COUNT | 6 | 规划入口 |
| DEEP_DIVE_PLANNED_COUNT | 19 | P0 + P1 规划 Owner |
| DIAGRAM_PLANNED_COUNT | 13 | 8 Mermaid + 5 SVG |
| EXAMPLE_PLANNED_COUNT | 8 | 2 个可自动测试候选 + 6 组隔离手工实验 |

JVM_OWNER_COUNT 为 19 个计划 Deep Dive Owner；未来扩展 P2 时先更新 Ownership Matrix，避免出现第二个权威来源。

## 下一步建设建议

开始正文时先写 **JVM执行模型与运行时数据区.md**。它定义后续文章共同使用的执行与内存术语，能在第一篇固定 JVMS 抽象区域、Java Stack Frame 的后续展开边界、HotSpot Metaspace/Direct Memory 的实现边界，以及 JMM 的跨模块 Owner；其余类加载、对象、GC 和诊断主题都依赖这套词汇。
