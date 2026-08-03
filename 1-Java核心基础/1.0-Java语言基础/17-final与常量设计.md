# final 与常量设计

> 本章建立 `final` 在变量、字段、参数、方法和类上的统一模型，并把“只能赋值一次”“引用不可重新指向”“对象不可变”“编译期常量”“安全发布”这些容易混淆的概念彻底分开。重点落在可预测初始化、不可变边界、常量演进和二进制兼容。

---

## 17.1 本章定位

学完本章，应能够准确回答：

- `final` 可以修饰哪些语言元素？
- final 局部变量与 final 字段的确定赋值规则有什么区别？
- 什么是空白 final，实例空白 final 和静态空白 final 分别在哪里赋值？
- final 引用为什么不等于对象不可变？
- final 数组、集合和可变对象仍有哪些修改入口？
- final 参数有什么实际价值，为什么不能保护实参对象？
- 什么是 effectively final，为什么 Lambda 捕获需要它？
- final 方法、private 方法、static 方法和方法隐藏如何区分？
- final 类为什么不能继承，但仍可能是可变类？
- record、enum、sealed 与 final 如何关联？
- `static final` 是否一定是编译期常量？
- 常量变量必须满足哪些类型和初始化表达式条件？
- 常量表达式允许哪些运算，为什么方法调用通常不是常量表达式？
- 编译期常量内联为什么带来跨模块旧值风险？
- 为什么可变化配置不应暴露为 public 编译期常量？
- final 字段的 JMM 特殊语义解决什么问题？
- 构造期间 `this` 逸出为什么会破坏 final 字段初始化保证？
- final 是否能代替锁、volatile 或不可变对象设计？
- 常量类、枚举、值对象和配置对象分别适合什么语义？
- WMS 状态码、阈值、库位类型和运行时配置应如何建模？

`static` 与初始化触发规则见第 16 章；不可变对象的完整设计见第 10 章；并发安全发布的完整推导见并发模块。

## 17.2 学习主线

```text
final 的位置决定语义
↓
变量与字段：赋值一次
↓
引用不变 ≠ 对象不变
↓
方法：禁止重写
↓
类：禁止继承
↓
空白 final 与确定赋值
↓
effectively final 与闭包捕获
↓
static final 与常量变量分离
↓
常量表达式与编译期内联
↓
二进制兼容与配置边界
↓
final 字段初始化保证
↓
不可变对象、枚举和值对象设计
```

分析 `final` 问题时先问：它修饰的是变量、字段、方法还是类；若是变量，再区分变量本身保存的值与该值引用的对象状态。

## 17.3 final 的统一含义

`final` 的共同方向是限制后续变化，但限制对象不同：

| 位置 | 约束对象 | 核心语义 |
|---|---|---|
| 局部变量/参数 | 变量槽中的值 | 确定赋值后不能再次赋值 |
| 实例字段 | 每个对象中的字段 | 构造完成前确定赋值，之后普通代码不能再赋值 |
| 静态字段 | 类变量 | 类初始化完成前确定赋值，之后普通代码不能再赋值 |
| 方法 | 方法实现 | 子类不能重写 |
| 类 | 类型层次 | 不能声明子类 |

它不是统一的“不可变”开关。

## 17.4 final 局部变量

```java
final int retryCount = 3;
```

变量在确定赋值后不能再赋值：

```java
// retryCount = 4;
```

每次进入新的块或循环迭代可以创建新的 final 变量，因此以下合法：

```java
for (int i = 0; i < 3; i++) {
    final int snapshot = i;
}
```

## 17.5 确定赋值

编译器进行控制流分析，要求 final 变量在读取前已经赋值，并且在任何可达路径上不会被赋值超过一次。

```java
final int timeout;
if (production) {
    timeout = 30;
} else {
    timeout = 5;
}
System.out.println(timeout);
```

两条正常路径都恰好赋值一次，因此合法。确定赋值是语言规则，不是运行时检查。

## 17.6 空白 final 局部变量

声明时未初始化的 final 变量称为空白 final。它可以在后续控制流中赋值，但编译器必须证明最多一次且使用前必有值。

```java
final String zone;
try {
    zone = loadZone();
} catch (RuntimeException error) {
    throw error;
}
```

不要把复杂分支设计成“挑战编译器”的谜题，明确的工厂方法通常更可读。

## 17.7 final 实例字段

实例 final 字段必须在每个构造路径结束前完成赋值。合法位置包括声明处、实例初始化块或构造器。

```java
public final class OrderId {
    private final String value;

    public OrderId(String value) {
        this.value = value;
    }
}
```

它强制对象构造后拥有完整身份，但不自动验证值是否合法，仍需构造器校验。

## 17.8 构造器链与 final 字段

多个构造器可以通过 `this(...)` 汇聚到主构造器：

```java
public Config() {
    this("prod");
}

public Config(String environment) {
    this.environment = environment;
}
```

每个实际构造路径必须保证字段赋值一次。把赋值集中到主构造器可减少重复和遗漏。

## 17.9 实例初始化块

```java
private final String id;
{
    id = java.util.UUID.randomUUID().toString();
}
```

实例初始化块会在构造器主体前执行，可为所有构造器提供共同赋值。企业代码通常优先声明初始化或主构造器，因为执行顺序更直观。

## 17.10 静态空白 final

静态空白 final 必须在声明处或类初始化逻辑中赋值：

```java
private static final String ENVIRONMENT;
static {
    ENVIRONMENT = System.getProperty("app.env", "dev");
}
```

它只保证引用在类初始化后不再重新赋值，不说明该值是编译期常量，也不保证外部配置可热更新。

## 17.11 final 引用

```java
final java.util.List<String> items = new java.util.ArrayList<>();
items.add("A");       // 合法
// items = List.of(); // 非法
```

final 限制引用变量保存的引用值，列表对象本身仍可变化。

```text
引用不可重新指向
≠
引用对象不可修改
```

## 17.12 final 数组

```java
final int[] thresholds = {10, 20};
thresholds[0] = 99; // 合法
```

数组引用不能替换，但元素可修改。公开 `public static final` 数组会把全局可变状态暴露给所有调用方，是典型错误常量设计。

## 17.13 final 集合

```java
private final List<String> codes;
```

若构造器直接保存调用方传入的可变列表，即使字段 final，外部仍能修改对象内部状态。安全边界通常使用：

```java
this.codes = List.copyOf(codes);
```

返回时也不要暴露内部可变集合。

## 17.14 不可修改视图与不可变快照

`Collections.unmodifiableList(source)` 是只读视图，底层 `source` 变化仍会反映；`List.copyOf(source)` 创建独立不可修改快照（元素对象本身仍可能可变）。

选择取决于是否需要观察源集合后续变化。常量和不可变对象通常更需要快照语义。

## 17.15 final 参数

```java
void process(final Order order) {
    // order = another; // 非法
    order.cancel();     // 可能合法
}
```

final 参数防止在方法体内重新给形参赋值，不保护调用方变量，也不阻止对象状态变化。它主要是局部可读性约束，不是 API 契约或并发保证。

## 17.16 effectively final

变量未显式写 `final`，但初始化后从未重新赋值，则是 effectively final：

```java
String prefix = "WMS";
Runnable task = () -> System.out.println(prefix);
```

一旦存在重新赋值，该变量不再 effectively final，不能被 Lambda 或局部/匿名类按该规则捕获。

## 17.17 为什么闭包捕获要求有效 final

局部变量生命周期可能短于 Lambda 对象。Java 捕获的是变量值的快照语义，而不是可被多个作用域共同修改的局部槽。

如需共享可变状态，应显式使用对象、原子类或受控状态容器，同时承担并发与可读性成本。

## 17.18 final 方法

```java
public final void validate() {
}
```

子类不能重写该实例方法。适合保护父类不变量、固定安全检查或模板方法整体流程。

final 方法仍能被继承和调用；“不能重写”不等于“不能使用”。

## 17.19 private 方法与 final

private 方法对子类不可见，不形成重写关系，因此再加 final 通常没有额外设计价值。子类声明同名方法只是独立方法，不是覆盖父类 private 方法。

## 17.20 static 方法与 final

静态方法不参与实例重写，只能隐藏。对静态方法添加 final 的价值有限，主要是禁止子类声明具有可隐藏关系的同签名静态方法；它仍不是动态分派。

## 17.21 final 类

```java
public final class Money {
}
```

final 类不能被继承。它适合语义完整的值对象、安全敏感类型、工具类或不希望扩展的实现。

final 类中的实例方法无需再逐个标记 final，因为没有外部子类可以重写。

## 17.22 final 类不等于不可变类

```java
public final class MutableUser {
    private String name;
    public void rename(String name) { this.name = name; }
}
```

它不能被继承，但对象仍然可变。不可变还需要字段封装、构造校验、防御性复制和不提供状态修改入口。

## 17.23 record 与 final

record 类隐式为 final，不能被普通类继承。record 组件对应的字段是 private final，但若组件类型本身可变，record 仍可能只是浅不可变。

```java
record Batch(List<String> items) {
    Batch { items = List.copyOf(items); }
}
```

需要防御性复制才能建立更强不可变边界。

## 17.24 enum 与 final

枚举类型不能被普通类继承，也不能显式继承其他类。带有常量特定类体的枚举在实现层次上有特殊子类语义，因此不要机械说所有 enum 字节码都简单标记 final；工程结论是外部代码不能扩展枚举类型。

## 17.25 sealed 与 final

sealed 表示只允许列出的子类型；final 表示完全禁止子类型。sealed 层次中的直接子类必须明确使用 `final`、`sealed` 或 `non-sealed` 继续声明扩展策略。

## 17.26 static final

`static final` 同时表达类级成员和一次赋值：

```java
public static final int MAX_RETRY = 3;
```

它常用于常量，但不应把所有 static final 都称为编译期常量。

## 17.27 常量变量

JLS 中的 constant variable 需要：

- 是 final 变量；
- 类型为基本类型或 `String`；
- 使用常量表达式初始化。

它可以是局部变量、参数之外的字段等满足规则的变量，不要求一定是 `static`。但公共常量通常使用 `public static final`。

## 17.28 不是常量变量的 static final

```java
static final Integer BOXED = 3;
static final int RUNTIME = Integer.parseInt("3");
static final Object TOKEN = new Object();
```

它们都只能赋值一次，但不属于语言规范意义上的常量变量。读取这些字段通常需要真实字段访问并可能触发类初始化。

## 17.29 常量表达式

常量表达式由字面量、常量变量、部分运算符和不产生异常完成的语言构造组成，并且类型为基本类型或 String。

普通方法调用、对象创建、数组创建和运行时属性读取不属于常量表达式。

## 17.30 字符串常量表达式

```java
static final String PREFIX = "WMS-";
static final String CODE = PREFIX + "001";
```

如果各部分都是常量表达式，编译器可在编译期折叠为一个字符串常量。运行时字符串拼接和方法结果则不是同一语义。

## 17.31 常量变量与类初始化

读取其他类的常量变量可能直接使用调用方字节码中的值，不触发声明类初始化。读取运行时 final 字段则通常需要初始化声明类。

这也是第 16 章中“主动使用静态字段，但常量变量例外”的根源。

## 17.32 常量内联

调用方编译时可能把 public 常量值直接写入自己的 class 文件：

```text
Constants.TIMEOUT
→ 编译为常量 30
```

运行时不必读取 `Constants.TIMEOUT` 字段。

## 17.33 跨模块旧值风险

库 A 把 `TIMEOUT` 从 30 改为 60，只替换 A 的 jar 而未重新编译消费者 B，B 仍可能使用内联的 30。

这不是 JVM 缓存错误，而是 B 的字节码已经包含旧值。发布流程应重新编译所有消费者，或避免用可内联常量表达会变化的配置。

## 17.34 public 常量的兼容性

删除、改名或改变类型可能造成源码或二进制不兼容；只改变被内联值可能造成新旧消费者行为不一致。

公共常量一旦发布就是 API。评审应像评审方法签名一样评审其语义稳定性。

## 17.35 常量与配置

稳定协议事实可用常量：

```java
static final int HEADER_LENGTH = 16;
```

环境、租户、运营策略或可调整阈值应使用运行时配置对象：

```java
record AllocationConfig(int maxRetry, Duration timeout) { }
```

“当前暂时不变”不等于“语言常量”。

## 17.36 常量类

相关常量可以放在语义明确的最终类中：

```java
public final class ProtocolLimits {
    public static final int MAX_FRAME_SIZE = 1 << 20;
    private ProtocolLimits() { }
}
```

避免一个全局 `Constants` 垃圾桶混合所有领域。

## 17.37 常量接口反模式

类仅为了直接引用字段而 `implements Constants` 会污染类型关系，并把实现细节暴露为公共 API。常量应通过声明类限定名、枚举或值对象访问。

## 17.38 枚举替代状态常量

```java
enum OrderStatus { CREATED, ALLOCATED, CANCELLED }
```

枚举提供受限取值、类型安全、行为和穷尽处理，比字符串或整数魔法值更适合封闭状态集合。

外部协议仍需显式序列化值，避免依赖 `ordinal()`。

## 17.39 值对象替代复合常量

金额、时间窗口、尺寸、库位坐标等语义不应拆成互不关联的原始常量。值对象可以统一校验、单位和相等性：

```java
record WeightLimit(BigDecimal kilograms) { }
```

## 17.40 可变对象常量

```java
public static final List<String> TYPES = new ArrayList<>();
```

字段引用不可替换，但任何调用方都可修改列表。公共常量必须避免暴露可变对象，使用 `List.of`、`Set.of`、`Map.of` 或防御性复制。

## 17.41 final 字段的 JMM 语义

Java 内存模型对正确构造对象的 final 字段提供特殊初始化保证：如果构造期间 `this` 未逸出，其他线程在获得该对象引用后，对 final 字段及其按规则可达状态具有比普通字段更强的可见性保证。

这不是说所有发布方式都推荐无同步，也不是说对象整体线程安全。

## 17.42 构造期间 this 逸出

```java
static Holder leaked;
Holder() {
    leaked = this;
    value = 42;
}
```

对象在 final 字段赋值完成前被其他线程看见，破坏安全构造前提。不要在构造器中注册监听器、启动线程、调用可重写方法或发布到静态集合。

## 17.43 final 与线程安全

final 可以帮助建立不可变状态和安全初始化，但不能让可变成员操作原子化：

```java
final List<String> items = new ArrayList<>();
```

多线程并发修改仍不安全。线程安全取决于对象是否不可变、是否正确同步以及发布方式。

## 17.44 final 与性能

不要为了猜测 JIT 优化而到处添加 final。JVM 能基于实际类层次、类型剖析和运行时假设进行去虚拟化；final 的首要价值是表达设计约束和保持不变量。

## 17.45 反射与底层修改

普通 Java 源码遵守 final 赋值限制。反射、Unsafe、VarHandle、序列化框架或 JVM 内部可能存在特殊能力和限制变化，不应以这些绕过手段否定语言语义，也不应把修改 final 作为业务方案。

## 17.46 WMS 常量分类

```text
协议字段长度、固定算法参数
→ 编译期常量

订单状态、作业类型
→ enum 或 sealed 值

重量、体积、库位坐标
→ 值对象

超时、重试、波次阈值
→ 运行时配置

租户策略
→ 注入的策略对象
```

分类依据是变化频率和业务语义，不是“写起来是否方便”。

## 17.47 常量评审清单

发布常量前确认：

1. 该值在协议和业务上是否真正稳定；
2. 是否可能按环境、租户或时间变化；
3. 是否会被跨模块内联；
4. 类型是否表达单位和取值范围；
5. 是否暴露可变对象；
6. 修改时消费者是否会重新编译；
7. 是否应改用枚举、值对象或配置。

## 17.48 建议实验

### 实验1：final 局部变量

**目标**：验证赋值一次的基本规则。

```java
public class FinalLocalDemo {
    public static void main(String[] args) {
        final int value = 10;
        // value = 20;
        System.out.println(value);
    }
}
```

预期或观察重点：

```text
10
```

### 实验2：分支确定赋值

**目标**：验证所有正常路径恰好赋值一次。

```java
public class FinalBranchDemo {
    static int choose(boolean flag) {
        final int value;
        if (flag) value = 1; else value = 2;
        return value;
    }
    public static void main(String[] args) {
        System.out.println(choose(true));
        System.out.println(choose(false));
    }
}
```

预期或观察重点：

```text
1
2
```

### 实验3：循环中的新 final 变量

**目标**：理解每次迭代创建新的变量。

```java
public class FinalLoopDemo {
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            final int snapshot = i;
            System.out.println(snapshot);
        }
    }
}
```

预期或观察重点：

```text
0
1
2
```

### 实验4：构造器初始化空白 final

**目标**：验证每个对象可拥有不同 final 值。

```java
public class BlankFinalFieldDemo {
    static class User {
        final String id;
        User(String id) { this.id = id; }
    }
    public static void main(String[] args) {
        System.out.println(new User("U1").id);
        System.out.println(new User("U2").id);
    }
}
```

预期或观察重点：

```text
U1
U2
```

### 实验5：构造器链汇聚赋值

**目标**：验证 this 构造器调用集中初始化 final 字段。

```java
public class FinalConstructorChainDemo {
    static class Config {
        final String env;
        Config() { this("dev"); }
        Config(String env) { this.env = env; }
    }
    public static void main(String[] args) { System.out.println(new Config().env); }
}
```

预期或观察重点：

```text
dev
```

### 实验6：静态空白 final

**目标**：验证静态代码块完成一次赋值。

```java
public class StaticBlankFinalDemo {
    static final String ENV;
    static { ENV = "test"; }
    public static void main(String[] args) { System.out.println(ENV); }
}
```

预期或观察重点：

```text
test
```

### 实验7：final 引用仍可修改对象

**目标**：区分引用不变和对象状态不变。

```java
import java.util.ArrayList;
import java.util.List;
public class FinalReferenceDemo {
    public static void main(String[] args) {
        final List<String> values = new ArrayList<>();
        values.add("A");
        System.out.println(values);
    }
}
```

预期或观察重点：

```text
[A]
```

### 实验8：final 数组元素可变

**目标**：观察数组引用 final 不限制元素。

```java
import java.util.Arrays;
public class FinalArrayDemo {
    public static void main(String[] args) {
        final int[] values = {1, 2};
        values[0] = 9;
        System.out.println(Arrays.toString(values));
    }
}
```

预期或观察重点：

```text
[9, 2]
```

### 实验9：不可修改视图与快照

**目标**：比较 unmodifiableList 与 copyOf 的源集合变化语义。

```java
import java.util.*;
public class ViewAndSnapshotDemo {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        List<String> view = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);
        source.add("B");
        System.out.println(view);
        System.out.println(snapshot);
    }
}
```

预期或观察重点：

```text
[A, B]
[A]
```

### 实验10：final 参数只限制形参

**目标**：验证仍可修改传入对象。

```java
public class FinalParameterDemo {
    static class Box { int value; }
    static void update(final Box box) { box.value = 10; }
    public static void main(String[] args) {
        Box box = new Box(); update(box); System.out.println(box.value);
    }
}
```

预期或观察重点：

```text
10
```

### 实验11：effectively final 捕获

**目标**：验证未显式 final 的未重赋值变量可被 Lambda 捕获。

```java
public class EffectivelyFinalDemo {
    public static void main(String[] args) {
        String prefix = "WMS";
        Runnable task = () -> System.out.println(prefix);
        task.run();
    }
}
```

预期或观察重点：

```text
WMS
```

### 实验12：final 方法固定模板

**目标**：观察子类只实现钩子而不能替换整体流程。

```java
public class FinalMethodTemplateDemo {
    static abstract class Task {
        final void execute() { System.out.println("start"); run(); System.out.println("end"); }
        abstract void run();
    }
    static class Job extends Task { void run() { System.out.println("job"); } }
    public static void main(String[] args) { new Job().execute(); }
}
```

预期或观察重点：

```text
start
job
end
```

### 实验13：final 类仍可变

**目标**：证明禁止继承不等于不可变。

```java
public class FinalClassMutableDemo {
    static final class User {
        private String name;
        void rename(String name) { this.name = name; }
        String name() { return name; }
    }
    public static void main(String[] args) {
        User user = new User(); user.rename("Java"); System.out.println(user.name());
    }
}
```

预期或观察重点：

```text
Java
```

### 实验14：record 的浅不可变

**目标**：观察 record 组件引用可指向可变列表。

```java
import java.util.ArrayList;
import java.util.List;
public class RecordShallowImmutableDemo {
    record Batch(List<String> items) { }
    public static void main(String[] args) {
        List<String> source = new ArrayList<>();
        Batch batch = new Batch(source);
        source.add("A");
        System.out.println(batch.items());
    }
}
```

预期或观察重点：

```text
[A]
```

### 实验15：record 防御性复制

**目标**：通过紧凑构造器建立不可修改快照。

```java
import java.util.ArrayList;
import java.util.List;
public class RecordDefensiveCopyDemo {
    record Batch(List<String> items) { Batch { items = List.copyOf(items); } }
    public static void main(String[] args) {
        List<String> source = new ArrayList<>();
        Batch batch = new Batch(source);
        source.add("A");
        System.out.println(batch.items());
    }
}
```

预期或观察重点：

```text
[]
```

### 实验16：编译期常量不触发初始化

**目标**：验证常量内联相关初始化行为。

```java
public class CompileTimeConstantDemo {
    static class Constants {
        static final int VALUE = 3;
        static { System.out.println("initialized"); }
    }
    public static void main(String[] args) { System.out.println(Constants.VALUE); }
}
```

预期或观察重点：

```text
3
```

### 实验17：运行时 final 触发初始化

**目标**：验证方法调用结果不是常量表达式。

```java
public class RuntimeConstantDemo {
    static class Constants {
        static final int VALUE = Integer.parseInt("3");
        static { System.out.println("initialized"); }
    }
    public static void main(String[] args) { System.out.println(Constants.VALUE); }
}
```

预期或观察重点：

```text
initialized
3
```

### 实验18：字符串常量折叠

**目标**：观察常量表达式拼接结果。

```java
public class StringConstantDemo {
    static final String PREFIX = "WMS-";
    static final String CODE = PREFIX + "001";
    public static void main(String[] args) { System.out.println(CODE); }
}
```

预期或观察重点：

```text
WMS-001
```

### 实验19：枚举替代魔法值

**目标**：验证封闭状态集合的类型安全。

```java
public class EnumConstantDemo {
    enum Status { CREATED, COMPLETED }
    static String describe(Status status) {
        return switch (status) {
            case CREATED -> "new";
            case COMPLETED -> "done";
        };
    }
    public static void main(String[] args) { System.out.println(describe(Status.CREATED)); }
}
```

预期或观察重点：

```text
new
```

### 实验20：final 字段正确构造

**目标**：展示构造器完成后发布不可变值对象。

```java
public class FinalFieldConstructionDemo {
    static final class Pair {
        final int left;
        final int right;
        Pair(int left, int right) { this.left = left; this.right = right; }
    }
    public static void main(String[] args) {
        Pair pair = new Pair(1, 2);
        System.out.println(pair.left + pair.right);
    }
}
```

预期或观察重点：

```text
3
```

## 17.49 高频面试题

1. `final` 可以修饰哪些语言元素？
2. final 局部变量的核心约束是什么？
3. final 变量必须在声明处初始化吗？
4. 什么是确定赋值？
5. 编译器如何判断 final 变量是否可能重复赋值？
6. 为什么分支中分别赋值一次可能合法？
7. 循环体中为什么可以每次声明新的 final 变量？
8. 什么是空白 final？
9. 实例空白 final 可以在哪里赋值？
10. 静态空白 final 可以在哪里赋值？
11. 每个构造器都必须直接给 final 字段赋值吗？
12. 构造器链如何帮助初始化 final 字段？
13. 实例初始化块能否给 final 字段赋值？
14. final 字段是否有默认值？
15. 为什么不能依赖默认值让 final 字段保持未赋值？
16. final 引用限制的是什么？
17. final 引用指向的对象能否修改？
18. final 数组的元素能否修改？
19. final 集合能否 add 或 clear？
20. 为什么 public static final 数组不是安全常量？
21. `Collections.unmodifiableList` 与 `List.copyOf` 有什么区别？
22. 不可修改集合中的元素对象是否一定不可变？
23. final 参数限制调用方变量吗？
24. final 参数能否阻止对象内部状态修改？
25. 显式 final 参数的主要工程价值是什么？
26. 什么是 effectively final？
27. Lambda 为什么允许捕获 effectively final 变量？
28. 重新赋值后变量还能被 Lambda 捕获吗？
29. 闭包捕获局部变量为什么不提供直接共享可变槽？
30. 需要闭包共享计数时应注意什么？
31. final 方法禁止什么？
32. final 方法能否被继承和调用？
33. private 方法为什么通常不需要 final？
34. static 方法加 final 的语义是什么？
35. 静态方法是重写还是隐藏？
36. final 类禁止什么？
37. final 类中的对象一定不可变吗？
38. final 类中的方法是否还需要逐个 final？
39. record 是否可以被继承？
40. record 组件是深不可变吗？
41. record 包含 List 时如何建立不可变边界？
42. enum 能否被普通类继承？
43. sealed 与 final 有什么区别？
44. sealed 直接子类为什么必须声明扩展策略？
45. `static final` 的两个修饰符分别表达什么？
46. `static final` 一定是编译期常量吗？
47. JLS 常量变量需要满足哪些条件？
48. 常量变量的类型为什么只允许基本类型或 String？
49. `static final Integer` 是常量变量吗？
50. 方法调用结果能否成为常量表达式？
51. 对象创建表达式能否成为常量表达式？
52. 字符串常量拼接何时会被编译期折叠？
53. 常量变量读取为什么可能不触发类初始化？
54. 什么是常量内联？
55. 调用方字节码如何使用被内联值？
56. 只更新常量库为什么消费者仍可能读旧值？
57. 常量值变化属于什么兼容性风险？
58. 如何避免跨模块常量旧值？
59. 什么值适合 public 编译期常量？
60. 运行时配置为什么不适合编译期常量？
61. 常量类为什么应有语义边界？
62. 什么是常量接口反模式？
63. 为什么不建议一个全局 Constants 类承载所有值？
64. 枚举相比字符串状态常量有什么优势？
65. 为什么不能用 enum.ordinal 作为稳定协议值？
66. 值对象何时比原始常量更好？
67. 单位为什么应进入类型而不是变量名？
68. final Map 引用是否保证 Map 不可变？
69. 如何公开不可变映射？
70. final 字段的 JMM 特殊语义解决什么问题？
71. final 字段语义是否让对象无需正确发布？
72. 什么是构造期间 this 逸出？
73. 构造器注册监听器为什么可能导致 this 逸出？
74. 构造器启动线程为什么危险？
75. 父类构造器调用可重写方法与 final 字段有何关系？
76. final 能否替代 synchronized？
77. final 能否替代 volatile？
78. final List 是否线程安全？
79. 不可变对象为什么天然便于并发共享？
80. final 是否一定带来性能提升？
81. JIT 能否优化非 final 方法调用？
82. 为什么不应使用反射修改 final 字段作为业务设计？
83. 框架反序列化 final 字段时应注意什么？
84. 静态 final 配置能否热更新？
85. WMS 固定协议长度适合用什么？
86. WMS 订单状态适合用什么？
87. WMS 重试次数适合常量还是运行时配置？
88. 多租户阈值为什么应使用注入配置？
89. 评审 public 常量时最重要的问题是什么？
90. 本章区分 final 与不可变的核心表达是什么？

## 17.50 易错点

### 易错点 1：把 final 理解成对象不可变

final 变量只限制变量再次赋值；引用对象状态仍可能变化。

### 易错点 2：认为 final 必须声明时赋值

空白 final 可在构造器、初始化块或控制流中确定赋值。

### 易错点 3：遗漏某个构造路径

每个正常完成的构造路径都必须保证实例 final 字段已赋值。

### 易错点 4：构造器重复赋值 final 字段

即使新值相同，语言规则也禁止再次赋值。

### 易错点 5：认为 final 字段没有默认值

准备或对象分配阶段仍有默认值，只是语言要求构造完成前显式确定赋值。

### 易错点 6：公开 final 数组

数组元素仍可被任意调用方修改。

### 易错点 7：公开 final 可变集合

final 固定引用，不固定内容。

### 易错点 8：把 unmodifiable 视图当成快照

源集合修改仍会透过视图显示。

### 易错点 9：认为 List.copyOf 深复制元素

它只建立集合结构快照，元素对象仍可能可变。

### 易错点 10：认为 final 参数保护实参

它只限制方法内部形参重新赋值。

### 易错点 11：为所有参数机械添加 final

大量噪音未必提高可读性，应遵循团队一致规则。

### 易错点 12：把 effectively final 当成隐式修饰符

它是基于赋值行为的编译期判定。

### 易错点 13：用单元素数组绕过 Lambda 限制

这引入隐藏可变状态，并发时尤其危险。

### 易错点 14：认为 final 方法不能调用

final 只禁止子类重写，不禁止继承和调用。

### 易错点 15：把子类同名 private 方法当重写

父类 private 方法对子类不可见。

### 易错点 16：把 static final 方法理解为动态绑定

静态方法仍按编译时类型隐藏。

### 易错点 17：认为 final 类自动不可变

类中仍可有 setter 和可变字段。

### 易错点 18：认为 record 自动深不可变

可变组件需要防御性复制。

### 易错点 19：认为 enum 是普通 final 类

枚举有特殊语言和实现规则，工程上只需确认外部不可扩展。

### 易错点 20：把 sealed 等同于 final

sealed 允许受控子类型，final 完全禁止。

### 易错点 21：把所有 static final 称为编译期常量

方法调用、包装类型和对象实例不满足常量变量条件。

### 易错点 22：认为包装类常量会同样内联

`Integer` 不是常量变量允许的基本类型或 String。

### 易错点 23：把运行时配置写成 public 常量

内联和发布演进会导致不可控旧值。

### 易错点 24：只更新常量 jar

消费者必须重新编译才能获得新内联值。

### 易错点 25：用常量接口共享值

实现关系失去类型语义并污染 API。

### 易错点 26：使用万能 Constants 类

不同领域和生命周期的值被混在一起。

### 易错点 27：用字符串魔法值表达状态

缺乏类型安全和穷尽检查。

### 易错点 28：使用 enum.ordinal 作为数据库值

调整枚举顺序会改变协议值。

### 易错点 29：用 double 常量表示金额

金额语义应使用 BigDecimal 或最小货币单位和值对象。

### 易错点 30：忽略单位

`MAX_WEIGHT = 100` 无法表达 kg、g 或 lb。

### 易错点 31：认为 final 字段让对象线程安全

其他可变成员和复合操作仍需同步。

### 易错点 32：构造期间发布 this

可能让其他线程看到半初始化对象。

### 易错点 33：构造器调用可重写方法

子类状态可能尚未初始化，且可能间接发布 this。

### 易错点 34：认为 final 替代 volatile

两者解决的问题和适用语义不同。

### 易错点 35：认为 final 替代锁

它不保证可变复合操作原子性。

### 易错点 36：为了性能滥加 final

设计意图应优先，性能需测量。

### 易错点 37：通过反射改 final 后期待所有读取更新

编译器和 JVM 可能已内联或优化，行为不可靠。

### 易错点 38：把配置中心值缓存为永不替换的 final

若需要热更新，应使用版本化不可变快照引用。

### 易错点 39：把所有常量公开

能保持私有的实现细节不要成为长期 API。

### 易错点 40：忽略二进制兼容

常量类型、字段存在性和值内联都影响独立部署模块。

## 17.51 工程实践建议

### 工程实践 1：默认使用构造器建立 final 字段

让对象创建后即满足不变量。

### 工程实践 2：汇聚构造器链

把校验和赋值集中到一个主构造器。

### 工程实践 3：优先不可变快照

集合字段使用 `copyOf`，避免外部别名修改。

### 工程实践 4：不公开可变数组

返回副本或只读抽象。

### 工程实践 5：区分引用不可变与对象不可变

代码评审逐层检查可变对象图。

### 工程实践 6：用 final 表达设计约束

不要把它仅当语法装饰。

### 工程实践 7：只在有价值时写 final 参数

保持团队规范一致，避免视觉噪音。

### 工程实践 8：闭包捕获保持简单

需要共享状态时显式建模对象和并发策略。

### 工程实践 9：模板整体方法可 final

阻止子类破坏固定流程。

### 工程实践 10：扩展点最小化

只开放真正需要重写的 protected 方法。

### 工程实践 11：语义完整类型可 final

降低继承破坏不变量的风险。

### 工程实践 12：record 组件做防御性复制

特别是集合、日期可变类型和数组。

### 工程实践 13：封闭层次按变化选择 sealed

需要受控扩展而非完全禁止时使用。

### 工程实践 14：只把稳定事实做编译期常量

协议固定值、数学事实和不可变限制更合适。

### 工程实践 15：配置使用实例对象

通过构造器注入并支持环境和租户差异。

### 工程实践 16：可热更新配置使用不可变快照

用原子引用替换整个配置对象，避免字段逐个修改。

### 工程实践 17：发布 public 常量前评审兼容性

确认变化策略和消费者重编译机制。

### 工程实践 18：跨模块常量变更触发全量重编译

在构建依赖图中自动完成。

### 工程实践 19：常量按领域分组

例如 ProtocolLimits、InventoryDefaults，而不是 GlobalConstants。

### 工程实践 20：优先枚举表达封闭状态

并定义显式持久化 code。

### 工程实践 21：使用值对象携带单位

重量、金额、时长和坐标避免裸数字。

### 工程实践 22：避免常量接口

通过限定类名或静态导入使用。

### 工程实践 23：公共集合常量使用不可修改实现

`List.of`、`Set.of`、`Map.of`。

### 工程实践 24：检查元素深层可变性

必要时复制元素或使用不可变元素类型。

### 工程实践 25：构造器中不发布 this

不要注册、启动线程或写入全局注册表。

### 工程实践 26：构造期间只调用 private/final 方法

避免动态绑定进入半初始化子类。

### 工程实践 27：正确发布对象

即使有 final 字段，也使用清晰安全的发布机制。

### 工程实践 28：共享可变成员使用并发控制

final 不能替代锁或并发容器。

### 工程实践 29：避免反射修改 final

框架适配应使用公开构造或工厂协议。

### 工程实践 30：以 JMH 验证性能猜测

不要假设 final 必然提升速度。

### 工程实践 31：状态持久化不依赖 ordinal

枚举使用稳定 code 并校验未知值。

### 工程实践 32：常量名称包含语义和单位

例如 `MAX_WEIGHT_KILOGRAMS`，更好的是 WeightLimit 类型。

### 工程实践 33：避免重复常量

定义唯一权威来源并避免复制粘贴。

### 工程实践 34：不要把数据库值硬编码成常量

由迁移、配置或领域字典管理。

### 工程实践 35：运行时阈值暴露来源和版本

便于审计配置为何生效。

### 工程实践 36：不可变对象修改返回新对象

使用 with 方法或工厂，而非内部突变。

### 工程实践 37：缓存 hashCode 仅用于真正不可变对象

否则缓存值会与 equals 状态不一致。

### 工程实践 38：审核序列化兼容

final 字段新增和默认值需要迁移策略。

### 工程实践 39：为常量编写边界测试

验证单位、范围和协议映射。

### 工程实践 40：文档说明常量稳定承诺

调用方必须知道它是协议事实还是默认值。

## 17.52 官方参考资料

- Java Language Specification 4.12.4：final variables 与 constant variables；
- Java Language Specification 15.28：constant expressions；
- Java Language Specification 16：definite assignment；
- Java Language Specification 17.5：final field semantics；
- Java Language Specification 13：二进制兼容；
- Java SE 26 `List.copyOf`、不可修改集合和 record 文档。

## 17.53 本章总结

```text
final 变量：赋值一次
final 引用：引用不换，对象仍可能变化
final 方法：不能重写
final 类：不能继承
↓
空白 final 依赖确定赋值
↓
effectively final 支持闭包捕获
↓
static final 不一定是常量变量
↓
常量变量可能被调用方内联
↓
可变化配置不能伪装成编译期常量
↓
final 字段有特殊初始化保证，但前提是正确构造且 this 不逸出
↓
不可变还需要封装、防御性复制和正确发布
```

## 17.54 面试口述版

`final` 的语义取决于修饰位置：变量和字段只能确定赋值一次，方法不能被重写，类不能被继承。final 引用只保证引用不再指向别的对象，不保证对象内部不可变，所以 final 数组和集合仍可能被修改。空白 final 可以在构造器、初始化块或静态初始化中赋值，只要编译器能证明所有正常路径上恰好完成一次。没有显式 final 但从未重新赋值的局部变量属于 effectively final，可以被 Lambda 捕获。`static final` 也不一定是编译期常量；只有基本类型或 String、使用常量表达式初始化的 final 变量才是常量变量，调用方可能把它内联到自身字节码，因此跨模块修改常量必须重新编译消费者。JMM 对正确构造且构造期间没有 `this` 逸出的 final 字段提供特殊初始化可见性，但这不等于对象整体线程安全，也不能替代锁、volatile、防御性复制和正确发布。
