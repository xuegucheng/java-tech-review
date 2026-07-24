# Java 平台与程序运行基础

## 1.1 本章定位

本章用于建立 Java 程序从源码到运行的整体认知，回答以下问题：

- JDK、JRE、JVM 分别是什么？
- Java 为什么可以跨平台？
- Java 是编译型语言还是解释型语言？
- .java 文件如何变成可运行程序？
- JVM、字节码、JIT 分别承担什么职责？
- Java 版本与 JDK 发行版是什么关系？
本章只讲全局运行链路，不深入 JVM 内部实现。

## 1.2 Java 语言与 Java 平台

**1.2.1 Java 不只是一门语言**

Java 通常包含三个层面的含义：

```text
Java
├── Java 语言
│ └── 语法、类型系统、面向对象、异常、泛型等
│
├── Java 平台
│ └── JVM + 标准类库 + 工具链
│
└── Java 生态
└── Spring、Maven、Gradle、中间件及各种框架
```

需要明确：

- Java 语言规定代码怎么写。
- JVM 规定字节码怎么运行。
- Java 标准类库提供常用 API。
- JDK 提供开发、编译、运行、诊断工具。

**1.2.2 Java 的主要特征**

面向对象
静态类型、强类型
自动内存管理
跨平台
支持多线程
编译与运行时优化结合
完整的标准类库和生态体系

## 1.3 JDK、JRE、JVM

**1.3.1 JVM**

JVM，Java Virtual Machine，Java 虚拟机。

职责：

- 加载和执行字节码
- 管理运行时内存
- 管理对象和垃圾回收
- 提供线程运行环境
- 将字节码解释或编译为机器指令
- 屏蔽操作系统和硬件差异
- 核心理解：
JVM 是一套规范，也可以指这套规范的具体实现。
例如 HotSpot 是 JVM 的一种实现。

**1.3.2 JRE**

JRE，Java Runtime Environment，Java 运行环境。
传统理解：

```text
JRE
├── JVM
└── Java 标准类库及运行所需组件
```

JRE 用于运行 Java 程序，但通常不包含完整的开发工具，例如 javac 。
需要补充版本变化：
传统 Java 8 时代经常单独安装 JRE。现代 JDK 通常不再强调独立 JRE 产品，可以使用完整 JDK，或通过 jlink 构建裁剪后的运行时镜像。

**1.3.3 JDK**

JDK，Java Development Kit，Java 开发工具包。

```text
JDK
├── Java 运行环境
│ ├── JVM
│ └── 标准类库
│
└── 开发与诊断工具
├── javac
├── java
├── javap
├── javadoc
├── jar
├── jdb
├── jstack
├── jmap
└── jstat
```

核心关系可以概括为：

```
JDK ⊃ JRE ⊃ JVM
```

但要注明：
这是便于理解的传统包含关系，不应机械理解为现代 JDK 安装目录中一定存在一个独立的 jre 子目录。

**1.3.4 三者对比**

| 名称 | 定位 | 主要内容 | 主要使用者 |
|---|---|---|---|
| JVM | 字节码执行引擎 | 类加载、执行、内存管理、GC | Java 程序 |

| JRE | 运行环境 | JVM | + | 运行类库 | 只运行程序的环境 |
|---|---|---|---|---|---|
| JDK | 开发工具包 | 运行环境 | + | 编译、调试、诊断工具 | 开发人员 |

## 1.4 Java 程序从源码到运行

**1.4.1 整体流程**

```bash
Main.java
↓ javac 编译
Main.class
↓ JVM 启动
类加载
↓
字节码验证、准备、解析、初始化
↓
解释器执行字节码
↓
热点代码被 JIT 编译
↓
本地机器指令
↓
CPU 执行
```

**1.4.2 编译阶段**

```bash
javac Main.java
```

javac 的主要工作：

- 词法和语法检查
- 类型检查
- 泛型检查和类型擦除
- 部分语法糖转换
- 生成 .class 字节码文件
- 这里需要强调：
javac 生成的不是某个 CPU 平台的机器码，而是 JVM 能识别的字节码。

**1.4.3 运行阶段**

```
java Main
```

java 命令主要负责：

- 启动 JVM
- 设置类路径
- 加载指定主类
- 寻找 main 方法
- 执行程序
- 标准入口：

```java
public static void main(String[] args) {
}
```

可以解释各部分：

- public ：JVM 可以访问
- static ：无需创建对象即可调用
- void ：不向 JVM 返回结果
- String[] args ：接收命令行参数
无需讨论“main 能不能重载”等低价值问题，最多放入易错题。

## 1.5 字节码是什么

**1.5.1 字节码的定位**

字节码是：

- Java 源代码编译后的中间表示
- JVM 指令集
- 平台无关
- 比源代码更接近底层执行模型
- 但不是物理 CPU 直接执行的机器码

```
Java 源代码
↓
JVM 字节码
↓
x86 / ARM 等平台机器码
```

**1.5.2 .class 文件不等于 Java 专属源码产物**

JVM 运行的是合法字节码，不关心最初一定来自 Java 源代码。

其他 JVM 语言也可以生成字节码，例如：

- Kotlin
- Scala
- Groovy
- Clojure
- 由此可以得出：
Java 跨平台的直接基础不是 Java 源代码，而是平台无关的 JVM 字节码。

**1.5.3 javap 查看字节码**

基础示例：

```bash
javac Main.java
javap -c Main
```

建议在本章只展示一个简单实验：

```java
public class Main {
public static int add(int a, int b) {
return a + b;
}
}
```

对应字节码可以简单识别：

```
iload_0
iload_1
iadd
ireturn
```

目的不是学习全部字节码指令，而是证明：
Java 源代码经过编译后，变成了 JVM 指令。

## 1.6 Java 为什么能够跨平台

**1.6.1 核心链路**

```bash
同一份 Java 源代码
↓ javac
同一套字节码
↓
Windows JVM → Windows 机器指令
Linux JVM → Linux 机器指令
macOS JVM → macOS 机器指令
```

核心表达：
Java 编译器将源码编译为平台无关的字节码，不同平台提供各自的 JVM 实现，JVM 再将字节码转换为对应平台的机器指令，因此实现“一次编译，到处运行”。

**1.6.2 跨平台的是字节码，不是 JVM**

这是本节最重要的易错点：

- .class 字节码通常是平台无关的。
- JVM 本身是平台相关的。
- Windows、Linux、macOS 需要不同的 JVM 实现。
- JNI、本地库、文件路径、编码、操作系统命令等可能破坏业务程序的跨平台性。
更准确的说法：
Java 提供的是较强的平台无关能力，不代表所有 Java 程序天然完全跨平台。

**1.6.3 “一次编写，到处运行”的边界**

下列代码可能存在平台差异：

- 使用本地动态库
- 使用 JNI
- 调用系统命令
- 写死 Windows 或 Linux 路径
- 依赖默认字符编码
- 依赖操作系统文件权限
- 依赖特定 CPU 架构能力

## 1.7 Java 是编译型还是解释型语言

**1.7.1 不能简单二选一**

Java 同时包含：

- 编译阶段
- 解释执行
- JIT 即时编译
- 因此更准确地说：
Java 是先编译为字节码，再由 JVM 通过解释执行和即时编译相结合的方式运行。

**1.7.2 解释器**

解释器逐条读取字节码并执行。

优点：

- 启动后可以立即运行
- 不需要先把所有代码编译为机器码
- 缺点：
- 重复执行同一段代码时，解释成本较高

**1.7.3 JIT 即时编译**

JIT，Just-In-Time Compiler。

作用：

- JVM 在运行过程中识别热点代码
- 将热点字节码编译成本地机器码
- 后续直接执行机器码
- 进行方法内联、逃逸分析等优化
- 本章只说明 JIT 的角色，不展开：
- C1、C2 编译器
- 分层编译
- OSR
- 逃逸分析细节
- 反优化
这些放在 JVM 模块。

**1.7.4 AOT 简要补充**

可以用一小段说明：
除了解释器和 JIT，Java 生态也存在提前编译 AOT，将程序提前编译为本地可执行文件，例如 Native Image。但 AOT 的启动速度、内存占用、动态能力和运行时峰值
性能具有不同取舍。

## 1.8 JVM 规范与 JVM 实现

**1.8.1 JVM 是规范**

JVM 规范定义：

- .class 文件格式
- 字节码指令
- 运行时数据区域的逻辑模型
- 类加载与初始化规则
- 异常处理规则
- 方法调用语义

**1.8.2 JVM 实现**

常见实现可以简单列出：

- HotSpot
- OpenJ9
- GraalVM 相关运行时

**1.8.3 Java 语言规范与 JVM 规范**

需要区分：

- 规范 主要定义
- Java Language Specification Java 源码语法和语言语义
- Java Virtual Machine Specification class 文件、字节码和 JVM 执行语义
- 例如：
方法重载规则属于 Java 语言规范。
字节码指令格式属于 JVM 规范。
int 溢出语义属于 Java 语言规范。
类文件常量池结构属于 JVM 规范。

## 1.9 Java 版本、JDK 版本与发行版

**1.9.1 Java SE 版本**

例如：

- Java 8
- Java 11
- Java 17
- Java 21
- 在日常表达中，“Java 17”和“JDK 17”常被混用，但概念上：
Java SE 17 表示平台规范版本。
JDK 17 表示对应版本的开发工具包实现。

**1.9.2 LTS 与非 LTS**

只需要建立概念：

- LTS：长期支持版本
- 非 LTS：生命周期较短，但可能包含新特性
- 企业项目通常优先选择成熟的 LTS 版本

**1.9.3 OpenJDK 与 Oracle JDK**

核心结论：

- OpenJDK 是 Java SE 的开源参考实现和主要开发基础。
- Oracle JDK 基于 OpenJDK 构建。
- 还存在其他厂商发行版。
- Java 版本和 JDK 发行厂商是两个维度。

```
Java 版本：8 / 11 / 17 / 21
+
JDK 发行版：Oracle JDK / Temurin / Corretto / Zulu 等
```

## 1.10 classpath、包与主类启动

**1.10.1 包名**

```
package com.example.demo;
```

作用：

- 组织类
- 防止类名冲突
- 配合访问权限
- 对应类的完全限定名
- 例如：

```
com.example.demo.Main
```

**1.10.2 classpath**

classpath 是 JVM 和编译器寻找类的位置集合。
例如：

```bash
java -cp target/classes com.example.demo.Main
```

需要讲清：

- java Main 中的 Main 是类名，不是文件名。
- 带包名的类必须使用完全限定名启动。
- JVM 根据 classpath 和类名寻找 .class 。
- classpath 不等于操作系统的 PATH 。

**1.10.3 PATH 与 classpath 区别**

配置 用途

| PATH | 操作系统寻找 | java | 、 | javac | 等可执行程序 |
|---|---|---|---|---|---|
| classpath | JVM | 或编译器寻找 | Java | 类 |  |

## 1.11 本章总结

本章最终要形成下面这条知识链：

```bash
Java 源代码
↓ javac
平台无关字节码
↓ JVM 类加载
解释器执行
↓ 热点识别
JIT 编译成本地机器码
↓
CPU 执行
```

面试口述版：

- Java 程序首先通过 javac 编译成平台无关的 class 字节码，然后由不同操作系统上的 JVM 加载和执行。JVM 初始可以通过解释器执行字节码，对于频繁执行的热点代
- 码，会通过 JIT 编译成本地机器码来提升性能。因此 Java 不是单纯的编译型或解释型语言，而是编译、解释和即时编译相结合。Java 能跨平台，是因为字节码平台无
关，而各个平台都有对应的 JVM 实现。
