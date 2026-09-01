# 运行链路与版本边界

> **P1 · JDK 8+ 主线 · 5 分钟复习**

## 面试结论

Java 源码通常先由 `javac` 编译为平台无关的 class 字节码，再由目标平台的 JVM 加载、链接、初始化并执行；解释器和 JIT 都属于执行链路。JDK 是开发工具包，JVM 是字节码执行引擎，JRE 是运行环境概念，不能把三者混为一谈。

## 30 秒回答

```text
.java --javac--> .class 字节码 --JVM--> 解释执行 / JIT 本地代码
```

跨平台依靠 class 文件和 JVM 规范，但路径、编码、本地库、文件系统和操作系统能力仍可能破坏应用层跨平台性。版本回答要先说经典主线，再说现代补充。

## 版本标签

| 标签 | 复习含义 |
| --- | --- |
| `JDK 8+` | 经典企业面试主线和长期稳定语言语义 |
| `Java 17+` | `sealed` 等正式现代语言能力 |
| `Java 21+` | Sequenced Collections 等现代 API |
| `Java 25+` | 本仓库标记的最新语言/入口补充，不替代传统工程写法 |

## 追问链

1. Java 是编译型还是解释型？两种说法都不完整；编译为字节码，运行时可解释并对热点代码 JIT。
2. class 文件是机器码吗？不是，它是 JVM 规范定义的中间表示。
3. `PATH`、classpath、module path 有何不同？分别服务工具定位、类路径搜索和模块解析边界。
4. 为什么不能只说“对象在堆、局部变量在栈”？那是常见实现模型，不是所有优化后的物理布局保证。

## Deep Dive

- [运行与工具链](../deep-dive/platform-and-execution.md)
- [static 与类初始化](../deep-dive/static-and-class-initialization.md)
- [接口与版本演进](../deep-dive/interfaces.md)

## 一句话复盘

> 源码被编译成字节码，JVM 负责加载和执行；版本标签必须跟着结论走。
