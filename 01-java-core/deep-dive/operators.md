# Java 运算符与表达式陷阱

> **本章定位：表达式规则收口页。** 只保留会影响结果、控制流或代码审查的运算符语义：数值提升、除法/溢出、引用相等、短路、位移、复合赋值、条件表达式和模式匹配。数值转换的完整规则见 [conversions-and-numeric-precision.md](conversions-and-numeric-precision.md)，业务相等性见 [equals-and-hashcode-contract.md](equals-and-hashcode-contract.md)。

## 4.1 表达式分析顺序

遇到一行复杂表达式，按下面顺序拆开：

```text
1. 操作数的静态类型
2. 运算符是否短路、是否改变状态
3. 一元/二元数值提升或拆箱
4. 求值顺序与异常点
5. 结果类型与溢出/精度风险
```

不要只凭变量声明类型或最终赋值类型推测中间结果。

## 4.2 算术运算与数值提升

`byte`、`short`、`char` 参与多数二元算术运算时会先提升为 `int`：

```java
byte left = 1;
byte right = 2;
var sum = left + right; // int
```

所以以下赋值需要显式转换：

```java
byte result = (byte) (left + right);
```

显式转换不是安全性证明，转换前仍要判断范围。更大的类型通常按二元数值提升规则参与计算；完整优先级和窄化规则统一看数值转换专题。

## 4.3 整数除法、余数与溢出

```java
System.out.println(5 / 2);    // 2
System.out.println(5 / 2.0);  // 2.5
System.out.println(-5 % 2);   // -1，符号跟随被除数
```

整数除零抛 `ArithmeticException`；浮点除零遵循 IEEE 754，可得到无穷大或 NaN。整数溢出默认回绕，不抛异常：

```java
int max = Integer.MAX_VALUE;
int wrapped = max + 1; // Integer.MIN_VALUE
```

金额、计数和边界计算要明确使用 `long`、`BigInteger`、`BigDecimal` 或显式溢出检查；不要把 `int` 的回绕当成业务规则。

## 4.4 `++`、`--` 与副作用

前缀先修改再取值，后缀先取旧值再修改：

```java
int value = 1;
int a = ++value; // value=2, a=2
int b = value++; // b=2, value=3
```

不要把多个自增、自减和数组下标塞入一条表达式。表达式可读性和求值顺序比节省一个临时变量更重要。

## 4.5 `==`：基本值与引用身份

```java
1 == 1                         // 基本值比较
new String("x") == new String("x") // false，通常是不同对象身份
```

对引用，`==` 只问两个引用值是否指向同一对象或同时为 `null`；它不是业务逻辑相等。字符串、值对象和领域对象的相等性见 [equals 与 hashCode 契约](equals-and-hashcode-contract.md)。不要依赖字符串驻留或包装类型缓存让 `==` “偶尔正确”。

## 4.6 短路逻辑与非短路位运算

```java
if (value != null && value.isReady()) {
    // 右侧只在非 null 时求值
}
```

`&&`、`||` 会短路；`&`、`|` 对布尔操作数仍会求值两侧：

```java
boolean ok = first != null && first.isValid(); // 安全守卫
boolean both = first != null & first.isValid(); // first 为 null 时仍可能 NPE
```

短路可以表达前置条件，但不要把带副作用的方法调用隐藏在复杂逻辑中。需要保证两侧都执行时，写成两条语句通常更清楚。

## 4.7 位运算与移位

整数位运算适合掩码、标志位和底层协议：

```java
int flags = READ | WRITE;
boolean writable = (flags & WRITE) != 0;
```

- `<<` 左移；
- `>>` 带符号右移，复制符号位；
- `>>>` 无符号右移，高位补零。

移位距离只使用右操作数的低位：`int` 有效位数按 32 取模，`long` 按 64 取模。因此 `value << 32` 对 `int` 并不是移动 32 位后的全零结果。移位也可能溢出，不等同于乘除法的无限精度版本。

## 4.8 复合赋值的隐式转换

```java
byte count = 1;
count += 100; // 等价于 count = (byte) (count + 100)
```

`+=`、`-=` 等会包含隐式转换，而 `count = count + 100` 通常因结果为 `int` 而不能直接赋给 `byte`。复合赋值不是“没有转换”，而是把转换藏在语法里；边界值计算应显式写出检查。

## 4.9 条件运算符 `?:` 的结果类型

条件运算符不只是缩短 `if`，它还要推导一个统一结果类型：

```java
var value = enabled ? 1 : 2L; // long
```

当两个分支一个是基本类型、一个是包装类型时，可能发生拆箱、提升或装箱；`null` 也会影响推导：

```java
Integer value = enabled ? 1 : null; // 结果可为 Integer
```

分支包含复杂转换或副作用时使用 `if`，不要为了“一行返回”牺牲可读性。

## 4.10 `instanceof` 与模式匹配

```java
if (candidate instanceof String text && !text.isBlank()) {
    System.out.println(text.length());
}
```

`null instanceof Type` 结果为 `false`。模式变量的作用域受 `&&`、否定和控制流支配；只有编译器能证明已匹配时才能使用。需要精确运行时类型时使用 `getClass() == Type.class`，需要允许子类型时使用 `instanceof`，两者的相等性语义不要混淆。

## 4.11 优先级与可读性

记不住优先级时加括号，尤其是：

```java
a + b << 1
mask & flag == 0
condition ? left : right
```

括号用于表达意图，而不是掩盖类型不清。条件复杂时拆成命名布尔变量，位掩码则明确每个标志的定义。

## 4.12 高频陷阱清单

| 误区 | 正确判断 |
| --- | --- |
| `byte + byte` 仍是 `byte` | 多数算术先提升为 `int` |
| 整数溢出会抛异常 | 默认回绕，需要显式检查 |
| `%` 总是数学模 | Java 余数符号跟随被除数 |
| `==` 能比较字符串内容 | 引用 `==` 比身份，内容用 `equals` |
| `&&` 和 `&` 一样 | `&&` 短路，`&` 不短路 |
| `>>` 总是补零 | `>>` 补符号位，`>>>` 才补零 |
| `+=` 与 `=` 加法完全相同 | 复合赋值包含隐式转换 |
| 三元表达式不会改变类型 | 分支会触发统一类型推导 |
| `instanceof` 能匹配 null | null 匹配结果为 false |

## 4.13 与其他权威页的边界

- 宽化、窄化、装箱和精度：见 [conversions-and-numeric-precision.md](conversions-and-numeric-precision.md)；
- 方法调用中的宽化/装箱/可变参数优先级：见 [methods-and-overloading.md](methods-and-overloading.md)；
- 对象身份与业务相等：见 [equals-and-hashcode-contract.md](equals-and-hashcode-contract.md)；
- `if`、`switch` 和循环结构：见 [control-flow.md](control-flow.md)。
