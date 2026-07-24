# final 与常量设计

## 8.15 final 的基本含义

final 可以修饰：

- 变量
- 字段
- 方法
- 类
- 方法参数
- 不同位置语义不同：

```
final 变量
→ 只能赋值一次
final 方法
→ 不能被子类重写
final 类
→ 不能被继承
```

## 8.16 final 局部变量

```
final int value = 10;
```

初始化后不能再次赋值：

```
value = 20;
```

编译失败。

**8.16.1 final 表示只能赋值一次**

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

**8.16.2 final 局部变量必须确定赋值**

错误：

```
final int value;
if (condition) {
value = 10;
}
System.out.println(value);
```

因为当 condition == false 时， value 没有初始化。

**8.16.3 循环中的 final 变量**

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

## 8.17 final 引用变量

```
final List<String> list =
new ArrayList<>();
```

允许：

```
list.add("Java");
list.clear();
```

不允许：

```
list = new ArrayList<>();
```

原因：

```
final 限制的是引用变量中的引用值不能改变
不限制引用对象的内部状态
```

**8.17.1 final 引用不等于不可变对象**

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

**8.17.2 final 集合仍然可以修改**

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

## 8.18 final 字段

实例字段可以声明为 final：

```java
public class User {
    private final String userId;
}
```

final 实例字段必须在对象构造完成前被赋值。

可以在：

- 1.声明处初始化
- 2.实例代码块中初始化
- 3.每个构造方法中初始化

**8.18.1 声明处初始化**

```java
private final String type = "DEFAULT";
```

每个对象的 type 初始化为 "DEFAULT" 。

**8.18.2 构造方法初始化**

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

**8.18.3 实例代码块初始化**

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

**8.18.4 final 字段不能重复赋值**

```
public User(String userId) {
this.userId = userId;
this.userId = "other";
}
```

编译失败。

## 8.19 static final 字段

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

**8.19.1 static final 不一定是编译期常量**

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

**8.19.2 编译期常量的条件**

通常需要满足：

- 基本类型或 String
- 使用 final
- 在声明处初始化
- 初始化表达式是常量表达式
- 例如：

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

**8.19.3 编译期常量内联风险**

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

## 8.20 空白 final

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

**8.20.1 静态空白 final**

```java
public class Config {
    private static final String ENVIRONMENT;
    static {
        ENVIRONMENT =
        System.getProperty("environment");
    }
}
```

静态空白 final 必须在：

- 声明处
- 静态代码块
完成赋值。
不能在普通实例构造方法中赋值，因为它属于类级字段。

## 8.21 final 方法参数

```
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

**8.21.1 final 参数的工程价值**

可能用于：

- 明确参数不会重新赋值
- 避免误操作
- 早期匿名内部类捕获
- 团队编码规范
- 现代 Java 中，局部变量和参数被 Lambda 捕获时只要求：

```
final 或 effectively final
```

不一定必须显式写 final 。

## 8.22 effectively final

有效 final 表示：
变量虽然没有显式使用 final，但初始化后从未重新赋值。
例如：

```
String name = "Java";
Runnable task = () ->
System.out.println(name);
```

name 没有重新赋值，因此是 effectively final。
如果之后重新赋值：

```
String name = "Java";
name = "JVM";
Runnable task = () ->
System.out.println(name);
```

编译失败。
Lambda 的完整捕获规则放在现代 Java 章节。
本章只记住：
effectively final 是行为上的不再赋值，而不是使用了 final 关键字。

## 8.23 final 方法

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

**8.23.1 final 方法的适用场景**

适合：

- 固定核心安全校验
- 固定状态维护逻辑
- 模板方法中的整体流程
- 防止子类破坏父类不变量
- 明确行为不允许扩展
- 例如：

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

**8.23.2 private 方法不需要 final**

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

**8.23.3 static final 方法**

静态方法不参与重写，只能被隐藏。
因此静态方法通常不需要使用 final 防止重写。

## 8.24 final 类

```
public final class String {
}
```

final 类不能被继承：

```java
public class CustomString extends String {
}
```

编译失败。

**8.24.1 final 类中的方法**

final 类不能有子类，因此其中的实例方法不会被外部子类重写。
不需要为每个方法再机械添加 final。

**8.24.2 final 类的适用场景**

适合：

- 不可变值对象
- 安全敏感类型
- 工具类
- 不希望扩展的基础类型
- 类语义完整，不适合作为父类
- 防止继承破坏对象不变量
- 例如：

```
public final class Money {
}
```

**8.24.3 final 类不等于不可变类**

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

## 8.25 final 与不可变对象

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

**8.25.1 private final 仍然不够**

```java
public final class Order {
    private final List<String> items;
    public Order(List<String> items) {
        this.items = items;
    }
}
```

外部仍然可以：

```
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

## 8.26 static 与对象生命周期

实例字段通常随对象生命周期存在。
静态字段通常随类及其类加载器生命周期存在。
可以简化为：

```
实例字段
→ 对象可达时存在
→ 对象不可达后可能被回收
静态字段
→ 类初始化后存在
→ 通常持续到类加载器被卸载
```

因此静态字段可能长期持有对象引用：

```java
private static final List<Object> CACHE =
new ArrayList<>();
```

如果持续添加：

```
CACHE.add(object);
```

这些对象会一直被静态引用持有，可能无法被垃圾回收。

这会产生：

- 内存泄漏
- 类加载器泄漏
- 缓存无限增长
- 测试污染

**8.26.1 静态集合必须考虑生命周期**

```java
private static final Map<String, Object> CACHE =
new ConcurrentHashMap<>();
```

至少需要考虑：

- 最大容量
- 过期策略
- 清理机制
- 并发安全
- 弱引用需求
- 类加载器边界
- 是否真的需要全局缓存

## 8.27 static 与线程安全

static 只表示类级共享，不表示线程安全。

```java
public class Counter {
    private static int count;
    public static void increment() {
        count++;
    }
}
```

多个线程同时执行：

```
Counter.increment();
```

可能发生丢失更新。
因为：

```
count++;
```

不是原子操作。
可以拆成：

```
读取 count
加 1
写回 count
```

多个线程可能互相覆盖。

解决方式可能包括：

- synchronized
- AtomicInteger
- LongAdder
- 锁
- 避免共享状态
具体放在并发模块。
本章只记住：
static 增加了共享范围，但不提供任何并发安全保证。

## 8.28 static 与多态

实例方法适合表达可替换行为：

```
PaymentProcessor processor =
new AlipayProcessor();
processor.pay();
```

静态方法不具备这种动态替换能力。

如果业务需要：

- 多种实现
- 测试替身
- 运行时选择
- 策略切换
- 依赖注入
不应将核心能力设计成静态方法。
不推荐：

```
PaymentUtils.pay(order);
```

更适合：

```
paymentProcessor.pay(order);
```

前者是固定类调用，后者可以通过接口实现多态。

## 8.29 静态工厂方法

静态方法也可以承担对象创建职责：

```java
public class Order {
    private final String orderNo;
    private OrderStatus status;
    private Order(
    String orderNo,
    OrderStatus status
    ) {
        this.orderNo = orderNo;
        this.status = status;
    }
    public static Order create(
    String orderNo
    ) {
        return new Order(
        orderNo,
        OrderStatus.CREATED
        );
    }
}
```

调用：

```
Order order = Order.create(orderNo);
```

静态工厂方法可以：

- 使用有意义的名称
- 隐藏构造细节
- 返回缓存对象
- 返回子类型
- 控制实例数量
- 执行参数校验
- 区分不同创建语义

## 8.30 静态内部类概览

类中可以声明静态嵌套类：

```java
public class Outer {
    public static class Nested {
    }
}
```

静态嵌套类：

- 不持有外部类实例引用
- 可以独立创建
- 只能直接访问外部类静态成员
- 常用于 Builder、结果对象、辅助结构
- 创建：

```
Outer.Nested nested =
new Outer.Nested();
```

内部类完整规则放在高级语言特性笔记。

**8.30.1 静态内部类单例思想**

```java
public class Singleton {
    private Singleton() {
    }
    private static class Holder {
        private static final Singleton INSTANCE =
        new Singleton();
    }
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

其思路是：

- 外部类初始化时不一定初始化 Holder
- 第一次调用 getInstance() 时主动使用 Holder
- JVM 保证类初始化过程的线程安全性
- INSTANCE 只初始化一次
单例完整讨论放在设计模式和并发模块。

## 8.31 类初始化的线程安全性

JVM 会保证同一个类的初始化过程由一个线程执行。
当多个线程同时首次使用某个类时：

```
一个线程执行类初始化逻辑
其他线程等待初始化完成
```

因此静态初始化常用于安全发布不可变对象：

```java
private static final Config INSTANCE =
new Config();
```

但需要注意：
初始化过程本身不应阻塞过久
不应形成类初始化循环依赖
不应在静态代码块中获取复杂外部锁
不应执行不可控网络调用

否则可能出现：

- 启动卡顿
- 初始化死锁
- 初始化失败
- 类永久不可用

## 8.32 类初始化循环依赖

示例：

```java
public class A {
    static int value = B.value + 1;
}
public class B {
    static int value = A.value + 1;
}
```

访问：

```
System.out.println(A.value);
```

可能得到不符合直觉的结果。
简化过程：

```
开始初始化 A
↓
A.value 先为 0
↓
读取 B.value
↓
开始初始化 B
↓
B.value 先为 0
↓
读取正在初始化中的 A.value
↓
得到默认值 0
↓
B.value = 1
↓
A.value = 2
```

因此可能出现：

```
A.value = 2
B.value = 1
```

核心原则：
避免静态初始化之间形成复杂双向依赖。

## 8.33 常量设计

**8.33.1 常量应表达稳定语义**

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

**8.33.2 避免魔法值**

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

**8.33.3 常量的可见性**

不是所有常量都应为 public：

```java
private static final int BATCH_SIZE = 100;
```

如果只在当前类内部使用，应保持 private。
只有构成公共协议的一部分时，才考虑 public。

**8.33.4 避免可变 public static final**

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

## 8.34 单例与静态工具类的区别

静态工具类：

```java
public final class Calculator {
    private Calculator() {
    }
    public static int add(int a, int b) {
        return a + b;
    }
}
```

特点：

- 没有对象身份
- 通常无状态
- 无多态
- 直接通过类调用
- 单例对象：

```java
public final class ConfigService {
    private static final ConfigService INSTANCE =
    new ConfigService();
    private ConfigService() {
    }
    public static ConfigService getInstance() {
        return INSTANCE;
    }
}
```

特点：

- 仍然是对象
- 可以实现接口
- 可以有实例字段
- 可以参与多态
- 生命周期由静态引用控制
但单例仍然具有全局状态和测试耦合风险。
在 Spring 项目中，通常更倾向让容器管理单例对象，而不是手写全局单例。

## 8.35 建议实验

实验一：静态字段共享

```java
public class StaticFieldDemo {
    public static void main(String[] args) {
        User user1 = new User();
        User user2 = new User();
        user1.increment();
        user2.increment();
        System.out.println(User.getCount());
    }
    static class User {
        private static int count;
        void increment() {
            count++;
        }
        static int getCount() {
            return count;
        }
    }
}
```

预期：

```
2
```

实验二：静态方法不能访问实例成员

```java
public class StaticMethodDemo {
    private String name = "Java";
    public static void print() {
        // System.out.println(name);
        // 编译错误
    }
}
```

修改为：

```java
public static void print(
StaticMethodDemo demo
) {
    System.out.println(demo.name);
}
```

验证可以通过对象引用访问实例字段。
实验三：静态初始化顺序

```java
public class StaticInitializationDemo {
    private static int first =
    print("first", 1);
    static {
        System.out.println("block 1");
    }
    private static int second =
    print("second", 2);
    static {
        System.out.println("block 2");
    }
    private static int print(
    String name,
    int value
    ) {
        System.out.println(name);
        return value;
    }
    public static void main(String[] args) {
        System.out.println("main");
    }
}
```

预期：

```
first
block 1
second
block 2
main
```

实验四：父子类初始化顺序

```java
public class ParentChildInitializationDemo {
    public static void main(String[] args) {
        new Child();
    }
    static class Parent {
        static {
            System.out.println("父类静态");
        }
        {
            System.out.println("父类实例");
        }
        Parent() {
            System.out.println("父类构造");
        }
    }
    static class Child extends Parent {
        static {
            System.out.println("子类静态");
        }
        {
            System.out.println("子类实例");
        }
        Child() {
            System.out.println("子类构造");
        }
    }
}
```

预期：

```
父类静态
子类静态
父类实例
父类构造
子类实例
子类构造
```

实验五：编译期常量不触发初始化

```java
public class ConstantInitializationDemo {
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

观察是否输出静态初始化信息。
然后改为：

```
static final int VALUE =
Integer.parseInt("10");
```

再次观察差异。
实验六：通过子类访问父类静态字段

```java
public class ParentStaticFieldDemo {
    static class Parent {
        static {
            System.out.println("Parent");
        }
        static int value = 10;
    }
    static class Child extends Parent {
        static {
            System.out.println("Child");
        }
    }
    public static void main(String[] args) {
        System.out.println(Child.value);
    }
}
```

预期：

```
Parent
10
```

通常不会输出 Child。
实验七：final 引用对象仍然可变

```java
public class FinalReferenceDemo {
    public static void main(String[] args) {
        final List<String> values =
        new ArrayList<>();
        values.add("A");
        values.add("B");
        System.out.println(values);
        // values = new ArrayList<>();
        // 编译错误
    }
}
```

预期：

```
[A, B]
```

实验八：static final 不一定是编译期常量

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
- 实验九：静态初始化循环依赖

```java
public class CircularInitializationDemo {
    static class A {
        static int value = B.value + 1;
    }
    static class B {
        static int value = A.value + 1;
    }
    public static void main(String[] args) {
        System.out.println(A.value);
        System.out.println(B.value);
    }
}
```

观察结果并分析默认值参与初始化的过程。
实验十：静态代码块初始化失败

```java
public class InitializationFailureDemo {
    static class Config {
        static {
            if (System.currentTimeMillis() > 0) {
                throw new RuntimeException(
                "初始化失败"
                );
            }
        }
    }
    public static void main(String[] args) {
        for (int i = 0; i < 2; i++) {
            try {
                System.out.println(
                Config.class
                );
                Class.forName(
                Config.class.getName(),
                true,
                Config.class
                .getClassLoader()
                );
            } catch (Throwable throwable) {
                System.out.println(
                throwable.getClass()
                .getSimpleName()
                );
            }
        }
    }
}
```

观察第一次和后续使用失败类时的异常差异。

## 8.36 高频面试题

本章建议保留以下问题：

- 1. static 表示什么？
- 2.静态字段和实例字段有什么区别？
- 3.静态字段是每个对象一份吗？
- 4.静态方法为什么不能直接访问实例字段？
- 5.静态方法中为什么不能使用 this ？
- 6.实例方法能否访问静态字段？
- 7.静态方法能否被重写？
- 8.什么是静态方法隐藏？
- 9.为什么建议通过类名调用静态方法？
- 10.静态代码块什么时候执行？
- 11.静态代码块会执行几次？
- 12.静态字段初始化表达式和静态代码块的顺序是什么？
- 13.静态字段是否有默认值？
- 14.类加载和类初始化有什么区别？
- 15.哪些行为会触发类初始化？
- 16.访问编译期常量为什么可能不触发类初始化？
- 17.访问非编译期常量为什么会触发类初始化？
- 18.通过子类访问父类静态字段，会初始化子类吗？
- 19.创建对象数组是否会初始化元素类型？
- 20.获取 SomeClass.class 是否一定会初始化类？
- 21.父类和子类的静态初始化顺序是什么？
- 22.完整的父子类初始化顺序是什么？
- 23.静态代码块抛异常会发生什么？
- 24.什么是类初始化循环依赖？
- 25. final 可以修饰哪些元素？
- 26.final 局部变量能否延迟赋值？
- 27.final 引用能否修改对象内容？
- 28.final 字段可以在哪里初始化？
- 29.什么是空白 final？
- 30.static final 一定是编译期常量吗？
- 31.什么条件下变量是编译期常量？
- 32.编译期常量内联有什么风险？
- 33.final 方法能否被重写？
- 34.private 方法是否需要 final？
- 35.final 类能否被继承？
- 36.final 类是否一定不可变？
- 37.final 字段是否能保证对象不可变？
- 38.什么是 effectively final？
- 39.Lambda 为什么要求捕获变量为 effectively final？
- 40.工具类为什么使用私有构造方法？
- 41.工具类为什么常声明为 final？
- 42.static 是否能保证线程安全？
- 43.静态可变字段有什么风险？
- 44.public static final 集合一定不可变吗？
- 45.静态工具类和单例对象有什么区别？
- 46.为什么复杂业务逻辑不适合全部设计成静态方法？
- 47.为什么静态缓存容易造成内存泄漏？
- 48.JVM 如何保证类初始化的线程安全？

## 8.37 易错点

**误区一：static 成员属于所有对象**

不准确。
static 成员属于类，只是所有对象都可以访问同一个类级成员。

**误区二：每创建一个对象，静态字段都会复制一份**

**错误。**

静态字段通常只有类级一份。

**误区三：静态方法不能访问任何实例成员**

不准确。
静态方法不能直接访问实例成员，但可以通过明确的对象引用访问。

**误区四：静态方法可以被子类重写**

**错误。**

子类同名静态方法属于隐藏，不参与运行时多态。

**误区五：静态代码块在程序启动时一定执行**

**错误。**

只有类被主动初始化时才执行。
未使用的类可能始终不会初始化。

**误区六：类被加载就一定已经初始化**

**错误。**

加载、链接和初始化是不同阶段。

**误区七：所有 static final 字段都是编译期常量**

**错误。**

初始化表达式需要运行时计算时，只是不可再次赋值的类级字段，不是编译期常量。

**误区八：访问任何 static final 字段都会初始化类**

**错误。**

访问被内联的编译期常量可能不会初始化声明类。

**误区九：通过 Child.value 一定初始化 Child**

**错误。**

如果 value 实际定义在 Parent 中，通常只初始化 Parent。

**误区十：创建 User[] 会创建多个 User 对象**

**错误。**

只创建数组对象，数组元素初始值是 null。

**误区十一：final 变量必须在声明时初始化**

**错误。**

可以在构造方法、初始化代码块或确定赋值路径中完成初始化。

**误区十二：final 引用表示对象不可变**

**错误。**

final 只限制引用变量不能重新赋值。

**误区十三：final 类一定是不可变类**

**错误。**

final 只禁止继承，类仍然可以提供 Setter 修改字段。

**误区十四：private final List 一定不可修改**

**错误。**

private 限制外部直接访问，final 限制引用重新赋值，但列表内容仍然可能变化。

**误区十五：public static final List 是安全常量**

不一定。
如果指向可变集合，任何调用方都可能修改其内容。

**误区十六：static 能保证线程安全**

**错误。**

static 只表示共享范围，反而可能扩大并发竞争。

**误区十七：静态方法一定比实例方法性能更好**

不应这样设计。

现代 JVM 会进行优化，业务设计应优先考虑：

- 语义
- 可测试性
- 可替换性
- 封装
- 多态需求
- 误区十八：工具类应该保存全局业务状态

**错误。**

工具类通常应保持无状态。

**误区十九：静态初始化适合执行复杂网络调用**

**错误。**

静态初始化失败会导致整个类不可用，而且很难恢复。

**误区二十：类初始化永远不会发生死锁**

**错误。**

如果多个类静态初始化相互依赖并获取不同锁，可能形成初始化死锁。

## 8.38 工程实践建议

**8.38.1 静态字段优先保持不可变**

推荐：

```java
private static final DateTimeFormatter FORMATTER =
DateTimeFormatter.ISO_LOCAL_DATE;
```

谨慎：

```java
private static Map<String, Object> globalState;
```

**8.38.2 不要使用静态变量保存请求级数据**

错误：

```java
private static String currentUserId;
```

在 Web 服务中多个请求线程共享该字段，容易发生用户数据串扰。

请求级数据应通过：

- 方法参数
- 请求上下文
- 明确作用域对象
- 必要时 ThreadLocal
传递。

**8.38.3 静态初始化保持简单**

推荐：

```java
private static final Set<String> SUPPORTED_TYPES =
Set.of("A", "B", "C");
```

谨慎：

```
static {
// 访问数据库
// 远程请求
// 启动线程
// 获取复杂锁
}
```

**8.38.4 工具类保持无状态**

推荐：

```java
public static String normalize(
String value
) {
    return value == null
    ? null
    : value.trim();
}
```

避免：

```java
private static String lastValue;
```

**8.38.5 可替换行为不要设计成静态方法**

需要测试和扩展时：

```
public interface MessageSender {
void send(Message message);
}
```

优于：

```
MessageUtils.send(message);
```

**8.38.6 常量只暴露必要范围**

类内使用：

```java
private static final int BATCH_SIZE = 100;
```

包内使用可考虑包级可见性。
只有对外协议需要时才使用：

```java
public static final
```

**8.38.7 可变集合不要公开暴露**

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

**8.38.8 可能变化的配置不要使用编译期常量**

不推荐：

```java
public static final int TIMEOUT = 30;
```

如果业务上需要动态调整，应使用：

```
config.getTimeout();
```

或配置中心、环境变量、配置文件。

**8.38.9 不可变对象要进行防御性复制**

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

**8.38.10 避免静态状态污染测试**

如果测试需要：

```
GlobalCache.clear();
GlobalConfig.reset();
```

说明代码可能依赖全局可变状态。

可以考虑：

- 实例化依赖
- 构造注入
- 测试独立对象
- 显式生命周期
- 容器管理

## 8.39 本章知识链路

```
类定义完成
↓
静态字段先获得默认值
↓
类被主动使用
↓
父类先初始化
↓
按源码顺序执行静态字段初始化
和静态代码块
↓
类级状态建立完成
↓
new 创建对象
↓
执行实例字段初始化和构造方法
```

static 知识链：

```
static
→ 属于类
→ 所有对象共享
→ 不依赖具体对象
→ 没有 this
→ 不具有实例多态
→ 可变状态需要考虑并发和生命周期
```

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

面试口述版：

- static 表示成员属于类级别，而不是某个具体对象。静态字段通常只有一份，由该类的所有实例共享；静态方法没有 this，因此不能直接访问实例成员，也不参与实例
- 方法的运行时多态。静态字段初始化表达式和静态代码块在类初始化阶段按源码顺序执行，初始化子类前会先初始化父类。访问编译期常量可能不会触发声明类初始
- 化，因为常量值可能被编译器内联。final 修饰变量时表示只能赋值一次，修饰方法时表示不能被重写，修饰类时表示不能被继承。final 引用只保证引用值不再改变，并
- 不代表引用对象不可变；真正的不可变对象还需要私有状态、构造时完整初始化、不提供修改行为以及对可变对象进行防御性复制。静态共享状态不会自动保证线程安
全，并且可能带来全局状态、测试污染和内存泄漏问题，因此应谨慎使用。
