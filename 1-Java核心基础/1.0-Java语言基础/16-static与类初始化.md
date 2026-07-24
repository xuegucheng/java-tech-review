# 第8章：static 与类初始化

## 8.1 本章定位

前面的章节已经讨论了：
类与对象
实例字段与实例方法
构造方法
继承关系
父子类初始化顺序
本章进一步解决类级成员、不可变约束和初始化时机问题。
主要回答以下问题：
static 修饰的成员属于类还是对象？

静态字段为什么会被所有对象共享？
静态方法为什么不能直接访问实例成员？
静态代码块什么时候执行？
类加载和类初始化有什么区别？
哪些行为会触发类初始化？
访问编译期常量为什么可能不会初始化类？
父类和子类的静态初始化顺序是什么？
final 修饰变量、方法和类分别表示什么？
final 引用为什么仍然可以修改对象内容？
final 变量是否一定是编译期常量？
static final 是否一定代表常量？
空白 final 如何初始化？
为什么工具类通常使用私有构造方法？
静态状态为什么容易引起并发和测试问题？
如何正确设计常量类、工具类和共享状态？
本章核心主线：
1 实例成员
2 → 属于具体对象
3
4 静态成员
5 → 属于类级别
6
7 final
8 → 限制再次赋值、重写或继承
9
10 初始化机制
11 → 决定类级状态和对象状态何时建立
12
本章暂不深入：
Class 文件加载、验证、准备、解析：放在 JVM 模块。
类加载器与双亲委派：放在 JVM 模块。
final 字段的 Java 内存模型语义：放在并发模块。
静态代理和动态代理：放在代理章节。
单例模式完整实现：放在设计模式或并发模块。
常量池和字符串池：放在 String 章节。

## 8.2 static 的基本含义

static 表示成员属于类级别，而不是某个具体对象。
例如：
1 public class User {
2
3 private static int count;
4
5 private String name;
6 }
7
其中：

1 count
2 → 属于 User 类
3 → 所有 User 对象共享
4
5 name
6 → 属于具体 User 对象
7 → 每个对象各有一份
8
创建两个对象：
1 User user1 = new User();
2 User user2 = new User();
3
可以抽象为：
1 User 类
2 └── static count
3
4 user1 ──→ User 对象1
5 └── name
6
7 user2 ──→ User 对象2
8 └── name
9
### 8.2.1 静态成员不依赖对象存在
即使没有创建任何对象，也可以访问静态成员：
1 User.getCount();
2
不需要：
1 new User().getCount();
2
因此更推荐通过类名访问静态成员：
1 User.getCount();
2 User.DEFAULT_NAME;
3
而不是通过对象引用访问：
1 user.getCount();
2 user.DEFAULT_NAME;

3
后者即使语法允许，也容易让人误以为静态成员属于对象。

## 8.3 静态字段

使用 static 修饰的字段称为静态字段或类变量：
1 public class User {
2
3 private static int count;
4 }
5
静态字段通常只有一份，由该类的所有实例共享。
### 8.3.1 静态字段共享示例
1 public class User {
2
3 private static int count;
4
5 public User() {
6 count++;
7 }
8
9 public static int getCount() {
10 return count;
11 }
12 }
13
调用：
1 new User();
2 new User();
3 new User();
4
5 System.out.println(User.getCount());
6
输出：
1 3
2
每次创建对象都会修改同一个 count 。
### 8.3.2 静态字段不是每个对象一份
错误理解：

1 user1 有一个 static count
2 user2 有一个 static count
3
准确理解：
1 User 类只有一个 count
2 user1 和 user2 都可以访问它
3
因此：
1 user1.count = 10;
2 user2.count = 20;
3
最终读取：
1 System.out.println(User.count);
2
结果是：
1 20
2
并不存在两个独立的静态字段。
### 8.3.3 静态字段的默认值
静态字段与实例字段一样，会获得默认值：
类型 默认值
整数类型 0
浮点类型 0.0
boolean false
char '\u0000'
引用类型 null
例如：
1 public class Config {
2
3 private static int timeout;
4 private static String environment;
5 }
6
初始化前：

1 timeout = 0
2 environment = null
3
但类初始化过程中，显式初始化表达式和静态代码块会覆盖默认值。
### 8.3.4 静态字段适合保存什么
适合保存：
类级常量
无状态共享对象
全局配置快照
类级缓存
统计计数
单例引用
与类整体相关的状态
例如：
1 private static final int DEFAULT_TIMEOUT = 30;
2
### 8.3.5 静态可变字段的风险
1 public class GlobalState {
2
3 public static int currentUserId;
4 }
5
问题：
所有调用方共享
修改来源难追踪
并发访问可能不安全
测试之间互相污染
生命周期过长
难以控制初始化顺序
容易形成隐式依赖
因此：
静态字段应优先用于不可变常量和无状态共享对象，谨慎保存可变业务状态。

## 8.4 静态方法

使用 static 修饰的方法称为静态方法或类方法：
1 public class MathUtils {
2
3 public static int max(int a, int b) {
4 return a > b ? a : b;
5 }
6 }

7
调用：
1 int result = MathUtils.max(10, 20);
2
不需要创建 MathUtils 对象。
### 8.4.1 静态方法没有 this
实例方法调用：
1 user.printName();
2
方法内部存在当前对象：
1 this
2
静态方法调用：
1 User.printCount();
2
调用不依赖任何具体对象，因此没有 this 。
错误：
1 public static void print() {
2 System.out.println(this);
3 }
4
编译失败。
### 8.4.2 静态方法不能直接访问实例成员
1 public class User {
2
3 private String name;
4
5 public static void printName() {
6 System.out.println(name);
7 }
8 }
9
编译失败。

原因：
1 name 属于具体对象
2 静态方法不对应任何具体对象
3 无法确定访问哪个对象的 name
4
### 8.4.3 静态方法通过对象访问实例成员
静态方法如果获得对象引用，就可以访问该对象的实例成员：
1 public static void printName(User user) {
2 System.out.println(user.name);
3 }
4
这里并不是静态方法直接拥有实例上下文。
而是通过参数 user 明确指定了要访问哪个对象。
### 8.4.4 实例方法可以访问静态成员
1 public class User {
2
3 private static int count;
4 private String name;
5
6 public void print() {
7 System.out.println(count);
8 System.out.println(name);
9 }
10 }
11
实例方法既有当前对象，也能够访问类级成员。
更明确的写法是：
1 System.out.println(User.count);
2 System.out.println(this.name);
3
### 8.4.5 静态方法适用场景
适合：
无状态工具方法
静态工厂方法
参数校验
类型转换
纯计算
与类整体相关的操作
不依赖对象字段的行为

例如：
1 public static Order create(String orderNo) {
2 return new Order(orderNo);
3 }
4
### 8.4.6 静态方法不适合的场景
如果方法依赖：
对象状态
可替换实现
多态行为
外部资源
运行时配置
数据库或网络服务
通常不适合设计成大量静态方法。
例如：
1 public static void createOrder(Order order) {
2 // 查询数据库
3 // 扣减库存
4 // 发送消息
5 }
6
这种设计：
难以替换依赖
难以单元测试
难以使用依赖注入
难以通过多态扩展
容易形成全局状态

## 8.5 静态方法隐藏

父类：
1 public class Parent {
2
3 public static void execute() {
4 System.out.println("Parent");
5 }
6 }
7
子类：
1 public class Child extends Parent {
2
3 public static void execute() {
4 System.out.println("Child");
5 }

6 }
7
这不是方法重写，而是静态方法隐藏。
调用：
1 Parent value = new Child();
2 value.execute();
3
输出：
1 Parent
2
因为静态方法根据引用的编译时类型绑定。
推荐直接通过类名调用：
1 Parent.execute();
2 Child.execute();
3
不要通过对象引用调用静态方法，以免误解为动态绑定。

## 8.6 静态代码块

静态代码块使用：
1 static {
2 // 静态初始化逻辑
3 }
4
示例：
1 public class Config {
2
3 private static final Map<String, String> VALUES;
4
5 static {
6 Map<String, String> values = new HashMap<>();
7 values.put("timeout", "30");
8 values.put("retry", "3");
9
10 VALUES = Map.copyOf(values);
11 }
12 }
13
静态代码块在类初始化时执行。

### 8.6.1 静态代码块的特点
属于类级初始化逻辑
类初始化时执行
一般只执行一次
可以访问静态成员
不能直接访问实例成员
不能使用 this
按源码顺序与静态字段初始化表达式一起执行
### 8.6.2 静态代码块不能保证程序启动时立即执行
类什么时候执行静态代码块，取决于该类什么时候被主动初始化。
例如：
1 public class Demo {
2
3 static {
4 System.out.println("Demo initialized");
5 }
6 }
7
仅仅把 Demo.class 文件放在 classpath 中，不代表静态代码块一定立即执行。
只有程序实际触发类初始化时才会执行。
### 8.6.3 静态代码块异常
如果静态初始化过程中发生异常：
1 public class Config {
2
3 static {
4 if (true) {
5 throw new RuntimeException("初始化失败");
6 }
7 }
8 }
9
第一次使用类时可能抛出：
1 ExceptionInInitializerError
2
之后再次尝试使用该类，可能出现：
1 NoClassDefFoundError
2
因为 JVM 已经记录该类初始化失败。
因此：
静态初始化逻辑应保持简单、可预测，避免网络、数据库和复杂外部 IO。

## 8.7 静态字段与静态代码块的执行顺序

静态字段初始化表达式和静态代码块按源码出现顺序执行。
1 public class StaticOrderDemo {
2
3 private static int a = print("初始化 a", 1);
4
5 static {
6 System.out.println("静态代码块 1");
7 }
8
9 private static int b = print("初始化 b", 2);
10
11 static {
12 System.out.println("静态代码块 2");
13 }
14
15 private static int print(String message, int value) {
16 System.out.println(message);
17 return value;
18 }
19 }
20
初始化类时输出：
1 初始化 a
2 静态代码块 1
3 初始化 b
4 静态代码块 2
5
核心规则：
静态字段初始化表达式和静态代码块按照源码顺序组成类初始化过程。
### 8.7.1 静态字段先获得默认值
1 public class StaticDefaultDemo {
2
3 private static int value = initialize();
4
5 private static int initialize() {
6 System.out.println(value);
7 return 10;
8 }
9 }
10
输出：
1 0
2

原因：
1 静态字段先获得默认值 0
2 ↓
3 执行 initialize()
4 ↓
5 读取 value 时仍为 0
6 ↓
7 返回 10
8 ↓
9 value = 10
10

## 8.8 类加载与类初始化

日常表达中经常说“类被加载时执行静态代码块”，但这不够准确。
从 JVM 生命周期看，类通常会经历：
1 加载
2 → 链接
3 → 初始化
4
链接又包括：
1 验证
2 → 准备
3 → 解析
4
本章只需要区分：
1 类加载
2 ≠
3 类初始化
4
静态字段的显式赋值和静态代码块主要发生在：
1 初始化阶段
2
### 8.8.1 准备阶段与初始化阶段的简化区别
假设：
1 public static int value = 10;
2

可以简化理解为：
1 准备阶段
2 → value 获得默认值 0
3
4 初始化阶段
5 → 执行 value = 10
6
但如果是编译期常量：
1 public static final int VALUE = 10;
2
其处理存在常量属性等细节，放到 JVM 模块深入。
本章只记住：
静态字段先有默认值，再执行源码中的显式初始化逻辑。

## 8.9 触发类初始化的典型行为

通常会主动触发类初始化的行为包括：
1.使用 new 创建该类对象
2.读取该类的非编译期常量静态字段
3.修改该类静态字段
4.调用该类静态方法
5.使用反射主动操作该类
6.初始化某个类时，先初始化其父类
7.JVM 启动时初始化包含 main() 的主类
### 8.9.1 new 触发初始化
1 new User();
2
创建对象前，必须确保 User 类已经初始化完成。
### 8.9.2 调用静态方法触发初始化
1 User.create();
2
通常会触发 User 类初始化。
### 8.9.3 访问普通静态字段触发初始化
1 System.out.println(Config.timeout);
2

如果 timeout 不是编译期常量，通常会触发 Config 初始化。

## 8.10 不一定触发类初始化的行为

某些行为可能不会触发目标类初始化：
访问编译期常量
通过子类引用父类定义的静态字段
创建某类的数组
获取类字面量 SomeClass.class
类加载但未主动初始化
### 8.10.1 访问编译期常量
1 public class Constants {
2
3 static {
4 System.out.println("Constants initialized");
5 }
6
7 public static final int TIMEOUT = 30;
8 }
9
调用：
1 System.out.println(Constants.TIMEOUT);
2
可能只输出：
1 30
2
而不会输出：
1 Constants initialized
2
原因是：
编译期常量可能在编译阶段被直接内联到调用方代码中，运行时不需要读取 Constants 类的字段。
### 8.10.2 非编译期常量会触发初始化
1 public class Constants {
2
3 static {
4 System.out.println("Constants initialized");
5 }
6
7 public static final int TIMEOUT =

8 Integer.parseInt("30");
9 }
10
这里 TIMEOUT 需要运行时计算，不是编译期常量。
访问：
1 System.out.println(Constants.TIMEOUT);
2
通常会先初始化 Constants。
### 8.10.3 通过子类访问父类静态字段
父类：
1 public class Parent {
2
3 static {
4 System.out.println("Parent initialized");
5 }
6
7 public static int value = 10;
8 }
9
子类：
1 public class Child extends Parent {
2
3 static {
4 System.out.println("Child initialized");
5 }
6 }
7
调用：
1 System.out.println(Child.value);
2
通常输出：
1 Parent initialized
2 10
3
不会输出：
1 Child initialized

2
因为 value 实际由 Parent 定义。
真正被主动使用的是 Parent。
### 8.10.4 创建数组不会初始化元素类型
1 User[] users = new User[10];
2
这会创建 User[] 数组对象，但不会创建 User 对象，也通常不会主动初始化 User 类。
数组中初始元素全部是：
1 null
2
### 8.10.5 类字面量
1 Class<User> type = User.class;
2
获取类字面量通常不会执行类的静态初始化逻辑。
但后续反射操作是否初始化，取决于具体 API 和参数。

## 8.11 父子类静态初始化顺序

初始化子类前，必须先初始化父类。
父类：
1 public class Parent {
2
3 static {
4 System.out.println("Parent static");
5 }
6 }
7
子类：
1 public class Child extends Parent {
2
3 static {
4 System.out.println("Child static");
5 }
6 }
7
第一次执行：

1 new Child();
2
静态部分输出：
1 Parent static
2 Child static
3
因为：
1 先确保 Parent 已初始化
2 ↓
3 再初始化 Child
4
### 8.11.1 完整父子类初始化顺序
第一次创建子类对象时：
1 父类静态字段与静态代码块
2 ↓
3 子类静态字段与静态代码块
4 ↓
5 父类实例字段与实例代码块
6 ↓
7 父类构造方法
8 ↓
9 子类实例字段与实例代码块
10 ↓
11 子类构造方法
12
再次创建子类对象时：
1 父类实例字段与实例代码块
2 ↓
3 父类构造方法
4 ↓
5 子类实例字段与实例代码块
6 ↓
7 子类构造方法
8
静态初始化不会重复执行。

## 8.12 初始化期间的前向引用

Java 对字段初始化中的前向引用存在限制。
错误示例：

1 public class Example {
2
3 private static int first = second;
4 private static int second = 10;
5 }
6
可能产生非法前向引用编译错误。
但通过方法间接读取：
1 public class Example {
2
3 private static int first = getSecond();
4 private static int second = 10;
5
6 private static int getSecond() {
7 return second;
8 }
9 }
10
可能编译通过。
此时 first 可能得到：
1 0
2
原因：
1 second 已获得默认值 0
2 但尚未执行 second = 10
3
因此：
不要设计依赖复杂声明顺序的静态初始化逻辑。

## 8.13 static import

Java 支持静态导入：
1 import static java.lang.Math.max;
2 import static java.lang.Math.min;
3
使用：
1 int upper = max(a, b);
2 int lower = min(a, b);
3
而不需要：

1 Math.max(a, b);
2
### 8.13.1 静态导入适用场景
适合：
测试断言
少量明确常量
数学方法
DSL 风格 API
例如：
1 import static org.junit.jupiter.api.Assertions.assertEquals;
2
### 8.13.2 不应滥用静态导入
如果大量导入：
1 import static A.*;
2 import static B.*;
3 import static C.*;
4
代码中出现：
1 execute();
2 create();
3 build();
4 DEFAULT_SIZE;
5
读者难以判断成员来源。
因此：
静态导入应只在能够明显提升可读性时使用。

## 8.14 工具类设计

工具类通常只提供静态方法：
1 public final class StringUtils {
2
3 private StringUtils() {
4 }
5
6 public static boolean isBlank(String value) {
7 return value == null || value.isBlank();
8 }
9 }

10
### 8.14.1 为什么工具类构造方法使用 private
如果不声明构造方法：
1 public class StringUtils {
2 }
3
编译器会生成可访问的默认构造方法，允许：
1 new StringUtils();
2
但工具类不需要实例对象。
因此：
1 private StringUtils() {
2 }
3
用于禁止外部实例化。
### 8.14.2 为什么工具类常使用 final
1 public final class StringUtils {
2 }
3
表示不允许继承。
原因：
工具类没有实例多态需求
子类继承静态方法没有意义
避免被误用为父类
明确表达工具类定位
### 8.14.3 工具类不要保存可变业务状态
不推荐：
1 public final class OrderUtils {
2
3 private static Order currentOrder;
4 }
5
这会使工具类变成全局状态容器。
工具类更适合：

1 无状态
2 纯函数
3 参数输入
4 结果输出
5
例如：
1 public static BigDecimal calculateTotal(
2 List<OrderItem> items
3 ) {
4 // 纯计算
5 }
6
