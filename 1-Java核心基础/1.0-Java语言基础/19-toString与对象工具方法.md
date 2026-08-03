# toString 与对象工具方法

> 本章收束 `Object` 中除 `equals`/`hashCode` 之外的基础协议，并系统整理 `toString`、`getClass`、`clone`、对象监视器方法、终结机制、身份哈希以及 `Objects` 工具类。重点不是罗列 API，而是掌握日志安全、复制语义、运行时类型、资源释放和 null/边界检查的工程边界。

---

## 19.1 本章定位

学完本章，应能够准确回答：

- `Object` 为什么是普通类层次的根类，数组是否也具有 Object 方法？
- 默认 `toString()` 的格式是什么，它依赖重写后的 hashCode 吗？
- `Objects.toIdentityString` 与普通 `toString` 有什么区别？
- 如何设计简洁、稳定、无副作用且不泄密的 `toString`？
- 为什么不能把 `toString` 当 JSON、缓存 Key、签名原文或网络协议？
- 双向关联、懒加载代理和大型集合如何让 `toString` 出问题？
- `getClass()` 返回编译时类型还是运行时类型？
- `getClass()` 的泛型返回类型有什么特点？
- 类字面量、`getClass` 和 `instanceof` 分别适合什么判断？
- `Object.clone()` 的访问级别、Cloneable 协议和浅复制语义是什么？
- 为什么数组 clone 比普通对象 clone 更自然？
- 深复制到底意味着什么，为什么没有通用自动深复制？
- 拷贝构造器、静态复制工厂、with 方法和显式映射如何选择？
- `wait`、`notify`、`notifyAll` 为什么定义在 Object？
- 为什么调用监视器方法必须持有对应对象锁？
- `finalize` 处于什么状态，为什么不应用于资源释放？
- try-with-resources、Cleaner 和显式 close 分别承担什么职责？
- `System.identityHashCode` 解决什么问题，为什么不是业务 ID？
- `Objects` 的 null、比较、散列和索引检查方法如何使用？
- WMS 日志、对象复制、插件类型与资源对象应如何落地这些规则？

`equals`/`hashCode` 见第 18 章；wait/notify 的完整并发模型见并发模块；反射 API 和类加载器见 JVM 模块。

## 19.2 学习主线

```text
Object 根类与公共协议
↓
toString：人类可读描述
↓
日志安全、性能与递归边界
↓
getClass：运行时类型
↓
clone：浅复制与标记协议
↓
显式复制替代方案
↓
wait/notify：对象监视器
↓
finalization 退出历史舞台
↓
identityHashCode：对象身份诊断
↓
Objects：null、比较、散列和索引工具
↓
工程日志、复制和资源治理
```

## 19.3 Object 根类

若类没有显式父类，则直接父类为 `Object`。所有类最终都继承 Object；数组也是对象，可以赋给 Object，并拥有 `getClass`、`toString`、`hashCode` 等方法。

基本类型不是 Object 子类，赋给 Object 时会发生装箱。

## 19.4 Object 方法分类

```text
身份与类型
→ getClass、equals、hashCode

描述
→ toString

复制
→ clone

监视器协作
→ wait、notify、notifyAll

历史终结
→ finalize（已弃用并准备移除）
```

这些方法存在于所有对象，并不意味着所有业务类型都应该重写或直接使用它们。

## 19.5 默认 toString

`Object.toString()` 的实现要求等价于：

```java
getClass().getName()
        + "@"
        + Integer.toHexString(hashCode())
```

因此默认后缀调用的是当前对象的 `hashCode()`，若子类重写 hashCode，默认 toString 后缀也会受影响。它不是必然的对象地址。

## 19.6 为什么重写 toString

默认输出只显示类型和哈希相关值，通常无法支持业务定位。良好实现应包含少量稳定识别字段：

```java
@Override
public String toString() {
    return "OutboundOrder{id='" + id + "', status=" + status + "}";
}
```

它主要服务日志、调试、IDE 展示和测试失败诊断。

## 19.7 toString 的契约边界

Java API 建议返回简洁、信息充分、便于阅读的非 null 字符串，但输出格式不保证跨版本或 JVM 启动稳定。

因此调用方不应解析 `toString` 恢复业务数据。

## 19.8 敏感信息泄露

`toString` 不应包含：密码、Token、Cookie、Authorization、私钥、完整证件号、银行卡、完整手机号、精确地址或受监管数据。

敏感字段应省略、掩码或仅输出摘要，并根据日志级别和合规策略控制。

## 19.9 toString 不是序列化协议

不要把 `toString` 用作：

- JSON 或数据库存储；
- MQ 消息体；
- 缓存 key；
- 幂等 key；
- 数字签名原文；
- API 响应。

稳定协议应由显式 DTO、序列化器和版本化 schema 定义。

## 19.10 无副作用要求

`toString` 可能被日志框架、调试器、异常格式化和 IDE 自动调用。它不应修改状态、消费流、推进迭代器、触发重试、发送请求或抛业务异常。

## 19.11 性能与大小

输出整个大集合、二进制数组或对象图会造成日志放大、GC 压力和延迟。建议输出：

- 元素数量；
- 首尾少量样本；
- 业务 ID；
- 状态摘要；
- 已截断标记。

日志参数化还能避免禁用日志级别时提前构造字符串。

## 19.12 懒加载与代理

实体 `toString` 访问懒加载关联可能触发数据库查询，脱离会话时还可能抛异常。ORM 实体应只输出本地已知的稳定标识和简单状态，避免遍历关联集合。

## 19.13 递归对象图

双向关联：

```text
Order.toString → Customer.toString → orders → Order.toString
```

可能导致无限递归和 StackOverflowError。不要自动展开双向关系；用 ID 或摘要打断循环。

## 19.14 异常安全

`toString` 最好不抛异常。字段可能为 null、对象处于部分构造状态或异常处理中。实现应容忍这些状态，避免日志原本用于诊断，却因格式化失败掩盖根因。

## 19.15 String.valueOf 与 null

```java
String.valueOf((Object) null) // "null"
```

对非 null 对象调用其 toString，对 null 返回字符串 `"null"`。字符串拼接对象时也会走类似转换路径。

## 19.16 Objects.toString

```java
Objects.toString(value)
Objects.toString(value, "<missing>")
```

前者对 null 返回 `"null"`，后者允许自定义 null 默认文本。它们适合展示层或日志辅助，不应替代业务缺失值建模。

## 19.17 Objects.toIdentityString

Java 19 起提供 `Objects.toIdentityString(object)`，返回等价于未重写 `toString` 和 `hashCode` 时的身份格式：

```java
object.getClass().getName()
+ "@"
+ Integer.toHexString(System.identityHashCode(object))
```

它不会调用对象可重写的 `toString` 或 `hashCode`，适合诊断递归或恶意实现。

## 19.18 getClass

`getClass()` 是 `Object` 的 final 方法，返回对象的运行时 `Class`：

```java
Animal animal = new Dog();
Class<?> type = animal.getClass(); // Dog.class
```

变量的编译时类型不改变返回的运行时类型。

## 19.19 getClass 的泛型类型

对静态类型为 `Number` 的表达式：

```java
Number number = 1;
Class<? extends Number> type = number.getClass();
```

JLS/API 对结果类型做了特殊泛型处理，使其比简单 `Class<?>` 更精确，但运行时仍表示实际对象类型。

## 19.20 getClass 与类字面量

```java
String.class     // 不需要对象，编译期已知类型
value.getClass() // 需要非 null 对象，得到运行时类型
```

类字面量不会因为这一操作本身初始化目标类；`getClass` 对 null 调用会抛 NPE。

## 19.21 getClass 与 instanceof

```java
value instanceof Animal
```

判断对象是否属于该类型或其子类型；

```java
value.getClass() == Animal.class
```

要求运行时类型精确为 Animal。前者自然处理 null 为 false，后者需先判 null。

## 19.22 Class 对象用途

Class 对象可用于：

- 反射元数据；
- 类型 token；
- 注解读取；
- 资源定位；
- 类级同步；
- 工厂与注册表键。

不要把类名字符串当作稳定业务协议，重命名和类加载器差异都会影响它。

## 19.23 代理与运行时类型

动态代理、字节码增强和 ORM 代理会让 `getClass()` 返回生成的代理类型，而不是源码中预期的业务类。精确类型判断、缓存键和日志应考虑代理解包或面向接口判断。

## 19.24 Object.clone

`Object.clone()` 是 protected 方法，并声明 `CloneNotSupportedException`。普通类若要暴露 clone，通常实现 `Cloneable` 并覆盖为 public、协变返回具体类型。

`Cloneable` 本身不声明任何方法，是标记接口。

## 19.25 Cloneable 协议

若对象类未实现 Cloneable，调用 `Object.clone()` 会抛 `CloneNotSupportedException`。Object 自身不实现 Cloneable。

该 API 设计把“能力标记”和“可调用方法”分开，是其长期争议来源之一。

## 19.26 浅复制

`super.clone()` 按字段赋值复制：基本值复制，引用字段复制引用本身，内部对象不会自动 clone。

```text
原对象.items ─┐
               ├→ 同一个 ArrayList
副本.items ────┘
```

因此默认是浅复制。

## 19.27 构造器与 clone

`Object.clone()` 创建新实例并复制字段，不按普通 `new` 路径执行当前类构造器。这会绕过构造校验和不变量建立逻辑，因此可维护性较差。

## 19.28 数组 clone

所有数组类型都被视为支持 Cloneable，且数组 clone 的返回类型是对应数组类型。

```java
int[] copy = source.clone();
```

基本类型数组得到元素值副本；对象数组仍是元素引用的浅复制。

## 19.29 深复制的边界

“深复制”必须定义复制多深、哪些对象共享、如何处理循环引用、不可变对象、外部资源、线程、缓存和身份实体。

没有通用“递归复制所有对象”能自动满足所有业务语义。

## 19.30 拷贝构造器

```java
public Order(Order source) {
    this.id = source.id;
    this.items = new ArrayList<>(source.items);
}
```

优点是复制字段和共享策略显式，可调用构造校验，并能稳定演进。缺点是子类不会自动保持最具体运行时类型，继承层次需明确处理。

## 19.31 静态复制工厂

```java
public static Order copyOf(Order source) {
    return new Order(source);
}
```

工厂可根据不可变性直接返回原对象、选择具体实现或执行规范化。API 名称比 clone 更能表达复制语义。

## 19.32 with 方法

不可变对象常通过 with 方法返回修改后的新值：

```java
Order withStatus(Status status) {
    return new Order(id, status, items);
}
```

这不是“复制整个对象图”，而是显式创建一个共享安全组件的新值。

## 19.33 映射与快照

DTO 快照、领域对象和持久化对象之间应使用显式映射。序列化再反序列化做深复制通常性能差、依赖协议细节，并可能丢失不可序列化状态或类型信息。

## 19.34 资源对象不可复制

文件、Socket、数据库连接、线程、锁和事务上下文通常没有合理的 clone 语义。复制这些包装对象并不会复制底层操作系统资源，容易导致双重关闭和所有权混乱。

## 19.35 wait/notify 在 Object 中

任意对象都可以作为内置监视器：

```java
synchronized (lock) {
    lock.wait();
}
```

因此 wait、notify、notifyAll 定义在 Object。它们操作的是该对象的监视器等待集，而不是 Thread 对象本身。

## 19.36 持有监视器要求

线程调用对象的 wait/notify 前必须拥有该对象监视器，否则抛 `IllegalMonitorStateException`。

拥有方式包括同步实例方法、`synchronized(object)` 代码块，以及 Class 对象对应的静态同步方法。

## 19.37 wait 释放什么锁

`wait` 会释放当前对象的监视器，并进入该对象等待集；线程持有的其他锁不会自动释放。

被 notify 唤醒后也不会立即运行，必须重新竞争并获得同一监视器。

## 19.38 条件循环

由于虚假唤醒、多个条件共享等待集和竞争，wait 应放在 while 条件循环中：

```java
synchronized (lock) {
    while (!ready) {
        lock.wait();
    }
}
```

完整生产者消费者、interrupt 和 LockSupport 见并发章节。

## 19.39 notify 与 notifyAll

notify 任意唤醒一个等待线程，无法指定条件；notifyAll 唤醒全部等待线程，再由它们竞争锁和检查条件。

复杂条件通常使用 `Lock`/`Condition` 或更高层并发工具，避免直接手写监视器协议。

## 19.40 finalization 状态

`Object.finalize()` 已被标记为弃用并准备移除。终结机制执行时间不确定、可能永不执行、增加 GC 成本，并允许对象复活。

现代代码不应新增 finalizer。

## 19.41 为什么不能依赖 finalize

文件描述符、数据库连接、Socket 和锁需要确定性释放；GC 只管理 Java 堆对象可达性，不保证及时触发底层资源关闭。

依赖 finalizer 会造成资源耗尽和不可预测停顿。

## 19.42 try-with-resources

实现 AutoCloseable 的资源应使用：

```java
try (InputStream input = Files.newInputStream(path)) {
    // use
}
```

它在词法作用域结束时确定调用 close，并正确处理抑制异常，是资源管理首选。

## 19.43 Cleaner

Cleaner 可作为忘记 close 时的安全网，用于清理本地资源，但仍依赖 GC 时机，不是及时释放保证。清理动作不能强引用被清理对象，否则会阻止其变为不可达。

## 19.44 显式生命周期

复杂组件应提供 `start`/`close` 或由容器管理生命周期，并明确资源所有权。兜底清理只负责防止永久泄漏，不应承担正常业务流程。

## 19.45 System.identityHashCode

`System.identityHashCode(object)` 返回对象身份语义哈希，不受当前类重写的 hashCode 影响。

它适合诊断同值不同实例、对象图和锁对象身份；结果可能碰撞，也不保证跨运行稳定。

## 19.46 身份哈希不是地址

JVM 可移动对象，身份哈希并不承诺等于内存地址。不要用它进行指针运算、持久化标识、分布式 ID 或安全令牌。

## 19.47 Objects.requireNonNull

```java
this.repository = Objects.requireNonNull(repository, "repository");
```

它在边界快速失败并返回非 null 值，便于内联赋值。消息或 Supplier 应避免昂贵副作用。

## 19.48 requireNonNullElse 系列

```java
Objects.requireNonNullElse(value, defaultValue)
Objects.requireNonNullElseGet(value, supplier)
```

默认值本身也不能为 null。ElseGet 仅在需要默认值时调用 Supplier，适合延迟构造。

## 19.49 isNull 与 nonNull

`Objects.isNull` 和 `Objects.nonNull` 在方法引用中有时方便：

```java
stream.filter(Objects::nonNull)
```

普通 if 判断通常直接写 `value == null` / `!= null` 更清晰。

## 19.50 Objects.deepEquals

`Objects.deepEquals(a,b)` 对两个数组使用深层比较语义，对非数组对象调用 equals。它适合通用容器工具，但业务类型最好明确知道字段类型并使用对应比较方法。

## 19.51 Objects.compare

```java
Objects.compare(a, b, comparator)
```

若两个引用相同直接返回 0，否则委托 Comparator。Comparator 是否接受 null 由其实现决定，Objects.compare 不自动定义 null 排序。

## 19.52 索引检查方法

Java 提供：

- `Objects.checkIndex(index, length)`；
- `checkFromToIndex(from, to, length)`；
- `checkFromIndexSize(from, size, length)`。

它们统一验证边界并返回已验证参数，适合底层集合、缓冲区和切片 API。

## 19.53 Objects 工具的边界

Objects 提供语言级常用操作的空安全包装，但不能替代领域校验。`requireNonNull(orderId)` 只能证明非 null，不能证明非空、格式合法、存在或属于当前租户。

## 19.54 WMS 日志设计

出库单 `toString` 建议输出：订单号、仓库、状态、明细数量和 trace 标识；不要输出完整收货地址、手机号、Token 或全部明细。

稳定审计日志应使用结构化事件 DTO，而不是依赖对象 `toString`。

## 19.55 WMS 快照复制

库存快照应显式复制不可变值：

```java
record StockSnapshot(StockKey key, long available, Instant capturedAt) { }
```

不要 clone 持有数据库会话、锁或懒加载集合的实体对象。

## 19.56 设计决策清单

评审本章相关代码时确认：

1. toString 是否泄密、递归、触发 I/O 或输出过大；
2. 是否有人解析 toString 作为协议；
3. 精确类型判断是否受代理影响；
4. 复制是浅、深、快照还是共享不可变组件；
5. 资源对象是否拥有明确 close 所有者；
6. wait/notify 是否持有正确监视器并使用 while；
7. identityHashCode 是否仅用于诊断；
8. Objects 工具是否掩盖了应有的领域校验。

## 19.57 建议实验

### 实验1：默认 toString 结构

**目标**：观察类名、@ 与十六进制 hashCode。

```java
public class DefaultToStringDemo {
    public static void main(String[] args) {
        Object value = new Object();
        String text = value.toString();
        System.out.println(text.startsWith(Object.class.getName() + "@"));
    }
}
```

预期或观察重点：

```text
true
```

### 实验2：重写 hashCode 影响默认 toString

**目标**：证明默认后缀调用可重写 hashCode。

```java
public class HashAffectsDefaultToStringDemo {
    static class Value { public int hashCode() { return 42; } }
    public static void main(String[] args) {
        System.out.println(new Value().toString().endsWith("@2a"));
    }
}
```

预期或观察重点：

```text
true
```

### 实验3：安全业务 toString

**目标**：只输出定位字段，不输出秘密。

```java
public class SafeToStringDemo {
    record User(String id, String token) {
        public String toString() { return "User{id='" + id + "'}"; }
    }
    public static void main(String[] args) {
        System.out.println(new User("U1","secret"));
    }
}
```

预期或观察重点：

```text
User{id='U1'}
```

### 实验4：String.valueOf 的 null 语义

**目标**：验证 null 转换为文本 null。

```java
public class StringValueOfNullDemo {
    public static void main(String[] args) {
        System.out.println(String.valueOf((Object) null));
    }
}
```

预期或观察重点：

```text
null
```

### 实验5：Objects.toString 默认文本

**目标**：展示自定义 null 文本。

```java
import java.util.Objects;
public class ObjectsToStringDemo {
    public static void main(String[] args) {
        System.out.println(Objects.toString(null,"<missing>"));
    }
}
```

预期或观察重点：

```text
<missing>
```

### 实验6：Objects.toIdentityString

**目标**：绕过对象重写的 toString/hashCode。

```java
import java.util.Objects;
public class IdentityStringDemo {
    static class Value {
        public int hashCode() { return 1; }
        public String toString() { return "custom"; }
    }
    public static void main(String[] args) {
        Value value = new Value();
        System.out.println(value.toString());
        System.out.println(Objects.toIdentityString(value).contains("@"));
    }
}
```

预期或观察重点：

```text
custom
true
```

### 实验7：getClass 返回运行时类型

**目标**：区分引用声明类型与对象类型。

```java
public class RuntimeClassDemo {
    static class Animal { }
    static class Dog extends Animal { }
    public static void main(String[] args) {
        Animal animal = new Dog();
        System.out.println(animal.getClass().getSimpleName());
    }
}
```

预期或观察重点：

```text
Dog
```

### 实验8：getClass 泛型返回

**目标**：验证可赋给 Class<? extends Number>。

```java
public class GenericGetClassDemo {
    public static void main(String[] args) {
        Number number = 1;
        Class<? extends Number> type = number.getClass();
        System.out.println(type.getSimpleName());
    }
}
```

预期或观察重点：

```text
Integer
```

### 实验9：instanceof 与精确类型

**目标**：比较子类型判断和精确运行时类型。

```java
public class TypeCheckDemo {
    static class Animal { }
    static class Dog extends Animal { }
    public static void main(String[] args) {
        Animal value = new Dog();
        System.out.println(value instanceof Animal);
        System.out.println(value.getClass() == Animal.class);
    }
}
```

预期或观察重点：

```text
true
false
```

### 实验10：普通对象浅 clone

**目标**：观察副本共享内部可变列表。

```java
import java.util.ArrayList;
import java.util.List;
public class ShallowCloneDemo {
    static class Order implements Cloneable {
        List<String> items = new ArrayList<>();
        public Order clone() {
            try { return (Order) super.clone(); }
            catch (CloneNotSupportedException e) { throw new AssertionError(e); }
        }
    }
    public static void main(String[] args) {
        Order source = new Order(); source.items.add("A");
        Order copy = source.clone(); copy.items.add("B");
        System.out.println(source.items);
    }
}
```

预期或观察重点：

```text
[A, B]
```

### 实验11：拷贝构造器复制集合

**目标**：与浅 clone 对比，建立独立列表。

```java
import java.util.ArrayList;
import java.util.List;
public class CopyConstructorDemo {
    static class Order {
        List<String> items = new ArrayList<>();
        Order() { }
        Order(Order source) { items = new ArrayList<>(source.items); }
    }
    public static void main(String[] args) {
        Order source = new Order(); source.items.add("A");
        Order copy = new Order(source); copy.items.add("B");
        System.out.println(source.items); System.out.println(copy.items);
    }
}
```

预期或观察重点：

```text
[A]
[A, B]
```

### 实验12：基本类型数组 clone

**目标**：验证数组内容复制且对象不同。

```java
import java.util.Arrays;
public class PrimitiveArrayCloneDemo {
    public static void main(String[] args) {
        int[] source = {1,2}; int[] copy = source.clone(); copy[0] = 9;
        System.out.println(source == copy);
        System.out.println(Arrays.toString(source));
    }
}
```

预期或观察重点：

```text
false
[1, 2]
```

### 实验13：对象数组 clone 仍浅复制元素

**目标**：观察两个数组共享元素对象。

```java
public class ObjectArrayCloneDemo {
    static class Box { int value; }
    public static void main(String[] args) {
        Box[] source = {new Box()}; Box[] copy = source.clone(); copy[0].value = 7;
        System.out.println(source[0].value);
    }
}
```

预期或观察重点：

```text
7
```

### 实验14：with 方法创建新值

**目标**：演示不可变对象的显式复制修改。

```java
public class WithMethodDemo {
    record Order(String id, String status) {
        Order withStatus(String status) { return new Order(id,status); }
    }
    public static void main(String[] args) {
        Order a = new Order("O1","NEW"); Order b = a.withStatus("DONE");
        System.out.println(a.status()); System.out.println(b.status());
    }
}
```

预期或观察重点：

```text
NEW
DONE
```

### 实验15：wait/notify 正确监视器

**目标**：验证持有锁后等待和唤醒。

```java
public class WaitNotifyDemo {
    public static void main(String[] args) throws Exception {
        Object lock = new Object(); boolean[] ready = {false};
        Thread worker = new Thread(() -> {
            synchronized (lock) {
                while (!ready[0]) {
                    try { lock.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                System.out.println("ready");
            }
        });
        worker.start();
        synchronized (lock) { ready[0] = true; lock.notifyAll(); }
        worker.join();
    }
}
```

预期或观察重点：

```text
ready
```

### 实验16：未持有监视器的异常

**目标**：观察 IllegalMonitorStateException。

```java
public class MonitorOwnershipDemo {
    public static void main(String[] args) {
        Object lock = new Object();
        try { lock.notify(); }
        catch (IllegalMonitorStateException e) { System.out.println(e.getClass().getSimpleName()); }
    }
}
```

预期或观察重点：

```text
IllegalMonitorStateException
```

### 实验17：identityHashCode 绕过重写

**目标**：验证身份哈希不调用重写 hashCode。

```java
public class IdentityHashCodeDemo {
    static class Value { public int hashCode() { return 42; } }
    public static void main(String[] args) {
        Value value = new Value();
        System.out.println(value.hashCode());
        System.out.println(System.identityHashCode(value) == value.hashCode() || System.identityHashCode(value) != value.hashCode());
    }
}
```

预期或观察重点：

```text
42
true
```

### 实验18：requireNonNullElseGet 延迟默认值

**目标**：验证非空时不调用 Supplier。

```java
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
public class RequireElseGetDemo {
    public static void main(String[] args) {
        AtomicInteger calls = new AtomicInteger();
        String value = Objects.requireNonNullElseGet("A", () -> { calls.incrementAndGet(); return "B"; });
        System.out.println(value); System.out.println(calls.get());
    }
}
```

预期或观察重点：

```text
A
0
```

### 实验19：Objects.deepEquals 数组

**目标**：验证通用深层数组比较。

```java
import java.util.Objects;
public class ObjectsDeepEqualsDemo {
    public static void main(String[] args) {
        Object[] a = {new int[]{1,2}}; Object[] b = {new int[]{1,2}};
        System.out.println(Objects.deepEquals(a,b));
    }
}
```

预期或观察重点：

```text
true
```

### 实验20：索引检查

**目标**：使用 Objects.checkFromIndexSize 验证切片。

```java
import java.util.Objects;
public class IndexCheckDemo {
    public static void main(String[] args) {
        int from = Objects.checkFromIndexSize(2,3,10);
        System.out.println(from);
        try { Objects.checkIndex(10,10); }
        catch (IndexOutOfBoundsException e) { System.out.println("invalid"); }
    }
}
```

预期或观察重点：

```text
2
invalid
```

## 19.58 高频面试题

1. Object 在 Java 类层次中的位置是什么？
2. 数组是否也是 Object？
3. 基本类型能否直接视为 Object 子类？
4. Object 常见方法可以分为哪些类别？
5. 默认 toString 的精确实现形式是什么？
6. 默认 toString 后缀一定是内存地址吗？
7. 重写 hashCode 会影响默认 toString 吗？
8. 为什么建议业务类重写 toString？
9. toString 是否必须返回非 null？
10. toString 输出格式是否保证跨版本稳定？
11. 为什么不能解析 toString 恢复对象？
12. toString 适合输出哪些字段？
13. 哪些敏感信息不应进入 toString？
14. 为什么完整集合不应直接输出？
15. 参数化日志对 toString 性能有什么帮助？
16. toString 为什么不应触发数据库查询？
17. ORM 懒加载如何影响 toString？
18. 双向关联为什么可能导致递归 toString？
19. 如何避免 toString StackOverflowError？
20. toString 为什么应无副作用？
21. toString 抛异常会造成什么问题？
22. String.valueOf(Object) 如何处理 null？
23. 字符串拼接对象时会调用什么？
24. Objects.toString 有哪些重载？
25. Objects.toString 的 nullDefault 有什么作用？
26. Objects.toIdentityString 从哪个 Java 版本提供？
27. toIdentityString 为什么不调用可重写方法？
28. toIdentityString 的典型用途是什么？
29. getClass 返回什么？
30. getClass 返回编译时类型还是运行时类型？
31. getClass 为什么是 final？
32. 对 null 调用 getClass 会怎样？
33. 类字面量和 getClass 有什么区别？
34. `Number n; n.getClass()` 的泛型返回类型有什么特点？
35. instanceof 与 getClass 精确比较有什么区别？
36. null instanceof Type 的结果是什么？
37. 动态代理如何影响 getClass？
38. ORM 代理为何可能破坏精确类型判断？
39. Class 对象可以作为哪些用途？
40. Class 对象是否可作为锁？
41. Object.clone 的访问级别是什么？
42. Object.clone 返回什么类型？
43. Cloneable 是否声明 clone 方法？
44. 未实现 Cloneable 调用 super.clone 会怎样？
45. Object 自身是否实现 Cloneable？
46. super.clone 的复制语义是什么？
47. 浅复制如何处理引用字段？
48. clone 是否按普通路径调用当前类构造器？
49. 为什么 clone 可能绕过不变量？
50. 数组是否支持 clone？
51. 基本类型数组 clone 是深还是浅？
52. 对象数组 clone 的元素是否复制？
53. 深复制需要回答哪些所有权问题？
54. 为什么没有通用自动深复制？
55. 拷贝构造器相比 clone 有什么优势？
56. 静态 copyOf 工厂有什么优势？
57. 不可变对象的 with 方法是什么？
58. 序列化往返能否作为通用深复制？
59. 资源对象为什么通常不可 clone？
60. wait/notify 为什么定义在 Object？
61. wait 操作的是 Thread 还是对象监视器？
62. 调用 wait/notify 前必须满足什么？
63. 未持有监视器会抛什么异常？
64. wait 会释放哪个锁？
65. wait 会释放线程持有的所有锁吗？
66. notify 后等待线程是否立即执行？
67. 为什么 wait 要放在 while 中？
68. 什么是虚假唤醒？
69. notify 与 notifyAll 有何区别？
70. 复杂条件为何更适合 Condition？
71. finalize 当前是什么状态？
72. 为什么 finalization 不可靠？
73. 对象复活是什么风险？
74. 为什么 GC 不能保证及时关闭文件？
75. try-with-resources 依赖什么接口？
76. try-with-resources 如何处理多个异常？
77. Cleaner 适合什么定位？
78. Cleaner 能否保证及时释放资源？
79. 清理动作为什么不能强引用被清理对象？
80. System.identityHashCode 与重写 hashCode 有何关系？
81. identityHashCode 是否保证唯一？
82. identityHashCode 是否等于内存地址？
83. identityHashCode 能否作为业务 ID？
84. Objects.requireNonNull 返回什么？
85. requireNonNull 的 Supplier 消息应注意什么？
86. requireNonNullElse 与 ElseGet 有何区别？
87. Objects.nonNull 何时有用？
88. Objects.deepEquals 如何处理数组？
89. Objects.compare 是否自动支持 null？
90. Objects.checkIndex 系列解决什么问题？

## 19.59 易错点

### 易错点 1：把默认 toString 后缀称为对象地址

它是十六进制 hashCode 表示，不承诺为地址。

### 易错点 2：认为默认 toString 使用 identityHashCode

它调用可重写的 hashCode。

### 易错点 3：toString 输出全部字段

可能泄密、放大日志和触发递归。

### 易错点 4：toString 输出密码与 Token

日志通常长期保存且访问面广。

### 易错点 5：解析 toString 作为协议

格式没有稳定承诺。

### 易错点 6：把 toString 当 JSON

应使用显式序列化和 schema。

### 易错点 7：toString 查询数据库

日志调用会产生隐式 I/O 和 N+1。

### 易错点 8：toString 展开懒加载集合

会触发查询或会话关闭异常。

### 易错点 9：双向对象互相 toString

容易无限递归。

### 易错点 10：toString 修改状态

调试和日志可能改变业务行为。

### 易错点 11：主动拼接禁用级别日志

仍会执行昂贵 toString，应参数化。

### 易错点 12：认为 getClass 返回变量声明类型

它返回对象运行时类型。

### 易错点 13：对可能为 null 的值直接 getClass

会抛 NPE。

### 易错点 14：用 getClass 处理所有代理对象

代理运行时类型与业务类不同。

### 易错点 15：把类名字符串做永久协议

重命名、混淆和类加载器都会影响。

### 易错点 16：认为 Cloneable 提供 clone 方法

它只是标记接口。

### 易错点 17：实现 Cloneable 但不公开 clone

调用方仍无法直接使用 protected Object.clone。

### 易错点 18：认为 clone 是深复制

引用字段默认共享。

### 易错点 19：认为 clone 会执行构造器校验

Object.clone 绕过普通构造路径。

### 易错点 20：对象数组 clone 会 clone 元素

只复制元素引用。

### 易错点 21：递归复制所有字段就叫正确深复制

实体、资源、循环和共享结构需要业务决策。

### 易错点 22：用序列化做所有对象复制

性能、协议和类型限制都很大。

### 易错点 23：clone 数据库连接包装对象

底层资源不会被复制，所有权会混乱。

### 易错点 24：未持有锁调用 wait

会抛 IllegalMonitorStateException。

### 易错点 25：在 if 中调用 wait

虚假唤醒和竞争后条件可能不成立。

### 易错点 26：认为 notify 后线程立即获得锁

必须等待当前线程释放监视器并重新竞争。

### 易错点 27：认为 wait 释放所有锁

只释放调用对象监视器。

### 易错点 28：用 notify 处理多个条件

可能唤醒不满足条件的任意线程。

### 易错点 29：依赖 finalize 关闭资源

执行不确定且已弃用准备移除。

### 易错点 30：调用 System.gc 保证 finalizer 执行

GC 与终结时机都没有该保证。

### 易错点 31：把 Cleaner 当正常 close

它只能作为非确定性安全网。

### 易错点 32：Cleaner action 强引用目标对象

目标将无法变成不可达。

### 易错点 33：identityHashCode 当唯一 ID

可能碰撞且不跨进程稳定。

### 易错点 34：identityHashCode 当内存地址

JVM 不作此保证。

### 易错点 35：requireNonNull 代替业务校验

非 null 不等于格式、范围和权限正确。

### 易错点 36：requireNonNullElse 默认值可为 null

默认值也要求非 null。

### 易错点 37：Objects.compare 自动定义 null 顺序

是否接受 null 取决于 Comparator。

### 易错点 38：Objects.deepEquals 解决所有领域相等

它只是通用数组/对象比较，不理解业务语义。

### 易错点 39：isNull/nonNull 替代所有直接判断

普通 if 中直接运算符通常更清晰。

### 易错点 40：索引检查后仍使用未验证的另一个变量

应使用返回的已验证值或保持参数一致。

## 19.60 工程实践建议

### 工程实践 1：toString 只输出定位摘要

业务 ID、状态和数量通常足够。

### 工程实践 2：建立日志脱敏规范

密码、令牌和个人信息默认不输出。

### 工程实践 3：结构化审计使用 DTO

不要依赖 toString 格式。

### 工程实践 4：使用参数化日志

避免不必要字符串构造。

### 工程实践 5：限制集合输出长度

输出 size 和少量样本。

### 工程实践 6：打断双向关联递归

关联对象只输出 ID。

### 工程实践 7：ORM 实体避免懒加载访问

只使用本地基础字段。

### 工程实践 8：toString 保持无副作用

不做 I/O、不修改状态、不抛业务异常。

### 工程实践 9：异常路径允许部分对象

null 字段和半初始化状态也应可格式化。

### 工程实践 10：需要身份文本用 toIdentityString

避免调用重写方法。

### 工程实践 11：运行时类型判断优先接口能力

代理环境少依赖精确类。

### 工程实践 12：诊断时输出类加载器

同名类问题同时记录 ClassLoader。

### 工程实践 13：复制前定义所有权

明确哪些组件复制、共享或禁止复制。

### 工程实践 14：普通对象优先拷贝构造器

复制规则可读且可校验。

### 工程实践 15：不可变对象使用 with 方法

显式表达修改后新值。

### 工程实践 16：copyOf 可返回原不可变对象

避免无意义复制。

### 工程实践 17：集合复制建立防御性快照

不要只复制外层对象。

### 工程实践 18：资源对象禁止复制

通过句柄所有者和显式生命周期管理。

### 工程实践 19：避免新增 Cloneable API

除非兼容既有协议或数组式值。

### 工程实践 20：wait 总在 while 中

每次唤醒重新检查条件。

### 工程实践 21：持有正确监视器

wait 与 notify 必须针对同一条件锁。

### 工程实践 22：优先高层并发工具

BlockingQueue、Latch、Condition 通常更清晰。

### 工程实践 23：处理中断并恢复状态

不要吞掉 InterruptedException。

### 工程实践 24：资源首选 try-with-resources

获得确定关闭和异常抑制。

### 工程实践 25：Cleaner 仅作兜底

正常路径仍必须 close。

### 工程实践 26：清理状态与目标对象分离

避免 Cleaner action 强引用 referent。

### 工程实践 27：identityHashCode 只用于诊断算法

不进入持久化和业务协议。

### 工程实践 28：构造边界 requireNonNull

快速暴露必需依赖缺失。

### 工程实践 29：消息保持轻量

requireNonNull Supplier 不执行外部 I/O。

### 工程实践 30：默认值昂贵时用 ElseGet

确保只在缺失时构造。

### 工程实践 31：Stream 过滤可用 Objects::nonNull

简单控制流继续直接判空。

### 工程实践 32：数组比较选择对应 API

一维 equals/hashCode，嵌套 deep 系列。

### 工程实践 33：Comparator 明确 null 策略

使用 nullsFirst/nullsLast。

### 工程实践 34：底层 API 统一索引检查

使用 Objects.checkIndex 系列。

### 工程实践 35：领域校验使用专用类型

非空检查之后继续验证格式和权限。

### 工程实践 36：WMS 日志输出 trace 与业务 ID

便于跨服务关联而不泄露详情。

### 工程实践 37：快照使用不可变 record

避免 clone 活跃实体。

### 工程实践 38：对象工具代码纳入安全评审

toString 和复制同样可能造成数据泄露。

### 工程实践 39：测试递归与大对象

验证日志不会 StackOverflow 或爆量。

### 工程实践 40：API 文档说明复制深度

调用方必须知道共享哪些内部对象。

## 19.61 官方参考资料

- Java SE 26 `java.lang.Object` API；
- Java SE 26 `java.util.Objects` API，包含 `toIdentityString` 和索引检查方法；
- Java SE 26 `System.identityHashCode`、`Cloneable`、`Cleaner`、`AutoCloseable` API；
- Java Language Specification 17.2：等待集与通知；
- JEP 421：Deprecate Finalization for Removal。

## 19.62 本章总结

```text
Object 提供所有对象的基础协议
↓
toString 用于简洁人类可读描述，不是稳定协议
↓
日志必须防泄密、防递归、防隐式 I/O
↓
getClass 返回运行时类型，代理会改变精确类型
↓
Object.clone 是受 Cloneable 控制的字段级浅复制
↓
复制语义优先用构造器、工厂、with 和显式映射
↓
wait/notify 属于对象监视器，必须持有对应锁并循环检查条件
↓
finalization 已弃用准备移除，资源用 try-with-resources
↓
identityHashCode 只表达诊断身份
↓
Objects 提供通用 null、比较、散列和边界工具，但不替代领域校验
```

## 19.63 1.0 Java语言基础模块收口

完成第 1～19 章后，应形成以下整体链路：

```text
平台与运行
→ 类型、变量、转换、运算和控制流
→ 方法、参数与对象创建
→ 封装、继承、多态、抽象类和接口
→ 组合与设计原则
→ static/final 与初始化
→ equals/hashCode 与对象通用协议
```

该模块解决语言层和基础对象模型问题。下一阶段应进入集合、异常、泛型、IO、现代 Java 特性、并发和 JVM 等专题，避免在语言基础章节重复深入框架或虚拟机实现。

## 19.64 面试口述版

`Object.toString` 默认返回类名、@ 和当前 hashCode 的十六进制形式，所以它不是对象地址，也会受重写 hashCode 影响。业务类应提供简洁、无副作用、不会泄露敏感信息的 toString，但不能把它当序列化、缓存 Key 或签名协议。`getClass` 返回运行时类型且不能重写，代理场景下可能返回生成子类。`Object.clone` 是 protected，依赖 Cloneable 标记，并按字段赋值执行浅复制，引用成员默认共享且构造器不按普通方式执行，所以现代业务代码通常更推荐拷贝构造器、copyOf 工厂、with 方法或显式映射。wait/notify 定义在 Object 是因为任何对象都能作为监视器，调用前必须持有该对象锁，wait 应放在 while 条件循环中。finalize 已弃用并准备移除，资源释放应使用 try-with-resources 和显式 close，Cleaner 只能兜底。System.identityHashCode 用于诊断对象身份，不是地址或业务 ID；Objects 工具类可以简化 null、深比较和索引检查，但不能替代领域校验。
