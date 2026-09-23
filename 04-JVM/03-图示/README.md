# JVM 图示

JVM 流程图使用 Mermaid 并放在对应正文附近；需要表达空间关系和边界的结构图使用自包含 SVG。

## 已创建

- [JVM 执行模型](../02-深度解析/JVM执行模型与运行时数据区.md#先看程序怎样进入-jvm)：展示 .java → .class → JVM 启动、运行时数据区与执行引擎的关系。
- [Runtime Data Areas 与 HotSpot/Native 边界](JVM/JVM运行时数据区.svg) → [对应 Deep Dive](../02-深度解析/JVM执行模型与运行时数据区.md)。
- [Class Loading Lifecycle](../02-深度解析/类文件与类加载生命周期.md)：展示 Loading、Verification、Preparation、Resolution 与 Initialization 的规范边界。
- [Class File → Runtime Type](../02-深度解析/类文件与类加载生命周期.md)：区分 Class File、VM 运行时类型、`Class<?>` 镜像和数组类创建路径。

图只画抽象层级和概念关系，不把区域位置画成物理地址，也不把 Metaspace、DirectByteBuffer 内容并入 JVMS 区域清单。
