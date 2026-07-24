# static 与类初始化

## 8.1 本章定位

前面的章节已经讨论了：

- 类与对象
- 实例字段与实例方法
- 构造方法
- 继承关系
- 父子类初始化顺序

本章进一步解决类级成员、不可变约束和初始化时机问题。

主要回答以下问题：

- static 修饰的成员属于类还是对象？
- 静态字段为什么会被所有对象共享？
- 静态方法为什么不能直接访问实例成员？
- 静态代码块什么时候执行？
- 类加载和类初始化有什么区别？
- 哪些行为会触发类初始化？
- 访问编译期常量为什么可能不会初始化类？
- 父类和子类的静态初始化顺序是什么？
- final 修饰变量、方法和类分别表示什么？
- final 引用为什么仍然可以修改对象内容？
- final 变量是否一定是编译期常量？
- static final 是否一定代表常量？
- 空白 final 如何初始化？
- 为什么工具类通常使用私有构造方法？
- 静态状态为什么容易引起并发和测试问题？
- 如何正确设计常量类、工具类和共享状态？

本章核心主线：

```
实例成员
→ 属于具体对象
静态成员
→ 属于类级别
final
→ 限制再次赋值、重写或继承
初始化机制
→ 决定类级状态和对象状态何时建立
```

本章暂不深入：

- Class 文件加载、验证、准备、解析：放在 JVM 模块。
- 类加载器与双亲委派：放在 JVM 模块。
- final 字段的 Java 内存模型语义：放在并发模块。
- 静态代理和动态代理：放在代理章节。
- 单例模式完整实现：放在设计模式或并发模块。
- 常量池和字符串池：放在 String 章节。

## 8.2 static 的基本含义

static 表示成员属于类级别，而不是某个具体对象。
例如：

```java
public class User {
    private static int count;
    private String name;
}
```

其中：

```
count
→ 属于 User 类
→ 所有 User 对象共享
name
→ 属于具体 User 对象
→ 每个对象各有一份
```

创建两个对象：

```
User user1 = new User();
User user2 = new User();
```

可以抽象为：

```bash
User 类
└── static count
user1 ──→ User 对象1
└── name
user2 ──→ User 对象2
└── name
```

**8.2.1 静态成员不依赖对象存在**

即使没有创建任何对象，也可以访问静态成员：

```
User.getCount();
```

不需要：

```
new User().getCount();
```

因此更推荐通过类名访问静态成员：

```
User.getCount();
User.DEFAULT_NAME;
```

而不是通过对象引用访问：

```
user.getCount();
user.DEFAULT_NAME;
```

后者即使语法允许，也容易让人误以为静态成员属于对象。

## 8.3 静态字段

使用 static 修饰的字段称为静态字段或类变量：

```java
public class User {
    private static int count;
}
```

静态字段通常只有一份，由该类的所有实例共享。

**8.3.1 静态字段共享示例**

```java
public class User {
    private static int count;
    public User() {
        count++;
    }
    public static int getCount() {
        return count;
    }
}
```

调用：

```java
new User();
new User();
new User();
System.out.println(User.getCount());
```

输出：

```
3
```

每次创建对象都会修改同一个 count 。

**8.3.2 静态字段不是每个对象一份**

错误理解：

```
user1 有一个 static count
user2 有一个 static count
```

准确理解：

```
User 类只有一个 count
user1 和 user2 都可以访问它
```

因此：

```
user1.count = 10;
user2.count = 20;
```

最终读取：

```java
System.out.println(User.count);
```

结果是：

```
20
```

并不存在两个独立的静态字段。

**8.3.3 静态字段的默认值**

静态字段与实例字段一样，会获得默认值：

- 类型 默认值
- 整数类型 0
- 浮点类型 0.0
- boolean false
- char '\u0000'
- 引用类型 null
- 例如：

```java
public class Config {
    private static int timeout;
    private static String environment;
}
```

初始化前：

```
timeout = 0
environment = null
```

但类初始化过程中，显式初始化表达式和静态代码块会覆盖默认值。

**8.3.4 静态字段适合保存什么**

适合保存：

- 类级常量
- 无状态共享对象
- 全局配置快照
- 类级缓存
- 统计计数
- 单例引用
- 与类整体相关的状态
- 例如：

```java
private static final int DEFAULT_TIMEOUT = 30;
```

**8.3.5 静态可变字段的风险**

```java
public class GlobalState {
    public static int currentUserId;
}
```

问题：

- 所有调用方共享
- 修改来源难追踪
- 并发访问可能不安全
- 测试之间互相污染
- 生命周期过长
- 难以控制初始化顺序
- 容易形成隐式依赖
- 因此：

静态字段应优先用于不可变常量和无状态共享对象，谨慎保存可变业务状态。

## 8.4 静态方法

使用 static 修饰的方法称为静态方法或类方法：

```java
public class MathUtils {
    public static int max(int a, int b) {
        return a > b ? a : b;
    }
}
```

调用：

```
int result = MathUtils.max(10, 20);
```

不需要创建 MathUtils 对象。

**8.4.1 静态方法没有 this**

实例方法调用：

```
user.printName();
```

方法内部存在当前对象：

```
this
```

静态方法调用：

```
User.printCount();
```

调用不依赖任何具体对象，因此没有 this 。
错误：

```java
public static void print() {
    System.out.println(this);
}
```

编译失败。

**8.4.2 静态方法不能直接访问实例成员**

```java
public class User {
    private String name;
    public static void printName() {
        System.out.println(name);
    }
}
```

编译失败。
原因：

```
name 属于具体对象
静态方法不对应任何具体对象
无法确定访问哪个对象的 name
```

**8.4.3 静态方法通过对象访问实例成员**

静态方法如果获得对象引用，就可以访问该对象的实例成员：

```java
public static void printName(User user) {
    System.out.println(user.name);
}
```

这里并不是静态方法直接拥有实例上下文。
而是通过参数 user 明确指定了要访问哪个对象。

**8.4.4 实例方法可以访问静态成员**

```java
public class User {
    private static int count;
    private String name;
    public void print() {
        System.out.println(count);
        System.out.println(name);
    }
}
```

实例方法既有当前对象，也能够访问类级成员。
更明确的写法是：

```java
System.out.println(User.count);
System.out.println(this.name);
```

**8.4.5 静态方法适用场景**

适合：

- 无状态工具方法
- 静态工厂方法
- 参数校验
- 类型转换
- 纯计算
- 与类整体相关的操作
- 不依赖对象字段的行为
- 例如：

```java
public static Order create(String orderNo) {
    return new Order(orderNo);
}
```

**8.4.6 静态方法不适合的场景**

如果方法依赖：

- 对象状态
- 可替换实现
- 多态行为
- 外部资源
- 运行时配置
- 数据库或网络服务

通常不适合设计成大量静态方法。
例如：

```java
public static void createOrder(Order order) {
    // 查询数据库
    // 扣减库存
    // 发送消息
}
```

这种设计：

- 难以替换依赖
- 难以单元测试
- 难以使用依赖注入
- 难以通过多态扩展
- 容易形成全局状态

## 8.5 静态方法隐藏

父类：

```java
public class Parent {
    public static void execute() {
        System.out.println("Parent");
    }
}
```

子类：

```java
public class Child extends Parent {
    public static void execute() {
        System.out.println("Child");
    }
}
```

这不是方法重写，而是静态方法隐藏。
调用：

```
Parent value = new Child();
value.execute();
```

输出：

```
Parent
```

因为静态方法根据引用的编译时类型绑定。
推荐直接通过类名调用：

```
Parent.execute();
Child.execute();
```

不要通过对象引用调用静态方法，以免误解为动态绑定。

## 8.6 静态代码块

静态代码块使用：

```
static {
// 静态初始化逻辑
}
```

示例：

```java
public class Config {
    private static final Map<String, String> VALUES;
    static {
        Map<String, String> values = new HashMap<>();
        values.put("timeout", "30");
        values.put("retry", "3");
        VALUES = Map.copyOf(values);
    }
}
```

静态代码块在类初始化时执行。

**8.6.1 静态代码块的特点**

属于类级初始化逻辑
类初始化时执行
一般只执行一次
可以访问静态成员
不能直接访问实例成员
不能使用 this
按源码顺序与静态字段初始化表达式一起执行

**8.6.2 静态代码块不能保证程序启动时立即执行**

类什么时候执行静态代码块，取决于该类什么时候被主动初始化。
例如：

```java
public class Demo {
    static {
        System.out.println("Demo initialized");
    }
}
```

仅仅把 Demo.class 文件放在 classpath 中，不代表静态代码块一定立即执行。
只有程序实际触发类初始化时才会执行。

**8.6.3 静态代码块异常**

如果静态初始化过程中发生异常：

```java
public class Config {
    static {
        if (true) {
            throw new RuntimeException("初始化失败");
        }
    }
}
```

第一次使用类时可能抛出：

```
ExceptionInInitializerError
```

之后再次尝试使用该类，可能出现：

```
NoClassDefFoundError
```

因为 JVM 已经记录该类初始化失败。
因此：
静态初始化逻辑应保持简单、可预测，避免网络、数据库和复杂外部 IO。

## 8.7 静态字段与静态代码块的执行顺序

静态字段初始化表达式和静态代码块按源码出现顺序执行。

```java
public class StaticOrderDemo {
    private static int a = print("初始化 a", 1);
    static {
        System.out.println("静态代码块 1");
    }
    private static int b = print("初始化 b", 2);
    static {
        System.out.println("静态代码块 2");
    }
    private static int print(String message, int value) {
        System.out.println(message);
        return value;
    }
}
```

初始化类时输出：

```
初始化 a
静态代码块 1
初始化 b
静态代码块 2
```

核心规则：
静态字段初始化表达式和静态代码块按照源码顺序组成类初始化过程。

**8.7.1 静态字段先获得默认值**

```java
public class StaticDefaultDemo {
    private static int value = initialize();
    private static int initialize() {
        System.out.println(value);
        return 10;
    }
}
```

输出：

```
0
```

原因：

```
静态字段先获得默认值 0
↓
执行 initialize()
↓
读取 value 时仍为 0
↓
返回 10
↓
value = 10
```

## 8.8 类加载与类初始化

日常表达中经常说“类被加载时执行静态代码块”，但这不够准确。
从 JVM 生命周期看，类通常会经历：

```
加载
→ 链接
→ 初始化
```

链接又包括：

```
验证
→ 准备
→ 解析
```

本章只需要区分：

```
类加载
≠
类初始化
```

静态字段的显式赋值和静态代码块主要发生在：

```
初始化阶段
```

**8.8.1 准备阶段与初始化阶段的简化区别**

假设：

```java
public static int value = 10;
```

可以简化理解为：

```
准备阶段
→ value 获得默认值 0
初始化阶段
→ 执行 value = 10
```

但如果是编译期常量：

```java
public static final int VALUE = 10;
```

其处理存在常量属性等细节，放到 JVM 模块深入。
本章只记住：
静态字段先有默认值，再执行源码中的显式初始化逻辑。

## 8.9 触发类初始化的典型行为

通常会主动触发类初始化的行为包括：

- 1.使用 new 创建该类对象
- 2.读取该类的非编译期常量静态字段
- 3.修改该类静态字段
- 4.调用该类静态方法
- 5.使用反射主动操作该类
- 6.初始化某个类时，先初始化其父类
- 7.JVM 启动时初始化包含 main() 的主类

**8.9.1 new 触发初始化**

```
new User();
```

创建对象前，必须确保 User 类已经初始化完成。

**8.9.2 调用静态方法触发初始化**

```
User.create();
```

通常会触发 User 类初始化。

**8.9.3 访问普通静态字段触发初始化**

```java
System.out.println(Config.timeout);
```

如果 timeout 不是编译期常量，通常会触发 Config 初始化。

## 8.10 不一定触发类初始化的行为

某些行为可能不会触发目标类初始化：

- 访问编译期常量
- 通过子类引用父类定义的静态字段
- 创建某类的数组
- 获取类字面量 SomeClass.class
- 类加载但未主动初始化

**8.10.1 访问编译期常量**

```java
public class Constants {
    static {
        System.out.println("Constants initialized");
    }
    public static final int TIMEOUT = 30;
}
```

调用：

```java
System.out.println(Constants.TIMEOUT);
```

可能只输出：

```
30
```

而不会输出：

```
Constants initialized
```

原因是：
编译期常量可能在编译阶段被直接内联到调用方代码中，运行时不需要读取 Constants 类的字段。

**8.10.2 非编译期常量会触发初始化**

```java
public class Constants {
    static {
        System.out.println("Constants initialized");
    }
    public static final int TIMEOUT =
    Integer.parseInt("30");
}
```

这里 TIMEOUT 需要运行时计算，不是编译期常量。
访问：

```java
System.out.println(Constants.TIMEOUT);
```

通常会先初始化 Constants。

**8.10.3 通过子类访问父类静态字段**

父类：

```java
public class Parent {
    static {
        System.out.println("Parent initialized");
    }
    public static int value = 10;
}
```

子类：

```java
public class Child extends Parent {
    static {
        System.out.println("Child initialized");
    }
}
```

调用：

```java
System.out.println(Child.value);
```

通常输出：

```
Parent initialized
10
```

不会输出：

```
Child initialized
```

因为 value 实际由 Parent 定义。
真正被主动使用的是 Parent。

**8.10.4 创建数组不会初始化元素类型**

```
User[] users = new User[10];
```

这会创建 User[] 数组对象，但不会创建 User 对象，也通常不会主动初始化 User 类。
数组中初始元素全部是：

```
null
```

**8.10.5 类字面量**

```
Class<User> type = User.class;
```

获取类字面量通常不会执行类的静态初始化逻辑。
但后续反射操作是否初始化，取决于具体 API 和参数。

## 8.11 父子类静态初始化顺序

初始化子类前，必须先初始化父类。
父类：

```java
public class Parent {
    static {
        System.out.println("Parent static");
    }
}
```

子类：

```java
public class Child extends Parent {
    static {
        System.out.println("Child static");
    }
}
```

第一次执行：

```
new Child();
```

静态部分输出：

```
Parent static
Child static
```

因为：

```
先确保 Parent 已初始化
↓
再初始化 Child
```

**8.11.1 完整父子类初始化顺序**

第一次创建子类对象时：

```
父类静态字段与静态代码块
↓
子类静态字段与静态代码块
↓
父类实例字段与实例代码块
↓
父类构造方法
↓
子类实例字段与实例代码块
↓
子类构造方法
```

再次创建子类对象时：

```
父类实例字段与实例代码块
↓
父类构造方法
↓
子类实例字段与实例代码块
↓
子类构造方法
```

静态初始化不会重复执行。

## 8.12 初始化期间的前向引用

Java 对字段初始化中的前向引用存在限制。
错误示例：

```java
public class Example {
    private static int first = second;
    private static int second = 10;
}
```

可能产生非法前向引用编译错误。
但通过方法间接读取：

```java
public class Example {
    private static int first = getSecond();
    private static int second = 10;
    private static int getSecond() {
        return second;
    }
}
```

可能编译通过。
此时 first 可能得到：

```
0
```

原因：

```
second 已获得默认值 0
但尚未执行 second = 10
```

因此：
不要设计依赖复杂声明顺序的静态初始化逻辑。

## 8.13 static import

Java 支持静态导入：

```
import static java.lang.Math.max;
import static java.lang.Math.min;
```

使用：

```
int upper = max(a, b);
int lower = min(a, b);
```

而不需要：

```
Math.max(a, b);
```

**8.13.1 静态导入适用场景**

适合：

- 测试断言
- 少量明确常量
- 数学方法
- DSL 风格 API
- 例如：

```
import static org.junit.jupiter.api.Assertions.assertEquals;
```

**8.13.2 不应滥用静态导入**

如果大量导入：

```
import static A.*;
import static B.*;
import static C.*;
```

代码中出现：

```
execute();
create();
build();
DEFAULT_SIZE;
```

读者难以判断成员来源。
因此：
静态导入应只在能够明显提升可读性时使用。

## 8.14 工具类设计

工具类通常只提供静态方法：

```java
public final class StringUtils {
    private StringUtils() {
    }
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
```

**8.14.1 为什么工具类构造方法使用 private**

如果不声明构造方法：

```java
public class StringUtils {
}
```

编译器会生成可访问的默认构造方法，允许：

```
new StringUtils();
```

但工具类不需要实例对象。
因此：

```java
private StringUtils() {
}
```

用于禁止外部实例化。

**8.14.2 为什么工具类常使用 final**

```java
public final class StringUtils {
}
```

表示不允许继承。

原因：

- 工具类没有实例多态需求
- 子类继承静态方法没有意义
- 避免被误用为父类
- 明确表达工具类定位

**8.14.3 工具类不要保存可变业务状态**

不推荐：

```java
public final class OrderUtils {
    private static Order currentOrder;
}
```

这会使工具类变成全局状态容器。
工具类更适合：

```
无状态
纯函数
参数输入
结果输出
```

例如：

```java
public static BigDecimal calculateTotal(
List<OrderItem> items
) {
    // 纯计算
}
```
