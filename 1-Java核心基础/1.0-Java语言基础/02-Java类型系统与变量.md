# Java 类型系统与变量

## 2.1 本章定位

这一章解决 Java 中最基础、但也最容易产生误解的问题：

- Java 是强类型语言是什么意思？
- 基本类型和引用类型的本质区别是什么？
- 变量、引用和对象分别是什么？
- 局部变量、成员变量、静态变量存在哪里？
- char 是不是一个完整的 Unicode 字符？
- 变量的作用域和遮蔽规则是什么？

本章重点是类型语义与变量规则。

类型转换与精度问题见 03。

final 和编译期常量见 17。

以下内容暂不深入：

- 自动装箱、包装类缓存：放在 String 与包装类型章节。
- 对象内存布局：放在 JVM 模块。
- 泛型类型系统：放在泛型章节。
- equals() 、 hashCode() ：放在 Object 章节。
- String 常量池：放在 String 章节。

## 2.2 Java 是静态类型、强类型语言

**2.2.1 静态类型**

Java 变量的类型在编译阶段就需要确定：

```
int count = 10;
String name = "Java";
```

编译器会根据变量类型检查：

- 可以赋什么值
- 可以执行什么操作
- 可以调用什么方法
- 方法参数是否匹配
- 返回值是否兼容
- 例如：

```
int count = "10";
```

编译时直接报错，不会等到运行时。
静态类型的意义
错误可以尽早在编译期发现
IDE 可以进行自动补全和重构
编译器可以进行更多类型检查和优化
代码契约更加明确

**2.2.2 强类型**

强类型表示：
不同类型之间不能随意混用，类型转换必须满足语言规则，必要时需要显式转换。
例如：

```
double value = 10; // 可以，int 自动提升为 double
int number = 10.5; // 不可以
int number = (int) 10.5; // 可以，但需要显式转换
```

需要注意：
“强类型”和“静态类型”不是同一个概念。
静态类型：类型检查主要发生在编译期。
强类型：类型之间的边界比较严格，不允许任意混用。

**2.2.3 var 是否让 Java 变成动态类型**

不会。

```
var name = "Java";
var count = 10;
```

编译器会推断：

```
String name = "Java";
int count = 10;
```

一旦推断完成，变量类型仍然固定：

```
var value = 10;
value = "Java"; // 编译错误
```

所以：
var 是局部变量类型推断，不是动态类型，也不是弱类型。

var 只能用于局部变量，不能用于：

- 成员变量
- 静态变量
- 方法参数
- 方法返回类型
- 没有初始化值的变量
- 错误示例：

```
var value; // 无法推断类型
```

## 2.3 Java 的两大类型体系

Java 类型可以分为：

```bash
Java 类型
├── 基本类型 Primitive Type
│ ├── byte
│ ├── short
│ ├── int
│ ├── long
│ ├── float
│ ├── double
│ ├── char
│ └── boolean
│
└── 引用类型 Reference Type
├── 类
├── 接口
├── 数组
├── 枚举
├── 注解
└── 记录类 Record
```

## 2.4 八种基本类型

**2.4.1 整数类型**

- 类型 位数 典型范围
- byte 8 -128～127
- short 16 -32768～32767
- int 32 -2³¹～2³¹-1
- long 64 -2⁶³～2⁶³-1
- 整数类型默认使用 int 。

```
int value = 100;
```

较大的整数字面量需要使用 L ：

```
long value = 3_000_000_000L;
```

推荐使用大写 L ，避免与数字 1 混淆：

```
long value = 10L;
```

不推荐：

```
long value = 10l;
```

**2.4.2 浮点类型**

- 类型 位数 有效精度
- float 32 大约 6～7 位十进制有效数字
- double 64 大约 15～16 位十进制有效数字

浮点数字面量默认是 double ：

```
double d = 1.5;
```

声明 float 需要添加 F ：

```
float f = 1.5F;
```

错误：

```
float f = 1.5;
```

因为右侧是 double ，不能自动窄化成 float 。

**2.4.3 字符类型 char**

char 占 16 位，取值范围：

```
0 ～ 65535
```

char 是无符号整数类型。

```java
char c1 = 'A';
char c2 = 65;
System.out.println(c1); // A
System.out.println(c2); // A
```

char 的本质
char 表示一个 UTF-16 code unit，而不是严格意义上的“一个完整 Unicode 字符”。
常见中文字符通常可以放进一个 char ：

```
char c = '中';
```

但部分 Unicode 字符需要两个 char 表示，例如某些 Emoji：

```java
String emoji = "😀";
System.out.println(emoji.length()); // 2
```

因此不能简单认为：
一个 char 永远等于一个人类可见字符。
更准确地说：
char 是一个 16 位 UTF-16 编码单元。
完整 Unicode 码点可通过：

```
int codePoint = emoji.codePointAt(0);
```

后面 String 章节再详细展开。

**2.4.4 boolean 类型**

```
boolean success = true;
boolean failed = false;
```

Java 中不能使用整数代替布尔值：

```
if (1) { // 编译错误
}
```

必须是：

```
if (success) {
}
```

需要避免下列说法：

- Java 的 boolean 固定占 1 位或 1 字节。
- Java 语言规范定义了 boolean 的语义，但没有简单规定其所有运行场景下统一占多少字节。字段、数组元素和局部变量在 JVM 中的具体表示可能不同。

本章只记住：
boolean 只有 true 和 false 两种值，不应机械回答固定内存大小。

## 2.5 基本类型与引用类型的区别

**2.5.1 基本类型变量直接保存值**

```
int a = 10;
int b = a;
b = 20;
```

此时：

```
a = 10
b = 20
```

因为赋值时复制的是数值 10 。

**2.5.2 引用类型变量保存引用值**

```
User user1 = new User();
User user2 = user1;
user2.setName("Java");
```

此时 user1 和 user2 指向同一个对象：

```bash
user1 ──┐
├──→ User 对象
user2 ──┘
```

通过 user2 修改对象状态后， user1 也能观察到变化。
需要准确区分三个概念：

```
变量 引用值 对象
user1 → 某个引用值 → User 实例
```

user1 是变量。
变量中保存的是引用值。
引用值指向对象。
变量不是对象。
引用也不是对象本身。

**2.5.3 引用变量可以为 null**

```
User user = null;
```

null 表示该引用变量当前没有指向任何对象。
调用：

```
user.getName();
```

会抛出：

```
NullPointerException
```

基本类型不能赋值为 null ：

```
int value = null; // 编译错误
```

包装类型可以：

```
Integer value = null;
```

但自动拆箱时存在 NPE 风险，后面包装类型章节展开。

**2.5.4 数组是引用类型**

即使数组元素是基本类型，数组本身仍然是对象：

```
int[] values = new int[3];
```

这里：

- values 是引用变量。
- new int[3] 创建数组对象。
- 数组对象内部保存三个 int 元素。

```bash
values
↓
int[] 数组对象
├── 0
├── 0
└── 0
```

因此数组可以调用：

```
values.length
```

也可以赋值为：

```
values = null;
```

## 2.6 字面量

字面量是直接写在代码中的值。

**2.6.1 整数字面量**

```
int decimal = 100; // 十进制
int binary = 0b1100100; // 二进制
int octal = 0144; // 八进制
int hex = 0x64; // 十六进制
```

结果都等于 100。
八进制字面量容易产生误解：

```
int value = 010;
```

这里不是十进制 10，而是八进制 10，即十进制 8。
实际业务代码不建议随意使用八进制字面量。

**2.6.2 数字下划线**

```
int amount = 1_000_000;
long orderId = 2026_0721_001L;
```

下划线只用于提高可读性，不影响数值。

不能放在：

- 数字开头
- 数字结尾
- 小数点旁边
- 类型后缀旁边
- 错误：

```
int a = _100;
int b = 100_;
double c = 1_.5;
long d = 100_L;
```

**2.6.3 字符字面量与字符串字面量**

```
char c = 'A';
String s = "A";
```

区别：

- 单引号表示 char
- 双引号表示 String
- char 是基本类型
- String 是引用类型
- 错误：

```
char c = "A";
String s = 'A';
```

**2.6.4 转义字符**

常见转义：

- 表达式 含义
- \n 换行
- \t 制表符
- \\ 反斜杠
- \" 双引号
- \' 单引号
- \r 回车
- \b 退格
- 示例：

```
String path = "C:\\Users\\Java";
```

## 2.7 变量的分类

按照声明位置，可以分为：

```bash
变量
├── 局部变量
├── 方法参数
├── 实例变量
└── 静态变量
```

**2.7.1 局部变量**

定义在方法、构造方法或代码块内部：

```java
public void execute() {
    int count = 10;
}
```

特点：

- 作用域局限于当前代码块
- 使用前必须显式初始化
- 没有默认值
- 生命周期随方法或代码块执行结束而结束
- 错误：

```java
public void execute() {
    int count;
    System.out.println(count); // 编译错误
}
```

**2.7.2 方法参数**

```java
public void execute(int count) {
}
```

方法被调用时，参数已经获得调用方传入的值，因此可以直接使用。
方法参数本质上也是局部变量。

**2.7.3 实例变量**

定义在类中，不使用 static ：

```java
public class User {
    private String name;
    private int age;
}
```

每个对象都有自己的一份实例变量：

```
User user1 = new User();
User user2 = new User();
```

user1.age 和 user2.age 彼此独立。

**2.7.4 静态变量**

使用 static 修饰：

```java
public class User {
    private static int count;
}
```

静态变量属于类，所有实例共享：

```bash
User 类
└── static count
user1 ──→ User 对象1
user2 ──→ User 对象2
```

不应该说“静态变量属于某个对象”。
更推荐通过类名访问：

```
User.count;
```

而不是：

```
user1.count;
```

即使语法允许，也容易误导读者。

## 2.8 默认值与初始化规则

**2.8.1 成员变量默认值**

实例变量和静态变量会获得默认值：

| 类型 | 默认值 |
|---|---|
| byte | 0 |
| short | 0 |
| int | 0 |
| long | 0L |
| float | 0.0F |
| double | 0.0D |
| char | '\u0000' |
| boolean | false |
| 引用类型 | null |

示例：

```java
public class User {
    private int age;
    private boolean active;
    private String name;
}
```

创建对象后：

```
age = 0
active = false
name = null
```

**2.8.2 局部变量没有默认值**

```java
public void test() {
    int value;
    System.out.println(value);
}
```

编译错误。
这是因为编译器要求局部变量满足“确定赋值”规则。

**2.8.3 条件分支下的确定赋值**

```java
int value;
if (condition) {
value = 1;
}
System.out.println(value); // 编译错误
```

因为当 condition == false 时， value 没有初始化。
正确：

```java
int value;
if (condition) {
value = 1;
} else {
value = 2;
}
System.out.println(value);
```

## 2.9 变量作用域与变量遮蔽

**2.9.1 作用域**

变量只能在声明它的作用域内使用：

```java
if (true) {
int value = 10;
}
System.out.println(value); // 编译错误
```

**2.9.2 局部变量遮蔽成员变量**

```java
public class User {
    private String name;
    public void setName(String name) {
        name = name;
    }
}
```

这里左右两边的 name 都是方法参数，实例变量没有被赋值。
正确：

```java
public void setName(String name) {
    this.name = name;
}
```

其中：

- this.name ：当前对象的实例变量
- name ：方法参数

**2.9.3 局部代码块不能重复声明同名变量**

```
int value = 10;
{
int value = 20; // 编译错误
}
```

Java 不允许局部作用域中的变量以这种方式遮蔽外层局部变量。
但是局部变量可以遮蔽成员变量。

## 2.10 本章总结

```
静态类型与强类型
→ 八种基本类型与引用类型
→ char 与 UTF-16
→ 字面量
→ 变量分类与默认值
→ 确定赋值
→ 作用域与变量遮蔽
```
