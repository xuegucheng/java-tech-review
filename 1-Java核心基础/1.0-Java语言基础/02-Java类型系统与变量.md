# Java 类型系统与变量

> 本章建立 Java 类型、值、变量、引用和对象之间的基础模型。重点是语言层语义，不把 JVM 内存实现、自动装箱、数值转换和精度问题混入本章。

## 2.1 本章定位

学完本章，应能够准确回答：

- Java 为什么是静态类型、强类型语言？
- 基本类型和引用类型的本质区别是什么？
- 八种基本类型分别表示什么？
- `boolean` 为什么不能与整数互相转换？
- `char` 为什么不等于一个完整 Unicode 字符？
- 变量、值、引用和对象分别是什么？
- `null` 是对象、值还是类型？
- 字段、局部变量、参数和数组元素的初始化规则有什么区别？
- 为什么局部变量没有默认值？
- 什么是确定赋值？
- `var` 是动态类型吗？
- `var` 可以出现在哪些位置？
- 变量的作用域、遮蔽和字段隐藏有什么区别？
- 引用赋值为什么不会复制对象？
- 为什么不应简单断言“局部变量都在栈上，对象都在堆上”？

本章只讨论类型系统与变量规则，不深入：

- 自动宽化、强制窄化和数值提升：见 `03-类型转换与精度陷阱.md`；
- 运算符求值规则：见 `04-运算符详解.md`；
- 参数传递：见 `07-参数传递机制.md`；
- 对象创建和不可变设计：见 `10-对象创建与不可变设计.md`；
- `final` 与编译期常量：见 `17-final与常量设计.md`；
- `equals()` 与引用相等：见 `18-equals与hashCode.md`；
- 自动装箱、包装类型缓存和拆箱风险：放在包装类型专题；
- JVM 栈帧、堆、对象布局和逃逸分析：放在 JVM 模块。

---

## 2.2 Java 类型系统概览

### 2.2.1 Java 是静态类型语言

静态类型表示：

> 变量、表达式和方法调用的类型关系主要在编译期确定并检查。

例如：

```java
int count = 10;
count = "Java"; // 编译错误
```

编译器在程序运行前就能发现类型不兼容。

静态类型带来的主要价值：

- 提前发现类型错误；
- 支持 IDE 补全、重构和静态分析；
- 明确 API 输入输出契约；
- 为重载选择和泛型检查提供依据；
- 为编译器和 JVM 优化提供类型信息。

### 2.2.2 Java 是强类型语言

“强类型”不是 Java 语言规范中的单一正式标签，但工程上通常用于表达：

- 不允许把任意值无条件当成另一种类型使用；
- 类型转换受到明确规则约束；
- 引用类型转换需要存在合法类型关系；
- `boolean` 不能充当整数；
- 数值窄化需要显式转换；
- 对象调用的方法受编译时类型约束。

例如：

```java
int value = 1;
// if (value) { } // 编译错误
```

Java 不会像部分语言那样自动把非零整数当成 `true`。

### 2.2.3 编译时类型与运行时类型

引用变量同时涉及两个概念：

```java
Animal animal = new Dog();
```

其中：

```text
编译时类型
→ Animal

运行时对象类型
→ Dog
```

编译时类型决定：

- 代码能否访问某个成员；
- 重载如何选择；
- 是否允许赋值和强制转换。

运行时类型参与：

- 实例方法动态绑定；
- `instanceof` 判断；
- 向下转型是否成功。

完整的多态和动态绑定规则见 `12-多态与动态绑定.md`。

### 2.2.4 Java 类型的总体分类

```text
Java 类型
├── 基本类型 Primitive Type
│   ├── boolean
│   ├── 整数类型
│   │   ├── byte
│   │   ├── short
│   │   ├── int
│   │   ├── long
│   │   └── char
│   └── 浮点类型
│       ├── float
│       └── double
└── 引用类型 Reference Type
    ├── 类类型
    ├── 接口类型
    ├── 数组类型
    └── 类型变量
```

此外，Java 语言还定义了特殊的 `null` 类型，但程序员不能直接把它写成变量声明类型。

---

## 2.3 值、变量、引用与对象

### 2.3.1 值

值是表达式求值后的结果。

示例：

```java
int count = 10;
String name = "Java";
```

这里：

- `10` 是 `int` 类型的值；
- `"Java"` 表达式产生一个 `String` 引用值；
- `count` 和 `name` 是变量。

### 2.3.2 变量

变量是具有名称或存储位置语义、能够保存某种类型值的实体。

```java
int count = 10;
```

可以拆成：

```text
变量名
→ count

声明类型
→ int

当前保存的值
→ 10
```

变量不是对象本身，也不是类型本身。

### 2.3.3 引用

引用类型变量保存的是引用值。

```java
User user = new User();
```

语言层可以理解为：

```text
user 变量
↓ 保存
引用值
↓ 指向或定位
User 对象
```

需要避免把引用简单等同于“裸内存地址”：

- Java 语言规范没有要求引用必须以某种固定地址形式暴露；
- JVM 可以使用句柄、压缩引用或其他内部表示；
- 垃圾收集器可能移动对象而不改变 Java 代码的引用语义。

因此，更准确的表述是：

> 引用值用于访问某个对象，具体底层表示由 JVM 实现决定。

### 2.3.4 对象

对象是类的实例或数组实例。

```java
User user = new User();
int[] values = new int[3];
```

这里创建了：

- 一个 `User` 对象；
- 一个 `int[]` 数组对象。

对象可以具有：

- 运行时类型；
- 字段状态；
- 实例方法行为；
- 对其他对象的引用。

### 2.3.5 一条声明中可能没有对象创建

```java
User first = null;
User second = first;
```

这段代码：

- 创建了两个引用变量；
- 没有创建 `User` 对象；
- 两个变量当前都保存空引用。

只有执行类似下面的表达式时，才通常发生对象创建：

```java
new User()
```

但字符串字面量、装箱、反射、反序列化和 JVM 内部机制也可能产生对象，因此“只有 `new` 才创建对象”同样不严谨。

---

## 2.4 基本类型与引用类型

### 2.4.1 基本类型

基本类型变量直接保存对应基本值：

```java
int age = 18;
boolean active = true;
```

Java 一共有八种基本类型：

```text
boolean
byte
short
int
long
char
float
double
```

### 2.4.2 引用类型

引用类型变量保存引用值：

```java
String name = "Java";
User user = new User();
int[] values = {1, 2, 3};
```

类、接口、数组和类型变量都属于引用类型。

### 2.4.3 核心区别

| 维度 | 基本类型 | 引用类型 |
|---|---|---|
| 变量保存内容 | 基本值 | 引用值 |
| 是否可以为 `null` | 不可以 | 可以 |
| 是否有实例方法 | 基本值本身没有对象方法 | 可通过引用访问对象成员 |
| 赋值效果 | 复制基本值 | 复制引用值 |
| `==` 含义 | 比较基本值 | 比较引用是否指向同一对象 |
| 泛型类型参数 | 不能直接使用 | 可以使用 |

例如：

```java
int first = 10;
int second = first;
second = 20;

System.out.println(first); // 10
```

基本类型赋值复制基本值。

引用类型：

```java
User first = new User();
User second = first;

second.name = "Java";

System.out.println(first.name); // Java
```

引用赋值复制引用值，两个变量因此可以访问同一个对象。

### 2.4.4 引用复制不等于对象复制

```java
User second = first;
```

只发生：

```text
复制 first 当前保存的引用值
↓
写入 second
```

没有自动创建新的 `User` 对象。

要创建独立对象，需要显式定义复制语义，例如：

- 拷贝构造器；
- 静态复制工厂；
- 映射转换；
- 显式创建新对象。

深拷贝和浅拷贝见 `07-参数传递机制.md` 与对象设计专题。

---

## 2.5 boolean 类型

### 2.5.1 基本语义

`boolean` 只有两个值：

```java
true
false
```

示例：

```java
boolean enabled = true;

if (enabled) {
    System.out.println("enabled");
}
```

### 2.5.2 boolean 不是整数类型

以下代码不能编译：

```java
boolean enabled = 1; // 编译错误
int value = true;    // 编译错误
```

也不能写：

```java
int count = 1;

// if (count) { } // 编译错误
```

必须显式构造布尔表达式：

```java
if (count != 0) {
    System.out.println("count is not zero");
}
```

### 2.5.3 boolean 的存储大小

Java 语言规范定义了 `boolean` 的值域和语义，但不要求普通 `boolean` 变量必须占用固定的 1 位、1 字节或 4 字节。

具体布局可能受到以下因素影响：

- JVM 实现；
- 字段布局；
- 数组表示；
- 对象对齐；
- JIT 优化。

因此，面试中不应绝对回答：

```text
boolean 一定占 1 字节
```

更准确的回答是：

> Java 语言层没有规定普通 boolean 变量的固定存储大小，具体表示由 JVM 实现决定。

---

## 2.6 整数类型

### 2.6.1 类型范围

| 类型 | 位数 | 最小值 | 最大值 |
|---|---:|---:|---:|
| `byte` | 8 | -128 | 127 |
| `short` | 16 | -32,768 | 32,767 |
| `int` | 32 | -2³¹ | 2³¹ - 1 |
| `long` | 64 | -2⁶³ | 2⁶³ - 1 |
| `char` | 16 | 0 | 65,535 |

`byte`、`short`、`int` 和 `long` 是有符号二进制补码整数类型。

`char` 也是整数类型，但它：

- 没有负数；
- 表示 UTF-16 代码单元；
- 不等于完整 Unicode 字符。

### 2.6.2 默认整数类型是 int

整数字面量默认通常是 `int`：

```java
int count = 100;
```

超出 `int` 范围时，需要使用 `long` 后缀：

```java
long population = 8_000_000_000L;
```

推荐使用大写 `L`，避免小写 `l` 与数字 `1` 混淆。

### 2.6.3 整数运算不会自动抛出溢出异常

```java
int value = Integer.MAX_VALUE;
value++;

System.out.println(value); // Integer.MIN_VALUE
```

整数溢出和 `Math.addExact()` 等检查方法放在 `03-类型转换与精度陷阱.md`。

### 2.6.4 byte 和 short 不一定更快

虽然 `byte`、`short` 占用的值域更小，但普通整数运算经常提升为 `int`。

因此，不能简单认为：

```text
byte 比 int 运算更快
```

`byte` 更常用于：

- 二进制协议；
- 文件和网络数据；
- 大型紧凑数组；
- 明确要求 8 位数据语义的场景。

普通业务计数和索引通常优先使用 `int`。

---

## 2.7 浮点类型

### 2.7.1 float 与 double

| 类型 | 位数 | 常见有效十进制精度 | 默认字面量 |
|---|---:|---:|---|
| `float` | 32 | 约 6～7 位 | 否 |
| `double` | 64 | 约 15～16 位 | 是 |

浮点字面量默认是 `double`：

```java
double rate = 0.1;
```

声明 `float` 时通常需要后缀：

```java
float rate = 0.1F;
```

### 2.7.2 浮点值不只包含普通有限数

`float` 和 `double` 还可以表示：

- 正无穷大；
- 负无穷大；
- `NaN`；
- 正零；
- 负零。

示例：

```java
double infinity = 1.0 / 0.0;
double notANumber = 0.0 / 0.0;
```

### 2.7.3 浮点数不适合表示所有精确十进制数

```java
System.out.println(0.1 + 0.2);
```

常见输出：

```text
0.30000000000000004
```

本章只记住：

> `float` 和 `double` 是二进制浮点类型，许多十进制小数不能被精确表示。

精度比较、`NaN`、舍入和金额计算见 `03-类型转换与精度陷阱.md`。

---

## 2.8 char、Unicode 与字符串

### 2.8.1 char 是 UTF-16 代码单元

`char` 是 16 位无符号整数类型。

```java
char letter = 'A';
char chinese = '中';
```

它表示一个 UTF-16 代码单元，而不是保证表示一个完整 Unicode 字符。

### 2.8.2 BMP 字符通常可由一个 char 表示

基本多文种平面中的许多字符可以直接放入一个 `char`：

```java
char value = '中';
```

但超出 BMP 的字符需要一对代理项：

```java
String emoji = "😀";

System.out.println(emoji.length()); // 2
```

这里：

- 用户看到一个 Unicode 字符；
- UTF-16 使用两个 `char` 代码单元；
- `String.length()` 返回代码单元数量，不是人类感知字符数量。

### 2.8.3 code point

需要按 Unicode 码点处理时，可以使用：

```java
String text = "😀";

int codePoint = text.codePointAt(0);
int count = text.codePointCount(0, text.length());

System.out.println(codePoint);
System.out.println(count); // 1
```

核心区别：

```text
char
→ UTF-16 代码单元

Unicode code point
→ Unicode 码点

用户感知字符
→ 可能由一个或多个码点组成
```

因此，即使按 code point 统计，也不一定等于用户眼中的“字符数”，因为组合字符和表情序列可能由多个码点构成。

### 2.8.4 字符串不是基本类型

`String` 是类，是引用类型：

```java
String language = "Java";
```

字符串字面量会产生 `String` 引用值。

不能因为字符串语法简洁，就把 `String` 当作第九种基本类型。

---

## 2.9 引用类型

### 2.9.1 类类型

```java
User user = new User();
String text = "Java";
```

`User` 和 `String` 都是类类型。

类类型引用可以指向：

- 当前类对象；
- 子类对象；
- `null`。

### 2.9.2 接口类型

```java
List<String> values = new ArrayList<>();
```

这里：

```text
变量编译时类型
→ List<String>

运行时对象类型
→ ArrayList<String>
```

接口类型常用于面向抽象编程。

### 2.9.3 数组类型

数组是对象，数组类型属于引用类型：

```java
int[] numbers = new int[3];
String[] names = new String[3];
```

数组具有：

- 固定长度；
- 运行时组件类型；
- `length` 字段；
- 默认初始化的元素；
- 继承自 `Object` 的对象行为。

数组变量可以为 `null`：

```java
int[] values = null;
```

但数组元素的类型可以是基本类型或引用类型。

### 2.9.4 类型变量

泛型中的类型参数在语言层属于类型变量：

```java
public class Box<T> {

    private T value;
}
```

这里的 `T` 不是具体类名，而是类型变量。

泛型完整规则放在泛型专题中。

### 2.9.5 引用类型变量可以为 null

```java
User user = null;
```

`null` 表示当前没有引用任何对象。

调用实例成员会抛出 `NullPointerException`：

```java
user.getName();
```

但以下操作合法：

```java
System.out.println(user == null); // true
```

---

## 2.10 null 类型与空引用

### 2.10.1 null 不是对象

`null`：

- 不是 `Object` 实例；
- 没有运行时类；
- 不能调用实例方法；
- 不能赋给基本类型变量。

错误示例：

```java
int value = null; // 编译错误
```

### 2.10.2 null 字面量具有特殊 null 类型

Java 语言定义了特殊的 `null` 类型。

`null` 字面量可以赋给引用类型：

```java
String text = null;
User user = null;
int[] values = null;
```

程序不能直接声明：

```java
null value; // 不存在这种语法
```

### 2.10.3 null 可以转换为任意引用类型

```java
Object value = null;
String text = null;
Runnable task = null;
```

因此，下面的重载调用可能产生歧义：

```java
static void print(String value) {
}

static void print(StringBuilder value) {
}

// print(null); // 编译错误：两个重载都适用，且互不更具体
```

重载选择见 `06-方法定义与重载.md`。

### 2.10.4 null 不是“空对象”

空字符串、空集合和 `null` 含义不同：

```java
String first = null;
String second = "";
List<String> third = List.of();
```

分别表示：

```text
null
→ 没有 String 引用

""
→ 存在 String 对象，内容长度为 0

空 List
→ 存在集合对象，元素数量为 0
```

API 设计应明确区分这些语义。

---

## 2.11 字面量

### 2.11.1 整数字面量

十进制：

```java
int decimal = 100;
```

二进制：

```java
int binary = 0b0110_0100;
```

十六进制：

```java
int hexadecimal = 0x64;
```

八进制：

```java
int octal = 0144;
```

八进制以单个 `0` 开头，容易误读，业务代码中通常不建议使用。

### 2.11.2 数字分隔符

下划线可以提高较长数字的可读性：

```java
int oneMillion = 1_000_000;
long cardNumber = 6222_0210_1234_5678L;
```

不能随意放置：

```java
// int first = _100;   // 编译错误
// int second = 100_;  // 编译错误
// double third = 1_.0; // 编译错误
```

下划线不会改变数值。

### 2.11.3 long 字面量

```java
long value = 3_000_000_000L;
```

如果没有 `L`，过大的十进制整数字面量可能在解析时直接失败。

推荐大写 `L`：

```java
long timeout = 30L;
```

### 2.11.4 浮点字面量

默认是 `double`：

```java
double first = 3.14;
```

`float` 使用 `F` 或 `f`：

```java
float second = 3.14F;
```

科学计数法：

```java
double distance = 1.5E8;
```

十六进制浮点字面量：

```java
double value = 0x1.0p3; // 8.0
```

其中 `p3` 表示乘以 2³。

### 2.11.5 char 字面量

```java
char first = 'A';
char newline = '\n';
char tab = '\t';
char quote = '\'';
char unicode = '\u4E2D';
```

字符字面量使用单引号，并且必须表示一个 UTF-16 代码单元。

以下代码不能编译：

```java
// char emoji = '😀';
```

因为该字符需要两个 UTF-16 代码单元。

### 2.11.6 String 字面量

```java
String language = "Java";
```

字符串字面量使用双引号。

文本块可以表达多行字符串：

```java
String json = """
        {
          "name": "Java"
        }
        """;
```

`String` 是引用类型，文本块也不会创建新的基本类型。

### 2.11.7 boolean 与 null 字面量

```java
boolean success = true;
boolean failed = false;
String text = null;
```

`true`、`false` 和 `null` 都是小写关键字形式，不能写成：

```java
// boolean value = TRUE;
// String text = NULL;
```

---

## 2.12 变量声明与分类

### 2.12.1 声明、初始化与赋值

声明：

```java
int count;
```

初始化：

```java
int count = 10;
```

后续赋值：

```java
count = 20;
```

三者不是同一概念。

### 2.12.2 实例字段

```java
public class User {

    private String name;
    private int age;
}
```

实例字段属于对象状态。

不同对象通常拥有各自的实例字段值：

```java
User first = new User();
User second = new User();
```

### 2.12.3 静态字段

```java
public class User {

    private static int count;
}
```

静态字段属于类级状态，由该类的对象共同访问。

完整规则见 `16-static与类初始化.md`。

### 2.12.4 局部变量

局部变量可以出现在：

- 方法体；
- 构造器体；
- 初始化块；
- 循环初始化部分；
- 代码块；
- `try` 资源声明。

示例：

```java
public void execute() {
    int count = 10;
}
```

局部变量的作用域和生命周期受所在语句块约束。

### 2.12.5 方法参数

```java
public void print(String message) {
    System.out.println(message);
}
```

`message` 是参数变量。

方法调用时，实参值会按值传给参数。完整参数传递规则见 `07-参数传递机制.md`。

### 2.12.6 构造器参数

```java
public User(String name) {
    this.name = name;
}
```

构造器参数同样是参数变量。

### 2.12.7 异常参数

```java
try {
    execute();
} catch (IllegalArgumentException exception) {
    System.out.println(exception.getMessage());
}
```

`exception` 是异常参数，其作用域位于对应 `catch` 块。

### 2.12.8 Lambda 参数

```java
Function<String, Integer> length = text -> text.length();
```

`text` 是 Lambda 参数。

Lambda、目标类型和变量捕获在现代 Java 特性专题中展开。

### 2.12.9 模式变量

现代 Java 中，模式匹配可以引入模式变量：

```java
if (value instanceof String text) {
    System.out.println(text.length());
}
```

`text` 只在编译器能够确定模式匹配成功的作用域中可用。

模式变量的作用域由控制流分析决定，不应简单理解为普通花括号作用域。

---

## 2.13 默认值与确定赋值

### 2.13.1 字段具有默认值

实例字段和静态字段在初始化表达式执行前会先获得默认值。

| 类型 | 默认值 |
|---|---|
| `byte`、`short`、`int`、`long` | `0` |
| `float`、`double` | `0.0` |
| `char` | `'\u0000'` |
| `boolean` | `false` |
| 引用类型 | `null` |

示例：

```java
public class DefaultValueDemo {

    private int count;
    private boolean enabled;
    private String name;

    public void print() {
        System.out.println(count);   // 0
        System.out.println(enabled); // false
        System.out.println(name);    // null
    }
}
```

### 2.13.2 数组元素具有默认值

```java
int[] counts = new int[3];
String[] names = new String[3];

System.out.println(counts[0]); // 0
System.out.println(names[0]);  // null
```

数组对象创建时，其元素会按组件类型获得默认值。

### 2.13.3 局部变量没有默认值

```java
public void print() {
    int count;
    System.out.println(count); // 编译错误
}
```

不是因为局部变量“底层一定没有任何比特值”，而是因为 Java 编译器不允许在无法证明已赋值时读取它。

### 2.13.4 确定赋值

确定赋值是编译器的数据流分析规则。

```java
int value;

if (condition) {
    value = 1;
} else {
    value = 2;
}

System.out.println(value);
```

无论条件如何，`value` 都会被赋值，因此可以读取。

错误示例：

```java
int value;

if (condition) {
    value = 1;
}

System.out.println(value); // 编译错误
```

当 `condition == false` 时，编译器无法证明 `value` 已赋值。

### 2.13.5 参数在方法入口处已确定赋值

```java
public void print(String message) {
    System.out.println(message);
}
```

方法参数在方法体开始执行时已经接收到实参值，因此被视为已确定赋值。

即使实参是 `null`，参数仍然是“已赋值”，只是赋到的值为空引用。

### 2.13.6 默认值不等于有效业务值

```java
private int status;
```

字段默认是 `0`，但 `0` 未必是合法业务状态。

工程上应通过：

- 构造器；
- 静态工厂；
- 字段初始化；
- 明确枚举；
- 校验逻辑；

确保对象创建完成后处于合法状态。

不要把 JVM 默认值当作业务初始化策略。

---

## 2.14 var 局部变量类型推断

### 2.14.1 var 不代表动态类型

```java
var name = "Java";
```

编译器会推断：

```text
name 的静态类型
→ String
```

之后不能赋入其他不兼容类型：

```java
var name = "Java";
// name = 100; // 编译错误
```

因此：

> `var` 只是省略局部变量声明中的显式类型，不会让 Java 变成动态类型语言。

### 2.14.2 var 必须有可推断的初始化器

```java
var count = 10;
var text = "Java";
var values = List.of("A", "B");
```

不能只声明不初始化：

```java
// var value; // 编译错误
```

也不能直接用 `null`：

```java
// var value = null; // 编译错误
```

因为编译器无法从 `null` 推断出唯一的具体引用类型。

### 2.14.3 var 不能用于字段

```java
public class User {

    // private var name = "Java"; // 编译错误
}
```

字段是类对外和对内结构的一部分，需要显式声明类型。

### 2.14.4 var 不能用于普通方法参数和返回类型

```java
// public var query() { }         // 编译错误
// public void print(var value) { } // 普通方法参数中编译错误
```

方法签名需要稳定、明确的类型。

### 2.14.5 var 可以用于循环变量

```java
for (var index = 0; index < 10; index++) {
    System.out.println(index);
}
```

增强 `for`：

```java
for (var value : values) {
    System.out.println(value);
}
```

### 2.14.6 var 可以用于 try-with-resources 局部变量

```java
try (var input = Files.newInputStream(path)) {
    System.out.println(input.read());
}
```

推断出的类型仍然是静态类型。

### 2.14.7 Lambda 参数中的 var

Java 11 起，Lambda 参数可以统一使用 `var`：

```java
BiFunction<Integer, Integer, Integer> add =
        (var left, var right) -> left + right;
```

所有参数必须保持一致：

```java
// (var left, right) -> left + right // 编译错误
```

Lambda 参数使用 `var` 的主要价值是允许添加注解：

```java
(var value) -> value.trim()
```

普通 Lambda 通常直接省略参数类型即可，不应机械添加 `var`。

### 2.14.8 var 不能直接推断无目标类型的 Lambda 和方法引用

```java
// var task = () -> System.out.println("run"); // 编译错误
// var parser = Integer::parseInt;             // 编译错误
```

Lambda 和方法引用需要目标函数式接口类型：

```java
Runnable task = () -> System.out.println("run");
Function<String, Integer> parser = Integer::parseInt;
```

### 2.14.9 var 不能与多个变量声明混用

```java
// var first = 1, second = 2; // 编译错误
```

一条 `var` 声明只能声明一个变量。

### 2.14.10 var 的使用边界

适合：

```java
var user = userRepository.findById(userId);
var values = new ArrayList<String>();
var result = calculateOrderAmount(order);
```

谨慎：

```java
var data = service.process(input);
```

当右侧表达式不能清楚体现类型和业务含义时，显式类型通常更易读。

核心原则：

> `var` 应减少重复，不应隐藏重要类型信息。

---

## 2.15 作用域、遮蔽与隐藏

### 2.15.1 作用域

作用域决定一个名字在源码中的可见范围。

```java
if (condition) {
    int value = 10;
}

System.out.println(value); // 编译错误
```

`value` 只在对应代码块内可见。

### 2.15.2 方法局部变量

```java
public void first() {
    int count = 10;
}

public void second() {
    int count = 20;
}
```

两个 `count` 位于不同方法作用域，不冲突。

### 2.15.3 局部变量遮蔽字段

```java
public class User {

    private String name;

    public void setName(String name) {
        name = name;
    }
}
```

这里左右两边的 `name` 都解析为参数变量，字段没有被赋值。

正确写法：

```java
public void setName(String name) {
    this.name = name;
}
```

其中：

```text
this.name
→ 当前对象的实例字段

name
→ 当前方法参数
```

### 2.15.4 Java 不允许嵌套局部变量重复声明

```java
int value = 10;

{
    // int value = 20; // 编译错误
}
```

局部变量不能在其作用域内部被另一个同名局部变量或参数重新声明。

但局部变量可以遮蔽字段：

```java
public class Demo {

    private int value;

    public void execute() {
        int value = 10;
        System.out.println(value);
    }
}
```

### 2.15.5 字段隐藏

父类和子类可以声明同名字段：

```java
class Parent {
    String name = "Parent";
}

class Child extends Parent {
    String name = "Child";
}
```

这称为字段隐藏，不是方法重写。

字段访问主要根据引用的编译时类型和显式限定确定：

```java
Parent value = new Child();

System.out.println(value.name); // Parent
```

字段隐藏与实例方法动态绑定的区别见 `11` 和 `12`。

### 2.15.6 模式变量的流作用域

```java
if (!(value instanceof String text)) {
    return;
}

System.out.println(text.length());
```

虽然 `text` 在 `if` 条件中声明，但编译器能够证明执行到后续语句时匹配一定成功，因此允许使用。

这种作用域称为基于控制流的作用域，不能只看花括号位置判断。

---

## 2.16 赋值语义与内存认知边界

### 2.16.1 基本类型赋值复制值

```java
int first = 10;
int second = first;

second = 20;

System.out.println(first); // 10
```

修改 `second` 不影响 `first`。

### 2.16.2 引用类型赋值复制引用值

```java
User first = new User();
User second = first;

second.name = "Java";

System.out.println(first.name); // Java
```

两个变量保存相同引用值，因此访问同一个对象。

### 2.16.3 给引用变量重新赋值不修改其他变量

```java
User first = new User();
User second = first;

second = new User();
```

现在：

```text
first
→ 原对象

second
→ 新对象
```

给 `second` 重新赋值，不会自动修改 `first`。

### 2.16.4 变量不可变不等于对象不可变

```java
final List<String> values = new ArrayList<>();
values.add("Java");
```

`final` 限制变量不能重新保存另一个引用值，但列表对象仍然可变。

完整规则见 `17-final与常量设计.md`。

### 2.16.5 不要把语言变量机械映射到栈和堆

常见简化说法是：

```text
局部变量在栈上
对象在堆上
```

它适合作为 JVM 入门图，但不是完整的 Java 语言保证。

原因包括：

- Java 语言规范主要规定可观察语义，不强制某个变量必须位于特定物理区域；
- JVM 可能进行标量替换；
- JIT 可能消除对象分配；
- 变量值可能进入寄存器；
- 逃逸分析可能改变对象的实际分配方式；
- 调试信息和优化后的机器执行状态可能不同。

在语言基础中应优先掌握：

```text
变量保存值
基本变量保存基本值
引用变量保存引用值
引用值用于访问对象
```

具体栈帧、堆、对象布局和优化行为放在 JVM 模块中讨论。

---

## 2.17 建议实验

### 实验一：基本类型赋值

```java
public class PrimitiveAssignmentDemo {

    public static void main(String[] args) {
        int first = 10;
        int second = first;

        second = 20;

        System.out.println(first);
        System.out.println(second);
    }
}
```

预期：

```text
10
20
```

用于证明基本类型赋值复制基本值。

### 实验二：引用赋值

```java
public class ReferenceAssignmentDemo {

    public static void main(String[] args) {
        User first = new User();
        User second = first;

        second.name = "Java";

        System.out.println(first.name);
    }

    static class User {
        String name;
    }
}
```

预期：

```text
Java
```

用于证明引用赋值不会复制对象。

### 实验三：重新赋值引用

```java
public class ReferenceReplaceDemo {

    public static void main(String[] args) {
        User first = new User("first");
        User second = first;

        second = new User("second");

        System.out.println(first.name);
        System.out.println(second.name);
    }

    static class User {

        String name;

        User(String name) {
            this.name = name;
        }
    }
}
```

预期：

```text
first
second
```

### 实验四：字段默认值与局部变量

```java
public class DefaultValueDemo {

    private int fieldCount;
    private String fieldName;

    public static void main(String[] args) {
        DefaultValueDemo demo = new DefaultValueDemo();

        System.out.println(demo.fieldCount);
        System.out.println(demo.fieldName);

        int localCount;
        // System.out.println(localCount); // 编译错误
    }
}
```

观察字段和局部变量规则不同。

### 实验五：确定赋值

```java
public class DefiniteAssignmentDemo {

    public static void main(String[] args) {
        boolean condition = args.length > 0;
        int value;

        if (condition) {
            value = 1;
        } else {
            value = 2;
        }

        System.out.println(value);
    }
}
```

删除 `else` 分支，再观察编译结果。

### 实验六：char 与 Unicode 码点

```java
public class UnicodeDemo {

    public static void main(String[] args) {
        String text = "😀";

        System.out.println(text.length());
        System.out.println(text.codePointCount(0, text.length()));
        System.out.println(Integer.toHexString(text.codePointAt(0)));
    }
}
```

预期：

```text
2
1
1f600
```

### 实验七：var 仍是静态类型

```java
public class VarDemo {

    public static void main(String[] args) {
        var value = "Java";

        System.out.println(value.toUpperCase());

        // value = 100; // 编译错误
    }
}
```

### 实验八：var 的非法位置

```java
public class InvalidVarDemo {

    // private var name = "Java"; // 编译错误

    public static void main(String[] args) {
        // var first;             // 编译错误
        // var second = null;     // 编译错误
        // var a = 1, b = 2;      // 编译错误
    }
}
```

### 实验九：局部变量遮蔽字段

```java
public class ShadowingDemo {

    private String name;

    public void wrong(String name) {
        name = name;
    }

    public void correct(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        ShadowingDemo demo = new ShadowingDemo();

        demo.wrong("Java");
        System.out.println(demo.name);

        demo.correct("JVM");
        System.out.println(demo.name);
    }
}
```

预期：

```text
null
JVM
```

### 实验十：数组元素默认值

```java
public class ArrayDefaultValueDemo {

    public static void main(String[] args) {
        int[] numbers = new int[2];
        boolean[] flags = new boolean[2];
        String[] names = new String[2];

        System.out.println(numbers[0]);
        System.out.println(flags[0]);
        System.out.println(names[0]);
    }
}
```

---

## 2.18 高频面试题

1. Java 为什么属于静态类型语言？
2. Java 的强类型主要体现在哪些方面？
3. 基本类型和引用类型有什么区别？
4. Java 有哪八种基本类型？
5. `boolean` 能否与整数互相转换？
6. Java 语言是否规定 `boolean` 一定占一个字节？
7. `byte`、`short`、`int` 和 `long` 的位数分别是多少？
8. 为什么普通整数计算通常优先使用 `int`？
9. `float` 与 `double` 有什么区别？
10. 为什么浮点字面量默认是 `double`？
11. `char` 是不是一个完整 Unicode 字符？
12. UTF-16 代码单元、Unicode 码点和用户感知字符有什么区别？
13. `String` 是基本类型吗？
14. 什么是引用类型？
15. 数组是基本类型还是引用类型？
16. 变量、引用和对象分别是什么？
17. 引用能否简单理解成对象的裸内存地址？
18. 引用赋值会不会复制对象？
19. `null` 是对象吗？
20. `null` 能否赋给基本类型变量？
21. 空字符串、空集合和 `null` 有什么区别？
22. 字段、局部变量和参数有什么区别？
23. 哪些变量具有默认值？
24. 为什么局部变量没有默认值？
25. 什么是确定赋值？
26. 参数是否需要在方法体中再次初始化？
27. `var` 是动态类型吗？
28. `var` 可以用于字段吗？
29. `var` 为什么不能直接初始化为 `null`？
30. `var` 能否直接接收 Lambda 表达式？
31. Lambda 参数为什么可以使用 `var`？
32. 什么是变量作用域？
33. 参数与字段同名时如何访问字段？
34. Java 是否允许在嵌套代码块中重新声明同名局部变量？
35. 字段隐藏和局部变量遮蔽有什么区别？
36. 模式变量的作用域为什么与普通局部变量不同？
37. 基本类型赋值和引用类型赋值分别复制什么？
38. 给一个引用变量重新赋值会影响其他引用变量吗？
39. `final` 引用是否表示对象不可变？
40. 为什么“局部变量都在栈上，对象都在堆上”不够严谨？

---

## 2.19 易错点

### 误区一：Java 变量在运行时可以随意改变类型

**错误。**

Java 是静态类型语言，变量类型在编译期确定。`var` 也只是类型推断，不会让变量动态变型。

### 误区二：Java 有九种基本类型，String 是第九种

**错误。**

Java 只有八种基本类型，`String` 是类类型。

### 误区三：boolean 本质上就是 0 和 1

**错误。**

Java 语言层的 `boolean` 只有 `true` 和 `false`，不能与整数直接互换。

### 误区四：boolean 一定占一个字节

**错误。**

Java 语言规范没有规定普通 `boolean` 变量的固定物理存储大小。

### 误区五：char 可以表示任意一个 Unicode 字符

**错误。**

`char` 只能表示一个 UTF-16 代码单元。部分 Unicode 字符需要两个 `char`。

### 误区六：String.length() 返回用户看到的字符数量

**错误。**

`String.length()` 返回 UTF-16 代码单元数量。

### 误区七：引用变量中保存的就是公开可见的对象内存地址

**不准确。**

引用的底层表示由 JVM 决定，Java 代码不应依赖其等同于裸地址。

### 误区八：声明引用变量就会创建对象

**错误。**

```java
User user;
```

只声明变量，没有创建 `User` 对象。

### 误区九：只有 new 才可能产生对象

**错误。**

字符串字面量、自动装箱、反射、反序列化等机制也可能产生或获得对象。

### 误区十：引用赋值会复制对象

**错误。**

引用赋值复制引用值，两个变量可能因此指向同一个对象。

### 误区十一：null 是一个特殊空对象

**错误。**

`null` 不是对象，没有运行时类，也不能调用实例方法。

### 误区十二：所有变量都有默认值

**错误。**

字段和数组元素具有默认值，局部变量必须满足确定赋值后才能读取。

### 误区十三：局部变量编译错误是因为 JVM 没给它分配内存

**错误。**

这是 Java 编译器的确定赋值规则，不能据此推断具体 JVM 内存布局。

### 误区十四：参数没有默认值，所以方法内不能直接使用

**错误。**

方法参数在方法入口已经接收到实参值，被视为已确定赋值。

### 误区十五：var 表示变量可以保存任意类型

**错误。**

`var` 推断出确定的静态类型，之后仍受该类型约束。

### 误区十六：var 可以用于任何省略类型的位置

**错误。**

它不能用于字段、普通方法参数、构造器参数和返回类型。

### 误区十七：var 可以直接初始化为 null

**错误。**

`null` 不能提供足够信息让编译器推断唯一类型。

### 误区十八：参数与字段同名时，赋值语句 name = name 会给字段赋值

**错误。**

两边通常都解析为参数，应使用 `this.name = name`。

### 误区十九：Java 允许在更小的局部代码块中重新声明外层同名局部变量

**错误。**

Java 不允许在局部变量作用域内重新声明同名局部变量或参数。

### 误区二十：局部变量一定在栈上，对象一定在堆上

**不严谨。**

这是便于入门的运行时模型，不是 Java 语言对优化后物理存储位置的绝对保证。

---

## 2.20 工程实践建议

1. 业务计数、索引和状态码通常优先使用 `int`，不要为了“省空间”机械使用 `byte` 或 `short`。
2. 只有明确需要 64 位范围时使用 `long`，并为字面量添加大写 `L`。
3. 不使用 `float` 或 `double` 表示要求精确的金额。
4. 处理 Unicode 文本时，不把 `char` 和 `String.length()` 直接等同于用户字符。
5. API 中明确区分 `null`、空字符串和空集合的语义。
6. 集合返回值通常优先返回空集合，而不是返回 `null`。
7. 不依赖字段默认值表达业务状态，对象创建时应主动建立合法状态。
8. 局部变量尽量靠近首次使用位置声明，缩小作用域。
9. 避免一行声明多个变量：

   ```java
   int first, second, third;
   ```

   更推荐分别声明并初始化。

10. 参数与字段同名时使用 `this.field = field`，但不要滥用 `this` 修饰所有字段访问。
11. `var` 用于消除明显重复，不用于隐藏关键业务类型。
12. 当方法返回类型不直观时，优先显式写出变量类型。
13. 不把引用日志、`identityHashCode()` 或调试地址当作稳定业务标识。
14. 需要复制对象时定义明确复制语义，不依赖引用赋值。
15. 不在语言基础层面过度推断栈、堆和寄存器位置；性能分析应以实际 JVM、JIT 和分析工具为准。
16. 使用枚举或专门值对象表达有限业务状态，避免用无语义整数承载复杂含义。

---

## 2.21 官方参考资料

- Java Language Specification：Types, Values, and Variables；
- Java Language Specification：Names；
- Java Language Specification：Blocks, Statements, and Patterns；
- Java Language Specification：Definite Assignment；
- Java Language Specification：Local Variable Type Inference；
- Unicode Standard 与 UTF-16；
- JEP 286：Local-Variable Type Inference；
- JEP 323：Local-Variable Syntax for Lambda Parameters。

本章内容主要是稳定语言规则。若未来 Java 对局部类型推断或模式变量语法进行扩展，应优先更新 `2.14` 和 `2.15`，不需要重写基本类型与引用语义。

---

## 2.22 本章总结

### 2.22.1 类型系统主线

```text
Java 类型
├── 基本类型
│   ├── boolean
│   ├── 整数类型
│   └── 浮点类型
└── 引用类型
    ├── 类
    ├── 接口
    ├── 数组
    └── 类型变量
```

### 2.22.2 变量和值

```text
基本类型变量
→ 保存基本值

引用类型变量
→ 保存引用值
→ 通过引用访问对象

引用赋值
→ 复制引用值
→ 不自动复制对象
```

### 2.22.3 初始化规则

```text
实例字段、静态字段、数组元素
→ 具有默认值

局部变量
→ 没有可直接读取的默认值
→ 读取前必须满足确定赋值

参数
→ 方法入口处已经接收到实参值
```

### 2.22.4 var

```text
var
→ 编译期局部变量类型推断
→ 仍然是静态类型
→ 必须具有可推断初始化器
→ 不能用于字段和普通方法签名
```

### 2.22.5 面试口述版

Java 是静态类型、强类型语言。Java 类型分为基本类型和引用类型：基本类型变量直接保存基本值，引用类型变量保存引用值，引用值用于访问对象。引用赋值复制的是引用值，不会自动复制对象，因此多个变量可能同时指向同一个对象。

Java 共有八种基本类型，`String` 不是基本类型。`char` 是 16 位 UTF-16 代码单元，不能保证表示完整 Unicode 字符。字段和数组元素具有默认值，局部变量必须通过编译器的确定赋值检查后才能读取。`var` 只是局部变量类型推断，编译器仍会推断出确定的静态类型，不能用于字段、普通方法参数或返回类型。

语言层应重点理解“变量保存值、引用用于访问对象”，不要把引用简单等同于裸内存地址，也不要把“局部变量在栈、对象在堆”当作优化后 JVM 的绝对物理布局规则。
