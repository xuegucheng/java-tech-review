# 第8章：final 与常量设计

## 8.15 final 的基本含义

final 可以修饰：
变量
字段
方法
类
方法参数
不同位置语义不同：
1 final 变量
2 → 只能赋值一次
3
4 final 方法
5 → 不能被子类重写
6
7 final 类
8 → 不能被继承
9

## 8.16 final 局部变量

1 final int value = 10;
2
初始化后不能再次赋值：
1 value = 20;
2
编译失败。

### 8.16.1 final 表示只能赋值一次
final 不一定要求声明时立刻赋值：
1 final int value;
2
3 if (condition) {
4 value = 10;
5 } else {
6 value = 20;
7 }
8
只要编译器能够确定：
1 所有正常路径上都会完成赋值
2 并且最多赋值一次
3
就可以编译。
### 8.16.2 final 局部变量必须确定赋值
错误：
1 final int value;
2
3 if (condition) {
4 value = 10;
5 }
6
7 System.out.println(value);
8
因为当 condition == false 时， value 没有初始化。
### 8.16.3 循环中的 final 变量
1 for (int i = 0; i < 10; i++) {
2 final int value = i;
3 }
4
每次循环都会创建一个新的 value 变量，因此合法。
但：
1 final int value;
2
3 for (int i = 0; i < 10; i++) {
4 value = i;
5 }

6
通常无法编译，因为同一个 final 变量可能被多次赋值。

## 8.17 final 引用变量

1 final List<String> list =
2 new ArrayList<>();
3
允许：
1 list.add("Java");
2 list.clear();
3
不允许：
1 list = new ArrayList<>();
2
原因：
1 final 限制的是引用变量中的引用值不能改变
2 不限制引用对象的内部状态
3
### 8.17.1 final 引用不等于不可变对象
1 final User user = new User();
2 user.setName("Java");
3
合法。
因为 user 仍然指向同一个 User 对象，只是对象内部字段变化。
真正的不可变对象还需要：
字段私有
状态不可修改
防御性复制
不暴露内部可变对象
修改操作返回新对象
### 8.17.2 final 集合仍然可以修改
1 private final List<String> items =
2 new ArrayList<>();

3
表示：
1 items 不能重新指向另一个列表
2
不表示：
1 列表内容不能变化
2
如果希望列表不可修改：
1 private final List<String> items;
2
3 public Order(List<String> items) {
4 this.items = List.copyOf(items);
5 }
6

## 8.18 final 字段

实例字段可以声明为 final：
1 public class User {
2
3 private final String userId;
4 }
5
final 实例字段必须在对象构造完成前被赋值。
可以在：
1.声明处初始化
2.实例代码块中初始化
3.每个构造方法中初始化
### 8.18.1 声明处初始化
1 private final String type = "DEFAULT";
2
每个对象的 type 初始化为 "DEFAULT" 。
### 8.18.2 构造方法初始化
1 public class User {

2
3 private final String userId;
4
5 public User(String userId) {
6 this.userId = userId;
7 }
8 }
9
每个构造方法必须保证 userId 被赋值。
错误：
1 public User() {
2 }
3
如果没有其他初始化方式，会编译失败。
### 8.18.3 实例代码块初始化
1 public class User {
2
3 private final String id;
4
5 {
6 id = UUID.randomUUID().toString();
7 }
8 }
9
所有构造方法执行前，实例代码块会初始化 id 。
但业务代码通常优先使用声明处或构造方法，意图更清晰。
### 8.18.4 final 字段不能重复赋值
1 public User(String userId) {
2 this.userId = userId;
3 this.userId = "other";
4 }
5
编译失败。

## 8.19 static final 字段

static final 表示类级变量只能赋值一次：
1 public static final int TIMEOUT = 30;
2
通常用于定义常量。

推荐命名：
1 全大写
2 单词之间使用下划线
3
例如：
1 public static final int DEFAULT_TIMEOUT = 30;
2 public static final String SYSTEM_USER = "SYSTEM";
3
### 8.19.1 static final 不一定是编译期常量
编译期常量：
1 public static final int TIMEOUT = 30;
2
运行时常量：
1 public static final int TIMEOUT =
2 Integer.parseInt("30");
3
二者都是 static final ，但只有前者属于典型编译期常量。
### 8.19.2 编译期常量的条件
通常需要满足：
基本类型或 String
使用 final
在声明处初始化
初始化表达式是常量表达式
例如：
1 public static final int A = 10;
2 public static final int B = A + 20;
3 public static final String NAME = "Java";
4
属于编译期常量。
但：
1 public static final Integer VALUE = 10;
2
类型是包装类，不属于 Java 语言规范意义上的基本类型或 String 编译期常量。

### 8.19.3 编译期常量内联风险
依赖模块 A：
1 public class Constants {
2
3 public static final int TIMEOUT = 30;
4 }
5
模块 B 编译时使用：
1 int timeout = Constants.TIMEOUT;
2
编译器可能把它直接编译成：
1 int timeout = 30;
2
后来只更新模块 A：
1 TIMEOUT = 60;
2
但没有重新编译模块 B，模块 B 可能仍然使用旧值：
1 30
2
因此：
跨模块可能变化的配置，不应设计为会被编译器内联的 public 编译期常量。
更适合：
配置中心
配置文件
方法访问
枚举
运行时对象

## 8.20 空白 final

没有在声明时初始化的 final 字段称为空白 final。
1 public class User {
2
3 private final String userId;
4
5 public User(String userId) {
6 this.userId = userId;
7 }
8 }

9
空白 final 的价值：
每个对象可以拥有不同值
初始化后不能再改变
支持不可变对象设计
强制构造过程完整
### 8.20.1 静态空白 final
1 public class Config {
2
3 private static final String ENVIRONMENT;
4
5 static {
6 ENVIRONMENT =
7 System.getProperty("environment");
8 }
9 }
10
静态空白 final 必须在：
声明处
静态代码块
完成赋值。
不能在普通实例构造方法中赋值，因为它属于类级字段。

## 8.21 final 方法参数

1 public void execute(final User user) {
2 }
3
方法内部不能重新给参数赋值：
1 user = new User();
2
编译失败。
但可以修改对象：
1 user.setName("Java");
2
因此：
final 参数只限制形参变量，不能保证传入对象不可变。
### 8.21.1 final 参数的工程价值
可能用于：

明确参数不会重新赋值
避免误操作
早期匿名内部类捕获
团队编码规范
现代 Java 中，局部变量和参数被 Lambda 捕获时只要求：
1 final 或 effectively final
2
不一定必须显式写 final 。

## 8.22 effectively final

有效 final 表示：
变量虽然没有显式使用 final，但初始化后从未重新赋值。
例如：
1 String name = "Java";
2
3 Runnable task = () ->
4 System.out.println(name);
5
name 没有重新赋值，因此是 effectively final。
如果之后重新赋值：
1 String name = "Java";
2 name = "JVM";
3
4 Runnable task = () ->
5 System.out.println(name);
6
编译失败。
Lambda 的完整捕获规则放在现代 Java 章节。
本章只记住：
effectively final 是行为上的不再赋值，而不是使用了 final 关键字。

## 8.23 final 方法

1 public class Account {
2
3 public final void validate() {
4 }
5 }
6
子类不能重写：
1 public class SavingsAccount extends Account {
2

3 @Override
4 public void validate() {
5 }
6 }
7
编译失败。
### 8.23.1 final 方法的适用场景
适合：
固定核心安全校验
固定状态维护逻辑
模板方法中的整体流程
防止子类破坏父类不变量
明确行为不允许扩展
例如：
1 public abstract class ImportTemplate {
2
3 public final void execute() {
4 validate();
5 read();
6 process();
7 finish();
8 }
9
10 protected abstract void read();
11
12 protected abstract void process();
13
14 private void validate() {
15 }
16
17 private void finish() {
18 }
19 }
20
### 8.23.2 private 方法不需要 final
private 方法对子类不可见，本身不能被重写。
因此：
1 private final void validate() {
2 }
3
虽然可能语法允许，但 final 没有额外价值。
直接写：
1 private void validate() {
2 }

3
即可。
### 8.23.3 static final 方法
静态方法不参与重写，只能被隐藏。
因此静态方法通常不需要使用 final 防止重写。

## 8.24 final 类

1 public final class String {
2 }
3
final 类不能被继承：
1 public class CustomString extends String {
2 }
3
编译失败。
### 8.24.1 final 类中的方法
final 类不能有子类，因此其中的实例方法不会被外部子类重写。
不需要为每个方法再机械添加 final。
### 8.24.2 final 类的适用场景
适合：
不可变值对象
安全敏感类型
工具类
不希望扩展的基础类型
类语义完整，不适合作为父类
防止继承破坏对象不变量
例如：
1 public final class Money {
2 }
3
### 8.24.3 final 类不等于不可变类
1 public final class User {
2
3 private String name;
4

5 public void setName(String name) {
6 this.name = name;
7 }
8 }
9
该类不能被继承，但对象仍然可变。
不可变需要同时控制：
字段
构造
修改方法
内部可变对象
对外暴露

## 8.25 final 与不可变对象

一个典型不可变对象：
1 public final class Money {
2
3 private final BigDecimal amount;
4 private final Currency currency;
5
6 public Money(
7 BigDecimal amount,
8 Currency currency
9 ) {
10 this.amount =
11 Objects.requireNonNull(amount);
12 this.currency =
13 Objects.requireNonNull(currency);
14 }
15
16 public BigDecimal amount() {
17 return amount;
18 }
19
20 public Currency currency() {
21 return currency;
22 }
23
24 public Money add(Money other) {
25 if (!currency.equals(other.currency)) {
26 throw new IllegalArgumentException(
27 "币种不一致"
28 );
29 }
30
31 return new Money(
32 amount.add(other.amount),
33 currency
34 );
35 }
36 }
37
关键点：

类使用 final
字段使用 private final
构造时完成初始化
不提供 Setter
修改操作返回新对象
引用字段本身应不可变或进行防御性复制
### 8.25.1 private final 仍然不够
1 public final class Order {
2
3 private final List<String> items;
4
5 public Order(List<String> items) {
6 this.items = items;
7 }
8 }
9
外部仍然可以：
1 List<String> source =
2 new ArrayList<>();
3
4 Order order = new Order(source);
5
6 source.clear();
7
导致对象内部变化。
应使用：
1 this.items = List.copyOf(items);
2
并避免直接返回内部可变集合。

## 8.26 static 与对象生命周期

实例字段通常随对象生命周期存在。
静态字段通常随类及其类加载器生命周期存在。
可以简化为：
1 实例字段
2 → 对象可达时存在
3 → 对象不可达后可能被回收
4
5 静态字段
6 → 类初始化后存在
7 → 通常持续到类加载器被卸载
8

因此静态字段可能长期持有对象引用：
1 private static final List<Object> CACHE =
2 new ArrayList<>();
3
如果持续添加：
1 CACHE.add(object);
2
这些对象会一直被静态引用持有，可能无法被垃圾回收。
这会产生：
内存泄漏
类加载器泄漏
缓存无限增长
测试污染
### 8.26.1 静态集合必须考虑生命周期
1 private static final Map<String, Object> CACHE =
2 new ConcurrentHashMap<>();
3
至少需要考虑：
最大容量
过期策略
清理机制
并发安全
弱引用需求
类加载器边界
是否真的需要全局缓存

## 8.27 static 与线程安全

static 只表示类级共享，不表示线程安全。
1 public class Counter {
2
3 private static int count;
4
5 public static void increment() {
6 count++;
7 }
8 }
9
多个线程同时执行：
1 Counter.increment();

2
可能发生丢失更新。
因为：
1 count++;
2
不是原子操作。
可以拆成：
1 读取 count
2 加 1
3 写回 count
4
多个线程可能互相覆盖。
解决方式可能包括：
synchronized
AtomicInteger
LongAdder
锁
避免共享状态
具体放在并发模块。
本章只记住：
static 增加了共享范围，但不提供任何并发安全保证。

## 8.28 static 与多态

实例方法适合表达可替换行为：
1 PaymentProcessor processor =
2 new AlipayProcessor();
3
4 processor.pay();
5
静态方法不具备这种动态替换能力。
如果业务需要：
多种实现
测试替身
运行时选择
策略切换
依赖注入
不应将核心能力设计成静态方法。
不推荐：
1 PaymentUtils.pay(order);
2
更适合：

1 paymentProcessor.pay(order);
2
前者是固定类调用，后者可以通过接口实现多态。

## 8.29 静态工厂方法

静态方法也可以承担对象创建职责：
1 public class Order {
2
3 private final String orderNo;
4 private OrderStatus status;
5
6 private Order(
7 String orderNo,
8 OrderStatus status
9 ) {
10 this.orderNo = orderNo;
11 this.status = status;
12 }
13
14 public static Order create(
15 String orderNo
16 ) {
17 return new Order(
18 orderNo,
19 OrderStatus.CREATED
20 );
21 }
22 }
23
调用：
1 Order order = Order.create(orderNo);
2
静态工厂方法可以：
使用有意义的名称
隐藏构造细节
返回缓存对象
返回子类型
控制实例数量
执行参数校验
区分不同创建语义

## 8.30 静态内部类概览

类中可以声明静态嵌套类：
1 public class Outer {
2

3 public static class Nested {
4 }
5 }
6
静态嵌套类：
不持有外部类实例引用
可以独立创建
只能直接访问外部类静态成员
常用于 Builder、结果对象、辅助结构
创建：
1 Outer.Nested nested =
2 new Outer.Nested();
3
内部类完整规则放在高级语言特性笔记。
### 8.30.1 静态内部类单例思想
1 public class Singleton {
2
3 private Singleton() {
4 }
5
6 private static class Holder {
7
8 private static final Singleton INSTANCE =
9 new Singleton();
10 }
11
12 public static Singleton getInstance() {
13 return Holder.INSTANCE;
14 }
15 }
16
其思路是：
外部类初始化时不一定初始化 Holder
第一次调用 getInstance() 时主动使用 Holder
JVM 保证类初始化过程的线程安全性
INSTANCE 只初始化一次
单例完整讨论放在设计模式和并发模块。

## 8.31 类初始化的线程安全性

JVM 会保证同一个类的初始化过程由一个线程执行。
当多个线程同时首次使用某个类时：
1 一个线程执行类初始化逻辑
2 其他线程等待初始化完成
3

因此静态初始化常用于安全发布不可变对象：
1 private static final Config INSTANCE =
2 new Config();
3
但需要注意：
初始化过程本身不应阻塞过久
不应形成类初始化循环依赖
不应在静态代码块中获取复杂外部锁
不应执行不可控网络调用
否则可能出现：
启动卡顿
初始化死锁
初始化失败
类永久不可用

## 8.32 类初始化循环依赖

示例：
1 public class A {
2
3 static int value = B.value + 1;
4 }
5
1 public class B {
2
3 static int value = A.value + 1;
4 }
5
访问：
1 System.out.println(A.value);
2
可能得到不符合直觉的结果。
简化过程：
1 开始初始化 A
2 ↓
3 A.value 先为 0
4 ↓
5 读取 B.value
6 ↓
7 开始初始化 B
8 ↓
9 B.value 先为 0
10 ↓

11 读取正在初始化中的 A.value
12 ↓
13 得到默认值 0
14 ↓
15 B.value = 1
16 ↓
17 A.value = 2
18
因此可能出现：
1 A.value = 2
2 B.value = 1
3
核心原则：
避免静态初始化之间形成复杂双向依赖。

## 8.33 常量设计

### 8.33.1 常量应表达稳定语义
适合：
1 public static final int MAX_RETRY_COUNT = 3;
2 public static final String SYSTEM_OPERATOR =
3 "SYSTEM";
4
不适合：
1 public static final int CURRENT_TIMEOUT = 30;
2
如果该值可能需要通过配置动态修改，就不应硬编码为编译期常量。
### 8.33.2 避免魔法值
不推荐：
1 if (status == 30) {
2 }
3
可以定义：
1 private static final int COMPLETED_STATUS = 30;
2
但如果值集合具有明确类型和行为，更推荐枚举：

1 public enum OrderStatus {
2
3 CREATED(10),
4 PROCESSING(20),
5 COMPLETED(30);
6
7 private final int code;
8 }
9
### 8.33.3 常量的可见性
不是所有常量都应为 public：
1 private static final int BATCH_SIZE = 100;
2
如果只在当前类内部使用，应保持 private。
只有构成公共协议的一部分时，才考虑 public。
### 8.33.4 避免可变 public static final
1 public static final List<String> VALUES =
2 new ArrayList<>();
3
虽然引用不能重新赋值，但外部可以：
1 VALUES.clear();
2 VALUES.add("invalid");
3
这不是安全常量。
应使用：
1 public static final List<String> VALUES =
2 List.of("A", "B");
3
或者使用私有字段加只读访问。

## 8.34 单例与静态工具类的区别

静态工具类：
1 public final class Calculator {
2
3 private Calculator() {
4 }

5
6 public static int add(int a, int b) {
7 return a + b;
8 }
9 }
10
特点：
没有对象身份
通常无状态
无多态
直接通过类调用
单例对象：
1 public final class ConfigService {
2
3 private static final ConfigService INSTANCE =
4 new ConfigService();
5
6 private ConfigService() {
7 }
8
9 public static ConfigService getInstance() {
10 return INSTANCE;
11 }
12 }
13
特点：
仍然是对象
可以实现接口
可以有实例字段
可以参与多态
生命周期由静态引用控制
但单例仍然具有全局状态和测试耦合风险。
在 Spring 项目中，通常更倾向让容器管理单例对象，而不是手写全局单例。

## 8.35 建议实验

实验一：静态字段共享
1 public class StaticFieldDemo {
2
3 public static void main(String[] args) {
4 User user1 = new User();
5 User user2 = new User();
6
7 user1.increment();
8 user2.increment();
9
10 System.out.println(User.getCount());
11 }
12
13 static class User {
14

15 private static int count;
16
17 void increment() {
18 count++;
19 }
20
21 static int getCount() {
22 return count;
23 }
24 }
25 }
26
预期：
1 2
2
实验二：静态方法不能访问实例成员
1 public class StaticMethodDemo {
2
3 private String name = "Java";
4
5 public static void print() {
6 // System.out.println(name);
7 // 编译错误
8 }
9 }
10
修改为：
1 public static void print(
2 StaticMethodDemo demo
3 ) {
4 System.out.println(demo.name);
5 }
6
验证可以通过对象引用访问实例字段。
实验三：静态初始化顺序
1 public class StaticInitializationDemo {
2
3 private static int first =
4 print("first", 1);
5
6 static {
7 System.out.println("block 1");

8 }
9
10 private static int second =
11 print("second", 2);
12
13 static {
14 System.out.println("block 2");
15 }
16
17 private static int print(
18 String name,
19 int value
20 ) {
21 System.out.println(name);
22 return value;
23 }
24
25 public static void main(String[] args) {
26 System.out.println("main");
27 }
28 }
29
预期：
1 first
2 block 1
3 second
4 block 2
5 main
6
实验四：父子类初始化顺序
1 public class ParentChildInitializationDemo {
2
3 public static void main(String[] args) {
4 new Child();
5 }
6
7 static class Parent {
8
9 static {
10 System.out.println("父类静态");
11 }
12
13 {
14 System.out.println("父类实例");
15 }
16
17 Parent() {
18 System.out.println("父类构造");
19 }
20 }
21

22 static class Child extends Parent {
23
24 static {
25 System.out.println("子类静态");
26 }
27
28 {
29 System.out.println("子类实例");
30 }
31
32 Child() {
33 System.out.println("子类构造");
34 }
35 }
36 }
37
预期：
1 父类静态
2 子类静态
3 父类实例
4 父类构造
5 子类实例
6 子类构造
7
实验五：编译期常量不触发初始化
1 public class ConstantInitializationDemo {
2
3 static class Constants {
4
5 static {
6 System.out.println(
7 "Constants initialized"
8 );
9 }
10
11 static final int VALUE = 10;
12 }
13
14 public static void main(String[] args) {
15 System.out.println(Constants.VALUE);
16 }
17 }
18
观察是否输出静态初始化信息。
然后改为：
1 static final int VALUE =
2 Integer.parseInt("10");

3
再次观察差异。
实验六：通过子类访问父类静态字段
1 public class ParentStaticFieldDemo {
2
3 static class Parent {
4
5 static {
6 System.out.println("Parent");
7 }
8
9 static int value = 10;
10 }
11
12 static class Child extends Parent {
13
14 static {
15 System.out.println("Child");
16 }
17 }
18
19 public static void main(String[] args) {
20 System.out.println(Child.value);
21 }
22 }
23
预期：
1 Parent
2 10
3
通常不会输出 Child。
实验七：final 引用对象仍然可变
1 public class FinalReferenceDemo {
2
3 public static void main(String[] args) {
4 final List<String> values =
5 new ArrayList<>();
6
7 values.add("A");
8 values.add("B");
9
10 System.out.println(values);
11
12 // values = new ArrayList<>();
13 // 编译错误

14 }
15 }
16
预期：
1 [A, B]
2
实验八：static final 不一定是编译期常量
1 public class RuntimeConstantDemo {
2
3 static class Config {
4
5 static {
6 System.out.println(
7 "Config initialized"
8 );
9 }
10
11 static final int COMPILE_TIME = 10;
12
13 static final int RUNTIME =
14 Integer.parseInt("20");
15 }
16
17 public static void main(String[] args) {
18 System.out.println(
19 Config.COMPILE_TIME
20 );
21
22 System.out.println(
23 Config.RUNTIME
24 );
25 }
26 }
27
观察：
访问 COMPILE_TIME 时可能不初始化 Config
访问 RUNTIME 时会初始化 Config
实验九：静态初始化循环依赖
1 public class CircularInitializationDemo {
2
3 static class A {
4
5 static int value = B.value + 1;
6 }
7

8 static class B {
9
10 static int value = A.value + 1;
11 }
12
13 public static void main(String[] args) {
14 System.out.println(A.value);
15 System.out.println(B.value);
16 }
17 }
18
观察结果并分析默认值参与初始化的过程。
实验十：静态代码块初始化失败
1 public class InitializationFailureDemo {
2
3 static class Config {
4
5 static {
6 if (System.currentTimeMillis() > 0) {
7 throw new RuntimeException(
8 "初始化失败"
9 );
10 }
11 }
12 }
13
14 public static void main(String[] args) {
15 for (int i = 0; i < 2; i++) {
16 try {
17 System.out.println(
18 Config.class
19 );
20
21 Class.forName(
22 Config.class.getName(),
23 true,
24 Config.class
25 .getClassLoader()
26 );
27 } catch (Throwable throwable) {
28 System.out.println(
29 throwable.getClass()
30 .getSimpleName()
31 );
32 }
33 }
34 }
35 }
36
观察第一次和后续使用失败类时的异常差异。

## 8.36 高频面试题

本章建议保留以下问题：
1. static 表示什么？
2.静态字段和实例字段有什么区别？
3.静态字段是每个对象一份吗？
4.静态方法为什么不能直接访问实例字段？
5.静态方法中为什么不能使用 this ？
6.实例方法能否访问静态字段？
7.静态方法能否被重写？
8.什么是静态方法隐藏？
9.为什么建议通过类名调用静态方法？
10.静态代码块什么时候执行？
11.静态代码块会执行几次？
12.静态字段初始化表达式和静态代码块的顺序是什么？
13.静态字段是否有默认值？
14.类加载和类初始化有什么区别？
15.哪些行为会触发类初始化？
16.访问编译期常量为什么可能不触发类初始化？
17.访问非编译期常量为什么会触发类初始化？
18.通过子类访问父类静态字段，会初始化子类吗？
19.创建对象数组是否会初始化元素类型？
20.获取 SomeClass.class 是否一定会初始化类？
21.父类和子类的静态初始化顺序是什么？
22.完整的父子类初始化顺序是什么？
23.静态代码块抛异常会发生什么？
24.什么是类初始化循环依赖？
25. final 可以修饰哪些元素？
26.final 局部变量能否延迟赋值？
27.final 引用能否修改对象内容？
28.final 字段可以在哪里初始化？
29.什么是空白 final？
30.static final 一定是编译期常量吗？
31.什么条件下变量是编译期常量？
32.编译期常量内联有什么风险？
33.final 方法能否被重写？
34.private 方法是否需要 final？
35.final 类能否被继承？
36.final 类是否一定不可变？
37.final 字段是否能保证对象不可变？
38.什么是 effectively final？
39.Lambda 为什么要求捕获变量为 effectively final？
40.工具类为什么使用私有构造方法？
41.工具类为什么常声明为 final？
42.static 是否能保证线程安全？
43.静态可变字段有什么风险？
44.public static final 集合一定不可变吗？
45.静态工具类和单例对象有什么区别？
46.为什么复杂业务逻辑不适合全部设计成静态方法？
47.为什么静态缓存容易造成内存泄漏？
48.JVM 如何保证类初始化的线程安全？

## 8.37 易错点

误区一：static 成员属于所有对象
不准确。
static 成员属于类，只是所有对象都可以访问同一个类级成员。
误区二：每创建一个对象，静态字段都会复制一份
错误。
静态字段通常只有类级一份。
误区三：静态方法不能访问任何实例成员
不准确。
静态方法不能直接访问实例成员，但可以通过明确的对象引用访问。
误区四：静态方法可以被子类重写
错误。
子类同名静态方法属于隐藏，不参与运行时多态。
误区五：静态代码块在程序启动时一定执行
错误。
只有类被主动初始化时才执行。
未使用的类可能始终不会初始化。
误区六：类被加载就一定已经初始化
错误。
加载、链接和初始化是不同阶段。
误区七：所有 static final 字段都是编译期常量
错误。
初始化表达式需要运行时计算时，只是不可再次赋值的类级字段，不是编译期常量。
误区八：访问任何 static final 字段都会初始化类
错误。
访问被内联的编译期常量可能不会初始化声明类。
误区九：通过 Child.value 一定初始化 Child
错误。
如果 value 实际定义在 Parent 中，通常只初始化 Parent。
误区十：创建 User[] 会创建多个 User 对象
错误。
只创建数组对象，数组元素初始值是 null。
误区十一：final 变量必须在声明时初始化
错误。
可以在构造方法、初始化代码块或确定赋值路径中完成初始化。
误区十二：final 引用表示对象不可变

错误。
final 只限制引用变量不能重新赋值。
误区十三：final 类一定是不可变类
错误。
final 只禁止继承，类仍然可以提供 Setter 修改字段。
误区十四：private final List 一定不可修改
错误。
private 限制外部直接访问，final 限制引用重新赋值，但列表内容仍然可能变化。
误区十五：public static final List 是安全常量
不一定。
如果指向可变集合，任何调用方都可能修改其内容。
误区十六：static 能保证线程安全
错误。
static 只表示共享范围，反而可能扩大并发竞争。
误区十七：静态方法一定比实例方法性能更好
不应这样设计。
现代 JVM 会进行优化，业务设计应优先考虑：
语义
可测试性
可替换性
封装
多态需求
误区十八：工具类应该保存全局业务状态
错误。
工具类通常应保持无状态。
误区十九：静态初始化适合执行复杂网络调用
错误。
静态初始化失败会导致整个类不可用，而且很难恢复。
误区二十：类初始化永远不会发生死锁
错误。
如果多个类静态初始化相互依赖并获取不同锁，可能形成初始化死锁。

## 8.38 工程实践建议

### 8.38.1 静态字段优先保持不可变
推荐：
1 private static final DateTimeFormatter FORMATTER =
2 DateTimeFormatter.ISO_LOCAL_DATE;

3
谨慎：
1 private static Map<String, Object> globalState;
2
### 8.38.2 不要使用静态变量保存请求级数据
错误：
1 private static String currentUserId;
2
在 Web 服务中多个请求线程共享该字段，容易发生用户数据串扰。
请求级数据应通过：
方法参数
请求上下文
明确作用域对象
必要时 ThreadLocal
传递。
### 8.38.3 静态初始化保持简单
推荐：
1 private static final Set<String> SUPPORTED_TYPES =
2 Set.of("A", "B", "C");
3
谨慎：
1 static {
2 // 访问数据库
3 // 远程请求
4 // 启动线程
5 // 获取复杂锁
6 }
7
### 8.38.4 工具类保持无状态
推荐：
1 public static String normalize(
2 String value
3 ) {
4 return value == null
5 ? null
6 : value.trim();

7 }
8
避免：
1 private static String lastValue;
2
### 8.38.5 可替换行为不要设计成静态方法
需要测试和扩展时：
1 public interface MessageSender {
2
3 void send(Message message);
4 }
5
优于：
1 MessageUtils.send(message);
2
### 8.38.6 常量只暴露必要范围
类内使用：
1 private static final int BATCH_SIZE = 100;
2
包内使用可考虑包级可见性。
只有对外协议需要时才使用：
1 public static final
2
### 8.38.7 可变集合不要公开暴露
不推荐：
1 public static final List<String> VALUES =
2 new ArrayList<>();
3
推荐：

1 public static final List<String> VALUES =
2 List.of("A", "B");
3
或者：
1 private static final List<String> VALUES =
2 List.of("A", "B");
3
4 public static List<String> values() {
5 return VALUES;
6 }
7
### 8.38.8 可能变化的配置不要使用编译期常量
不推荐：
1 public static final int TIMEOUT = 30;
2
如果业务上需要动态调整，应使用：
1 config.getTimeout();
2
或配置中心、环境变量、配置文件。
### 8.38.9 不可变对象要进行防御性复制
1 public final class Order {
2
3 private final List<OrderItem> items;
4
5 public Order(List<OrderItem> items) {
6 this.items = List.copyOf(items);
7 }
8
9 public List<OrderItem> getItems() {
10 return items;
11 }
12 }
13
还需确认 OrderItem 本身是否可变。
### 8.38.10 避免静态状态污染测试
如果测试需要：

1 GlobalCache.clear();
2 GlobalConfig.reset();
3
说明代码可能依赖全局可变状态。
可以考虑：
实例化依赖
构造注入
测试独立对象
显式生命周期
容器管理

## 8.39 本章知识链路

1 类定义完成
2 ↓
3 静态字段先获得默认值
4 ↓
5 类被主动使用
6 ↓
7 父类先初始化
8 ↓
9 按源码顺序执行静态字段初始化
10 和静态代码块
11 ↓
12 类级状态建立完成
13 ↓
14 new 创建对象
15 ↓
16 执行实例字段初始化和构造方法
17
static 知识链：
1 static
2 → 属于类
3 → 所有对象共享
4 → 不依赖具体对象
5 → 没有 this
6 → 不具有实例多态
7 → 可变状态需要考虑并发和生命周期
8
final 知识链：
1 final 变量
2 → 只能赋值一次
3
4 final 引用
5 → 引用值不变
6 → 对象状态仍可能变化
7
8 final 方法

9 → 不能重写
10
11 final 类
12 → 不能继承
13
14 不可变对象
15 → 不仅需要 final
16 → 还需要封装和防御性复制
17
面试口述版：
static 表示成员属于类级别，而不是某个具体对象。静态字段通常只有一份，由该类的所有实例共享；静态方法没有 this，因此不能直接访问实例成员，也不参与实例
方法的运行时多态。静态字段初始化表达式和静态代码块在类初始化阶段按源码顺序执行，初始化子类前会先初始化父类。访问编译期常量可能不会触发声明类初始
化，因为常量值可能被编译器内联。final 修饰变量时表示只能赋值一次，修饰方法时表示不能被重写，修饰类时表示不能被继承。final 引用只保证引用值不再改变，并
不代表引用对象不可变；真正的不可变对象还需要私有状态、构造时完整初始化、不提供修改行为以及对可变对象进行防御性复制。静态共享状态不会自动保证线程安
全，并且可能带来全局状态、测试污染和内存泄漏问题，因此应谨慎使用。
