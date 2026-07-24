# equals 与 hashCode

## 9.1 本章定位

java.lang.Object 是 Java 类体系的根类。
如果一个类没有显式继承其他类：

```java
public class User {
}
```

可以理解为：

```java
public class User extends Object {
}
```

因此，所有普通 Java 对象都具备 Object 定义的基础能力。

本章主要解决以下问题：

- Object 为什么是所有类的父类？
- == 与 equals() 有什么区别？
- Object 默认的 equals() 比较什么？
- 什么情况下应该重写 equals() ？
- equals() 必须满足哪些契约？
- 为什么重写 equals() 后必须重写 hashCode() ？
- HashMap 和 HashSet 为什么同时依赖 hashCode() 与 equals() ？
- 哪些字段适合参与对象相等性判断？
- 可变字段参与 hashCode() 会有什么风险？
- getClass() 与 instanceof 在 equals 中如何选择？
- toString() 应该输出哪些内容？
- clone() 为什么通常不推荐直接使用？
- getClass() 、 wait() 、 notify() 等 Object 方法分别有什么作用？
- 实体对象和值对象的相等性应该如何设计？

本章核心主线：

```
所有对象继承 Object
↓
Object 提供基础对象协议
↓
== 判断引用或基本值
↓
equals() 定义逻辑相等
↓
hashCode() 支持散列容器
↓
equals 与 hashCode 必须保持一致
↓
形成稳定的对象相等性契约
```

本章暂不深入：

- HashMap 的哈希桶、红黑树和扩容机制：放在集合框架。
- 对象头和 identity hash code：放在 JVM 模块。
- wait() 、 notify() 的线程协作机制：放在并发编程。
- Java Record 自动生成的 equals/hashCode：放在现代 Java 特性。
- ORM 实体代理与 equals 的复杂问题：放在框架实战部分。

## 9.2 Object 是类体系的根类

Java 普通类最终都继承自 Object：

```
Object
↑
Animal
↑
Dog
```

因此：

```
Dog dog = new Dog();
Object object = dog;
```

是合法的向上转型。
Object 可以引用任意普通对象：

```
Object value1 = "Java";
Object value2 = 10;
Object value3 = new User();
Object value4 = new int[]{1, 2, 3};
```

需要注意：

```
Object value2 = 10;
```

这里发生了自动装箱：

```
Object value2 = Integer.valueOf(10);
```

基本类型本身不是 Object 的子类，但对应的包装类型是对象。

**9.2.1 Object 提供的核心方法**

Object 中最常见的方法包括：

```
getClass()
hashCode()
equals(Object)
clone()
toString()
wait()
notify()
notifyAll()
```

历史上还存在对象终结相关机制，但不应依赖它管理资源。
这些方法大致可以分为：

```bash
对象身份与类型
├── getClass()
├── equals()
└── hashCode()
对象描述
└── toString()
对象复制
└── clone()
线程协作
├── wait()
├── notify()
└── notifyAll()
```

## 9.3 == 运算符

== 的语义取决于操作数类型。

**9.3.1 基本类型的 ==**

对于基本类型， == 比较数值是否相等：

```java
int a = 10;
int b = 10;
System.out.println(a == b); // true
```

不同数值类型之间可能先发生类型提升：

```java
int a = 10;
long b = 10L;
System.out.println(a == b); // true
```

这里 a 会提升为 long 后再比较。

**9.3.2 引用类型的 ==**

对于引用类型， == 比较：
两个引用值是否指向同一个对象。

```java
User user1 = new User("A");
User user2 = user1;
User user3 = new User("A");
System.out.println(user1 == user2); // true
System.out.println(user1 == user3); // false
```

引用关系：

```bash
user1 ─────┐
├────→ User 对象1
user2 ─────┘
user3 ──────────→ User 对象2
```

即使两个对象字段完全相同：

```
user1.name = "A";
user3.name = "A";
```

只要不是同一个对象：

```
user1 == user3
```

仍然是 false 。

**9.3.3 null 比较**

引用可以与 null 比较：

```
if (user == null) {
}
```

判断引用是否为空时，应使用：

```
user == null
user != null
```

而不是：

```
user.equals(null)
```

因为当 user == null 时，调用方法会抛出 NullPointerException 。

## 9.4 equals() 方法

Object 定义了：

```
public boolean equals(Object obj)
```

它用于表达对象之间的逻辑相等关系。
默认实现可以近似理解为：

```
public boolean equals(Object obj) {
return this == obj;
}
```

因此，如果一个类没有重写 equals() ：

```java
User user1 = new User("A");
User user2 = new User("A");
System.out.println(user1.equals(user2));
```

通常结果是：

```
false
```

因为默认 equals 仍然判断是否为同一个对象。

**9.4.1 身份相等与逻辑相等**

- 对象可以有两种不同的相等概念。
- 身份相等

判断是否为同一个对象：

```
user1 == user2
```

逻辑相等
判断两个对象在业务意义上是否相等：

```
user1.equals(user2)
```

例如两个金额对象：

```
Money money1 = new Money("100.00", "CNY");
Money money2 = new Money("100.00", "CNY");
```

它们不是同一个对象：

```
money1 == money2 // false
```

但业务上可以认为相等：

```
money1.equals(money2) // true
```

## 9.5 什么时候应该重写 equals()

适合重写 equals 的典型对象：

- 值对象
- DTO
- 配置对象
- 复合键
- 集合元素
- Map Key
- 需要按字段比较的业务对象
- 测试中需要内容比较的对象
- 例如：

```java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
}
```

Money 的相等性通常由：

```
金额
+
币种
```

共同决定。

**9.5.1 不一定需要重写 equals 的对象**

部分对象更强调唯一身份：

- 连接对象
- 线程对象
- IO 资源
- 某些服务对象
- 某些有生命周期的实体
- 仅用于内部行为封装的对象

对于这些对象，默认身份相等可能已经足够。
因此：
不是所有类都必须重写 equals 和 hashCode。
相等性必须根据对象语义设计。

## 9.6 equals() 的基本实现

示例：

```java
public final class User {
    private final String userId;
    private final String name;
    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return Objects.equals(userId, user.userId)
        && Objects.equals(name, user.name);
    }
}
```

可以分为三个步骤。

**9.6.1 第一步：判断是否为同一对象**

```
if (this == other) {
return true;
}
```

优点：

- 同一对象必然相等
- 避免后续字段比较
- 提高常见场景性能
- 正确处理自身比较

**9.6.2 第二步：判断类型**

```
if (!(other instanceof User user)) {
return false;
}
```

同时处理：

- other == null
- 类型不兼容
- 安全类型转换
- 因为：

```
null instanceof User
```

结果是 false 。

**9.6.3 第三步：比较关键字段**

```
return Objects.equals(userId, user.userId)
&& Objects.equals(name, user.name);
```

Objects.equals(a, b) 可以安全处理 null：

```
Objects.equals(null, null) // true
Objects.equals(null, "A") // false
Objects.equals("A", "A") // true
```

近似逻辑：

```
a == b || (a != null && a.equals(b))
```

## 9.7 equals() 的契约

正确的 equals 必须满足以下性质：

```
自反性
对称性
传递性
一致性
非空性
```

**9.7.1 自反性**

任何非 null 对象都必须与自身相等：

```
x.equals(x) == true
```

错误示例：

```java
@Override
public boolean equals(Object other) {
    return false;
}
```

这会直接破坏集合和业务判断。

**9.7.2 对称性**

如果：

```
x.equals(y) == true
```

那么必须保证：

```
y.equals(x) == true
```

不能出现：

```
x 认为 y 相等
y 认为 x 不相等
```

**9.7.3 传递性**

如果：

```
x.equals(y) == true
y.equals(z) == true
```

则必须保证：

```
x.equals(z) == true
```

否则集合中的去重、查找和分组可能产生不一致结果。

**9.7.4 一致性**

只要参与 equals 比较的字段没有变化，多次调用结果应保持一致：

```
x.equals(y)
x.equals(y)
x.equals(y)
```

不应一会儿 true，一会儿 false。
错误示例：

```java
@Override
public boolean equals(Object other) {
    return Math.random() > 0.5;
}
```

也不应把当前时间、随机数或外部可变状态放入 equals。

**9.7.5 非空性**

任何非 null 对象都必须满足：

```
x.equals(null) == false
```

不应抛异常，也不能返回 true。

## 9.8 对称性破坏示例

父类：

```java
public class Point {
    private final int x;
    private final int y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Point point)) {
            return false;
        }
        return x == point.x && y == point.y;
    }
}
```

子类：

```java
public class ColorPoint extends Point {
    private final String color;
    public ColorPoint(
    int x,
    int y,
    String color
    ) {
        super(x, y);
        this.color = color;
    }
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ColorPoint point)) {
            return false;
        }
        return super.equals(point)
        && Objects.equals(
        color,
        point.color
        );
    }
}
```

调用：

```
Point point = new Point(1, 2);
ColorPoint colorPoint =
new ColorPoint(1, 2, "RED");
```

可能出现：

```
point.equals(colorPoint) // true
colorPoint.equals(point) // false
```

对称性被破坏。
这说明：
可扩展类中的值相等性设计非常困难。

## 9.9 getClass() 与 instanceof

equals 类型判断常见两种写法。

**9.9.1 使用 instanceof**

```
if (!(other instanceof User user)) {
return false;
}
```

特点：

- 允许子类对象参与比较
- 支持父子类型相等
- 更灵活
- 容易因子类增加字段破坏对称性或传递性

**9.9.2 使用 getClass()**

```
if (other == null
|| getClass() != other.getClass()) {
return false;
}
```

特点：

- 只有完全相同运行时类型才允许比较
- 更容易维护 equals 契约
- 父类对象与子类对象永远不相等
- 可能与 ORM 代理等框架类型发生冲突
- 完整示例：

```java
@Override
public boolean equals(Object other) {
    if (this == other) {
        return true;
    }
    if (other == null
    || getClass() != other.getClass()) {
        return false;
    }
    User user = (User) other;
    return Objects.equals(
    userId,
    user.userId
    );
}
```

**9.9.3 如何选择**

对于不可继承的值对象：

```java
public final class Money {
}
```

使用：

```
instanceof Money
```

通常较安全，因为不会出现子类扩展问题。
对于允许继承、但不同子类不应互相相等的类，可以考虑：

```
getClass() == other.getClass()
```

但框架实体、代理对象和继承模型需要结合实际设计。
工程上更简单的方式是：
需要稳定值相等性的类，优先设计成 final 不可变类。

## 9.10 equals 中选择哪些字段

equals 不应该机械比较所有字段。
应选择：
能够定义对象逻辑身份或值语义的稳定字段。
例如 Money：

```
amount
currency
```

通常应该参与相等性。

但以下字段通常不适合：

- 创建时间
- 修改时间
- 缓存字段
- 统计字段
- 临时状态
- 日志追踪信息
- 懒加载对象
- 运行时计算结果

**9.10.1 值对象**

值对象没有独立身份，其相等性由全部业务值决定。
例如坐标：

```java
public final class Coordinate {
    private final int x;
    private final int y;
}
```

相等条件：

```
x 相等
并且
y 相等
```

**9.10.2 实体对象**

实体通常具有稳定身份：

```java
public class Order {
    private OrderId orderId;
    private OrderStatus status;
    private List<OrderItem> items;
}
```

相等性可能只由：

```
orderId
```

决定。
状态和订单项发生变化，不代表订单变成另一个实体。
因此：

```
order1.equals(order2)
```

是否相等，通常取决于二者是否代表同一业务订单。

**9.10.3 数据库自增 ID 的问题**

实体创建前，数据库 ID 可能为空：

```
Order order1 = new Order();
Order order2 = new Order();
```

二者：

```
id = null
```

如果简单写：

```
Objects.equals(id, other.id)
```

两个未持久化对象会被错误判断为相等。

可能的处理方式包括：

- 使用创建时生成的业务 ID
- 未分配 ID 时仅进行身份比较
- 延迟将对象用作 Set 元素或 Map Key
- 避免依赖数据库 ID 设计内存对象相等性
- 示例：

```java
@Override
public boolean equals(Object other) {
    if (this == other) {
        return true;
    }
    if (!(other instanceof Order order)) {
        return false;
    }
    return orderId != null
    && orderId.equals(order.orderId);
}
```

但这仍然要与 hashCode 设计保持一致。

## 9.11 hashCode()

Object 定义：

```
public native int hashCode();
```

hashCode() 返回一个整数散列值。

它主要用于：

- HashMap
- HashSet
- Hashtable
- ConcurrentHashMap
- 其他哈希结构

哈希值用于快速缩小查找范围，但不直接代表对象唯一身份。

**9.11.1 hashCode 不是对象内存地址**

不应机械认为：

```
hashCode = 对象内存地址
```

Java 规范不要求 hashCode 必须等于内存地址。
对象还可能在内存中移动，而 hashCode 契约要求在相关状态不变时保持稳定。
更准确的说法：
Object 默认 hashCode 通常与对象身份有关，但具体生成方式由 JVM 实现决定。

**9.11.2 hashCode 不保证唯一**

不同对象可能拥有相同 hashCode：

```
对象数量可以远大于 int 可表示的散列值数量
```

这种情况称为哈希冲突。
因此：

```
a.hashCode() == b.hashCode()
```

不能推出：

```
a.equals(b)
```

## 9.12 equals 与 hashCode 的契约

核心规则：
如果两个对象通过 equals 判断相等，它们的 hashCode 必须相同。
即：

```
a.equals(b) == true
```

必须保证：

```
a.hashCode() == b.hashCode()
```

反过来不成立：

```
a.hashCode() == b.hashCode()
```

不一定意味着：

```
a.equals(b) == true
```

因为哈希冲突允许存在。

**9.12.1 契约总结**

```
equals 相等
→ hashCode 必须相等
hashCode 相等
→ equals 不一定相等
equals 不相等
→ hashCode 可以相同，也可以不同
```

## 9.13 为什么重写 equals 后必须重写 hashCode

错误示例：

```java
public final class User {
    private final String userId;
    public User(String userId) {
        this.userId = userId;
    }
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof User user)) {
            return false;
        }
        return Objects.equals(
        userId,
        user.userId
        );
    }
}
```

没有重写 hashCode。
创建：

```java
User user1 = new User("U001");
User user2 = new User("U001");
System.out.println(user1.equals(user2));
```

结果：

```
true
```

但默认 hashCode 可能不同：

```
user1.hashCode() != user2.hashCode()
```

放入 HashSet：

```java
Set<User> users = new HashSet<>();
users.add(user1);
System.out.println(users.contains(user2));
```

可能输出：

```
false
```

虽然逻辑上 user1 与 user2 相等。
原因是 HashSet 通常先根据 hashCode 定位桶，再使用 equals 比较。
两个对象进入不同哈希位置后，可能根本不会互相比较。

## 9.14 正确重写 hashCode

可以使用：

```java
@Override
public int hashCode() {
    return Objects.hash(userId, name);
}
```

完整示例：

```java
public final class User {
    private final String userId;
    private final String name;
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return Objects.equals(
        userId,
        user.userId
        )
        && Objects.equals(
        name,
        user.name
        );
    }
    @Override
    public int hashCode() {
        return Objects.hash(
        userId,
        name
        );
    }
}
```

关键原则：
equals 使用哪些字段，hashCode 通常也应该使用同一组字段。

**9.14.1 手动计算 hashCode**

经典形式：

```java
@Override
public int hashCode() {
    int result = 17;
    result = 31 * result
    + Objects.hashCode(userId);
    result = 31 * result
    + Objects.hashCode(name);
    return result;
}
```

其中 31 常见的原因包括：

- 奇数
- 质数
- 分布效果通常较好
- 可以通过移位和减法优化
- 但实际开发通常直接使用：

```
Objects.hash(...)
```

或 IDE 自动生成。
对极端性能敏感的场景，再考虑减少可变参数数组创建等开销。

## 9.15 HashMap 如何使用 hashCode 和 equals

假设：

```java
Map<User, String> map =
new HashMap<>();
```

写入：

```
map.put(user1, "A");
```

读取：

```
map.get(user2);
```

简化流程：

```
计算 user2.hashCode()
↓
根据哈希值定位桶
↓
在桶内寻找候选键
↓
比较 hashCode
↓
调用 equals()
↓
找到逻辑相等的键
```

因此：

| hashCode() | 用于快速定位范围 |
|---|---|
| equals() | 用于最终确认逻辑相等 |

**9.15.1 为什么不能只使用 equals**

如果集合中有一百万个对象，每次查找都逐个调用 equals：

```
时间复杂度接近线性扫描
```

通过 hashCode，可以先快速定位少量候选对象。

**9.15.2 为什么不能只使用 hashCode**

- 因为不同对象可能拥有相同 hashCode。
- 哈希冲突时，仍然需要 equals 判断是否为同一个逻辑键。

## 9.16 可变对象作为 HashMap Key 的风险

定义：

```java
public class UserKey {
    private String userId;
    @Override
    public boolean equals(Object other) {
        // 使用 userId
    }
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
```

使用：

```java
UserKey key = new UserKey("U001");
Map<UserKey, String> map =
new HashMap<>();
map.put(key, "data");
```

随后修改：

```
key.setUserId("U002");
```

再次查询：

```
map.get(key);
```

可能返回：

```
null
```

原因：

```
放入时：
hashCode 基于 U001
→ 对象进入桶 A
修改后：
hashCode 基于 U002
→ 查询桶 B
对象实际仍在桶 A
→ 无法找到
```

因此：
作为 HashMap Key 或 HashSet 元素的对象，其 equals/hashCode 依赖字段应尽量保持不可变。

**9.16.1 安全 Key 设计**

推荐：

```java
public final class UserKey {
    private final String userId;
    public UserKey(String userId) {
        this.userId =
        Objects.requireNonNull(userId);
    }
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserKey key)) {
            return false;
        }
        return userId.equals(key.userId);
    }
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
```

特点：

- final 类
- final 字段
- 无 Setter
- 相等性稳定
- 适合作为 Map Key

## 9.17 BigDecimal 的 equals 特殊性

```
BigDecimal a =
new BigDecimal("1.0");
BigDecimal b =
new BigDecimal("1.00");
```

执行：

```
a.equals(b)
```

结果通常是：

```
false
```

因为 BigDecimal 的 equals 同时考虑：

- 数值
- scale
- 即：

```
1.0
和
1.00
```

在 equals 语义上不同。
但：

```
a.compareTo(b)
```

结果是：

```
0
```

表示数值大小相等。
因此金额判断需要明确：

```
判断严格表示相等
→ equals()
判断数值大小相等
→ compareTo() == 0
```

这也说明：
equals 的语义由具体类定义，不能假设所有对象的 equals 都只比较数学值。

## 9.18 数组的 equals

数组继承 Object，但没有重写为元素内容比较。

```java
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(a.equals(b));
```

通常结果：

```
false
```

因为数组默认 equals 仍是身份比较。
比较数组内容应使用：

```
Arrays.equals(a, b)
```

多维数组：

```
Arrays.deepEquals(a, b)
```

**9.18.1 数组 hashCode**

数组默认 hashCode 通常也是身份语义。
计算元素内容哈希应使用：

```
Arrays.hashCode(array)
```

多维数组：

```
Arrays.deepHashCode(array)
```

## 9.19 集合的 equals

Java 集合通常已经定义了内容相等语义。
例如 List：

```java
List<String> a =
List.of("A", "B");
List<String> b =
List.of("A", "B");
System.out.println(a.equals(b));
```

结果：

```
true
```

List 通常比较：

- 元素数量
- 元素顺序
- 每个位置的元素是否相等

**9.19.1 Set 的 equals**

```
Set<String> a =
Set.of("A", "B");
Set<String> b =
Set.of("B", "A");
```

通常：

```
a.equals(b)
```

结果为 true。

Set 更关注：

- 元素集合是否相同
- 不关心元素顺序

**9.19.2 Map 的 equals**

Map 通常比较：

- 键值映射关系是否一致
- Key 是否相等
- Value 是否相等

不同 Map 实现之间也可能逻辑相等。
这体现：
equals 描述的是抽象数据语义，不一定要求具体实现类相同。

## 9.20 包装类型的 equals

包装类型通常按包装值比较：

```java
Integer a = 1000;
Integer b = 1000;
System.out.println(a.equals(b)); // true
```

而：

```
a == b
```

比较引用身份，结果可能为 false。
因此包装类型比较数值应优先：

```
Objects.equals(a, b)
```

或者在明确非 null 时：

```
a.equals(b)
```

不要依赖缓存范围判断 == 。

## 9.21 String 的 equals

String 重写了 equals，比较字符序列内容：

```java
String a = new String("Java");
String b = new String("Java");
System.out.println(a == b); // false
System.out.println(a.equals(b)); // true
```

因此：

```
== → 是否同一个 String 对象
equals → 字符内容是否相同
```

字符串常量池会让部分 == 结果看起来为 true，但不应使用 == 判断字符串内容。
字符串完整机制放到高级语言特性笔记。

## 9.22 Objects.equals()

直接调用：

```
a.equals(b)
```

要求 a 非 null。
更安全：

```
Objects.equals(a, b)
```

其行为：

- a b 结果
- null null true
- null 非 null false
- 非 null null false
- 非 null 非 null a.equals(b)
- 业务代码中，比较可能为空的引用时，推荐：

```
Objects.equals(expected, actual)
```

**9.22.1 常量放前面的写法**

传统上常见：

```
"SUCCESS".equals(status)
```

可以防止：

```
status.equals("SUCCESS")
```

在 status 为 null 时抛 NPE。
现代代码也可以使用：

```
Objects.equals(status, "SUCCESS")
```

但状态值更适合使用枚举而不是字符串魔法值。
