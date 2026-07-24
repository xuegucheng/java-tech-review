# 第9章：toString 与对象工具方法

## 9.23 Objects.deepEquals()

Objects.deepEquals(a, b) 可以处理数组：
1 int[] a = {1, 2};
2 int[] b = {1, 2};
3
4 System.out.println(
5 Objects.deepEquals(a, b)
6 );
7
结果通常为 true。
对于普通对象，则仍然依赖 equals。

## 9.24 toString()

Object 默认的 toString() 通常生成类似：
1 com.example.User@1a2b3c
2
形式通常包含：
1 类名
2 @
3 十六进制形式的哈希相关值
4
但不应依赖默认格式作为稳定协议。
### 9.24.1 为什么重写 toString
默认输出缺乏业务信息：
1 System.out.println(user);
2
可能只看到：
1 com.example.User@5f184fc6
2
重写后：

1 @Override
2 public String toString() {
3 return "User{"
4 + "userId='" + userId + '\''
5 + ", name='" + name + '\''
6 + '}';
7 }
8
输出：
1 User{userId='U001', name='Java'}
2
有利于：
调试
日志
测试失败分析
IDE 查看对象
问题定位
### 9.24.2 toString 不应泄露敏感信息
不应输出：
密码
Token
API Key
身份证号
银行卡号
完整手机号
私钥
Cookie
Authorization Header
错误：
1 @Override
2 public String toString() {
3 return "User{"
4 + "password='" + password + '\''
5 + ", token='" + token + '\''
6 + '}';
7 }
8
日志可能长期保存并被多人访问。
### 9.24.3 toString 不应承担业务序列化协议
不建议把 toString() 当成：
JSON 序列化
数据库存储格式
网络传输协议

缓存 Key 协议
签名原文
因为 toString 主要用于人类可读描述，格式可能随代码调整。
需要稳定格式时，应使用明确的：
JSON 序列化
DTO
协议对象
专用 format 方法
### 9.24.4 toString 应避免复杂副作用
不推荐在 toString 中：
查询数据库
发起网络请求
修改对象状态
执行耗时计算
抛出业务异常
访问可能未初始化的懒加载属性
日志框架、IDE 或调试器可能自动调用 toString。
因此应保持：
1 轻量
2 稳定
3 无副作用
4

## 9.25 getClass()

Object 定义：
1 public final native Class<?> getClass();
2
用于获取对象的运行时类型：
1 User user = new User();
2
3 Class<?> type = user.getClass();
4
例如：
1 System.out.println(
2 user.getClass().getName()
3 );
4
可能输出：
1 com.example.User

2
### 9.25.1 getClass 返回运行时类型
1 Animal animal = new Dog();
2
3 System.out.println(
4 animal.getClass()
5 );
6
返回的是 Dog 的 Class 对象，而不是 Animal。
因为：
1 编译时类型：Animal
2 运行时类型：Dog
3
getClass() 获取运行时类型。
### 9.25.2 getClass() 与 instanceof
1 animal instanceof Animal
2
判断对象是否属于某类型或其子类型。
1 animal.getClass() == Animal.class
2
判断运行时类型是否精确等于 Animal。
例如 Dog：
1 dog instanceof Animal // true
2 dog.getClass() == Animal.class // false
3
### 9.25.3 getClass() 不能被重写
getClass() 是 final 方法。
对象不能伪造自己的实际运行时 Class。
Class 与反射机制后续单独展开。

## 9.26 clone()

Object 提供：

1 protected native Object clone()
2 throws CloneNotSupportedException;
3
clone 用于复制对象，但其设计存在较多限制。
### 9.26.1 Cloneable 标记接口
要正常调用 Object.clone()，类通常需要实现：
1 Cloneable
2
示例：
1 public class User implements Cloneable {
2
3 private String name;
4
5 @Override
6 public User clone() {
7 try {
8 return (User) super.clone();
9 } catch (
10 CloneNotSupportedException exception
11 ) {
12 throw new AssertionError(exception);
13 }
14 }
15 }
16
Cloneable 本身没有声明 clone 方法，它只是告诉 Object.clone：
1 允许进行字段级复制
2
这是一种标记接口。
### 9.26.2 clone 默认是浅拷贝
1 public class Order implements Cloneable {
2
3 private String orderNo;
4 private List<String> items;
5 }
6
执行 clone 后：

1 原 Order 对象
2 └── items ──┐
3 ├──→ 同一个 List
4 克隆 Order 对象
5 └── items ──┘
6
外层对象被复制，但内部引用字段仍然共享。
因此：
1 clonedOrder.getItems().add("B");
2
可能影响原对象。
### 9.26.3 为什么通常不推荐 clone
clone 的问题包括：
Cloneable 接口没有定义 clone 方法
Object.clone 是 protected
默认只做浅拷贝
构造方法通常不会按普通方式执行
深拷贝需要手工处理
继承体系下容易出错
可变字段复制语义不明确
异常处理不自然
更推荐：
拷贝构造方法
1 public Order(Order source) {
2 this.orderNo = source.orderNo;
3 this.items =
4 new ArrayList<>(source.items);
5 }
6
静态复制工厂
1 public static Order copyOf(Order source) {
2 return new Order(
3 source.orderNo,
4 source.items
5 );
6 }
7
显式映射

1 OrderCopyMapper.copy(source);
2
这些方式的复制语义更清晰。

## 9.27 wait()、notify() 与 notifyAll()

Object 定义了：
1 wait()
2 notify()
3 notifyAll()
4
原因是：
Java 中任何对象都可以作为监视器锁对象。
例如：
1 synchronized (lock) {
2 lock.wait();
3 }
4
唤醒：
1 synchronized (lock) {
2 lock.notifyAll();
3 }
4
这些方法必须在持有对象监视器时调用，否则会抛出：
1 IllegalMonitorStateException
2
完整机制包括：
Monitor
Wait Set
锁释放
虚假唤醒
条件循环
notify 与 notifyAll
已放在并发编程笔记中，本章只理解：
它们属于 Object，是因为任意对象都可以充当同步监视器。

## 9.28 对象终结机制

历史上 Object 提供过对象终结相关机制，但它存在：
执行时间不确定
不保证及时执行

可能永远不执行
性能开销较高
容易导致对象复活
异常处理困难
资源释放不可靠
因此不能依赖对象被垃圾回收来关闭：
文件
数据库连接
Socket
线程池
锁
本地资源
资源管理应使用：
1 try-with-resources
2
例如：
1 try (
2 InputStream input =
3 Files.newInputStream(path)
4 ) {
5 // 使用资源
6 }
7
对象终结机制只作为历史知识了解，不应作为现代资源管理方案。

## 9.29 identityHashCode()

System 提供：
1 System.identityHashCode(object)
2
它用于获得基于对象身份的哈希值，不受类重写 hashCode 影响。
例如：
1 public class User {
2
3 @Override
4 public int hashCode() {
5 return 1;
6 }
7 }
8
调用：
1 User user = new User();

2
3 System.out.println(user.hashCode());
4 System.out.println(
5 System.identityHashCode(user)
6 );
7
两个结果可能不同。
适合：
调试对象身份
分析引用关系
身份型数据结构
排查对象重复创建
不应把它作为业务唯一 ID。

## 9.30 IdentityHashMap

普通 HashMap 使用：
1 hashCode()
2 +
3 equals()
4
判断键。
IdentityHashMap 使用：
1 引用身份
2 ==
3
判断键。
示例：
1 String a = new String("Java");
2 String b = new String("Java");
3
普通 HashMap：
1 Map<String, Integer> map =
2 new HashMap<>();
3
4 map.put(a, 1);
5 map.put(b, 2);
6
7 System.out.println(map.size());
8
通常为：
1 1

2
因为：
1 a.equals(b) == true
2
IdentityHashMap：
1 Map<String, Integer> map =
2 new IdentityHashMap<>();
3
4 map.put(a, 1);
5 map.put(b, 2);
6
7 System.out.println(map.size());
8
通常为：
1 2
2
因为：
1 a != b
2
IdentityHashMap 适合少数需要身份语义的场景，不应替代普通 HashMap。

## 9.31 equals 与继承的设计难题

假设父类 equals 使用：
1 instanceof Parent
2
那么子类对象可能与父类对象相等。
但子类新增字段后：
1 class Child extends Parent {
2
3 private String extra;
4 }
5
会出现问题：
忽略 extra：子类不同状态可能被判相等
比较 extra：可能破坏与父类的对称性
限定同类型：父类与子类不再可相等

因此，对具有值语义的类型，常见建议是：
1 final 类
2 +
3 不可变字段
4 +
5 明确 equals/hashCode
6
例如：
1 public final class Money {
2 }
3
避免在可扩展继承体系中定义复杂值相等性。

## 9.32 canEqual 模式概览

某些继承体系会使用 canEqual() 尝试维护对称性。
父类：
1 public class Point {
2
3 protected boolean canEqual(
4 Object other
5 ) {
6 return other instanceof Point;
7 }
8
9 @Override
10 public boolean equals(Object other) {
11 if (!(other instanceof Point point)) {
12 return false;
13 }
14
15 return point.canEqual(this)
16 && x == point.x
17 && y == point.y;
18 }
19 }
20
子类重写：
1 @Override
2 protected boolean canEqual(
3 Object other
4 ) {
5 return other instanceof ColorPoint;
6 }
7
这种方式可以处理部分继承相等性问题，但设计复杂。

普通业务代码更推荐：
值对象禁止继承
使用组合
不同类型不互相相等
使用明确业务 ID 判断实体身份

## 9.33 Lombok 与自动生成 equals/hashCode

Lombok 可以通过注解生成：
1 @EqualsAndHashCode
2
或者：
1 @Data
2
但自动生成前必须确认：
哪些字段参与相等性
是否包含父类字段
是否包含可变字段
是否存在懒加载字段
是否是 ORM 实体
是否可能用作 HashMap Key
是否存在循环引用
是否包含大集合
不应因为方便就机械生成全部字段相等性。
例如实体对象包含：
1 List<OrderItem> items
2
如果自动加入 equals/hashCode：
比较成本可能很高
懒加载可能被触发
集合修改会改变 hashCode
双向关联可能递归
日志和调试可能出现栈溢出

## 9.34 实体对象的相等性设计

实体强调：
1 同一身份
2
而不是所有字段相同。
例如订单：

1 public class Order {
2
3 private final OrderId orderId;
4 private OrderStatus status;
5 }
6
即使状态变化：
1 CREATED
2 → PROCESSING
3 → COMPLETED
4
仍然是同一个订单。
因此实体相等性通常基于：
1 稳定业务 ID
2
而不是所有可变字段。
### 9.34.1 业务 ID 优于可变字段
不推荐：
1 return Objects.equals(status, other.status)
2 && Objects.equals(items, other.items)
3 && Objects.equals(address, other.address);
4
这些字段可能随业务变化。
更合理：
1 return Objects.equals(
2 orderId,
3 other.orderId
4 );
5
前提是 orderId：
创建时就存在
全局或业务范围唯一
生命周期内不改变
不依赖数据库持久化后才生成

## 9.35 值对象的相等性设计

值对象强调：

1 所有业务值相同
2
例如：
1 public final class Address {
2
3 private final String province;
4 private final String city;
5 private final String detail;
6 }
7
相等性可以基于：
1 province
2 city
3 detail
4
值对象通常适合：
final 类
final 字段
不可变
完整重写 equals/hashCode
修改时创建新对象
### 9.35.1 值对象没有独立身份
两个分别创建的地址对象：
1 Address a =
2 new Address(
3 "广东",
4 "深圳",
5 "南山区"
6 );
7
8 Address b =
9 new Address(
10 "广东",
11 "深圳",
12 "南山区"
13 );
14
虽然：
1 a != b
2
但业务上：

1 a.equals(b)
2
应为 true。

## 9.36 equals 的性能考虑

equals 可能被频繁调用，例如：
HashMap 查找
HashSet 去重
集合 contains
列表 remove
单元测试断言
因此 equals 应尽量：
无副作用
不访问数据库
不进行网络请求
不依赖外部服务
不执行超大对象图比较
先比较成本较低的字段
先使用 this == other
例如：
1 return id == other.id
2 && Objects.equals(code, other.code)
3 && Objects.equals(details, other.details);
4
可以先比较：
1 便宜且区分度高的字段
2
再比较复杂字段。

## 9.37 toString、equals、hashCode 中的循环引用

双向关联：
1 class Parent {
2
3 private List<Child> children;
4 }
5
1 class Child {
2
3 private Parent parent;
4 }

5
如果自动生成 toString：
1 Parent.toString()
2 → Child.toString()
3 → Parent.toString()
4 → ...
5
可能导致：
1 StackOverflowError
2
equals/hashCode 同样可能递归。
因此：
不要机械包含全部关联字段
双向关系中至少一侧排除
实体优先使用稳定 ID
日志只输出必要摘要
避免打印完整对象图

## 9.38 建议实验

实验一：默认 equals 是身份比较
1 public class DefaultEqualsDemo {
2
3 public static void main(String[] args) {
4 User user1 = new User("A");
5 User user2 = new User("A");
6
7 System.out.println(user1 == user2);
8 System.out.println(
9 user1.equals(user2)
10 );
11 }
12
13 static class User {
14
15 private final String name;
16
17 User(String name) {
18 this.name = name;
19 }
20 }
21 }
22
预期：

1 false
2 false
3
实验二：重写 equals
1 public class EqualsDemo {
2
3 public static void main(String[] args) {
4 User user1 = new User("U001");
5 User user2 = new User("U001");
6
7 System.out.println(
8 user1.equals(user2)
9 );
10 }
11
12 static final class User {
13
14 private final String userId;
15
16 User(String userId) {
17 this.userId = userId;
18 }
19
20 @Override
21 public boolean equals(Object other) {
22 if (this == other) {
23 return true;
24 }
25
26 if (!(other instanceof User user)) {
27 return false;
28 }
29
30 return Objects.equals(
31 userId,
32 user.userId
33 );
34 }
35
36 @Override
37 public int hashCode() {
38 return Objects.hash(userId);
39 }
40 }
41 }
42
预期：
1 true
2

实验三：只重写 equals 的 HashSet 问题
创建一个只重写 equals、不重写 hashCode 的类：
1 static class User {
2
3 private final String userId;
4
5 User(String userId) {
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
执行：
1 Set<User> users = new HashSet<>();
2
3 users.add(new User("U001"));
4 users.add(new User("U001"));
5
6 System.out.println(users.size());
7
观察结果，理解为什么逻辑相等对象仍可能同时存在。
实验四：可变 Key 失效
1 public class MutableKeyDemo {
2
3 public static void main(String[] args) {
4 UserKey key = new UserKey("U001");
5
6 Map<UserKey, String> map =
7 new HashMap<>();
8
9 map.put(key, "data");
10
11 System.out.println(map.get(key));
12
13 key.userId = "U002";
14

15 System.out.println(map.get(key));
16 }
17
18 static class UserKey {
19
20 private String userId;
21
22 UserKey(String userId) {
23 this.userId = userId;
24 }
25
26 @Override
27 public boolean equals(Object other) {
28 if (!(other instanceof UserKey key)) {
29 return false;
30 }
31
32 return Objects.equals(
33 userId,
34 key.userId
35 );
36 }
37
38 @Override
39 public int hashCode() {
40 return Objects.hash(userId);
41 }
42 }
43 }
44
观察修改字段后 Map 查找失败。
实验五：数组 equals
1 public class ArrayEqualsDemo {
2
3 public static void main(String[] args) {
4 int[] a = {1, 2, 3};
5 int[] b = {1, 2, 3};
6
7 System.out.println(a.equals(b));
8
9 System.out.println(
10 Arrays.equals(a, b)
11 );
12 }
13 }
14
预期：
1 false
2 true
3

实验六：String 的 == 与 equals
1 public class StringEqualsDemo {
2
3 public static void main(String[] args) {
4 String a = new String("Java");
5 String b = new String("Java");
6
7 System.out.println(a == b);
8 System.out.println(a.equals(b));
9 }
10 }
11
预期：
1 false
2 true
3
实验七：BigDecimal equals 与 compareTo
1 public class BigDecimalEqualsDemo {
2
3 public static void main(String[] args) {
4 BigDecimal a =
5 new BigDecimal("1.0");
6
7 BigDecimal b =
8 new BigDecimal("1.00");
9
10 System.out.println(a.equals(b));
11
12 System.out.println(
13 a.compareTo(b) == 0
14 );
15 }
16 }
17
预期：
1 false
2 true
3
实验八：toString 敏感信息泄露

1 public class ToStringDemo {
2
3 static class User {
4
5 private String username;
6 private String password;
7 private String token;
8
9 @Override
10 public String toString() {
11 return "User{"
12 + "username='" + username + '\''
13 + ", password='" + password + '\''
14 + ", token='" + token + '\''
15 + '}';
16 }
17 }
18 }
19
分析该实现为什么不适合生产日志。
改为：
1 @Override
2 public String toString() {
3 return "User{"
4 + "username='" + username + '\''
5 + '}';
6 }
7
实验九：浅克隆共享内部对象
1 public class CloneDemo {
2
3 public static void main(String[] args) {
4 Order source = new Order();
5 source.items.add("A");
6
7 Order copy = source.clone();
8 copy.items.add("B");
9
10 System.out.println(source.items);
11 System.out.println(copy.items);
12 }
13
14 static class Order implements Cloneable {
15
16 private List<String> items =
17 new ArrayList<>();
18
19 @Override
20 public Order clone() {
21 try {

22 return (Order) super.clone();
23 } catch (
24 CloneNotSupportedException exception
25 ) {
26 throw new AssertionError(exception);
27 }
28 }
29 }
30 }
31
预期两个对象都看到：
1 [A, B]
2
说明默认 clone 是浅拷贝。
实验十：IdentityHashMap
1 public class IdentityMapDemo {
2
3 public static void main(String[] args) {
4 String a = new String("Java");
5 String b = new String("Java");
6
7 Map<String, Integer> normal =
8 new HashMap<>();
9
10 normal.put(a, 1);
11 normal.put(b, 2);
12
13 Map<String, Integer> identity =
14 new IdentityHashMap<>();
15
16 identity.put(a, 1);
17 identity.put(b, 2);
18
19 System.out.println(normal.size());
20 System.out.println(identity.size());
21 }
22 }
23
预期：
1 1
2 2
3

## 9.39 高频面试题

本章建议保留以下问题：
1.Object 是什么？
2.所有 Java 类都继承 Object 吗？
3.Object 有哪些常见方法？
4.基本类型和引用类型使用 == 分别比较什么？
5. == 与 equals() 有什么区别？
6.Object 默认 equals 比较什么？
7.什么情况下应该重写 equals？
8.equals 必须满足哪些契约？
9.什么是 equals 的自反性？
10.什么是 equals 的对称性？
11.什么是 equals 的传递性？
12.equals 为什么不能依赖随机值和外部状态？
13. x.equals(null) 应该返回什么？
14.equals 中使用 instanceof 和 getClass() 有什么区别？
15.为什么可继承类的 equals 很难设计？
16.为什么重写 equals 后必须重写 hashCode？
17.equals 与 hashCode 的契约是什么？
18.hashCode 相等是否代表 equals 相等？
19.equals 相等时 hashCode 是否必须相等？
20.hashCode 是否是对象内存地址？
21.hashCode 是否保证唯一？
22.HashMap 如何使用 hashCode 和 equals？
23.为什么不能只使用 hashCode 判断对象相等？
24.为什么可变对象不适合作为 HashMap Key？
25.哪些字段适合参与 equals 和 hashCode？
26.实体和值对象的相等性有什么区别？
27.数据库自增 ID 参与 equals 有什么问题？
28.String 的 == 和 equals 有什么区别？
29.包装类型应该如何比较？
30.BigDecimal 的 equals 和 compareTo 有什么区别？
31.数组应该如何比较内容？
32. Objects.equals() 有什么作用？
33. Objects.deepEquals() 有什么作用？
34. toString() 的默认格式是什么？
35.为什么需要重写 toString？
36.toString 中为什么不能输出密码和 Token？
37.toString 能否作为稳定序列化协议？
38.getClass() 返回编译时类型还是运行时类型？
39. instanceof 与 getClass 精确判断有什么区别？
40.clone 默认是深拷贝还是浅拷贝？
41.Cloneable 接口中是否定义了 clone 方法？
42.为什么通常不推荐使用 clone？
43.wait、notify 为什么定义在 Object 中？
44.调用 wait、notify 为什么必须持有对象锁？
45.System.identityHashCode 有什么作用？
46.IdentityHashMap 与 HashMap 有什么区别？
47.Lombok 自动生成 equals/hashCode 有什么风险？
48.双向关联为什么可能导致 toString 或 equals 栈溢出？

## 9.40 易错点

误区一：Object 是接口
错误。
Object 是 Java 类体系的根类。
### 9.40.2 基本类型也是 Object 子类
错误。
基本类型不是对象。
赋值给 Object 时会发生自动装箱。
误区三：引用类型的 == 比较对象内容
错误。
引用类型的 == 比较是否指向同一个对象。
误区四：所有类默认 equals 都比较字段内容
错误。
Object 默认 equals 近似于 this == obj 。
误区五：重写 equals 就足够了
错误。
只要对象可能进入哈希容器，就必须同时正确重写 hashCode。
实际上从契约角度，只要重写 equals，就应该同步重写 hashCode。
误区六：hashCode 相等代表对象相等
错误。
哈希冲突允许不同对象拥有相同 hashCode。
误区七：对象不相等时 hashCode 必须不同
错误。
equals 不相等的对象可以拥有相同 hashCode。
误区八：hashCode 就是对象地址
错误。
规范不要求 hashCode 等于内存地址。
误区九：Map Key 修改字段后不影响查找
错误。
如果修改了参与 hashCode 的字段，键可能无法从原桶中找到。
误区十：数组 equals 比较数组内容
错误。
数组没有按元素重写 equals，应使用 Arrays.equals。
误区十一：BigDecimal 数值相等时 equals 一定为 true
错误。
BigDecimal.equals 还会比较 scale。

误区十二：final 类可以彻底解决 equals 设计
不完整。
final 可以避免继承破坏相等性，但仍需正确选择字段并维护 hashCode 契约。
误区十三：toString 可以随意打印所有字段
错误。
敏感字段和大型对象图不应输出。
误区十四：toString 适合作为业务数据协议
错误。
toString 主要用于人类可读描述，格式不应视为稳定协议。
误区十五：clone 会自动深拷贝
错误。
Object.clone 默认更接近字段级浅拷贝。
误区十六：Cloneable 定义了 clone 方法
错误。
Cloneable 是标记接口，本身没有声明方法。
误区十七：getClass 返回变量声明类型
错误。
getClass 返回对象的运行时类型。
误区十八：wait 和 notify 属于 Thread
错误。
它们定义在 Object 中，因为任意对象都能作为监视器。
误区十九：System.identityHashCode 是业务唯一 ID
错误。
它只适合对象身份相关的底层或调试场景，不保证全局唯一。
误区二十：自动生成 equals/hashCode 永远安全
错误。
自动生成可能错误包含可变字段、集合、懒加载属性或双向关联。

## 9.41 工程实践建议

### 9.41.1 值对象优先设计为不可变
推荐：
1 public final class OrderId {
2
3 private final String value;
4 }

5
配合稳定的 equals/hashCode。
### 9.41.2 实体相等性优先使用稳定业务 ID
推荐：
1 private final OrderId orderId;
2
避免使用：
status
更新时间
集合字段
数据库延迟生成 ID
随机变化字段
作为相等性基础。
### 9.41.3 equals 和 hashCode 使用同一组字段
1 @Override
2 public boolean equals(Object other) {
3 // userId、tenantId
4 }
5
6 @Override
7 public int hashCode() {
8 return Objects.hash(
9 userId,
10 tenantId
11 );
12 }
13
避免字段集合不一致。
### 9.41.4 Map Key 应保持不可变
推荐：
1 public record InventoryKey(
2 String warehouseCode,
3 String skuCode
4 ) {
5 }
6
不推荐使用可以随时修改字段的普通 JavaBean 作为 Key。
### 9.41.5 toString 只输出定位所需信息

推荐输出：
业务 ID
类型
关键状态
少量必要字段
避免输出：
密码
Token
大型集合
二进制内容
完整对象图
懒加载关联
### 9.41.6 不要在 equals 中访问外部资源
禁止：
1 @Override
2 public boolean equals(Object other) {
3 return repository.exists(...);
4 }
5
equals 应：
1 快速
2 稳定
3 纯内存
4 无副作用
5
### 9.41.7 不要机械使用 @Data
对于实体、聚合根、双向关联对象，谨慎使用会自动生成：
equals
hashCode
toString
Setter
的注解。
应明确控制：
相等字段
日志字段
状态修改入口
关联对象
### 9.41.8 复制对象优先使用明确 API
推荐：
1 Order copy = Order.copyOf(source);
2

或者：
1 Order copy = new Order(source);
2
优于依赖 clone 的隐式复制语义。

## 9.42 本章知识链路

1 Object 是所有普通类的根类
2 ↓
3 对象默认拥有基础协议
4 ↓
5 == 比较值或引用身份
6 ↓
7 Object.equals 默认仍是身份比较
8 ↓
9 业务对象重写 equals 定义逻辑相等
10 ↓
11 equals 必须满足五项契约
12 ↓
13 同时重写 hashCode
14 ↓
15 哈希容器先定位桶，再调用 equals
16 ↓
17 相等性依赖字段必须稳定
18 ↓
19 实体和值对象采用不同相等策略
20
== 与 equals：
1 基本类型 ==
2 → 数值比较
3
4 引用类型 ==
5 → 对象身份比较
6
7 equals
8 → 由类定义逻辑相等语义
9
equals 与 hashCode：
1 equals 相等
2 → hashCode 必须相等
3
4 hashCode 相等
5 → equals 不一定相等
6
对象设计：

1 实体对象
2 → 使用稳定身份判断相等
3
4 值对象
5 → 使用全部业务值判断相等
6
7 HashMap Key
8 → 相等性字段尽量不可变
9
面试口述版：
Object 是 Java 普通类体系的根类，默认提供 equals、hashCode、toString、getClass、clone 以及线程协作相关方法。对于基本类型， == 比较数值；对于引用类
型， == 比较两个引用是否指向同一对象。Object 默认的 equals 本质上也是身份比较，业务对象需要根据自身语义决定是否重写。equals 必须满足自反性、对称
性、传递性、一致性和非空性。重写 equals 后必须同步重写 hashCode，并保证 equals 相等的对象 hashCode 一定相等。HashMap 和 HashSet 会先通过 hashCode
定位候选位置，再通过 equals 确认逻辑相等，因此参与 equals 和 hashCode 的字段应保持稳定，尤其不应随意修改作为 Map Key 的对象。值对象通常按全部业务值
判断相等，实体对象通常按稳定业务身份判断相等。toString 应提供有用的调试信息，但不能泄露敏感数据；clone 默认是浅拷贝，工程上通常更推荐拷贝构造方法或
静态复制工厂。
