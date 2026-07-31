# Java 平台与程序运行基础

> 本章建立 Java 程序从源代码、字节码到 JVM 执行的完整认知。重点是平台边界、工具链和运行链路，不深入垃圾收集器、运行时内存区域、类加载器实现及 JIT 编译器内部算法。

## 1.1 本章定位

学完本章，应能够准确回答：

- Java 语言、Java 平台和 Java 生态分别是什么？
- JDK、JRE、JVM 有什么区别？
- `.java` 文件如何变成可以执行的程序？
- `.class` 文件和机器码有什么区别？
- Java 为什么具有跨平台能力？
- Java 是编译型语言还是解释型语言？
- 解释器、JIT 和 AOT 分别承担什么职责？
- 传统 `main`、实例 `main` 和紧凑源文件有什么区别？
- Java SE 版本、JDK 版本和 JDK 发行版是什么关系？
- `PATH`、classpath 和 module path 分别解决什么问题？
- 如何使用 `javac`、`java`、`javap`、`jar` 等基础工具？

本章不深入：

- JVM 运行时数据区与对象内存布局；
- 类加载器与双亲委派；
- 垃圾收集器和 GC 调优；
- C1、C2、分层编译、OSR 和反优化；
- Java 模块系统的完整规则；
- Maven、Gradle 和 Spring Boot 的构建启动机制。

这些内容分别放在 JVM、构建工具和框架模块中展开。

---

## 1.2 Java 语言、Java 平台与 Java 生态

### 1.2.1 Java 不只是一门编程语言

日常所说的“Java”可能指三个不同层面：

```text
Java
├── Java 语言
│   └── 语法、类型系统、面向对象、异常、泛型等
├── Java 平台
│   └── JVM、标准类库、工具链与平台规范
└── Java 生态
    └── Spring、Maven、Gradle、中间件和第三方类库
```

三者的职责不同：

| 层面 | 主要职责 | 典型内容 |
|---|---|---|
| Java 语言 | 规定源代码如何书写以及具有什么语义 | 类型、变量、方法、类、异常、泛型 |
| Java 平台 | 提供标准运行模型和基础 API | JVM、Java SE API、JDK 工具 |
| Java 生态 | 在平台之上解决工程问题 | Spring、Netty、MyBatis、Maven |

因此：

- Java 语言规范不等于 JVM 规范；
- JVM 不等于完整的 Java 平台；
- Spring 等框架也不是 Java 语言本身的一部分。

### 1.2.2 Java 平台的主要能力

Java 平台通常具有以下特征：

- 静态类型与强类型；
- 面向对象，同时支持函数式编程能力；
- 自动内存管理；
- 平台无关的字节码执行模型；
- 多线程与并发基础设施；
- 解释执行、JIT 优化和其他运行策略；
- 完整的标准类库；
- 成熟的开发、诊断和监控工具链。

这些特征共同构成 Java 的工程价值，不能只用“跨平台”概括。

---

## 1.3 JDK、JRE 与 JVM

### 1.3.1 JVM

JVM 是 Java Virtual Machine，即 Java 虚拟机。

JVM 的核心职责包括：

- 加载、验证和执行字节码；
- 提供字节码指令集的运行环境；
- 管理运行时内存；
- 创建、访问和回收对象；
- 提供线程执行环境；
- 处理异常、方法调用和同步语义；
- 通过解释器或编译器将字节码转化为本地机器指令；
- 屏蔽操作系统和 CPU 架构差异。

需要区分两个含义：

1. **JVM 规范**：规定 JVM 应表现出什么行为；
2. **JVM 实现**：真正运行程序的软件，例如 HotSpot、OpenJ9。

面试中说“JVM 是规范”或“JVM 是虚拟机进程”都可能成立，关键是说明当前语境。

### 1.3.2 JRE

JRE 是 Java Runtime Environment，即 Java 运行环境。

传统概念可以表示为：

```text
JRE
├── JVM
├── Java 标准类库
└── 运行 Java 程序所需的其他组件
```

JRE 的目标是“运行 Java 程序”，通常不强调完整的开发、编译和诊断工具。

Java 8 时代，独立安装 JRE 很常见。现代 JDK 采用模块化运行时镜像，主流发行版通常直接提供完整 JDK，也可以通过 `jlink` 构建只包含应用所需模块的定制运行时。

因此，JRE 仍然是有价值的概念，但不能机械认为现代 JDK 安装目录中一定存在独立的 `jre` 子目录。

### 1.3.3 JDK

JDK 是 Java Development Kit，即 Java 开发工具包。

JDK 通常包含：

```text
JDK
├── Java 运行环境
│   ├── JVM
│   └── Java 标准类库
└── 开发、打包和诊断工具
    ├── java
    ├── javac
    ├── jar
    ├── javap
    ├── javadoc
    ├── jshell
    ├── jdeps
    ├── jlink
    ├── jcmd
    ├── jstack
    ├── jmap
    └── jstat
```

开发 Java 程序通常需要 JDK，而不是只需要 JVM。

### 1.3.4 三者关系

便于记忆的传统表达是：

```text
JDK ⊃ JRE ⊃ JVM
```

它表达的是能力范围：

- JVM 负责执行字节码；
- JRE 提供运行程序所需环境；
- JDK 在运行环境之上增加开发和诊断工具。

这不是对现代 JDK 物理目录结构的严格描述。

### 1.3.5 对比表

| 名称 | 核心定位 | 主要内容 | 典型使用者 |
|---|---|---|---|
| JVM | 字节码执行引擎 | 类加载、执行、内存管理、GC | Java 程序 |
| JRE | 程序运行环境 | JVM、标准类库、运行组件 | 应用运行环境 |
| JDK | 开发工具包 | 运行环境、编译、打包、诊断工具 | 开发和运维人员 |

---

## 1.4 Java 程序从源码到执行

### 1.4.1 整体链路

传统 Java 程序的基本运行链路是：

```text
Main.java
↓ javac 编译
Main.class
↓ java 启动 JVM
加载初始类
↓
链接：验证、准备、按需解析
↓
初始化类
↓
调用可启动的 main 方法
↓
解释器执行字节码
↓
热点代码由 JIT 编译
↓
本地机器指令
↓
CPU 执行
```

这条链路同时包含编译期和运行期。

### 1.4.2 编译阶段

示例：

```powershell
javac Main.java
```

`javac` 的主要工作包括：

- 解析源代码；
- 执行语法检查；
- 执行类型检查；
- 检查访问权限和确定赋值；
- 处理泛型并执行类型擦除；
- 转换部分语法糖；
- 生成符合 class 文件格式的字节码。

`javac` 默认生成的是 JVM 字节码，不是 x86、ARM 等 CPU 直接执行的机器码。

### 1.4.3 JVM 启动阶段

示例：

```powershell
java Main
```

`java` 命令通常负责：

- 创建 JVM 进程；
- 解析启动参数；
- 建立 classpath 或 module path；
- 查找并加载初始类；
- 链接并初始化该类；
- 选择并调用可启动的 `main` 方法；
- 执行程序，直到非守护线程结束或进程退出。

这里的 `Main` 是类名，不是文件名，因此不能写成：

```powershell
java Main.class
```

### 1.4.4 类的加载、链接和初始化

从 JVM 规范视角，可以先建立以下高层认知：

```text
加载 Loading
↓
链接 Linking
├── 验证 Verification
├── 准备 Preparation
└── 解析 Resolution（允许按需发生）
↓
初始化 Initialization
```

- **加载**：找到 class 二进制数据并创建对应的运行时类型表示；
- **验证**：检查 class 文件结构和字节码是否合法；
- **准备**：为类变量分配空间并设置初始默认值；
- **解析**：将符号引用转换为直接引用，允许按 JVM 规则延迟；
- **初始化**：执行类变量初始化表达式和静态初始化块。

本章只建立阶段概念，类加载器、常量池和初始化锁在 JVM 模块中展开。

### 1.4.5 单文件源代码启动

从 JDK 11 开始，可以直接运行单个源文件：

```powershell
java Main.java
```

该模式由源代码启动器完成编译和运行，适合：

- 小型示例；
- 学习实验；
- 简单命令行程序；
- 快速验证 API。

它仍然使用 Java 编译器和 JVM，不代表 Java 源代码被逐行解释执行。

---

## 1.5 Java 程序入口

### 1.5.1 传统 main 方法

长期以来最通用、兼容范围最广的入口是：

```java
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, Java");
    }
}
```

各组成部分的含义：

- `public`：传统启动协议要求启动器能够访问；
- `static`：无需先创建 `Main` 对象；
- `void`：不向启动器返回 Java 方法值；
- `main`：约定的入口方法名；
- `String[] args`：接收命令行参数。

命令行参数示例：

```powershell
java Main Java JVM
```

```java
public class Main {

    public static void main(String[] args) {
        System.out.println(args[0]); // Java
        System.out.println(args[1]); // JVM
    }
}
```

在企业工程、框架启动类、需要兼容旧版本的程序中，传统入口仍然是默认选择。

### 1.5.2 Java 25 的可启动 main 方法

Java 25 正式交付 JEP 512：Compact Source Files and Instance Main Methods。

从 Java 25 开始，可启动的 `main` 方法可以进一步简化：

```java
class Main {

    void main() {
        System.out.println("Hello, Java 25");
    }
}
```

这一形式具有以下特点：

- `main` 可以是实例方法；
- 可以省略 `public`；
- 可以没有 `String[]` 参数；
- 返回类型仍然必须是 `void`；
- 方法不能是 `private`；
- 对实例 `main`，启动类需要具有非 `private` 的无参构造器。

启动方式：

```powershell
javac Main.java
java Main
```

如果同一个类型中同时存在可用的 `main(String[])` 和 `main()`，启动器优先选择带 `String[]` 参数的方法。

### 1.5.3 Java 25 紧凑源文件

Java 25 还允许省略显式类声明：

```java
void main() {
    System.out.println("Hello, compact source file");
}
```

文件中也可以声明其他字段和方法：

```java
String greeting() {
    return "Hello";
}

void main() {
    System.out.println(greeting());
}
```

可直接运行：

```powershell
java Main.java
```

也可以先编译再运行：

```powershell
javac Main.java
java Main
```

编译器会为紧凑源文件隐式声明一个类。源码中直接书写的方法仍然是该隐式类的成员，不是真正脱离类型系统的全局函数。

紧凑源文件适合：

- 入门教学；
- 快速实验；
- 小型命令行工具；
- 原型代码。

它不用于替代大型工程中的正常类型、包和模块设计。

### 1.5.4 不要把简化入口理解成另一门 Java

传统入口、实例入口和紧凑源文件使用的是同一套 Java 语言、编译器和 JVM。

```text
传统类文件
→ 显式声明类和 static main

实例 main
→ 显式声明类，入口可以是实例方法

紧凑源文件
→ 省略显式类声明，由编译器隐式声明类
```

Java 25 降低的是小程序的语法门槛，没有引入独立方言或独立工具链。

---

## 1.6 class 文件与 JVM 字节码

### 1.6.1 class 文件是什么

`.class` 是一种具有严格结构的二进制文件，通常包含：

- class 文件版本；
- 常量池；
- 类和父类信息；
- 字段信息；
- 方法信息；
- 字节码指令；
- 异常表；
- 注解、调试和其他属性。

class 文件使用固定魔数：

```text
0xCAFEBABE
```

魔数用于快速识别文件类型，但不能单独证明整个 class 文件合法。

### 1.6.2 字节码的定位

字节码具有以下特征：

- 是 JVM 指令集；
- 是 Java 源码常见的编译结果；
- 通常与具体操作系统和 CPU 无关；
- 比源代码更接近执行模型；
- 不是物理 CPU 直接执行的本地机器码。

```text
Java 源代码
↓
JVM 字节码
↓
解释执行或编译
↓
x86 / ARM 等本地机器指令
```

### 1.6.3 JVM 不只运行 Java 源码生成的字节码

JVM 只关心 class 文件是否符合规范，不要求其最初一定来自 Java 源代码。

能够生成 JVM 字节码的语言包括：

- Kotlin；
- Scala；
- Groovy；
- Clojure。

因此，“Java 跨平台”的直接执行载体是 JVM 字节码，而不是 `.java` 文本本身。

### 1.6.4 使用 javap 查看字节码

源码：

```java
public class Calculator {

    public static int add(int left, int right) {
        return left + right;
    }
}
```

编译和查看：

```powershell
javac Calculator.java
javap -c -p Calculator
```

核心字节码可能包含：

```text
iload_0
iload_1
iadd
ireturn
```

含义大致是：

```text
读取第一个 int 参数
读取第二个 int 参数
执行 int 加法
返回 int 结果
```

本章不要求背诵字节码指令，只需要理解 Java 源码会被转换为 JVM 指令。

### 1.6.5 字节码验证不能替代应用安全

JVM 会验证 class 文件和字节码的结构安全，例如：

- 指令格式是否合法；
- 操作数栈使用是否一致；
- 类型转换是否符合规则；
- 跳转目标是否有效。

但字节码验证不能保证：

- 业务逻辑正确；
- 数据没有越权；
- SQL 没有注入；
- 程序没有资源泄漏；
- 并发代码没有竞态条件。

“通过 JVM 验证”只说明字节码满足运行时结构约束。

---

## 1.7 Java 为什么能够跨平台

### 1.7.1 核心机制

```text
同一份 Java 源代码
↓ javac
平台无关的 class 字节码
↓
Windows JVM → Windows 机器指令
Linux JVM   → Linux 机器指令
macOS JVM   → macOS 机器指令
```

不同平台提供符合规范的 JVM 实现，JVM 将同一套字节码转化为当前平台能够执行的形式。

因此，经典表述是：

```text
Write Once, Run Anywhere
一次编写，到处运行
```

### 1.7.2 跨平台的是字节码，不是 JVM

这是最重要的边界：

- class 字节码通常具有平台无关性；
- JVM 的具体实现是平台相关的；
- Windows、Linux、macOS 和不同 CPU 架构需要对应构建的 JDK/JVM。

更准确的表达是：

> Java 通过统一的字节码规范和不同平台的 JVM 实现，提供较强的平台无关能力。

### 1.7.3 Java 程序并不天然完全跨平台

以下因素可能引入平台差异：

- JNI 和本地动态库；
- 操作系统命令；
- 硬编码文件路径；
- 文件权限模型；
- 字符编码；
- 换行符；
- 大小写敏感性；
- Locale、时区和区域格式；
- 特定 CPU 指令或硬件能力；
- 不同厂商 JDK 的实现差异；
- 外部数据库、消息系统和中间件配置。

例如，不应硬编码：

```java
String path = "C:\\data\\config.txt";
```

更合适的写法是：

```java
Path path = Path.of("data", "config.txt");
```

平台无关能力需要语言、标准库和工程实践共同保证。

---

## 1.8 Java 是编译型还是解释型语言

### 1.8.1 不能简单二选一

Java 的典型执行链同时包含：

```text
源码编译
+
字节码解释执行
+
JIT 即时编译
```

准确表述是：

> Java 源代码先编译为 JVM 字节码，运行时再由 JVM 通过解释执行和即时编译相结合的方式执行。

### 1.8.2 解释器

解释器逐条读取并执行字节码。

优势：

- 程序启动后可以较快开始执行；
- 不需要提前把全部方法都编译成本地机器码；
- 适合只执行一次或执行频率较低的代码。

代价：

- 同一段热点代码被反复解释时，执行开销较高。

### 1.8.3 JIT 即时编译

JIT 是 Just-In-Time Compiler，即即时编译器。

其基本思路是：

```text
程序运行
↓
收集执行信息
↓
识别热点方法或热点循环
↓
编译为本地机器码
↓
后续直接执行优化后的机器码
```

JIT 可以利用运行期信息进行优化，例如：

- 方法内联；
- 去虚拟化；
- 逃逸分析；
- 无效代码消除；
- 循环优化；
- 基于类型反馈的优化。

这些优化可能依赖当时的运行假设。当假设失效时，JVM 可以撤销部分优化并回到较通用的执行方式，这称为反优化。

### 1.8.4 AOT 与原生可执行文件

AOT 是 Ahead-of-Time Compilation，即提前编译。

其目标是在程序正式运行前完成部分或全部编译工作，以改善某些场景中的：

- 启动速度；
- 预热时间；
- 内存占用；
- 部署形态。

GraalVM Native Image 等工具还可以将应用构建成本地可执行文件。它与普通 JVM 模式存在不同取舍，例如：

- 动态类加载和反射需要额外配置；
- 构建时间更长；
- 启动通常更快；
- 运行时峰值性能未必在所有场景都优于充分预热的 JVM。

不能简单得出“AOT 一定比 JIT 快”的结论。

---

## 1.9 Java 规范与具体实现

### 1.9.1 Java 语言规范

Java Language Specification，简称 JLS，主要定义：

- 源代码语法；
- 类型系统；
- 表达式求值；
- 变量和确定赋值；
- 方法重载和重写规则；
- 类、接口、继承和初始化语义；
- 异常、泛型、Lambda 等语言规则。

例如：

- `byte + byte` 为什么得到 `int`；
- 重载如何选择；
- `final` 变量如何确定赋值；

这些主要属于 Java 语言规范。

### 1.9.2 JVM 规范

Java Virtual Machine Specification，简称 JVMS，主要定义：

- class 文件格式；
- 字节码指令；
- 运行时数据区的逻辑模型；
- 类加载、链接和初始化；
- 方法调用和返回；
- 异常处理；
- 线程和同步相关的 JVM 语义。

例如：

- class 常量池如何组织；
- `invokevirtual` 指令如何表示；
- JVM 如何启动初始类；

这些主要属于 JVM 规范。

### 1.9.3 规范不等于实现

规范描述“必须表现为什么”，实现决定“具体如何完成”。

常见 JVM 或运行时实现包括：

- HotSpot；
- Eclipse OpenJ9；
- GraalVM 相关运行时。

不同实现可以具有不同的：

- JIT 编译器；
- 垃圾收集器；
- 内存布局；
- 诊断工具；
- 启动与吞吐特征。

只要符合对应规范，应用通常不需要依赖其内部实现细节。

---

## 1.10 Java 版本、JDK 版本与发行版

### 1.10.1 三个维度

需要区分：

| 概念 | 示例 | 含义 |
|---|---|---|
| Java SE 版本 | Java SE 25 | 平台规范版本 |
| JDK 版本 | JDK 25 | 对应该平台版本的开发工具包 |
| JDK 发行版 | Temurin 25、Oracle JDK 25 | 厂商构建和发布的具体产品 |

日常交流中“Java 17”和“JDK 17”经常混用，但概念上并不完全相同。

### 1.10.2 OpenJDK 与厂商发行版

OpenJDK 是 Java SE 开源实现的主要开发基础，各厂商可以基于 OpenJDK 源码构建发行版。

常见发行版包括：

- Oracle JDK；
- Eclipse Temurin；
- Amazon Corretto；
- Azul Zulu；
- Microsoft Build of OpenJDK；
- Alibaba Dragonwell；
- IBM Semeru。

它们在语言和标准 API 上高度兼容，但可能在以下方面不同：

- 支持周期；
- 更新节奏；
- 安装包；
- 许可证；
- 默认配置；
- 附加工具；
- 商业支持；
- 特定平台优化。

选择 JDK 时不能只看版本号，还要确认厂商、许可证和维护策略。

### 1.10.3 LTS 的含义

LTS 是 Long-Term Support，即长期支持。

需要注意：

- LTS 主要是厂商支持策略，不是某个语言特性；
- 同一个 Java SE 版本，不同厂商的免费更新和商业支持期限可能不同；
- 非 LTS 版本仍然是正式 Java SE 版本，只是支持周期通常较短；
- 企业升级不能只看“是否 LTS”，还要评估框架、依赖、容器和运维支持。

### 1.10.4 当前版本定位

截至 2026 年 7 月：

| 版本 | 发布定位 | 复习建议 |
|---|---|---|
| Java 8 | 历史长期基线 | 理解存量项目和经典语义 |
| Java 11 | LTS | 认识模块化后的长期版本 |
| Java 17 | LTS | 大量企业项目的重要基线 |
| Java 21 | LTS | 虚拟线程等现代能力的重要版本 |
| Java 25 | 最新 LTS | 本套语言笔记的现代特性基线 |
| Java 26 | 最新功能版本 | 跟踪新特性，不作为默认长期基线 |

JDK 25 于 2025 年 9 月 16 日达到 GA。JDK 26 于 2026 年 3 月 17 日发布，当前属于非 LTS 功能版本。

本套笔记的策略：

```text
基础语义
→ 覆盖 Java 8 以来稳定规则

现代语法
→ 以 Java 25 为主要基线

Java 26
→ 用于标注已经正式交付的新变化
```

---

## 1.11 版本兼容与编译选项

### 1.11.1 高版本 class 不能直接运行在低版本 JVM

如果使用较新的 JDK 编译：

```powershell
javac Main.java
```

生成的 class 文件可能要求更高版本的 JVM。放到旧 JVM 上运行时，可能出现：

```text
UnsupportedClassVersionError
```

核心规则：

```text
运行时 JVM
必须支持
class 文件所使用的版本
```

### 1.11.2 优先使用 --release

需要生成可在 Java 17 环境运行的产物时，可以使用：

```powershell
javac --release 17 Main.java
```

`--release` 同时约束：

- 可使用的语言级别；
- 生成的 class 文件版本；
- 可见的标准库 API。

相比只使用 `-source` 和 `-target`，`--release` 更能避免“字节码版本正确，但误用了新版本 API”的问题。

### 1.11.3 编译版本与运行版本不是同一概念

以下场景需要分别确认：

```text
编译使用哪个 JDK
↓
生成面向哪个 Java 版本的 class
↓
最终部署使用哪个 JVM
```

例如：

- 本地使用 JDK 25；
- 通过 `--release 17` 编译；
- 部署到 JDK 17 运行时。

这是一种正常组合。

### 1.11.4 预览特性

部分 Java 特性会经历 Preview 阶段。

使用预览特性通常需要在编译和运行时都显式开启：

```powershell
javac --enable-preview --release 26 Main.java
java --enable-preview Main
```

注意：

- 预览特性可能在后续版本变化或移除；
- 使用某个版本的预览 class，通常应使用对应版本 JVM 运行；
- 生产项目应明确评估升级和兼容成本；
- Java 25 的紧凑源文件与实例 `main` 已经正式交付，不再是预览特性。

---

## 1.12 包、classpath、module path 与启动方式

### 1.12.1 包名和完全限定名

源码：

```java
package com.example.demo;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

类的完全限定名是：

```text
com.example.demo.Main
```

工程中通常让目录结构与包名对应：

```text
src
└── com
    └── example
        └── demo
            └── Main.java
```

编译：

```powershell
javac -d out src/com/example/demo/Main.java
```

输出：

```text
out
└── com
    └── example
        └── demo
            └── Main.class
```

启动：

```powershell
java -cp out com.example.demo.Main
```

不能只写：

```powershell
java -cp out Main
```

因为 JVM 需要使用完全限定名定位带包名的类。

### 1.12.2 PATH

`PATH` 是操作系统环境变量，用于查找可执行程序。

例如终端执行：

```powershell
java --version
javac --version
```

操作系统会根据 `PATH` 查找 `java` 和 `javac`。

如果 `PATH` 配置错误，常见现象是：

```text
无法识别 java 或 javac 命令
```

### 1.12.3 classpath

classpath 是 JVM 和 Java 编译器查找类与资源的位置集合。

示例：

```powershell
java -cp out com.example.demo.Main
```

classpath 中可以包含：

- class 文件目录；
- JAR 文件；
- 多个目录和 JAR。

`PATH` 与 classpath 的区别：

| 配置 | 查找对象 |
|---|---|
| `PATH` | `java`、`javac` 等操作系统可执行文件 |
| classpath | Java 类和类路径资源 |

不建议长期依赖全局 `CLASSPATH` 环境变量。工程项目通常由 Maven、Gradle、IDE 或启动脚本显式构造 classpath。

### 1.12.4 module path

Java 9 引入模块系统后，具名模块可以通过 module path 定位：

```powershell
java --module-path mods --module com.example.app/com.example.Main
```

本章只区分：

```text
classpath
→ 传统类路径模型

module path
→ Java 模块系统的模块查找路径
```

模块声明、可读性、导出和开放规则在模块化专题中展开。

### 1.12.5 可执行 JAR

编译后可以创建具有主类信息的 JAR：

```powershell
javac Main.java
jar --create --file app.jar --main-class Main Main.class
java -jar app.jar
```

`java -jar` 会读取 JAR 清单中的 `Main-Class` 并启动对应类。

企业应用的可执行 JAR 可能还包含依赖打包、类加载器和启动器逻辑，这属于构建工具或框架范畴。

---

## 1.13 常用 JDK 工具

| 工具 | 主要用途 | 常见命令 |
|---|---|---|
| `java` | 启动 JVM、运行类、JAR 或源文件 | `java Main` |
| `javac` | 编译 Java 源文件 | `javac Main.java` |
| `jar` | 创建和查看 JAR | `jar --list --file app.jar` |
| `javap` | 反汇编 class、查看字节码和签名 | `javap -c Main` |
| `javadoc` | 生成 API 文档 | `javadoc Main.java` |
| `jshell` | 交互式执行 Java 片段 | `jshell` |
| `jdeps` | 分析类或模块依赖 | `jdeps app.jar` |
| `jlink` | 构建定制运行时镜像 | `jlink ...` |
| `jcmd` | 向 JVM 发送诊断命令 | `jcmd <pid> VM.version` |
| `jps` | 查看 Java 进程 | `jps -l` |
| `jstack` | 查看线程栈 | `jstack <pid>` |
| `jmap` | 查看堆和对象统计 | `jmap -histo <pid>` |
| `jstat` | 查看 JVM 统计信息 | `jstat -gc <pid>` |

工具可用性和参数可能随 JDK 发行版、操作系统和版本变化。诊断命令应优先使用目标环境实际安装的 JDK 文档。

---

## 1.14 建议实验

### 实验一：传统编译与运行

创建 `Main.java`：

```java
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, Java");
    }
}
```

执行：

```powershell
javac Main.java
java Main
```

观察：

- 当前目录生成 `Main.class`；
- `java Main` 可以运行；
- `java Main.class` 不能按预期启动。

### 实验二：源文件启动模式

删除或忽略已生成的 class，直接执行：

```powershell
java Main.java
```

观察程序可以运行，理解该命令背后仍包含编译过程。

### 实验三：包名与 classpath

创建：

```text
src/com/example/demo/Main.java
```

内容：

```java
package com.example.demo;

public class Main {

    public static void main(String[] args) {
        System.out.println("classpath");
    }
}
```

执行：

```powershell
javac -d out src/com/example/demo/Main.java
java -cp out com.example.demo.Main
```

然后尝试：

```powershell
java -cp out Main
```

观察类名定位失败，理解完全限定名和 classpath 的关系。

### 实验四：Java 25 实例 main

使用 JDK 25 或更高版本：

```java
class InstanceMainDemo {

    void main() {
        System.out.println("instance main");
    }
}
```

执行：

```powershell
javac InstanceMainDemo.java
java InstanceMainDemo
```

观察无需 `public static void main(String[] args)` 也能启动。

### 实验五：Java 25 紧凑源文件

创建 `CompactDemo.java`：

```java
String greeting() {
    return "compact source file";
}

void main() {
    System.out.println(greeting());
}
```

执行：

```powershell
java CompactDemo.java
```

思考：

- 源码没有显式类声明；
- 方法仍然属于编译器隐式声明的类；
- 它不是真正的全局函数。

### 实验六：查看字节码

```java
public class Calculator {

    public static int add(int left, int right) {
        return left + right;
    }
}
```

执行：

```powershell
javac Calculator.java
javap -c -p Calculator
```

查找：

```text
iload
iadd
ireturn
```

理解源码和 JVM 指令的对应关系。

### 实验七：使用 --release

在较新 JDK 中执行：

```powershell
javac --release 17 Main.java
javap -verbose Main
```

在输出中查找 class 文件版本信息。

然后尝试在代码中使用高于 Java 17 才提供的标准 API，观察 `--release 17` 如何阻止编译。

### 实验八：创建可执行 JAR

```powershell
javac Main.java
jar --create --file app.jar --main-class Main Main.class
java -jar app.jar
```

再执行：

```powershell
jar --list --file app.jar
```

观察 JAR 内容和启动方式。

---

## 1.15 高频面试题

1. Java 语言、Java 平台和 Java 生态有什么区别？
2. JDK、JRE、JVM 分别是什么？
3. 为什么 `JDK ⊃ JRE ⊃ JVM` 只是便于理解的概念模型？
4. Java 源代码从编译到运行经历哪些主要步骤？
5. `.class` 文件是什么？
6. 字节码和机器码有什么区别？
7. Java 为什么能够跨平台？
8. 为什么说跨平台的是字节码，而不是 JVM？
9. Java 程序是否天然完全跨平台？
10. Java 是编译型语言还是解释型语言？
11. 解释器和 JIT 分别有什么作用？
12. JIT 为什么能够执行运行期优化？
13. AOT 和 JIT 的主要取舍是什么？
14. `javac` 与 `java` 命令分别做什么？
15. `java Main` 中的 `Main` 是类名还是文件名？
16. 传统 `main` 方法为什么声明为 `public static void`？
17. Java 25 的实例 `main` 和紧凑源文件是什么？
18. 紧凑源文件中的方法是真正的全局函数吗？
19. classpath 与 `PATH` 有什么区别？
20. classpath 与 module path 有什么区别？
21. 为什么带包名的类需要使用完全限定名启动？
22. `java Main.java` 是否代表 Java 源码被直接解释？
23. Java SE 版本、JDK 版本和 JDK 发行版有什么区别？
24. OpenJDK、Oracle JDK 和 Temurin 等发行版是什么关系？
25. LTS 是否属于 Java 语言特性？
26. 高版本 JDK 编译的 class 为什么可能无法在低版本 JVM 运行？
27. `--release`、`-source` 和 `-target` 的关注点有什么区别？
28. `UnsupportedClassVersionError` 通常说明什么？
29. `javap` 有什么用途？
30. JVM 规范与 JVM 实现有什么区别？

---

## 1.16 易错点

### 误区一：JDK、JRE 和 JVM 是三个互不相关的软件

**错误。**

JVM 提供字节码执行能力，JRE 表示运行环境，JDK 在运行能力之上提供开发和诊断工具。三者属于不同层级的概念。

### 误区二：现代 JDK 中一定存在独立的 jre 目录

**错误。**

现代 JDK 使用模块化运行时镜像，传统包含关系不能机械映射为安装目录结构。

### 误区三：JVM 是跨平台的，所以同一个 JVM 安装包可以在所有系统运行

**错误。**

JVM 实现本身依赖操作系统和 CPU 架构。跨平台的是符合规范的字节码和 Java API 模型。

### 误区四：Java 源代码可以被 CPU 直接执行

**错误。**

通常需要先编译成字节码，再由 JVM 解释或编译为机器指令。

### 误区五：class 文件就是本地机器码

**错误。**

class 文件主要保存 JVM 字节码和类型元数据，不是 x86 或 ARM 指令文件。

### 误区六：Java 是纯解释型语言

**错误。**

Java 通常包含源码编译、解释执行和 JIT 即时编译。

### 误区七：使用 `java Main.java` 就跳过了编译

**错误。**

源代码启动器仍然需要编译源码，只是把编译和启动流程封装在一条命令中。

### 误区八：Java 25 删除了传统 main 方法

**错误。**

传统 `public static void main(String[] args)` 继续有效，并且仍是企业工程最常见的入口。

### 误区九：Java 25 紧凑源文件中的方法是真正的全局函数

**错误。**

编译器会隐式声明一个类，源码中的字段和方法仍然是该类的成员。

### 误区十：JIT 会在启动前编译全部代码

**错误。**

JIT 通常在程序运行过程中根据执行信息识别并编译热点代码。

### 误区十一：AOT 一定比 JIT 快

**错误。**

AOT 常有启动优势，但 JIT 可以基于真实运行数据做更激进的优化。最终表现取决于应用和工作负载。

### 误区十二：只要是 Java 程序，就一定不受操作系统差异影响

**错误。**

本地库、文件系统、编码、权限、时区和系统命令都可能引入平台差异。

### 误区十三：Java 25 是 LTS，所以 LTS 是 Java 规范的一部分

**错误。**

LTS 是发行厂商的支持策略。不同厂商的维护期限和许可可能不同。

### 误区十四：只设置 `-target 17` 就一定能在 Java 17 运行

**错误。**

代码仍可能引用新版本标准库 API。较新 JDK 中优先使用 `--release 17` 同时约束语言、class 版本和标准 API。

### 误区十五：classpath 就是操作系统 PATH

**错误。**

`PATH` 查找可执行程序，classpath 查找 Java 类和资源。

### 误区十六：字节码验证通过就说明程序安全且业务正确

**错误。**

字节码验证只检查 JVM 结构和类型安全，不负责业务授权、数据校验和并发正确性。

---

## 1.17 工程实践建议

1. 开发、测试和生产环境应明确记录 JDK 厂商、主版本和更新版本。
2. 企业项目优先选择维护策略清晰的 LTS 版本，但不要长期停留在停止维护的旧更新版本。
3. 使用构建工具的 Toolchain 或明确配置，避免“本机能编译、CI 不能编译”。
4. 面向旧版本部署时优先使用 `--release`，不要只依赖 `-source` 和 `-target`。
5. 不依赖全局 `CLASSPATH`，由 Maven、Gradle、IDE 或启动脚本构造类路径。
6. 启动带包名的类时使用完全限定名。
7. 不把本地路径、默认编码、默认时区和默认 Locale 当作稳定契约。
8. 使用预览特性时，在构建、测试、运行和部署环境统一开启，并明确升级计划。
9. 不把紧凑源文件直接扩展为大型生产代码；代码增长后应及时引入正常的包和类型结构。
10. 诊断生产 JVM 时，优先使用与目标 JVM 版本兼容的 JDK 工具。
11. 升级 JDK 时同时检查框架、构建插件、字节码增强工具、监控 Agent 和容器镜像。
12. 对外发布公共编译产物时，明确最低运行版本和支持的 JDK 发行版。

---

## 1.18 官方参考资料

- Java Language Specification；
- Java Virtual Machine Specification；
- OpenJDK JDK Project；
- JEP 512：Compact Source Files and Instance Main Methods；
- OpenJDK JDK 25 Release；
- Java SE 26 Release Notes；
- JDK 工具官方文档：`java`、`javac`、`javap`、`jar`、`jlink`、`jdeps`。

版本性内容以 2026 年 7 月公开资料为基准。后续 Java 版本发布时，应优先更新 `1.10.4 当前版本定位`，不需要重写本章稳定原理。

---

## 1.19 本章总结

### 1.19.1 核心知识链

```text
Java 源代码
↓ javac
class 文件与 JVM 字节码
↓ java 启动 JVM
类加载、链接和初始化
↓
选择并调用 main
↓
解释器开始执行
↓
JIT 编译热点代码
↓
本地机器指令
↓
CPU 执行
```

跨平台链路：

```text
同一套字节码
↓
不同平台的 JVM 实现
↓
对应平台的机器指令
```

平台组成：

```text
Java 语言
→ 规定源码语义

Java 平台
→ JVM、标准类库和工具链

JDK
→ 运行环境 + 开发与诊断工具
```

### 1.19.2 面试口述版

Java 程序通常先由 `javac` 编译成平台无关的 class 字节码，再由目标平台上的 JVM 加载、链接、初始化和执行。JVM 可以先解释字节码，再把频繁执行的热点代码通过 JIT 编译成本地机器码，因此 Java 不是单纯的编译型或解释型语言，而是编译、解释和即时编译相结合。

Java 的跨平台基础是统一的 class 文件与字节码规范，不同操作系统和 CPU 架构提供各自的 JVM 实现。JVM 本身是平台相关的，而且本地库、路径、编码和操作系统能力仍可能破坏应用的跨平台性。

JDK 是开发工具包，包含运行 Java 程序所需能力以及 `javac`、`java`、`jar`、`javap` 等工具；JRE 表示运行环境；JVM 是字节码执行引擎。Java 25 正式支持实例 `main` 和紧凑源文件，但传统 `public static void main(String[] args)` 仍是企业工程最通用的入口。
