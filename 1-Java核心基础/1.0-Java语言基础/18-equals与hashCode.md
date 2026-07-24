# 第9章：equals 与 hashCode

## 9.1 本章定位

java.lang.Object 是 Java 类体系的根类。
如果一个类没有显式继承其他类：
1 public class User {
2 }
3
可以理解为：
1 public class User extends Object {
2 }
3
因此，所有普通 Java 对象都具备 Object 定义的基础能力。
本章主要解决以下问题：
Object 为什么是所有类的父类？
== 与 equals() 有什么区别？
Object 默认的 equals() 比较什么？
什么情况下应该重写 equals() ？
equals() 必须满足哪些契约？
为什么重写 equals() 后必须重写 hashCode() ？
HashMap 和 HashSet 为什么同时依赖 hashCode() 与 equals() ？
哪些字段适合参与对象相等性判断？
可变字段参与 hashCode() 会有什么风险？
getClass() 与 instanceof 在 equals 中如何选择？
toString() 应该输出哪些内容？
clone() 为什么通常不推荐直接使用？
getClass() 、 wait() 、 notify() 等 Object 方法分别有什么作用？
实体对象和值对象的相等性应该如何设计？
本章核心主线：
1 所有对象继承 Object
2 ↓

3 Object 提供基础对象协议
4 ↓
5 == 判断引用或基本值
6 ↓
7 equals() 定义逻辑相等
8 ↓
9 hashCode() 支持散列容器
10 ↓
11 equals 与 hashCode 必须保持一致
12 ↓
13 形成稳定的对象相等性契约
14
本章暂不深入：
HashMap 的哈希桶、红黑树和扩容机制：放在集合框架。
对象头和 identity hash code：放在 JVM 模块。
wait() 、 notify() 的线程协作机制：放在并发编程。
Java Record 自动生成的 equals/hashCode：放在现代 Java 特性。
ORM 实体代理与 equals 的复杂问题：放在框架实战部分。

## 9.2 Object 是类体系的根类

Java 普通类最终都继承自 Object：
1 Object
2 ↑
3 Animal
4 ↑
5 Dog
6
因此：
1 Dog dog = new Dog();
2
3 Object object = dog;
4
是合法的向上转型。
Object 可以引用任意普通对象：
1 Object value1 = "Java";
2 Object value2 = 10;
3 Object value3 = new User();
4 Object value4 = new int[]{1, 2, 3};
5
需要注意：
1 Object value2 = 10;
2
这里发生了自动装箱：

1 Object value2 = Integer.valueOf(10);
2
基本类型本身不是 Object 的子类，但对应的包装类型是对象。
### 9.2.1 Object 提供的核心方法
Object 中最常见的方法包括：
1 getClass()
2 hashCode()
3 equals(Object)
4 clone()
5 toString()
6 wait()
7 notify()
8 notifyAll()
9
历史上还存在对象终结相关机制，但不应依赖它管理资源。
这些方法大致可以分为：
1 对象身份与类型
2 ├── getClass()
3 ├── equals()
4 └── hashCode()
5
6 对象描述
7 └── toString()
8
9 对象复制
10 └── clone()
11
12 线程协作
13 ├── wait()
14 ├── notify()
15 └── notifyAll()
16

## 9.3 == 运算符

== 的语义取决于操作数类型。
### 9.3.1 基本类型的 ==
对于基本类型， == 比较数值是否相等：
1 int a = 10;
2 int b = 10;
3
4 System.out.println(a == b); // true
5

不同数值类型之间可能先发生类型提升：
1 int a = 10;
2 long b = 10L;
3
4 System.out.println(a == b); // true
5
这里 a 会提升为 long 后再比较。
### 9.3.2 引用类型的 ==
对于引用类型， == 比较：
两个引用值是否指向同一个对象。
1 User user1 = new User("A");
2 User user2 = user1;
3 User user3 = new User("A");
4
5 System.out.println(user1 == user2); // true
6 System.out.println(user1 == user3); // false
7
引用关系：
1 user1 ─────┐
2 ├────→ User 对象1
3 user2 ─────┘
4
5 user3 ──────────→ User 对象2
6
即使两个对象字段完全相同：
1 user1.name = "A";
2 user3.name = "A";
3
只要不是同一个对象：
1 user1 == user3
2
仍然是 false 。
### 9.3.3 null 比较
引用可以与 null 比较：
1 if (user == null) {
2 }

3
判断引用是否为空时，应使用：
1 user == null
2 user != null
3
而不是：
1 user.equals(null)
2
因为当 user == null 时，调用方法会抛出 NullPointerException 。

## 9.4 equals() 方法

Object 定义了：
1 public boolean equals(Object obj)
2
它用于表达对象之间的逻辑相等关系。
默认实现可以近似理解为：
1 public boolean equals(Object obj) {
2 return this == obj;
3 }
4
因此，如果一个类没有重写 equals() ：
1 User user1 = new User("A");
2 User user2 = new User("A");
3
4 System.out.println(user1.equals(user2));
5
通常结果是：
1 false
2
因为默认 equals 仍然判断是否为同一个对象。
### 9.4.1 身份相等与逻辑相等
对象可以有两种不同的相等概念。

身份相等
判断是否为同一个对象：
1 user1 == user2
2
逻辑相等
判断两个对象在业务意义上是否相等：
1 user1.equals(user2)
2
例如两个金额对象：
1 Money money1 = new Money("100.00", "CNY");
2 Money money2 = new Money("100.00", "CNY");
3
它们不是同一个对象：
1 money1 == money2 // false
2
但业务上可以认为相等：
1 money1.equals(money2) // true
2

## 9.5 什么时候应该重写 equals()

适合重写 equals 的典型对象：
值对象
DTO
配置对象
复合键
集合元素
Map Key
需要按字段比较的业务对象
测试中需要内容比较的对象
例如：
1 public final class Money {
2
3 private final BigDecimal amount;
4 private final Currency currency;
5 }

6
Money 的相等性通常由：
1 金额
2 +
3 币种
4
共同决定。
### 9.5.1 不一定需要重写 equals 的对象
部分对象更强调唯一身份：
连接对象
线程对象
IO 资源
某些服务对象
某些有生命周期的实体
仅用于内部行为封装的对象
对于这些对象，默认身份相等可能已经足够。
因此：
不是所有类都必须重写 equals 和 hashCode。
相等性必须根据对象语义设计。

## 9.6 equals() 的基本实现

示例：
1 public final class User {
2
3 private final String userId;
4 private final String name;
5
6 public User(String userId, String name) {
7 this.userId = userId;
8 this.name = name;
9 }
10
11 @Override
12 public boolean equals(Object other) {
13 if (this == other) {
14 return true;
15 }
16
17 if (!(other instanceof User user)) {
18 return false;
19 }
20
21 return Objects.equals(userId, user.userId)
22 && Objects.equals(name, user.name);
23 }
24 }
25

可以分为三个步骤。
### 9.6.1 第一步：判断是否为同一对象
1 if (this == other) {
2 return true;
3 }
4
优点：
同一对象必然相等
避免后续字段比较
提高常见场景性能
正确处理自身比较
### 9.6.2 第二步：判断类型
1 if (!(other instanceof User user)) {
2 return false;
3 }
4
同时处理：
other == null
类型不兼容
安全类型转换
因为：
1 null instanceof User
2
结果是 false 。
### 9.6.3 第三步：比较关键字段
1 return Objects.equals(userId, user.userId)
2 && Objects.equals(name, user.name);
3
Objects.equals(a, b) 可以安全处理 null：
1 Objects.equals(null, null) // true
2 Objects.equals(null, "A") // false
3 Objects.equals("A", "A") // true
4
近似逻辑：

1 a == b || (a != null && a.equals(b))
2

## 9.7 equals() 的契约

正确的 equals 必须满足以下性质：
1 自反性
2 对称性
3 传递性
4 一致性
5 非空性
6
### 9.7.1 自反性
任何非 null 对象都必须与自身相等：
1 x.equals(x) == true
2
错误示例：
1 @Override
2 public boolean equals(Object other) {
3 return false;
4 }
5
这会直接破坏集合和业务判断。
### 9.7.2 对称性
如果：
1 x.equals(y) == true
2
那么必须保证：
1 y.equals(x) == true
2
不能出现：
1 x 认为 y 相等
2 y 认为 x 不相等

3
### 9.7.3 传递性
如果：
1 x.equals(y) == true
2 y.equals(z) == true
3
则必须保证：
1 x.equals(z) == true
2
否则集合中的去重、查找和分组可能产生不一致结果。
### 9.7.4 一致性
只要参与 equals 比较的字段没有变化，多次调用结果应保持一致：
1 x.equals(y)
2 x.equals(y)
3 x.equals(y)
4
不应一会儿 true，一会儿 false。
错误示例：
1 @Override
2 public boolean equals(Object other) {
3 return Math.random() > 0.5;
4 }
5
也不应把当前时间、随机数或外部可变状态放入 equals。
### 9.7.5 非空性
任何非 null 对象都必须满足：
1 x.equals(null) == false
2
不应抛异常，也不能返回 true。

## 9.8 对称性破坏示例

父类：

1 public class Point {
2
3 private final int x;
4 private final int y;
5
6 public Point(int x, int y) {
7 this.x = x;
8 this.y = y;
9 }
10
11 @Override
12 public boolean equals(Object other) {
13 if (!(other instanceof Point point)) {
14 return false;
15 }
16
17 return x == point.x && y == point.y;
18 }
19 }
20
子类：
1 public class ColorPoint extends Point {
2
3 private final String color;
4
5 public ColorPoint(
6 int x,
7 int y,
8 String color
9 ) {
10 super(x, y);
11 this.color = color;
12 }
13
14 @Override
15 public boolean equals(Object other) {
16 if (!(other instanceof ColorPoint point)) {
17 return false;
18 }
19
20 return super.equals(point)
21 && Objects.equals(
22 color,
23 point.color
24 );
25 }
26 }
27
调用：
1 Point point = new Point(1, 2);
2
3 ColorPoint colorPoint =

4 new ColorPoint(1, 2, "RED");
5
可能出现：
1 point.equals(colorPoint) // true
2 colorPoint.equals(point) // false
3
对称性被破坏。
这说明：
可扩展类中的值相等性设计非常困难。

## 9.9 getClass() 与 instanceof

equals 类型判断常见两种写法。
### 9.9.1 使用 instanceof
1 if (!(other instanceof User user)) {
2 return false;
3 }
4
特点：
允许子类对象参与比较
支持父子类型相等
更灵活
容易因子类增加字段破坏对称性或传递性
### 9.9.2 使用 getClass()
1 if (other == null
2 || getClass() != other.getClass()) {
3 return false;
4 }
5
特点：
只有完全相同运行时类型才允许比较
更容易维护 equals 契约
父类对象与子类对象永远不相等
可能与 ORM 代理等框架类型发生冲突
完整示例：
1 @Override
2 public boolean equals(Object other) {
3 if (this == other) {
4 return true;
5 }
6

7 if (other == null
8 || getClass() != other.getClass()) {
9 return false;
10 }
11
12 User user = (User) other;
13
14 return Objects.equals(
15 userId,
16 user.userId
17 );
18 }
19
### 9.9.3 如何选择
对于不可继承的值对象：
1 public final class Money {
2 }
3
使用：
1 instanceof Money
2
通常较安全，因为不会出现子类扩展问题。
对于允许继承、但不同子类不应互相相等的类，可以考虑：
1 getClass() == other.getClass()
2
但框架实体、代理对象和继承模型需要结合实际设计。
工程上更简单的方式是：
需要稳定值相等性的类，优先设计成 final 不可变类。

## 9.10 equals 中选择哪些字段

equals 不应该机械比较所有字段。
应选择：
能够定义对象逻辑身份或值语义的稳定字段。
例如 Money：
1 amount
2 currency
3
通常应该参与相等性。
但以下字段通常不适合：
创建时间
修改时间

缓存字段
统计字段
临时状态
日志追踪信息
懒加载对象
运行时计算结果
### 9.10.1 值对象
值对象没有独立身份，其相等性由全部业务值决定。
例如坐标：
1 public final class Coordinate {
2
3 private final int x;
4 private final int y;
5 }
6
相等条件：
1 x 相等
2 并且
3 y 相等
4
### 9.10.2 实体对象
实体通常具有稳定身份：
1 public class Order {
2
3 private OrderId orderId;
4 private OrderStatus status;
5 private List<OrderItem> items;
6 }
7
相等性可能只由：
1 orderId
2
决定。
状态和订单项发生变化，不代表订单变成另一个实体。
因此：
1 order1.equals(order2)
2

是否相等，通常取决于二者是否代表同一业务订单。
### 9.10.3 数据库自增 ID 的问题
实体创建前，数据库 ID 可能为空：
1 Order order1 = new Order();
2 Order order2 = new Order();
3
二者：
1 id = null
2
如果简单写：
1 Objects.equals(id, other.id)
2
两个未持久化对象会被错误判断为相等。
可能的处理方式包括：
使用创建时生成的业务 ID
未分配 ID 时仅进行身份比较
延迟将对象用作 Set 元素或 Map Key
避免依赖数据库 ID 设计内存对象相等性
示例：
1 @Override
2 public boolean equals(Object other) {
3 if (this == other) {
4 return true;
5 }
6
7 if (!(other instanceof Order order)) {
8 return false;
9 }
10
11 return orderId != null
12 && orderId.equals(order.orderId);
13 }
14
但这仍然要与 hashCode 设计保持一致。

## 9.11 hashCode()

Object 定义：
1 public native int hashCode();
2

hashCode() 返回一个整数散列值。
它主要用于：
HashMap
HashSet
Hashtable
ConcurrentHashMap
其他哈希结构
哈希值用于快速缩小查找范围，但不直接代表对象唯一身份。
### 9.11.1 hashCode 不是对象内存地址
不应机械认为：
1 hashCode = 对象内存地址
2
Java 规范不要求 hashCode 必须等于内存地址。
对象还可能在内存中移动，而 hashCode 契约要求在相关状态不变时保持稳定。
更准确的说法：
Object 默认 hashCode 通常与对象身份有关，但具体生成方式由 JVM 实现决定。
### 9.11.2 hashCode 不保证唯一
不同对象可能拥有相同 hashCode：
1 对象数量可以远大于 int 可表示的散列值数量
2
这种情况称为哈希冲突。
因此：
1 a.hashCode() == b.hashCode()
2
不能推出：
1 a.equals(b)
2

## 9.12 equals 与 hashCode 的契约

核心规则：
如果两个对象通过 equals 判断相等，它们的 hashCode 必须相同。
即：
1 a.equals(b) == true
2
必须保证：

1 a.hashCode() == b.hashCode()
2
反过来不成立：
1 a.hashCode() == b.hashCode()
2
不一定意味着：
1 a.equals(b) == true
2
因为哈希冲突允许存在。
### 9.12.1 契约总结
1 equals 相等
2 → hashCode 必须相等
3
4 hashCode 相等
5 → equals 不一定相等
6
7 equals 不相等
8 → hashCode 可以相同，也可以不同
9

## 9.13 为什么重写 equals 后必须重写 hashCode

错误示例：
1 public final class User {
2
3 private final String userId;
4
5 public User(String userId) {
6 this.userId = userId;
7 }
8
9 @Override
10 public boolean equals(Object other) {
11 if (!(other instanceof User user)) {
12 return false;
13 }
14
15 return Objects.equals(
16 userId,
17 user.userId
18 );
19 }

20 }
21
没有重写 hashCode。
创建：
1 User user1 = new User("U001");
2 User user2 = new User("U001");
3
4 System.out.println(user1.equals(user2));
5
结果：
1 true
2
但默认 hashCode 可能不同：
1 user1.hashCode() != user2.hashCode()
2
放入 HashSet：
1 Set<User> users = new HashSet<>();
2 users.add(user1);
3
4 System.out.println(users.contains(user2));
5
可能输出：
1 false
2
虽然逻辑上 user1 与 user2 相等。
原因是 HashSet 通常先根据 hashCode 定位桶，再使用 equals 比较。
两个对象进入不同哈希位置后，可能根本不会互相比较。

## 9.14 正确重写 hashCode

可以使用：
1 @Override
2 public int hashCode() {
3 return Objects.hash(userId, name);
4 }
5
完整示例：

1 public final class User {
2
3 private final String userId;
4 private final String name;
5
6 @Override
7 public boolean equals(Object other) {
8 if (this == other) {
9 return true;
10 }
11
12 if (!(other instanceof User user)) {
13 return false;
14 }
15
16 return Objects.equals(
17 userId,
18 user.userId
19 )
20 && Objects.equals(
21 name,
22 user.name
23 );
24 }
25
26 @Override
27 public int hashCode() {
28 return Objects.hash(
29 userId,
30 name
31 );
32 }
33 }
34
关键原则：
equals 使用哪些字段，hashCode 通常也应该使用同一组字段。
### 9.14.1 手动计算 hashCode
经典形式：
1 @Override
2 public int hashCode() {
3 int result = 17;
4
5 result = 31 * result
6 + Objects.hashCode(userId);
7
8 result = 31 * result
9 + Objects.hashCode(name);
10
11 return result;
12 }
13
其中 31 常见的原因包括：

奇数
质数
分布效果通常较好
可以通过移位和减法优化
但实际开发通常直接使用：
1 Objects.hash(...)
2
或 IDE 自动生成。
对极端性能敏感的场景，再考虑减少可变参数数组创建等开销。

## 9.15 HashMap 如何使用 hashCode 和 equals

假设：
1 Map<User, String> map =
2 new HashMap<>();
3
写入：
1 map.put(user1, "A");
2
读取：
1 map.get(user2);
2
简化流程：
1 计算 user2.hashCode()
2 ↓
3 根据哈希值定位桶
4 ↓
5 在桶内寻找候选键
6 ↓
7 比较 hashCode
8 ↓
9 调用 equals()
10 ↓
11 找到逻辑相等的键
12
因此：
hashCode() 用于快速定位范围
equals() 用于最终确认逻辑相等
### 9.15.1 为什么不能只使用 equals

如果集合中有一百万个对象，每次查找都逐个调用 equals：
1 时间复杂度接近线性扫描
2
通过 hashCode，可以先快速定位少量候选对象。
### 9.15.2 为什么不能只使用 hashCode
因为不同对象可能拥有相同 hashCode。
哈希冲突时，仍然需要 equals 判断是否为同一个逻辑键。

## 9.16 可变对象作为 HashMap Key 的风险

定义：
1 public class UserKey {
2
3 private String userId;
4
5 @Override
6 public boolean equals(Object other) {
7 // 使用 userId
8 }
9
10 @Override
11 public int hashCode() {
12 return Objects.hash(userId);
13 }
14
15 public void setUserId(String userId) {
16 this.userId = userId;
17 }
18 }
19
使用：
1 UserKey key = new UserKey("U001");
2
3 Map<UserKey, String> map =
4 new HashMap<>();
5
6 map.put(key, "data");
7
随后修改：
1 key.setUserId("U002");
2
再次查询：

1 map.get(key);
2
可能返回：
1 null
2
原因：
1 放入时：
2 hashCode 基于 U001
3 → 对象进入桶 A
4
5 修改后：
6 hashCode 基于 U002
7 → 查询桶 B
8
9 对象实际仍在桶 A
10 → 无法找到
11
因此：
作为 HashMap Key 或 HashSet 元素的对象，其 equals/hashCode 依赖字段应尽量保持不可变。
### 9.16.1 安全 Key 设计
推荐：
1 public final class UserKey {
2
3 private final String userId;
4
5 public UserKey(String userId) {
6 this.userId =
7 Objects.requireNonNull(userId);
8 }
9
10 @Override
11 public boolean equals(Object other) {
12 if (this == other) {
13 return true;
14 }
15
16 if (!(other instanceof UserKey key)) {
17 return false;
18 }
19
20 return userId.equals(key.userId);
21 }
22
23 @Override
24 public int hashCode() {
25 return userId.hashCode();

26 }
27 }
28
特点：
final 类
final 字段
无 Setter
相等性稳定
适合作为 Map Key

## 9.17 BigDecimal 的 equals 特殊性

1 BigDecimal a =
2 new BigDecimal("1.0");
3
4 BigDecimal b =
5 new BigDecimal("1.00");
6
执行：
1 a.equals(b)
2
结果通常是：
1 false
2
因为 BigDecimal 的 equals 同时考虑：
数值
scale
即：
1 1.0
2 和
3 1.00
4
在 equals 语义上不同。
但：
1 a.compareTo(b)
2
结果是：
1 0

2
表示数值大小相等。
因此金额判断需要明确：
1 判断严格表示相等
2 → equals()
3
4 判断数值大小相等
5 → compareTo() == 0
6
这也说明：
equals 的语义由具体类定义，不能假设所有对象的 equals 都只比较数学值。

## 9.18 数组的 equals

数组继承 Object，但没有重写为元素内容比较。
1 int[] a = {1, 2, 3};
2 int[] b = {1, 2, 3};
3
4 System.out.println(a.equals(b));
5
通常结果：
1 false
2
因为数组默认 equals 仍是身份比较。
比较数组内容应使用：
1 Arrays.equals(a, b)
2
多维数组：
1 Arrays.deepEquals(a, b)
2
### 9.18.1 数组 hashCode
数组默认 hashCode 通常也是身份语义。
计算元素内容哈希应使用：
1 Arrays.hashCode(array)
2

多维数组：
1 Arrays.deepHashCode(array)
2

## 9.19 集合的 equals

Java 集合通常已经定义了内容相等语义。
例如 List：
1 List<String> a =
2 List.of("A", "B");
3
4 List<String> b =
5 List.of("A", "B");
6
7 System.out.println(a.equals(b));
8
结果：
1 true
2
List 通常比较：
元素数量
元素顺序
每个位置的元素是否相等
### 9.19.1 Set 的 equals
1 Set<String> a =
2 Set.of("A", "B");
3
4 Set<String> b =
5 Set.of("B", "A");
6
通常：
1 a.equals(b)
2
结果为 true。
Set 更关注：
元素集合是否相同
不关心元素顺序

### 9.19.2 Map 的 equals
Map 通常比较：
键值映射关系是否一致
Key 是否相等
Value 是否相等
不同 Map 实现之间也可能逻辑相等。
这体现：
equals 描述的是抽象数据语义，不一定要求具体实现类相同。

## 9.20 包装类型的 equals

包装类型通常按包装值比较：
1 Integer a = 1000;
2 Integer b = 1000;
3
4 System.out.println(a.equals(b)); // true
5
而：
1 a == b
2
比较引用身份，结果可能为 false。
因此包装类型比较数值应优先：
1 Objects.equals(a, b)
2
或者在明确非 null 时：
1 a.equals(b)
2
不要依赖缓存范围判断 == 。

## 9.21 String 的 equals

String 重写了 equals，比较字符序列内容：
1 String a = new String("Java");
2 String b = new String("Java");
3
4 System.out.println(a == b); // false
5 System.out.println(a.equals(b)); // true
6
因此：

1 == → 是否同一个 String 对象
2 equals → 字符内容是否相同
3
字符串常量池会让部分 == 结果看起来为 true，但不应使用 == 判断字符串内容。
字符串完整机制放到高级语言特性笔记。

## 9.22 Objects.equals()

直接调用：
1 a.equals(b)
2
要求 a 非 null。
更安全：
1 Objects.equals(a, b)
2
其行为：
a b 结果
null null true
null 非 null false
非 null null false
非 null 非 null a.equals(b)
业务代码中，比较可能为空的引用时，推荐：
1 Objects.equals(expected, actual)
2
### 9.22.1 常量放前面的写法
传统上常见：
1 "SUCCESS".equals(status)
2
可以防止：
1 status.equals("SUCCESS")
2
在 status 为 null 时抛 NPE。
现代代码也可以使用：

1 Objects.equals(status, "SUCCESS")
2
但状态值更适合使用枚举而不是字符串魔法值。
