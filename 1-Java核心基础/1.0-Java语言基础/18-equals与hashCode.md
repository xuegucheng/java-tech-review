# equals 与 hashCode

## 18.1 本章定位

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
- 实体对象和值对象的相等性应该如何设计？
- 可变字段作为 HashMap Key 有什么风险？

toString()、clone()、getClass()、wait()/notify() 等方法放在 19。

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

## 18.2 Object 是类体系的根类

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

**18.2.1 Object 提供的核心方法**

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

## 18.3 == 运算符

== 的语义取决于操作数类型。

**18.3.1 基本类型的 ==**

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

**18.3.2 引用类型的 ==**

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

**18.3.3 null 比较**

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

## 18.4 equals() 方法

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

**18.4.1 身份相等与逻辑相等**

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

## 18.5 什么时候应该重写 equals()

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

**18.5.1 不一定需要重写 equals 的对象**

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

## 18.6 equals() 的基本实现

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

**18.6.1 第一步：判断是否为同一对象**

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

**18.6.2 第二步：判断类型**

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

**18.6.3 第三步：比较关键字段**

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

## 18.7 equals() 的契约

正确的 equals 必须满足以下性质：

```
自反性
对称性
传递性
一致性
非空性
```

**18.7.1 自反性**

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

**18.7.2 对称性**

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

**18.7.3 传递性**

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

**18.7.4 一致性**

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

**18.7.5 非空性**

任何非 null 对象都必须满足：

```
x.equals(null) == false
```

不应抛异常，也不能返回 true。

## 18.8 对称性破坏示例

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

## 18.9 getClass() 与 instanceof

equals 类型判断常见两种写法。

**18.9.1 使用 instanceof**

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

**18.9.2 使用 getClass()**

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

**18.9.3 如何选择**

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

## 18.10 equals 中选择哪些字段

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

**18.10.1 值对象**

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

**18.10.2 实体对象**

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

**18.10.3 数据库自增 ID 的问题**

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

## 18.11 hashCode()

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

**18.11.1 hashCode 不是对象内存地址**

不应机械认为：

```
hashCode = 对象内存地址
```

Java 规范不要求 hashCode 必须等于内存地址。
对象还可能在内存中移动，而 hashCode 契约要求在相关状态不变时保持稳定。
更准确的说法：
Object 默认 hashCode 通常与对象身份有关，但具体生成方式由 JVM 实现决定。

**18.11.2 hashCode 不保证唯一**

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

## 18.12 equals 与 hashCode 的契约

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

**18.12.1 契约总结**

```
equals 相等
→ hashCode 必须相等
hashCode 相等
→ equals 不一定相等
equals 不相等
→ hashCode 可以相同，也可以不同
```

## 18.13 为什么重写 equals 后必须重写 hashCode

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

## 18.14 正确重写 hashCode

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

**18.14.1 手动计算 hashCode**

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

## 18.15 HashMap 如何使用 hashCode 和 equals

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

**18.15.1 为什么不能只使用 equals**

如果集合中有一百万个对象，每次查找都逐个调用 equals：

```
时间复杂度接近线性扫描
```

通过 hashCode，可以先快速定位少量候选对象。

**18.15.2 为什么不能只使用 hashCode**

- 因为不同对象可能拥有相同 hashCode。
- 哈希冲突时，仍然需要 equals 判断是否为同一个逻辑键。

## 18.16 可变对象作为 HashMap Key 的风险

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

**18.16.1 安全 Key 设计**

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

## 18.17 BigDecimal 的 equals 特殊性

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

## 18.18 数组的 equals

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

**18.18.1 数组 hashCode**

数组默认 hashCode 通常也是身份语义。
计算元素内容哈希应使用：

```
Arrays.hashCode(array)
```

多维数组：

```
Arrays.deepHashCode(array)
```

## 18.19 集合的 equals

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

**18.19.1 Set 的 equals**

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

**18.19.2 Map 的 equals**

Map 通常比较：

- 键值映射关系是否一致
- Key 是否相等
- Value 是否相等

不同 Map 实现之间也可能逻辑相等。
这体现：
equals 描述的是抽象数据语义，不一定要求具体实现类相同。

## 18.20 包装类型的 equals

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

## 18.21 String 的 equals

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

## 18.22 Objects.equals()

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

**18.22.1 常量放前面的写法**

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

## 18.23 Objects.deepEquals()

Objects.deepEquals(a, b) 可以处理数组：

```java
int[] a = {1, 2};
int[] b = {1, 2};
System.out.println(Objects.deepEquals(a, b));
```

结果为 true。
对于普通对象，则仍然依赖 equals。

## 18.24 IdentityHashMap

普通 HashMap 使用：

```
hashCode()
+
equals()
```

判断键。
IdentityHashMap 使用：

```
引用身份
==
```

判断键。
示例：

```
String a = new String("Java");
String b = new String("Java");
```

普通 HashMap：

```java
Map<String, Integer> map =
new HashMap<>();
map.put(a, 1);
map.put(b, 2);
System.out.println(map.size());
```

通常为：

```
1
```

因为：

```
a.equals(b) == true
```

IdentityHashMap：

```java
Map<String, Integer> map =
new IdentityHashMap<>();
map.put(a, 1);
map.put(b, 2);
System.out.println(map.size());
```

通常为：

```
2
```

因为：

```
a != b
```

IdentityHashMap 适合少数需要身份语义的场景，不应替代普通 HashMap。

## 18.25 equals 与继承的设计难题

假设父类 equals 使用：

```
instanceof Parent
```

那么子类对象可能与父类对象相等。
但子类新增字段后：

```java
class Child extends Parent {
    private String extra;
}
```

会出现问题：

- 忽略 extra：子类不同状态可能被判相等
- 比较 extra：可能破坏与父类的对称性
- 限定同类型：父类与子类不再可相等
- 因此，对具有值语义的类型，常见建议是：

```
final 类
+
不可变字段
+
明确 equals/hashCode
```

例如：

```java
public final class Money {
}
```

避免在可扩展继承体系中定义复杂值相等性。

## 18.26 canEqual 模式概览

某些继承体系会使用 canEqual() 尝试维护对称性。
父类：

```java
public class Point {
    protected boolean canEqual(
    Object other
    ) {
        return other instanceof Point;
    }
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Point point)) {
            return false;
        }
        return point.canEqual(this)
        && x == point.x
        && y == point.y;
    }
}
```

子类重写：

```java
@Override
protected boolean canEqual(
Object other
) {
return other instanceof ColorPoint;
}
```

这种方式可以处理部分继承相等性问题，但设计复杂。

普通业务代码更推荐：

- 值对象禁止继承
- 使用组合
- 不同类型不互相相等
- 使用明确业务 ID 判断实体身份

## 18.27 Lombok 与自动生成 equals/hashCode

Lombok 可以通过注解生成：

```
@EqualsAndHashCode
```

或者：

```
@Data
```

但自动生成前必须确认：

- 哪些字段参与相等性
- 是否包含父类字段
- 是否包含可变字段
- 是否存在懒加载字段
- 是否是 ORM 实体
- 是否可能用作 HashMap Key
- 是否存在循环引用
- 是否包含大集合

不应因为方便就机械生成全部字段相等性。
例如实体对象包含：

```
List<OrderItem> items
```

如果自动加入 equals/hashCode：

- 比较成本可能很高
- 懒加载可能被触发
- 集合修改会改变 hashCode
- 双向关联可能递归
- 日志和调试可能出现栈溢出

## 18.28 实体对象的相等性设计

实体强调：

```
同一身份
```

而不是所有字段相同。
例如订单：

```java
public class Order {
    private final OrderId orderId;
    private OrderStatus status;
}
```

即使状态变化：

```
CREATED
→ PROCESSING
→ COMPLETED
```

仍然是同一个订单。
因此实体相等性通常基于：

```
稳定业务 ID
```

而不是所有可变字段。

**18.28.1 业务 ID 优于可变字段**

不推荐：

```
return Objects.equals(status, other.status)
&& Objects.equals(items, other.items)
&& Objects.equals(address, other.address);
```

这些字段可能随业务变化。
更合理：

```
return Objects.equals(
orderId,
other.orderId
);
```

前提是 orderId：

- 创建时就存在
- 全局或业务范围唯一
- 生命周期内不改变
- 不依赖数据库持久化后才生成

## 18.29 值对象的相等性设计

值对象强调：

```
所有业务值相同
```

例如：

```java
public final class Address {
    private final String province;
    private final String city;
    private final String detail;
}
```

相等性可以基于：

```
province
city
detail
```

值对象通常适合：

- final 类
- final 字段
- 不可变
- 完整重写 equals/hashCode
- 修改时创建新对象

**18.29.1 值对象没有独立身份**

两个分别创建的地址对象：

```
Address a =
new Address(
"广东",
"深圳",
"南山区"
);
Address b =
new Address(
"广东",
"深圳",
"南山区"
);
```

虽然：

```
a != b
```

但业务上：

```
a.equals(b)
```

应为 true。

## 18.30 equals 的性能考虑

equals 可能被频繁调用，例如：

- HashMap 查找
- HashSet 去重
- 集合 contains
- 列表 remove
- 单元测试断言
- 因此 equals 应尽量：
- 无副作用
- 不访问数据库
- 不进行网络请求
- 不依赖外部服务
- 不执行超大对象图比较
- 先比较成本较低的字段
- 先使用 this == other
- 例如：

```
return id == other.id
&& Objects.equals(code, other.code)
&& Objects.equals(details, other.details);
```

可以先比较：

```
便宜且区分度高的字段
```

再比较复杂字段。

## 18.31 toString、equals、hashCode 中的循环引用

双向关联：

```java
class Parent {
    private List<Child> children;
}
class Child {
    private Parent parent;
}
```

如果自动生成 toString：

```
Parent.toString()
→ Child.toString()
→ Parent.toString()
→ ...
```

可能导致：

```
StackOverflowError
```

equals/hashCode 同样可能递归。
因此：
不要机械包含全部关联字段
双向关系中至少一侧排除
实体优先使用稳定 ID
日志只输出必要摘要
避免打印完整对象图

实验一：默认 equals 是身份比较

```java
public class DefaultEqualsDemo {
    public static void main(String[] args) {
        User user1 = new User("A");
        User user2 = new User("A");
        System.out.println(user1 == user2);
        System.out.println(
        user1.equals(user2)
        );
    }
    static class User {
        private final String name;
        User(String name) {
            this.name = name;
        }
    }
}
```

预期：

```
false
false
```

实验二：重写 equals

```java
public class EqualsDemo {
    public static void main(String[] args) {
        User user1 = new User("U001");
        User user2 = new User("U001");
        System.out.println(
        user1.equals(user2)
        );
    }
    static final class User {
        private final String userId;
        User(String userId) {
            this.userId = userId;
        }
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
            );
        }
        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }
}
```

预期：

```
true
```

实验三：只重写 equals 的 HashSet 问题
创建一个只重写 equals、不重写 hashCode 的类：

```java
static class User {
    private final String userId;
    User(String userId) {
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

执行：

```java
Set<User> users = new HashSet<>();
users.add(new User("U001"));
users.add(new User("U001"));
System.out.println(users.size());
```

观察结果，理解为什么逻辑相等对象仍可能同时存在。
实验四：可变 Key 失效

```java
public class MutableKeyDemo {
    public static void main(String[] args) {
        UserKey key = new UserKey("U001");
        Map<UserKey, String> map =
        new HashMap<>();
        map.put(key, "data");
        System.out.println(map.get(key));
        key.userId = "U002";
        System.out.println(map.get(key));
    }
    static class UserKey {
        private String userId;
        UserKey(String userId) {
            this.userId = userId;
        }
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof UserKey key)) {
                return false;
            }
            return Objects.equals(
            userId,
            key.userId
            );
        }
        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }
}
```

观察修改字段后 Map 查找失败。
实验五：数组 equals

```java
public class ArrayEqualsDemo {
    public static void main(String[] args) {
        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        System.out.println(a.equals(b));
        System.out.println(
        Arrays.equals(a, b)
        );
    }
}
```

预期：

```
false
true
```

实验六：String 的 == 与 equals

```java
public class StringEqualsDemo {
    public static void main(String[] args) {
        String a = new String("Java");
        String b = new String("Java");
        System.out.println(a == b);
        System.out.println(a.equals(b));
    }
}
```

预期：

```
false
true
```

实验七：BigDecimal equals 与 compareTo

```java
public class BigDecimalEqualsDemo {
    public static void main(String[] args) {
        BigDecimal a =
        new BigDecimal("1.0");
        BigDecimal b =
        new BigDecimal("1.00");
        System.out.println(a.equals(b));
        System.out.println(
        a.compareTo(b) == 0
        );
    }
}
```

预期：

```
false
true
```



## 18.32 高频面试题

本章建议保留以下问题：

- 1.Object 是什么？
- 2.所有 Java 类都继承 Object 吗？
- 3.Object 有哪些常见方法？
- 4.基本类型和引用类型使用 == 分别比较什么？
- 5. == 与 equals() 有什么区别？
- 6.Object 默认 equals 比较什么？
- 7.什么情况下应该重写 equals？
- 8.equals 必须满足哪些契约？
- 9.什么是 equals 的自反性？
- 10.什么是 equals 的对称性？
- 11.什么是 equals 的传递性？
- 12.equals 为什么不能依赖随机值和外部状态？
- 13. x.equals(null) 应该返回什么？
- 14.equals 中使用 instanceof 和 getClass() 有什么区别？
- 15.为什么可继承类的 equals 很难设计？
- 16.为什么重写 equals 后必须重写 hashCode？
- 17.equals 与 hashCode 的契约是什么？
- 18.hashCode 相等是否代表 equals 相等？
- 19.equals 相等时 hashCode 是否必须相等？
- 20.hashCode 是否是对象内存地址？
- 21.hashCode 是否保证唯一？
- 22.HashMap 如何使用 hashCode 和 equals？
- 23.为什么不能只使用 hashCode 判断对象相等？
- 24.为什么可变对象不适合作为 HashMap Key？
- 25.哪些字段适合参与 equals 和 hashCode？
- 27.数据库自增 ID 参与 equals 有什么问题？
- 28.String 的 == 和 equals 有什么区别？
- 29.包装类型应该如何比较？
- 30.BigDecimal 的 equals 和 compareTo 有什么区别？
- 31.数组应该如何比较内容？
- 32. Objects.equals() 有什么作用？
- 33. Objects.deepEquals() 有什么作用？
- 48.双向关联为什么可能导致 toString 或 equals 栈溢出？


## 18.33 易错点

- 误区：Object 是接口。错误，Object 是类体系的根类。
- 误区：基本类型也是 Object 子类。错误，基本类型不是对象，赋值给 Object 时发生自动装箱。
- 误区：引用类型的 == 比较对象内容。错误，== 比较是否指向同一对象。
- 误区：所有类默认 equals 都比较字段内容。错误，Object 默认 equals 近似于 this == obj。
- 误区：重写 equals 就足够了。错误，必须同步重写 hashCode。
- 误区：hashCode 相等代表对象相等。错误，哈希冲突允许不同对象有相同 hashCode。
- 误区：对象不相等时 hashCode 必须不同。错误，equals 不相等的对象可以有相同 hashCode。
- 误区：hashCode 就是对象地址。错误，规范不要求 hashCode 等于内存地址。
- 误区：Map Key 修改字段后不影响查找。错误，修改参与 hashCode 的字段后可能找不到。
- 误区：数组 equals 比较数组内容。错误，数组没有按元素重写 equals，应使用 Arrays.equals。
- 误区：BigDecimal 数值相等时 equals 一定为 true。错误，BigDecimal.equals 还会比较 scale。
- 误区：final 类可以彻底解决 equals 设计。不完整，仍需正确选择字段并维护 hashCode 契约。
- 误区：自动生成 equals/hashCode 永远安全。错误，可能错误包含可变字段、集合、懒加载或双向关联。

## 18.34 工程实践建议

- 重写 equals 必须同步重写 hashCode，使用同一组字段。
- 只使用稳定字段参与 equals/hashCode。
- 避免可变字段作为 Map Key 相等性基础。
- 实体优先使用稳定业务 ID 判断相等性。
- 值对象优先设计为不可变，按全部业务值判断相等。
- 不在 equals/hashCode 中访问数据库、网络或懒加载大对象图。
- Lombok 生成前审查字段：排除可变字段、集合、懒加载和双向关联。
- 双向关联中至少一侧排除递归，避免 toString/equals/hashCode 栈溢出。
- 使用 EqualsVerifier 等工具只能作为辅助，不能替代语义设计。

## 18.35 本章总结

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
```

== 与 equals：

```
基本类型 ==
→ 数值比较
引用类型 ==
→ 对象身份比较
equals
→ 由类定义逻辑相等语义
```

equals 与 hashCode：

```
equals 相等
→ hashCode 必须相等
hashCode 相等
→ equals 不一定相等
```

对象设计：

```
实体对象
→ 使用稳定身份判断相等
值对象
→ 使用全部业务值判断相等
HashMap Key
→ 相等性字段尽量不可变
```

面试口述版：

- Object 是 Java 普通类体系的根类，默认提供 equals、hashCode、toString、getClass、clone 以及线程协作相关方法。对于基本类型， == 比较数值；对于引用类
- 型， == 比较两个引用是否指向同一对象。Object 默认的 equals 本质上也是身份比较，业务对象需要根据自身语义决定是否重写。equals 必须满足自反性、对称
- 性、传递性、一致性和非空性。重写 equals 后必须同步重写 hashCode，并保证 equals 相等的对象 hashCode 一定相等。HashMap 和 HashSet 会先通过 hashCode
- 定位候选位置，再通过 equals 确认逻辑相等，因此参与 equals 和 hashCode 的字段应保持稳定，尤其不应随意修改作为 Map Key 的对象。值对象通常按全部业务值
- toString 应提供有用的调试信息，但不能泄露敏感数据；clone 默认是浅拷贝，工程上通常更推荐拷贝构造方法或

静态复制工厂。
