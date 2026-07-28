# final 与常量设计

## 17.1 本章定位

前面的章节已经讨论了类与对象、继承、static 与类初始化等内容。
本章专门讨论 final 关键字的完整语义以及常量设计实践。

主要回答以下问题：

- final 可以修饰哪些元素？
- final 变量、字段、方法、类各自的语义是什么？
- final 引用是否等于对象不可变？
- 什么是空白 final？
- 什么是 effectively final？
- static final 一定是编译期常量吗？
- 编译期常量内联有什么风险？
- 如何设计安全的常量？

本章核心主线：

```
final 变量
→ 只能赋值一次

final 引用
→ 引用值不变，对象状态仍可能变化

final 方法
→ 不能被重写

final 类
→ 不能被继承

不可变对象
→ 不仅需要 final，还需要封装和防御性复制
```

本章暂不深入：

- static 成员、类初始化等内容见 16-static与类初始化.md。
- 不可变对象完整设计见 10-对象创建与不可变设计.md。
- final 字段的 Java 内存模型语义（安全发布）：见并发模块。
- Lambda 捕获变量的完整规则：见现代 Java 特性笔记。

## 17.2 final 的基本含义

final 可以修饰：

- 变量
- 字段
- 方法
- 类
- 方法参数

不同位置语义不同：

```
final 变量
→ 只能赋值一次
final 方法
→ 不能被子类重写
final 类
→ 不能被继承
```

## 17.3 final 局部变量

```
final int value = 10;
```

初始化后不能再次赋值：

```
value = 20;
```

编译失败。

**17.3.1 循环中的 final 变量**

```
for (int i = 0; i < 10; i++) {
    final int value = i;
}
```

每次循环都会创建一个新的 value 变量，因此合法。
但：

```
final int value;
for (int i = 0; i < 10; i++) {
    value = i;
}
```

通常无法编译，因为同一个 final 变量可能被多次赋值。

## 17.4 空白 final 与确定赋值

final 不一定要求声明时立刻赋值：

```
final int value;
if (condition) {
    value = 10;
} else {
    value = 20;
}
```

只要编译器能够确定：

```
所有正常路径上都会完成赋值
并且最多赋值一次
```

就可以编译。

**17.4.1 final 局部变量必须确定赋值**

错误：

```java
final int value;
if (condition) {
    value = 10;
}
System.out.println(value);
```

因为当 condition == false 时， value 没有初始化。

**17.4.2 空白 final 字段**

没有在声明时初始化的 final 字段称为空白 final。

```java
public class User {
    private final String userId;
    public User(String userId) {
        this.userId = userId;
    }
}
```

空白 final 的价值：

- 每个对象可以拥有不同值
- 初始化后不能再改变
- 支持不可变对象设计
- 强制构造过程完整

**17.4.3 静态空白 final**

```java
public class Config {
    private static final String ENVIRONMENT;
    static {
        ENVIRONMENT =
        System.getProperty("environment");
    }
}
```

静态空白 final 必须在声明处或静态代码块中完成赋值，不能在普通实例构造方法中赋值。static 字段的完整初始化规则见 16-static与类初始化.md。

## 17.5 final 引用

```java
final List<String> list =
new ArrayList<>();
```

允许：

```
list.add("Java");
list.clear();
```

不允许：

```java
list = new ArrayList<>();
```

原因：

```
final 限制的是引用变量中的引用值不能改变
不限制引用对象的内部状态
```

**17.5.1 final 引用不等于不可变对象**

```
final User user = new User();
user.setName("Java");
```

合法。
因为 user 仍然指向同一个 User 对象，只是对象内部字段变化。

真正的不可变对象还需要：

- 字段私有
- 状态不可修改
- 防御性复制
- 不暴露内部可变对象
- 修改操作返回新对象

**17.5.2 final 集合仍然可以修改**

```java
private final List<String> items =
new ArrayList<>();
```

表示：

```
items 不能重新指向另一个列表
```

不表示：

```
列表内容不能变化
```

如果希望列表不可修改：

```java
private final List<String> items;
public Order(List<String> items) {
    this.items = List.copyOf(items);
}
```

## 17.6 final 字段

实例字段可以声明为 final：

```java
public class User {
    private final String userId;
}
```

final 实例字段必须在对象构造完成前被赋值。

可以在：

- 声明处初始化
- 实例代码块中初始化
- 每个构造方法中初始化

**17.6.1 声明处初始化**

```java
private final String type = "DEFAULT";
```

每个对象的 type 初始化为 "DEFAULT" 。

**17.6.2 构造方法初始化**

```java
public class User {
    private final String userId;
    public User(String userId) {
        this.userId = userId;
    }
}
```

每个构造方法必须保证 userId 被赋值。
错误：

```
public User() {
}
```

如果没有其他初始化方式，会编译失败。

**17.6.3 实例代码块初始化**

```java
public class User {
    private final String id;
    {
        id = UUID.randomUUID().toString();
    }
}
```

所有构造方法执行前，实例代码块会初始化 id 。
但业务代码通常优先使用声明处或构造方法，意图更清晰。

**17.6.4 final 字段不能重复赋值**

```
public User(String userId) {
this.userId = userId;
this.userId = "other";
}
```

编译失败。

## 17.7 final 方法参数

```java
public void execute(final User user) {
}
```

方法内部不能重新给参数赋值：

```
user = new User();
```

编译失败。
但可以修改对象：

```
user.setName("Java");
```

因此：
final 参数只限制形参变量，不能保证传入对象不可变。

**17.7.1 final 参数的工程价值**

可能用于：

- 明确参数不会重新赋值
- 避免误操作
- 早期匿名内部类捕获
- 团队编码规范

现代 Java 中，局部变量和参数被 Lambda 捕获时只要求：

```
final 或 effectively final
```

不一定必须显式写 final 。

## 17.8 effectively final

有效 final 表示：
变量虽然没有显式使用 final，但初始化后从未重新赋值。
例如：

```java
String name = "Java";
Runnable task = () ->
System.out.println(name);
```

name 没有重新赋值，因此是 effectively final。
如果之后重新赋值：

```java
String name = "Java";
name = "JVM";
Runnable task = () ->
System.out.println(name);
```

编译失败。
Lambda 的完整捕获规则放在现代 Java 章节。
本章只记住：
effectively final 是行为上的不再赋值，而不是使用了 final 关键字。

## 17.9 final 方法

```java
public class Account {
    public final void validate() {
    }
}
```

子类不能重写：

```java
public class SavingsAccount extends Account {
    @Override
    public void validate() {
    }
}
```

编译失败。

**17.9.1 final 方法的适用场景**

适合：

- 固定核心安全校验
- 固定状态维护逻辑
- 模板方法中的整体流程
- 防止子类破坏父类不变量
- 明确行为不允许扩展

例如：

```java
public abstract class ImportTemplate {
    public final void execute() {
        validate();
        read();
        process();
        finish();
    }
    protected abstract void read();
    protected abstract void process();
    private void validate() {
    }
    private void finish() {
    }
}
```

**17.9.2 private 方法不需要 final**

private 方法对子类不可见，本身不能被重写。
因此：

```java
private final void validate() {
}
```

虽然可能语法允许，但 final 没有额外价值。
直接写：

```java
private void validate() {
}
```

即可。

## 17.10 final 类

```java
public final class String {
}
```

final 类不能被继承：

```java
public class CustomString extends String {
}
```

编译失败。

**17.10.1 final 类中的方法**

- final 类不能有子类，因此其中的实例方法不会被外部子类重写。
- 不需要为每个方法再机械添加 final。

**17.10.2 final 类的适用场景**

适合：

- 不可变值对象
- 安全敏感类型
- 工具类
- 不希望扩展的基础类型
- 类语义完整，不适合作为父类
- 防止继承破坏对象不变量

例如：

```java
public final class Money {
}
```

**17.10.3 final 类不等于不可变类**

```java
public final class User {
    private String name;
    public void setName(String name) {
        this.name = name;
    }
}
```

该类不能被继承，但对象仍然可变。

不可变需要同时控制：

- 字段
- 构造
- 修改方法
- 内部可变对象
- 对外暴露

## 17.11 static final 与常量变量

static final 表示类级变量只能赋值一次：

```java
public static final int TIMEOUT = 30;
```

通常用于定义常量。
推荐命名：

```
全大写
单词之间使用下划线
```

例如：

```java
public static final int DEFAULT_TIMEOUT = 30;
public static final String SYSTEM_USER = "SYSTEM";
```

static 修饰符的完整语义、类初始化规则见 16-static与类初始化.md；本章只关注 static final 中与常量相关的部分。

## 17.12 编译期常量

**17.12.1 static final 不一定是编译期常量**

编译期常量：

```java
public static final int TIMEOUT = 30;
```

运行时常量：

```java
public static final int TIMEOUT =
Integer.parseInt("30");
```

二者都是 static final ，但只有前者属于典型编译期常量。

**17.12.2 编译期常量的条件**

通常需要满足：

- 基本类型或 String
- 使用 final
- 在声明处初始化
- 初始化表达式是常量表达式

例如：

```java
public static final int A = 10;
public static final int B = A + 20;
public static final String NAME = "Java";
```

属于编译期常量。
但：

```java
public static final Integer VALUE = 10;
```

类型是包装类，不属于 Java 语言规范意义上的基本类型或 String 编译期常量。

编译期常量是否触发类初始化的完整讨论见 16-static与类初始化.md。

## 17.13 常量内联与二进制兼容风险

依赖模块 A：

```java
public class Constants {
    public static final int TIMEOUT = 30;
}
```

模块 B 编译时使用：

```
int timeout = Constants.TIMEOUT;
```

编译器可能把它直接编译成：

```
int timeout = 30;
```

后来只更新模块 A：

```
TIMEOUT = 60;
```

但没有重新编译模块 B，模块 B 可能仍然使用旧值：

```
30
```

因此：
跨模块可能变化的配置，不应设计为会被编译器内联的 public 编译期常量。

更适合：

- 配置中心
- 配置文件
- 方法访问
- 枚举
- 运行时对象

## 17.14 常量设计

**17.14.1 常量应表达稳定语义**

适合：

```java
public static final int MAX_RETRY_COUNT = 3;
public static final String SYSTEM_OPERATOR =
"SYSTEM";
```

不适合：

```java
public static final int CURRENT_TIMEOUT = 30;
```

如果该值可能需要通过配置动态修改，就不应硬编码为编译期常量。

**17.14.2 避免魔法值**

不推荐：

```
if (status == 30) {
}
```

可以定义：

```java
private static final int COMPLETED_STATUS = 30;
```

但如果值集合具有明确类型和行为，更推荐枚举：

```java
public enum OrderStatus {
    CREATED(10),
    PROCESSING(20),
    COMPLETED(30);
    private final int code;
}
```

**17.14.3 常量的可见性**

不是所有常量都应为 public：

```java
private static final int BATCH_SIZE = 100;
```

如果只在当前类内部使用，应保持 private。
只有构成公共协议的一部分时，才考虑 public。

**17.14.4 避免可变 public static final**

```java
public static final List<String> VALUES =
new ArrayList<>();
```

虽然引用不能重新赋值，但外部可以：

```
VALUES.clear();
VALUES.add("invalid");
```

这不是安全常量。
应使用：

```java
public static final List<String> VALUES =
List.of("A", "B");
```

或者使用私有字段加只读访问。

## 17.15 final 与不可变对象

一个典型不可变对象：

```java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    public Money(
    BigDecimal amount,
    Currency currency
    ) {
        this.amount =
        Objects.requireNonNull(amount);
        this.currency =
        Objects.requireNonNull(currency);
    }
    public BigDecimal amount() {
        return amount;
    }
    public Currency currency() {
        return currency;
    }
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
            "币种不一致"
            );
        }
        return new Money(
        amount.add(other.amount),
        currency
        );
    }
}
```

关键点：

- 类使用 final
- 字段使用 private final
- 构造时完成初始化
- 不提供 Setter
- 修改操作返回新对象
- 引用字段本身应不可变或进行防御性复制

**17.15.1 private final 仍然不够**

```java
public final class Order {
    private final List<String> items;
    public Order(List<String> items) {
        this.items = items;
    }
}
```

外部仍然可以：

```java
List<String> source =
new ArrayList<>();
Order order = new Order(source);
source.clear();
```

导致对象内部变化。
应使用：

```
this.items = List.copyOf(items);
```

并避免直接返回内部可变集合。

不可变对象完整设计见 10-对象创建与不可变设计.md。

## 17.16 建议实验

实验一：final 局部变量只能赋值一次

```java
public class FinalLocalDemo {
    public static void main(String[] args) {
        final int value = 10;
        // value = 20; // 编译错误
        System.out.println(value);
    }
}
```

预期：

```
10
```

实验二：final 变量延迟赋值

```java
public class FinalDeferredDemo {
    public static void main(String[] args) {
        final int value;
        if (args.length > 0) {
            value = 1;
        } else {
            value = 2;
        }
        System.out.println(value);
    }
}
```

验证编译器确定赋值路径的检查。
不带参数运行预期：

```
2
```

实验三：final 引用对象仍然可变

```java
public class FinalReferenceDemo {
    public static void main(String[] args) {
        final List<String> values =
        new ArrayList<>();
        values.add("A");
        values.add("B");
        System.out.println(values);
        // values = new ArrayList<>(); // 编译错误
    }
}
```

预期：

```
[A, B]
```

实验四：final 字段必须在构造完成前赋值

```java
public class FinalFieldDemo {
    static class User {
        private final String userId;
        public User(String userId) {
            this.userId = userId;
        }
        public String getUserId() {
            return userId;
        }
    }
    public static void main(String[] args) {
        User user = new User("U001");
        System.out.println(user.getUserId());
    }
}
```

预期：

```
U001
```

尝试取消注释空构造方法，观察编译错误。

实验五：static final 不一定是编译期常量

```java
public class RuntimeConstantDemo {
    static class Config {
        static {
            System.out.println(
            "Config initialized"
            );
        }
        static final int COMPILE_TIME = 10;
        static final int RUNTIME =
        Integer.parseInt("20");
    }
    public static void main(String[] args) {
        System.out.println(
        Config.COMPILE_TIME
        );
        System.out.println(
        Config.RUNTIME
        );
    }
}
```

观察：

- 访问 COMPILE_TIME 时可能不初始化 Config
- 访问 RUNTIME 时会初始化 Config

编译期常量是否触发类初始化的完整机制见 16-static与类初始化.md。

实验六：编译期常量内联验证

```java
public class InliningDemo {
    static class Constants {
        static {
            System.out.println(
            "Constants initialized"
            );
        }
        static final int VALUE = 10;
    }
    public static void main(String[] args) {
        System.out.println(Constants.VALUE);
    }
}
```

观察是否输出静态初始化信息（通常不输出，因为 VALUE 被内联）。

实验七：final 方法不能被重写

```java
public class FinalMethodDemo {
    static class Account {
        public final void validate() {
            System.out.println("validate");
        }
    }
    static class SavingsAccount extends Account {
        // @Override
        // public void validate() {} // 编译错误
    }
    public static void main(String[] args) {
        new SavingsAccount().validate();
    }
}
```

预期：

```
validate
```

取消注释子类的 validate 方法，观察编译失败。

实验八：不可变对象与防御性复制

```java
import java.util.*;

public class ImmutableOrderDemo {
    public static void main(String[] args) {
        List<String> source =
        new ArrayList<>(List.of("A", "B"));
        Order order = new Order(source);
        source.clear();
        System.out.println(order.getItems());
    }
    static final class Order {
        private final List<String> items;
        public Order(List<String> items) {
            this.items = List.copyOf(items);
        }
        public List<String> getItems() {
            return items;
        }
    }
}
```

预期：

```
[A, B]
```

## 17.17 高频面试题

本章建议保留以下问题：

1. final 可以修饰哪些元素？
2. final 局部变量能否延迟赋值？
3. final 变量的确定赋值规则是什么？
4. 什么是空白 final？
5. final 引用能否修改对象内容？
6. final 字段可以在哪里初始化？
7. final 字段不能重复赋值的原因是什么？
8. final 方法参数有什么限制？
9. 什么是 effectively final？
10. Lambda 为什么要求捕获变量为 effectively final？
11. final 方法能否被重写？
12. private 方法是否需要 final？
13. final 类能否被继承？
14. final 类是否一定不可变？
15. static final 一定是编译期常量吗？
16. 什么条件下变量是编译期常量？
17. 编译期常量内联有什么风险？
18. 如何避免魔法值？
19. public static final 集合一定不可变吗？
20. 常量应该使用什么可见性？

## 17.18 易错点

**误区一：final 变量必须在声明时初始化**

**错误。**

可以在构造方法、初始化代码块或确定赋值路径中完成初始化。

**误区二：final 引用表示对象不可变**

**错误。**

final 只限制引用变量不能重新赋值，不限制引用对象内部状态。

**误区三：final 集合内容不能修改**

**错误。**

final 只限制引用不能指向另一个集合，集合内容仍然可以增删。

**误区四：final 类一定是不可变类**

**错误。**

final 只禁止继承，类仍然可以提供 Setter 修改字段。

**误区五：private final List 一定不可修改**

**错误。**

private 限制外部直接访问，final 限制引用重新赋值，但列表内容仍然可能变化。

**误区六：所有 static final 字段都是编译期常量**

**错误。**

初始化表达式需要运行时计算时，只是不可再次赋值的类级字段，不是编译期常量。

**误区七：访问任何 static final 字段都会初始化类**

**错误。**

访问被内联的编译期常量可能不会初始化声明类。详见 16-static与类初始化.md。

**误区八：public static final List 是安全常量**

不一定。如果指向可变集合，任何调用方都可能修改其内容。

**误区九：编译期常量内联不会有风险**

**错误。**

跨模块使用编译期常量时，如果只更新声明模块而未重新编译使用模块，使用方可能仍持有旧值。

**误区十：final 方法参数保证传入对象不可变**

**错误。**

final 参数只限制方法内不能重新赋值形参变量，不影响对象内部状态。

**误区十一：final 字段可以在构造方法之后赋值**

**错误。**

final 实例字段必须在对象构造完成前赋值，构造方法结束后不能再赋值。

**误区十二：final 局部变量在循环中一定不合法**

**错误。**

每次循环迭代创建的是新的变量，因此循环内声明 final 局部变量合法。

## 17.19 工程实践建议

**17.19.1 常量只暴露必要范围**

类内使用：

```java
private static final int BATCH_SIZE = 100;
```

包内使用可考虑包级可见性。
只有对外协议需要时才使用：

```java
public static final
```

**17.19.2 可变集合不要公开暴露**

不推荐：

```java
public static final List<String> VALUES =
new ArrayList<>();
```

推荐：

```java
public static final List<String> VALUES =
List.of("A", "B");
```

或者：

```java
private static final List<String> VALUES =
List.of("A", "B");
public static List<String> values() {
    return VALUES;
}
```

**17.19.3 可能变化的配置不要使用编译期常量**

不推荐：

```java
public static final int TIMEOUT = 30;
```

如果业务上需要动态调整，应使用：

```
config.getTimeout();
```

或配置中心、环境变量、配置文件。

**17.19.4 不可变对象要进行防御性复制**

```java
public final class Order {
    private final List<OrderItem> items;
    public Order(List<OrderItem> items) {
        this.items = List.copyOf(items);
    }
    public List<OrderItem> getItems() {
        return items;
    }
}
```

还需确认 OrderItem 本身是否可变。
不可变对象完整设计见 10-对象创建与不可变设计.md。

**17.19.5 用枚举替代整数常量**

当值集合具有明确类型和行为时：

```java
public enum OrderStatus {
    CREATED(10),
    PROCESSING(20),
    COMPLETED(30);
    private final int code;
    OrderStatus(int code) {
        this.code = code;
    }
    public int getCode() {
        return code;
    }
}
```

优于：

```java
public static final int STATUS_CREATED = 10;
public static final int STATUS_PROCESSING = 20;
public static final int STATUS_COMPLETED = 30;
```

枚举提供类型安全、可遍历、可扩展行为。

**17.19.6 final 字段配合不可变设计**

推荐：

```java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
    }
}
```

final 字段保证赋值后不变，final 类防止子类破坏不变量。

**17.19.7 final 方法用于固定核心流程**

```java
public abstract class ImportTemplate {
    public final void execute() {
        validate();
        read();
        process();
        finish();
    }
    protected abstract void read();
    protected abstract void process();
    private void validate() {}
    private void finish() {}
}
```

模板方法用 final 固定流程，子类只填充可变步骤。

## 17.20 本章总结

final 知识链：

```
final 变量
→ 只能赋值一次
final 引用
→ 引用值不变
→ 对象状态仍可能变化
final 方法
→ 不能重写
final 类
→ 不能继承
不可变对象
→ 不仅需要 final
→ 还需要封装和防御性复制
```

常量知识链：

```
static final
→ 类级常量
→ 不一定编译期常量
编译期常量
→ 基本类型或 String
→ 声明处初始化
→ 常量表达式
→ 可能被内联
常量设计
→ 避免魔法值
→ 注意可见性
→ 可变集合需封装
→ 枚举优于整数常量
→ 可能变化的配置不要硬编码
```

面试口述版：

- final 修饰变量时表示只能赋值一次，修饰方法时表示不能被重写，修饰类时表示不能被继承。final 引用只保证引用值不再改变，并不代表引用对象不可变；真正的不可变对象还需要私有状态、构造时完整初始化、不提供修改行为以及对可变对象进行防御性复制。static final 通常用于定义常量，但不一定是编译期常量，只有基本类型或 String 在声明处用常量表达式初始化时才是编译期常量，可能被编译器内联，跨模块使用时需注意重新编译。常量设计应避免魔法值、注意可见性、可变集合不要公开暴露，值集合有明确类型时优先使用枚举，可能动态变化的配置不应硬编码为编译期常量。
