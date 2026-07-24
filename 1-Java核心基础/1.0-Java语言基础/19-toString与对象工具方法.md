# toString 与对象工具方法

## 9.23 Objects.deepEquals()

Objects.deepEquals(a, b) 可以处理数组：

```
int[] a = {1, 2};
int[] b = {1, 2};
```

3

```
System.out.println(
Objects.deepEquals(a, b)
);
```

7
结果通常为 true。
对于普通对象，则仍然依赖 equals。

## 9.24 toString()

Object 默认的 toString() 通常生成类似：

```
com.example.User@1a2b3c
```

2
形式通常包含：

```
类名
@
十六进制形式的哈希相关值
```

4
但不应依赖默认格式作为稳定协议。

**9.24.1 为什么重写 toString**

默认输出缺乏业务信息：

```
System.out.println(user);
```

2
可能只看到：

```
com.example.User@5f184fc6
```

2
重写后：

```
@Override
public String toString() {
return "User{"
+ "userId='" + userId + '\''
+ ", name='" + name + '\''
+ '}';
}
```

8
输出：

```
User{userId='U001', name='Java'}
```

2
有利于：
调试
日志
测试失败分析
IDE 查看对象
问题定位

**9.24.2 toString 不应泄露敏感信息**

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

```
@Override
public String toString() {
return "User{"
+ "password='" + password + '\''
+ ", token='" + token + '\''
+ '}';
}
```

8
日志可能长期保存并被多人访问。

**9.24.3 toString 不应承担业务序列化协议**

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

**9.24.4 toString 应避免复杂副作用**

不推荐在 toString 中：
查询数据库
发起网络请求
修改对象状态
执行耗时计算
抛出业务异常
访问可能未初始化的懒加载属性
日志框架、IDE 或调试器可能自动调用 toString。
因此应保持：

```
轻量
稳定
无副作用
```

4

## 9.25 getClass()

Object 定义：

```
public final native Class<?> getClass();
```

2
用于获取对象的运行时类型：

```
User user = new User();
```

2

```
Class<?> type = user.getClass();
```

4
例如：

```
System.out.println(
user.getClass().getName()
);
```

4
可能输出：

```
com.example.User
```

2

**9.25.1 getClass 返回运行时类型**

```
Animal animal = new Dog();
```

2

```
System.out.println(
animal.getClass()
);
```

6
返回的是 Dog 的 Class 对象，而不是 Animal。
因为：

```
编译时类型：Animal
运行时类型：Dog
```

3
getClass() 获取运行时类型。

**9.25.2 getClass() 与 instanceof**

```
animal instanceof Animal
```

2
判断对象是否属于某类型或其子类型。

```
animal.getClass() == Animal.class
```

2
判断运行时类型是否精确等于 Animal。
例如 Dog：

```
dog instanceof Animal // true
dog.getClass() == Animal.class // false
```

3

**9.25.3 getClass() 不能被重写**

getClass() 是 final 方法。
对象不能伪造自己的实际运行时 Class。
Class 与反射机制后续单独展开。

## 9.26 clone()

Object 提供：

```
protected native Object clone()
throws CloneNotSupportedException;
```

3
clone 用于复制对象，但其设计存在较多限制。

**9.26.1 Cloneable 标记接口**

要正常调用 Object.clone()，类通常需要实现：

```
Cloneable
```

2
示例：

```java
public class User implements Cloneable {
```

2

```java
private String name;
```

4

```
@Override
public User clone() {
try {
return (User) super.clone();
} catch (
CloneNotSupportedException exception
) {
throw new AssertionError(exception);
}
}
}
```

16
Cloneable 本身没有声明 clone 方法，它只是告诉 Object.clone：

```
允许进行字段级复制
```

2
这是一种标记接口。

**9.26.2 clone 默认是浅拷贝**

```java
public class Order implements Cloneable {
```

2

```java
private String orderNo;
private List<String> items;
}
```

6
执行 clone 后：

```text
原 Order 对象
└── items ──┐
├──→ 同一个 List
克隆 Order 对象
└── items ──┘
```

6
外层对象被复制，但内部引用字段仍然共享。
因此：

```
clonedOrder.getItems().add("B");
```

2
可能影响原对象。

**9.26.3 为什么通常不推荐 clone**

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

```
public Order(Order source) {
this.orderNo = source.orderNo;
this.items =
new ArrayList<>(source.items);
}
```

6
静态复制工厂

```java
public static Order copyOf(Order source) {
return new Order(
source.orderNo,
source.items
);
}
```

7
显式映射

```
OrderCopyMapper.copy(source);
```

2
这些方式的复制语义更清晰。

## 9.27 wait()、notify() 与 notifyAll()

Object 定义了：

```
wait()
notify()
notifyAll()
```

4
原因是：
Java 中任何对象都可以作为监视器锁对象。
例如：

```
synchronized (lock) {
lock.wait();
}
```

4
唤醒：

```
synchronized (lock) {
lock.notifyAll();
}
```

4
这些方法必须在持有对象监视器时调用，否则会抛出：

```
IllegalMonitorStateException
```

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

```
try-with-resources
```

2
例如：

```
try (
InputStream input =
Files.newInputStream(path)
) {
// 使用资源
}
```

7
对象终结机制只作为历史知识了解，不应作为现代资源管理方案。

## 9.29 identityHashCode()

System 提供：

```
System.identityHashCode(object)
```

2
它用于获得基于对象身份的哈希值，不受类重写 hashCode 影响。
例如：

```java
public class User {
```

2

```
@Override
public int hashCode() {
return 1;
}
}
```

8
调用：

```
User user = new User();
```

2

```
System.out.println(user.hashCode());
System.out.println(
System.identityHashCode(user)
);
```

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

```
hashCode()
+
equals()
```

4
判断键。
IdentityHashMap 使用：

```
引用身份
==
```

3
判断键。
示例：

```
String a = new String("Java");
String b = new String("Java");
```

3
普通 HashMap：

```
Map<String, Integer> map =
new HashMap<>();
```

3

```
map.put(a, 1);
map.put(b, 2);
```

6

```
System.out.println(map.size());
```

8
通常为：

```
1
```

2
因为：

```
a.equals(b) == true
```

2
IdentityHashMap：

```
Map<String, Integer> map =
new IdentityHashMap<>();
```

3

```
map.put(a, 1);
map.put(b, 2);
```

6

```
System.out.println(map.size());
```

8
通常为：

```
2
```

2
因为：

```
a != b
```

2
IdentityHashMap 适合少数需要身份语义的场景，不应替代普通 HashMap。

## 9.31 equals 与继承的设计难题

假设父类 equals 使用：

```
instanceof Parent
```

2
那么子类对象可能与父类对象相等。
但子类新增字段后：

```
class Child extends Parent {
```

2

```java
private String extra;
}
```

5
会出现问题：
忽略 extra：子类不同状态可能被判相等
比较 extra：可能破坏与父类的对称性
限定同类型：父类与子类不再可相等
因此，对具有值语义的类型，常见建议是：

```
final 类
+
不可变字段
+
明确 equals/hashCode
```

6
例如：

```
public final class Money {
}
```

3
避免在可扩展继承体系中定义复杂值相等性。

## 9.32 canEqual 模式概览

某些继承体系会使用 canEqual() 尝试维护对称性。
父类：

```java
public class Point {
```

2

```
protected boolean canEqual(
Object other
) {
return other instanceof Point;
}
```

8

```
@Override
public boolean equals(Object other) {
if (!(other instanceof Point point)) {
return false;
}
```

14

```
return point.canEqual(this)
&& x == point.x
&& y == point.y;
}
}
```

20
子类重写：

```
@Override
protected boolean canEqual(
Object other
) {
return other instanceof ColorPoint;
}
```

7
这种方式可以处理部分继承相等性问题，但设计复杂。
普通业务代码更推荐：
值对象禁止继承
使用组合
不同类型不互相相等
使用明确业务 ID 判断实体身份

## 9.33 Lombok 与自动生成 equals/hashCode

Lombok 可以通过注解生成：

```
@EqualsAndHashCode
```

2
或者：

```
@Data
```

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

```
List<OrderItem> items
```

2
如果自动加入 equals/hashCode：
比较成本可能很高
懒加载可能被触发
集合修改会改变 hashCode
双向关联可能递归
日志和调试可能出现栈溢出

## 9.34 实体对象的相等性设计

实体强调：

```
同一身份
```

2
而不是所有字段相同。
例如订单：

```java
public class Order {
```

2

```java
private final OrderId orderId;
private OrderStatus status;
}
```

6
即使状态变化：

```
CREATED
→ PROCESSING
→ COMPLETED
```

4
仍然是同一个订单。
因此实体相等性通常基于：

```
稳定业务 ID
```

2
而不是所有可变字段。

**9.34.1 业务 ID 优于可变字段**

不推荐：

```
return Objects.equals(status, other.status)
&& Objects.equals(items, other.items)
&& Objects.equals(address, other.address);
```

4
这些字段可能随业务变化。
更合理：

```
return Objects.equals(
orderId,
other.orderId
);
```

5
前提是 orderId：
创建时就存在
全局或业务范围唯一
生命周期内不改变
不依赖数据库持久化后才生成

## 9.35 值对象的相等性设计

值对象强调：

```
所有业务值相同
```

2
例如：

```
public final class Address {
```

2

```java
private final String province;
private final String city;
private final String detail;
}
```

7
相等性可以基于：

```
province
city
detail
```

4
值对象通常适合：
final 类
final 字段
不可变
完整重写 equals/hashCode
修改时创建新对象

**9.35.1 值对象没有独立身份**

两个分别创建的地址对象：

```
Address a =
new Address(
"广东",
"深圳",
"南山区"
);
```

7

```
Address b =
new Address(
"广东",
"深圳",
"南山区"
);
```

14
虽然：

```
a != b
```

2
但业务上：

```
a.equals(b)
```

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

```
return id == other.id
&& Objects.equals(code, other.code)
&& Objects.equals(details, other.details);
```

4
可以先比较：

```
便宜且区分度高的字段
```

2
再比较复杂字段。

## 9.37 toString、equals、hashCode 中的循环引用

双向关联：

```
class Parent {
```

2

```java
private List<Child> children;
}
```

5

```
class Child {
```

2

```java
private Parent parent;
}
```

5
如果自动生成 toString：

```
Parent.toString()
→ Child.toString()
→ Parent.toString()
→ ...
```

5
可能导致：

```
StackOverflowError
```

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

```java
public class DefaultEqualsDemo {
```

2

```java
public static void main(String[] args) {
User user1 = new User("A");
User user2 = new User("A");
```

6

```
System.out.println(user1 == user2);
System.out.println(
user1.equals(user2)
);
}
```

12

```
static class User {
```

14

```java
private final String name;
```

16

```
User(String name) {
this.name = name;
}
}
}
```

22
预期：

```
false
false
```

3
实验二：重写 equals

```java
public class EqualsDemo {
```

2

```java
public static void main(String[] args) {
User user1 = new User("U001");
User user2 = new User("U001");
```

6

```
System.out.println(
user1.equals(user2)
);
}
```

11

```
static final class User {
```

13

```java
private final String userId;
```

15

```
User(String userId) {
this.userId = userId;
}
```

19

```
@Override
public boolean equals(Object other) {
if (this == other) {
return true;
}
```

25

```
if (!(other instanceof User user)) {
return false;
}
```

29

```
return Objects.equals(
userId,
user.userId
);
}
```

35

```
@Override
public int hashCode() {
return Objects.hash(userId);
}
}
}
```

42
预期：

```
true
```

2
实验三：只重写 equals 的 HashSet 问题
创建一个只重写 equals、不重写 hashCode 的类：

```
static class User {
```

2

```java
private final String userId;
```

4

```
User(String userId) {
this.userId = userId;
}
```

8

```
@Override
public boolean equals(Object other) {
if (!(other instanceof User user)) {
return false;
}
```

14

```
return Objects.equals(
userId,
user.userId
);
}
}
```

21
执行：

```
Set<User> users = new HashSet<>();
```

2

```
users.add(new User("U001"));
users.add(new User("U001"));
```

5

```
System.out.println(users.size());
```

7
观察结果，理解为什么逻辑相等对象仍可能同时存在。
实验四：可变 Key 失效

```java
public class MutableKeyDemo {
```

2

```java
public static void main(String[] args) {
UserKey key = new UserKey("U001");
```

5

```
Map<UserKey, String> map =
new HashMap<>();
```

8

```
map.put(key, "data");
```

10

```
System.out.println(map.get(key));
```

12

```
key.userId = "U002";
```

14

```
System.out.println(map.get(key));
}
```

17

```
static class UserKey {
```

19

```java
private String userId;
```

21

```
UserKey(String userId) {
this.userId = userId;
}
```

25

```
@Override
public boolean equals(Object other) {
if (!(other instanceof UserKey key)) {
return false;
}
```

31

```
return Objects.equals(
userId,
key.userId
);
}
```

37

```
@Override
public int hashCode() {
return Objects.hash(userId);
}
}
}
```

44
观察修改字段后 Map 查找失败。
实验五：数组 equals

```java
public class ArrayEqualsDemo {
```

2

```java
public static void main(String[] args) {
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
```

6

```
System.out.println(a.equals(b));
```

8

```
System.out.println(
Arrays.equals(a, b)
);
}
}
```

14
预期：

```
false
true
```

3
实验六：String 的 == 与 equals

```java
public class StringEqualsDemo {
```

2

```java
public static void main(String[] args) {
String a = new String("Java");
String b = new String("Java");
```

6

```
System.out.println(a == b);
System.out.println(a.equals(b));
}
}
```

11
预期：

```
false
true
```

3
实验七：BigDecimal equals 与 compareTo

```java
public class BigDecimalEqualsDemo {
```

2

```java
public static void main(String[] args) {
BigDecimal a =
new BigDecimal("1.0");
```

6

```
BigDecimal b =
new BigDecimal("1.00");
```

9

```
System.out.println(a.equals(b));
```

11

```
System.out.println(
a.compareTo(b) == 0
);
}
}
```

17
预期：

```
false
true
```

3
实验八：toString 敏感信息泄露

```java
public class ToStringDemo {
```

2

```
static class User {
```

4

```java
private String username;
private String password;
private String token;
```

8

```
@Override
public String toString() {
return "User{"
+ "username='" + username + '\''
+ ", password='" + password + '\''
+ ", token='" + token + '\''
+ '}';
}
}
}
```

19
分析该实现为什么不适合生产日志。
改为：

```
@Override
public String toString() {
return "User{"
+ "username='" + username + '\''
+ '}';
}
```

7
实验九：浅克隆共享内部对象

```java
public class CloneDemo {
```

2

```java
public static void main(String[] args) {
Order source = new Order();
source.items.add("A");
```

6

```
Order copy = source.clone();
copy.items.add("B");
```

9

```
System.out.println(source.items);
System.out.println(copy.items);
}
```

13

```
static class Order implements Cloneable {
```

15

```java
private List<String> items =
new ArrayList<>();
```

18

```
@Override
public Order clone() {
try {
return (Order) super.clone();
} catch (
CloneNotSupportedException exception
) {
throw new AssertionError(exception);
}
}
}
}
```

31
预期两个对象都看到：

```
[A, B]
```

2
说明默认 clone 是浅拷贝。
实验十：IdentityHashMap

```java
public class IdentityMapDemo {
```

2

```java
public static void main(String[] args) {
String a = new String("Java");
String b = new String("Java");
```

6

```
Map<String, Integer> normal =
new HashMap<>();
```

9

```
normal.put(a, 1);
normal.put(b, 2);
```

12

```
Map<String, Integer> identity =
new IdentityHashMap<>();
```

15

```
identity.put(a, 1);
identity.put(b, 2);
```

18

```
System.out.println(normal.size());
System.out.println(identity.size());
}
}
```

23
预期：

```
1
2
```

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

**9.40.2 基本类型也是 Object 子类**

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

**9.41.1 值对象优先设计为不可变**

推荐：

```
public final class OrderId {
```

2

```java
private final String value;
}
```

5
配合稳定的 equals/hashCode。

**9.41.2 实体相等性优先使用稳定业务 ID**

推荐：

```java
private final OrderId orderId;
```

2
避免使用：
status
更新时间
集合字段
数据库延迟生成 ID
随机变化字段
作为相等性基础。

**9.41.3 equals 和 hashCode 使用同一组字段**

```
@Override
public boolean equals(Object other) {
// userId、tenantId
}
```

5

```
@Override
public int hashCode() {
return Objects.hash(
userId,
tenantId
);
}
```

13
避免字段集合不一致。

**9.41.4 Map Key 应保持不可变**

推荐：

```
public record InventoryKey(
String warehouseCode,
String skuCode
) {
}
```

6
不推荐使用可以随时修改字段的普通 JavaBean 作为 Key。

**9.41.5 toString 只输出定位所需信息**

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

**9.41.6 不要在 equals 中访问外部资源**

禁止：

```
@Override
public boolean equals(Object other) {
return repository.exists(...);
}
```

5
equals 应：

```
快速
稳定
纯内存
无副作用
```

5

**9.41.7 不要机械使用 @Data**

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

**9.41.8 复制对象优先使用明确 API**

推荐：

```
Order copy = Order.copyOf(source);
```

2
或者：

```
Order copy = new Order(source);
```

2
优于依赖 clone 的隐式复制语义。

## 9.42 本章知识链路

```
Object 是所有普通类的根类
↓
对象默认拥有基础协议
↓
== 比较值或引用身份
↓
Object.equals 默认仍是身份比较
↓
业务对象重写 equals 定义逻辑相等
↓
equals 必须满足五项契约
↓
同时重写 hashCode
↓
哈希容器先定位桶，再调用 equals
↓
相等性依赖字段必须稳定
↓
实体和值对象采用不同相等策略
```

20
== 与 equals：

```
基本类型 ==
→ 数值比较
```

3

```
引用类型 ==
→ 对象身份比较
```

6

```
equals
→ 由类定义逻辑相等语义
```

9
equals 与 hashCode：

```
equals 相等
→ hashCode 必须相等
```

3

```
hashCode 相等
→ equals 不一定相等
```

6
对象设计：

```
实体对象
→ 使用稳定身份判断相等
```

3

```
值对象
→ 使用全部业务值判断相等
```

6

```
HashMap Key
→ 相等性字段尽量不可变
```

9
面试口述版：
Object 是 Java 普通类体系的根类，默认提供 equals、hashCode、toString、getClass、clone 以及线程协作相关方法。对于基本类型， == 比较数值；对于引用类
型， == 比较两个引用是否指向同一对象。Object 默认的 equals 本质上也是身份比较，业务对象需要根据自身语义决定是否重写。equals 必须满足自反性、对称
性、传递性、一致性和非空性。重写 equals 后必须同步重写 hashCode，并保证 equals 相等的对象 hashCode 一定相等。HashMap 和 HashSet 会先通过 hashCode
定位候选位置，再通过 equals 确认逻辑相等，因此参与 equals 和 hashCode 的字段应保持稳定，尤其不应随意修改作为 Map Key 的对象。值对象通常按全部业务值
判断相等，实体对象通常按稳定业务身份判断相等。toString 应提供有用的调试信息，但不能泄露敏感数据；clone 默认是浅拷贝，工程上通常更推荐拷贝构造方法或
静态复制工厂。
