# Java 基础

> 一句话定位：这一专题考 Java 语言语义层的基本功——题目看起来人人会答，区分度全在第一个追问；与集合框架（讲数据结构怎么消费这些语义）、并发编程（讲语义在多线程下是否仍成立）的分界是：这里只回答「语言怎么规定、为什么这样规定」。

返回 [专题题库首页](README.md) · [面试首页](../README.md)。

## 专题定位

- 为什么高频：语言基础是每轮 Java 面试的固定开场，且题干极简——答出结论只是及格线，答得出设计取舍才加分；这里任何一题都可能成为后续追问的入口。
- 面试官主要验证三件事：概念口径是否精确（值传递/引用传递、==/equals、受检/非受检）、知不知道设计取舍（为什么不可变、为什么擦除、为什么 SPI 用上下文类加载器）、能不能把语义连到工程后果（循环拼接、反射性能、序列化漏洞）。
- 5 年以上工程师要答到的深度：不止「是什么」，要能给出规范或实现层面的依据（JLS、字节码、JVM 行为），并主动补版本边界（JDK 8 / 9 / 14～17 / 21 的差异）和代价分析。
- 标签口径：`P0`=优先复习，`P1`=常见，`P2`=了解即可；`高频`/`中频`/`低频` 是公开面经里的相对出现频率。两个维度独立——频率决定会不会被问，重要程度决定答不好扣多少分。
- 复习路径：按导航表 P0 → P2 过一遍「30 秒回答」，再用每题「容易答错的地方」自查口径，最后用文末追问链把 20 道题串成 5 条主线。

## 高频题目导航

| 编号 | 面试问题 | 重要程度 | 追问深度 |
| --- | --- | --- | --- |
| Q01 | Java 是值传递还是引用传递？ | P0 | 原理 |
| [Q02](Java基础/Q02-equals与hashCode.md) | 为什么重写 equals 就必须重写 hashCode？ | P0 | 源码 |
| Q03 | String 为什么设计成不可变？ | P0 | 原理 |
| Q04 | 泛型擦除带来了哪些限制？ | P1 | 源码 |
| Q05 | static 代码块和字段的初始化顺序是怎样的？ | P2 | 原理 |
| Q06 | 重载和重写分别在什么阶段确定？ | P0 | 源码 |
| Q07 | 包装类的缓存机制会在什么时候让 `==` 骗你？ | P1 | 工程实践 |
| Q08 | 接口和抽象类怎么选？ | P1 | 设计 |
| [Q09](Java基础/Q09-等号与equals.md) | `==` 和 equals 的区别？ | P0 | 工程实践 |
| Q10 | String、StringBuilder、StringBuffer 的区别？ | P0 | 工程实践 |
| Q11 | 泛型通配符 `? extends` 和 `? super`，PECS 原则？ | P1 | 设计 |
| Q12 | Java 异常体系是怎样的？ | P0 | 工程实践 |
| Q13 | 反射机制的原理和问题？ | P0 | 源码 |
| Q14 | 注解的原理？ | P1 | 源码 |
| Q15 | SPI 机制的原理？ | P1 | 工程实践 |
| Q16 | Java 8 及后续的重要语言特性？ | P0 | 工程实践 |
| Q17 | Stream 的常见陷阱？ | P1 | 工程实践 |
| Q18 | 深拷贝和浅拷贝？ | P1 | 工程实践 |
| Q19 | final、finally、finalize 的区别？ | P0 | 原理 |
| Q20 | 序列化：serialVersionUID 和 transient？ | P1 | 工程实践 |

## 题目正文

### Q01：Java 是值传递还是引用传递？ `P0` `高频`

**30 秒回答**：Java 只有值传递。传对象时传的是「引用值」的一份副本，所以既不是引用传递，也不是很多人理解的「把对象本身传过去」——方法内重新给形参赋值，调用方的变量不受影响；但通过形参修改对象内容，调用方能观察到变化。

**深入理解**：判据是「方法内重新给形参赋值，调用方是否受影响」——不受影响就说明传的是副本。

```java
void f(User u) {
    u = new User();      // 只改形参持有的引用值，调用方变量仍指向原对象
    u.setName("x");      // 改的是同一个堆对象，调用方可观察
}
```

常被混为一谈的还有 `String` 与包装类：方法内 `s = "b"` 改不动外部，原因是这两类**不可变**，与「传值」是两个独立性质。基本类型传的是值的副本，引用类型传的是引用值（地址）的副本——两者都是「复制」，区别只在复制的内容。

**为什么这样设计**：统一语义，避免 C++ 那种「值 / 引用 / 指针」三套参数传递规则，让「被调函数不能悄悄改变调用方的变量绑定」成为语言级保证。代价是 Java 没有真正的输出参数，需要返回多个值只能返回对象或用可变容器承载。

**面试官可能继续追问**：

- **那 C++ 里 `swap(a, b)` 为什么能成功？** —— 因为形参和实参是同一个变量绑定，重新赋值直接作用于调用方变量。Java 里交换两个引用必须把对象包进数组或容器、或用返回值，语言不提供这个能力。
- **形参声明为 final 能保证对象不被改吗？** —— 不能。final 只禁止形参重新绑定引用，对象字段该改还是能改。要不可变得靠类型本身（String、record）或防御性复制。
- **数组也是值传递吗？** —— 是。数组是对象，传的是数组引用的副本：`arr[0] = 1` 调用方可见，`arr = new int[10]` 调用方不可见。

**实际工程场景**：DTO/VO 作为参数进出服务层时，调用方持有的是同一个对象引用——方法内修改 DTO 字段会产生跨层副作用。防御性复制（拷贝构造器、`List.copyOf`）通常只放在系统边界（RPC 入口、缓存读写），内部调用靠「谁拥有可变状态」的约定控制。

**容易答错的地方**：说「对象按引用传递」是错的，正确说法是「引用类型按值传递，传的是引用值」；把「String 改不动」归因于值传递也是错的——那是不可变性，两回事。

> 权威内容：[参数传递与不可变对象](../../01-Java核心/01-面试速记/参数传递与不可变对象.md) · [参数传递机制](../../01-Java核心/02-深度解析/参数传递机制.md)

### Q02：为什么重写 equals 就必须重写 hashCode？ `P0` `高频`

完整标准答案已独立成页：[Q02 · 为什么重写 equals 就必须重写 hashCode？](Java基础/Q02-equals与hashCode.md)

### Q03：String 为什么设计成不可变？ `P0` `高频`

**30 秒回答**：不可变换来的是「可以安全共享」，而共享正是字符串常量池和哈希缓存两件优化的前提。

**深入理解**：`String` 内部承载字符的数组是 `final` 且不对外暴露（JDK 9 起底层是 `byte[]`，纯 Latin-1 内容时一个字符只占 1 字节），所有「修改」方法都返回新对象。由此得到三个直接收益：常量池可以放心复用同一实例，因为不可能被改掉；`hashCode` 可以惰性缓存，高频用作 `HashMap` 的 key 时不必重算；天然线程安全，无需同步。

安全收益常被忽略：类名、文件名、连接串、SQL 片段如果可变，改了它就能绕过校验——这也是 Java 安全模型大量使用 `String` 的原因。

**为什么这样设计**：可变字符串要么牺牲共享（每次修改都复制）、要么牺牲安全（共享实例被就地改），两者代价都高于「修改时新建」——因为字符串的典型使用模式是「构造一次、读取多次」，读取路径的性能远比修改路径重要。代价是循环拼接会产生大量临时对象，所以需要 `StringBuilder`。

**面试官可能继续追问**：

- **final 字段能保证不可变吗？** —— 不能，final 只禁止重新赋值；数组元素、对象字段照样能改。String 的不可变靠 final 数组 + 不暴露引用 + 无任何修改方法三者合一。
- **JDK 9 的 compact string 是什么？** —— 字符串底层从 `char[]` 改为 `byte[]`，纯 Latin-1 内容时每字符占 1 字节，堆占用约减半；含中文等非 Latin-1 字符时回退为 UTF-16（每字符 2 字节）。这是实现优化，不改变不可变语义。
- **反射能改 String 吗？** —— 技术上能（改私有 value 字段），但会破坏 JVM 内部假设（常量池、哈希缓存），属于攻击手法而非工程手段。

**实际工程场景**：SQL、URL、模板这类「拼一次、用多次」的串用普通 `+` 或 `String.join`；循环体内（尤其循环次数由数据量决定时）必须换 `StringBuilder`——一条 SQL 在循环里 `s += x` 是 O(n²) 的经典事故。

**容易答错的地方**：以为 `s.replace("a", "b")` 改了原串——它返回新串，原串不变；以为 `new String("abc")` 和 `"abc"` 相等——前者是堆上新对象，`==` 为 false、`equals` 为 true；`intern()` 的语义（JDK 7 起常量池移到堆后的行为变化）也是常见追问。

> 权威内容：[对象创建与不可变设计](../../01-Java核心/02-深度解析/对象创建与不可变设计.md) · [参数传递与不可变对象](../../01-Java核心/01-面试速记/参数传递与不可变对象.md)

### Q04：泛型擦除带来了哪些限制？ `P1` `中频`

**30 秒回答**：泛型只在编译期存在，运行期类型参数被擦成上界（无界则擦成 `Object`），所以拿不到类型实参——不能 `new T[]`、不能 `instanceof T`、静态成员不能用类型参数、基本类型不能作类型实参。

**深入理解**：擦除留下的痕迹是编译期插入的强制转换：`List<String>.get()` 在字节码里返回 `Object` 再 `checkcast` 成 `String`。桥接方法是擦除的副作用——实现 `Comparable<StringBox>` 时编译器会合成一个 `compareTo(Object)` 转发到真正的 `compareTo(StringBox)`，否则擦除后的签名对不上接口。也正因为运行期没有类型信息，`f(List<String>)` 与 `f(List<Integer>)` 无法重载，擦除后签名相同。想保留类型信息只能把 `Class<T>` 当参数显式传进来。

```java
// 堆污染：原始类型绕过编译期检查，运行期才炸
@SuppressWarnings("unchecked")
List<String> ls = new ArrayList();   // 原始类型赋给参数化引用
ls.add(1);                           // 编译期不报（unchecked 告警）
String s = ls.get(0);                // 运行期 ClassCastException
```

**为什么这样设计**：Java 5 引入泛型时必须保证既有代码和已编译字节码继续可用。若类型参数进入运行期，`List` 的二进制形态就变了，老库全部不兼容。擦除是「迁移兼容」的产物，代价是牺牲运行期类型能力与部分表达能力。

**面试官可能继续追问**：

- **什么是堆污染（heap pollution）？** —— 变量声明的参数化类型与运行期真实类型不一致，错误延迟到读取点才抛 `ClassCastException`；泛型 varargs 方法（需 `@SafeVarargs` 标注）是同一类风险。
- **`List<String>` 和 `List<Integer>` 运行期是同一个类吗？** —— 是，字节码层面完全相同，所以既不能靠 `getClass()` 区分，也不能重载。要保留类型信息得靠签名上的泛型元数据（反射的 `ParameterizedType`）。
- **反射能读到泛型信息吗？** —— 部分能：字段、方法、父类声明上的泛型可通过 `getGenericXxx` 系列读取，这是 Spring、Jackson 拿到真实类型的途径；但对象实例本身运行期不带类型参数。

**实际工程场景**：需要运行期类型的场景（JSON 反序列化、ORM 映射）用 `TypeReference` / `Class<T>` 显式传递，如 Jackson 的 `new TypeReference<List<Order>>() {}`；`new T[n]` 不可行，用集合替代或 `Array.newInstance`。

**容易答错的地方**：说「泛型完全不存在于运行期」——类/方法签名上的泛型元数据保留在字节码，反射可读；写 `instanceof List<String>`——非法，只能 `instanceof List<?>`；把 Valhalla 的泛型特化当成「即将修复擦除」——那是独立项目，尚未落地，不要写进答案。

> 权威内容：[Java 泛型与类型安全](../../01-Java核心/02-深度解析/Java泛型与类型安全.md)

### Q05：static 代码块和字段的初始化顺序是怎样的？ `P2` `中频`

**30 秒回答**：类初始化时按**源码顺序**依次执行静态字段赋值和静态代码块，父类先于子类，且只执行一次；实例初始化则是先父类构造、再本类字段赋值与实例块、最后本类构造体。

**深入理解**：静态部分分两步——准备阶段先给静态字段赋零值，随后的初始化阶段按源码顺序执行静态字段的初始化表达式和静态代码块，所以写在代码块之后的静态字段，在代码块里读到的是默认值。类初始化由「首次主动使用」触发：`new`、读写静态字段（编译期常量除外）、调静态方法、反射、初始化子类。`static final` 的编译期常量在编译阶段就被内联到调用方，不会触发本类初始化，这是最常被追问的边界。

```text
类初始化（首次主动使用，JVM 加锁保证只执行一次）：
  父类初始化
    → 本类静态字段赋值 / 静态代码块（按源码顺序）

实例化：
  父类构造器
    → 本类字段赋值 / 实例代码块（按源码顺序）
    → 本类构造器体
```

**为什么这样设计**：按源码顺序执行让「从上到下」的直觉与实际行为一致，准备阶段先赋零值则保证字段一定可读；而「首次主动使用才初始化」把类加载成本推迟到真正需要时，是延迟加载的基础。

**面试官可能继续追问**：

- **两个线程同时 new 同一个类，静态块执行几次？** —— 一次。类初始化由 JVM 按类加锁保证，并发方只阻塞等待；这也是「静态块里做单例初始化」天然线程安全的原因。
- **接口有静态代码块吗？** —— 接口没有静态代码块；JDK 8 前字段隐式 `public static final` 且必须声明处初始化，JDK 8 起接口可以有静态方法。接口的「初始化」在首次访问其非常量字段时触发，比类更隐蔽。
- **常量被内联有什么坑？** —— `static final int MAX = 100` 会被内联进调用方字节码，改了库里的值，已编译的调用方不重新编译就还是旧值——这是「修改常量不算是兼容变更」的根源。

**实际工程场景**：静态初始化适合放只读配置表和查找表；别在静态块里做 IO/网络调用（拖慢类加载且失败难以恢复），启动慢的模块把重活搬到懒加载单例，静态块只留轻量默认值。

**容易答错的地方**：认为「静态块总在 main 之前执行」——只有该类被主动使用时才初始化，且按主动使用链展开；认为实例块在构造器之后执行——实际在构造器体之前；把「准备阶段赋零值」与「初始化阶段真正赋值」混为一谈。

> 权威内容：[static 与类初始化](../../01-Java核心/02-深度解析/static与类初始化.md) · [类初始化与 final](../../01-Java核心/01-面试速记/类初始化与final.md)

### Q06：重载和重写分别在什么阶段确定？ `P0` `高频`

**30 秒回答**：重载在**编译期**按静态类型选方法（静态分派），重写在**运行期**按实际类型选方法（动态分派）——这个区别决定了「子类对象赋给父类引用」时到底调用谁。

**深入理解**：重载的选择依据是参数的最匹配，编译器只看静态类型；哪怕实参运行期是子类，也不会改选更精确的重载。重写依赖虚方法表：对象头的类型指针指向实际类的元数据，调用时按实际类型查表。两者交界处最容易出错——父类引用指向子类对象时，**调用哪个重载**由静态类型决定，**执行哪份方法体**由实际类型决定。`private`、`static`、`final` 方法不能重写，因为它们不进虚方法表。

```java
class Father { void f(Object o) { print("Father.f(Object)"); } }
class Son extends Father {
    @Override void f(Object o) { print("Son.f(Object)"); }  // 重写
    void f(Son s) { print("Son.f(Son)"); }                  // 重载
}
Father ref = new Son();
ref.f(new Son());
// 编译期：ref 的静态类型是 Father，可见签名只有 f(Object)，选 f(Object)
// 运行期：实际类型是 Son 且 Son 重写了 f(Object)，执行 Son.f(Object) —— 不是 Son.f(Son)
```

**为什么这样设计**：重载必须「可预测」，若按运行期类型选重载，编译期就无法确定调用点；重写必须「可扩展」，它正是多态的实现基础，只能延迟到运行期。一个在编译期解决、一个在运行期解决，各取所需。

**面试官可能继续追问**：

- **invokevirtual 和 invokestatic 的区别？** —— `invokevirtual` 走虚方法表按实际类型动态查找；`invokestatic`/`invokespecial` 在编译期确定目标，静态方法、私有方法、构造器调用用后两者。JDK 7 起 `invokedynamic` 支持动态分派，Lambda 的引导（`LambdaMetafactory`）就是典型应用。
- **为什么静态方法不能重写？** —— 静态方法属于类而不属于实例，调用在编译期就绑定到声明类型；子类写同名签名方法叫「隐藏」（hiding），通过父类引用调用永远执行父类版本。
- **重载能只靠返回值区分吗？** —— 不能。仅返回类型不同的两个方法签名相同，编译器无法从调用点判断该用哪个，直接编译错误。

**实际工程场景**：框架里大量利用重写实现扩展点（模板方法、Servlet 的 `doGet`）；重载要克制——签名相近的一组重载极易让调用方踩错，能用 Builder 或不同方法名就别堆重载。自动装箱引入后，重载解析在「精确匹配、宽化、装箱」之间的优先顺序是事故高发区。

**容易答错的地方**：以为「运行期实参是子类就会调用更精确的重载」——重载是纯编译期行为；以为子类可以降低父类方法的访问权限还能重写——非法（父类 `public`、子类 `protected` 编译不过）；忘了 static 方法是隐藏不是重写。

> 权威内容：[方法重载与调用解析](../../01-Java核心/01-面试速记/方法重载与调用解析.md) · [多态与动态绑定](../../01-Java核心/02-深度解析/多态与动态绑定.md)

### Q07：包装类的缓存机制会在什么时候让 `==` 骗你？ `P1` `中频`

**30 秒回答**：`Byte`、`Short`、`Integer`、`Long`、`Character` 在 `[-128, 127]` 内复用同一批缓存实例，所以落在这个区间的两个装箱值 `==` 为 true，超出范围就是两个新对象、`==` 为 false。

**深入理解**：区间上限由 `IntegerCache` 决定，可用 `-XX:AutoBoxCacheMax` 调高（只影响 `Integer`）。`Float`、`Double` **没有**缓存，任何时候 `==` 都比引用。触发点在自动装箱——`Integer a = 127; Integer b = 127;` 走 `valueOf` 命中缓存，`a == b` 为 true；换成 128 就为 false。规范做法是包装类型之间的值比较一律用 `equals`，或先转成基本类型再比。

```java
Integer a = 128, b = 128;
a == b;        // false —— 超出缓存，两个新实例
a.equals(b);   // true
Integer c = 128;
int d = 128;
c == d;        // true  —— 包装与基本类型比较会自动拆箱
```

**为什么这样设计**：缓存是为了省掉小整数装箱的开销，而实践中绝大多数计数、状态码、索引都落在小整数区间。代价是引入了「同一个值可能有多个实例」的语义分裂，而 `==` 检测的恰好就是实例身份，于是在区间边界上出现了不连续行为——这是 `==` 在本语言里最容易被误用的地方。

**面试官可能继续追问**：

- **`Integer i = 10; i++` 会发生什么？** —— 自动拆箱成 `int`、自增、再自动装箱，三次转换，运算后 `i` 指向新对象。`Integer` 参与任何算术都会触发拆箱。
- **valueOf 和 new Integer 的区别？** —— `valueOf` 走缓存（`-128`～`127` 命中共享实例），`new` 永远创建新对象；JDK 9 起 `new Integer` 已废弃，统一用 `valueOf` 或自动装箱。
- **`Map<Integer, X>` 用 int 取值有问题吗？** —— `get` 时 int 自动装箱成 `Integer`，`HashMap` 用 `equals` 而非 `==` 定位，所以取值本身没问题；真正的坑是业务代码自己用 `==` 比较两个包装对象。

**实际工程场景**：金额、ID、状态码全部用基本类型或 `BigDecimal`；包装类型只出现在泛型容器、可空字段、数据库实体字段。`Boolean.TRUE/FALSE`、`Character` 的小缓存偶尔出现在位标志代码里，读代码时能识别即可。

**容易答错的地方**：以为所有包装类都有缓存——`Float`/`Double` 没有；以为缓存上限写死 127——`-XX:AutoBoxCacheMax` 可调；混淆「拆箱后比较」与「装箱值 `==`」——`c == d`（一边是基本类型）会拆箱所以可能为 true，别用它推断「128 的 `==` 为 true」。

> 权威内容：[类型与转换陷阱](../../01-Java核心/01-面试速记/类型与转换陷阱.md) · [类型转换与数值精度](../../01-Java核心/02-深度解析/类型转换与数值精度.md)

### Q08：接口和抽象类怎么选？ `P1` `中频`

**30 秒回答**：抽象类表达「是什么」（单继承、可有实例状态和构造器），接口表达「能做什么」（可多实现、不能有实例状态）——先判断需求是「骨架复用」还是「能力契约」。

**深入理解**：抽象类可以有构造器、实例字段和 `protected` 成员，适合沉淀子类共用的状态与模板流程；接口只能有常量、抽象方法和默认方法，**不能有实例字段**，适合定义跨继承体系的能力（`Comparable`、`Runnable`）。Java 8 起接口能写默认方法，削弱了「只有抽象类才能复用代码」这个理由，但接口依然不能持有实例状态——这是最本质的分界。判据：需要共享可变状态或统一初始化流程 → 抽象类；需要让不相关的类具备同一能力 → 接口。常见做法是并存，如 `List` 与 `AbstractList`。

**为什么这样设计**：Java 放弃了 C++ 的多继承，因为菱形继承会带来状态合并歧义。折中方案把「状态继承」限定为单路（抽象类）、把「契约继承」放开为多路（接口）——接口无实例状态，多实现就不会产生状态冲突。

**面试官可能继续追问**：

- **两个接口的默认方法冲突了怎么办？** —— 必须显式重写，并可用 `InterfaceA.super.m()` 指定调用哪一个；类优先于接口——父类的具体实现优先于所有接口默认方法。
- **Java 8 之后抽象类还有什么存在意义？** —— 有：实例状态、构造器、`protected` 模板、非 public 成员。模板方法模式（`AbstractList`、`AbstractXxxDao`）依然是抽象类的典型用法。
- **为什么接口字段必须是 public static final？** —— 接口没有实例，字段只能挂在类型上（static），且多实现下可变常量必然冲突，所以 final。这是「接口不能有实例状态」的推论。

**实际工程场景**：新代码优先接口 + 默认方法（演进友好，加方法不破坏实现方）；领域骨架用抽象类（封装事务与日志模板）。微服务里接口还承担 RPC 契约（Feign/OpenAPI），实现类可替换、可 mock——这是「面向接口编程」在现代工程里的实际收益。

**容易答错的地方**：说「Java 8 后抽象类没用了」——实例状态和构造器它依然独占；说接口完全不能有方法实现——默认方法和静态方法都可以；分不清「implements 多个接口」与「extends 一个类」的编译期限制来源。

> 权威内容：[接口](../../01-Java核心/02-深度解析/接口.md) · [抽象类](../../01-Java核心/02-深度解析/抽象类.md)

### Q09：`==` 和 equals 的区别？ `P0` `高频`

完整标准答案已独立成页：[Q09 · == 和 equals 的区别？](Java基础/Q09-等号与equals.md)

### Q10：String、StringBuilder、StringBuffer 的区别？ `P0` `高频`

**30 秒回答**：`String` 不可变，每次「修改」都产生新对象；`StringBuilder` 可变、非线程安全，是单线程拼接的唯一选择；`StringBuffer` 可变且方法都加了 `synchronized`，只在多线程共享同一个缓冲时才用——现代代码更推荐每个线程一个 `StringBuilder` 或用 `String.join`。

**深入理解**：

| 维度 | String | StringBuilder | StringBuffer |
| --- | --- | --- | --- |
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 是（不可变） | 否 | 是（synchronized） |
| 底层 | `byte[]`（JDK 9+） | `AbstractStringBuilder` | `AbstractStringBuilder` |
| 单线程拼接性能 | 最差 | 最好 | 差（同步开销） |

编译器对 `+` 的优化仅限单条语句内：`String s = "a" + x + "b";` 会被编译成 `StringBuilder` 链（JDK 5+；JDK 9 起部分场景改用 `invokedynamic` + `StringConcatFactory` 生成更优策略）。但循环里 `s += x` 每轮仍新建 `StringBuilder`、`toString` 再拼回 `String`，是 O(n²) 的经典反模式。JDK 8 起 `String.join`/`StringJoiner`、JDK 15 起文本块（Text Blocks）覆盖了大多数拼接场景。

```java
// 反模式：循环内 +=
String s = "";
for (String part : parts) s += part;   // 每轮：new StringBuilder + append + toString

// 正确做法
String s = String.join(",", parts);
StringBuilder sb = new StringBuilder(parts.size() * 16);   // 预估容量，避免多次扩容
for (String part : parts) sb.append(part);
```

**为什么这样设计**：`String` 的不可变是共享与安全的前提（Q03）；`StringBuilder` 把「修改」变成数组原地 append，省掉中间对象；`StringBuffer` 的 `synchronized` 是 JDK 1.0 时代的并发答案，代价是所有调用点无竞争也付同步费——后来者（线程本地 `StringBuilder`、`String.join`）用「不共享」替代「共享加锁」，是更好的并发默认值。

**面试官可能继续追问**：

- **StringBuilder 和 StringBuffer 底层有区别吗？** —— 都继承 `AbstractStringBuilder`、都是数组扩容（JDK 9 起 `byte[]`），唯一区别是 `StringBuffer` 每个 public 方法都加 `synchronized`。且它的「线程安全」只到单次调用原子——`sb.append(x).append(y)` 这种组合操作仍要外部同步。
- **默认容量多少，怎么扩容？** —— 无参构造默认 16 个字符，超容量时扩容到约 `2 * old + 2`，旧数组整体拷贝——预先估算容量能避免多次扩容拷贝。
- **编译期 + 号优化有什么边界？** —— 只优化单条表达式语句；字面量之间直接折叠为常量进常量池；循环、跨语句累加都不优化。`final` 变量拼接会被当常量折叠（JLS 的常量表达式规则）。

**实际工程场景**：SQL 动态条件、JSON 片段、日志拼装在方法内用 `StringBuilder`（预估容量）；集合拼接用 `String.join` 或 `Collectors.joining`；可读性优先时用 `String.format`；共享给多线程的缓冲才考虑 `StringBuffer`，但更好的设计是每个线程构建自己的串。

**容易答错的地方**：说「有 + 号编译器都会优化」——循环里不会；说「StringBuffer 线程安全所以并发拼接首选」——它只保证单次调用原子，组合操作仍会串数据，且无竞争也付同步费；认为 `String.replace` 是原地修改——返回新串。

> 权威内容：[对象创建与不可变设计](../../01-Java核心/02-深度解析/对象创建与不可变设计.md) · [参数传递与不可变对象](../../01-Java核心/01-面试速记/参数传递与不可变对象.md)

### Q11：泛型通配符 `? extends` 和 `? super`，PECS 原则？ `P1` `中频`

**30 秒回答**：`? extends T` 是上界通配符（某种 T 的子类型），适合「只读」的生产者——能取出来当 T 用，不能往里加；`? super T` 是下界通配符（某种 T 的父类型），适合「只写」的消费者——能往里放 T，取出来只能是 `Object`。PECS：Producer Extends，Consumer Super。

**深入理解**：

```java
// 生产者用 extends：从 src 读出元素，只读
public static <T> void copy(List<? extends T> src, List<? super T> dst) {
    for (T t : src) dst.add(t);   // JDK 的 Collections.copy 正是这个签名
}

List<Integer> ints = List.of(1, 2, 3);
List<Number> nums = new ArrayList<>();
copy(ints, nums);   // Integer extends Number：src 可读；Number super Integer：dst 可写
```

`List<?>` 是只读视图：除 `add(null)` 外什么都加不了（编译器不知道元素类型），`get` 返回 `Object`。这与数组协变形成对比：`Integer[]` 是 `Number[]` 的子类型，装错类型要等运行期才抛 `ArrayStoreException`；泛型不变性把这类错误提前到了编译期。

**为什么这样设计**：数组协变是 JDK 1.0 为了实用（`Arrays.sort(Object[])` 复用）留下的历史妥协；泛型设计时选择了「不变 + 通配符按需放宽」——每个使用点单独声明能接受的范围，用编译期检查换掉运行期 `ArrayStoreException`。代价是签名变长、PECS 需要记。

**面试官可能继续追问**：

- **`List<? extends T>` 为什么 add 不了？** —— 编译器只知道元素是 T 的某个未知子类型，传进来的对象可能不是那个子类型（如实际是 `List<Integer>`，你 `add(Double)` 就破坏类型）。`add(null)` 合法因为 null 是所有引用类型的成员。
- **`? super T` 的 get 返回什么？** —— 只能保证是 `Object`（下界只知道父类型，不知道具体是哪个）。要 T 就用 `? extends T` 或直接用 `List<T>`。
- **`Class<?>` 和 `Class<T>` 怎么选？** —— 只做类型判断、读元数据用 `Class<?>`；要创建实例、转换对象用 `Class<T>`，配合 `getDeclaredConstructor().newInstance()`。

**实际工程场景**：写工具方法时按读写方向选通配符——「读集合、写目标」的 copy/merge 方法是 PECS 的经典场景；`Comparable`/`Comparator` 用 super（`Collections.sort` 的签名 `List<T>` + `Comparator<? super T>`），让比较器能作用于子类；API 参数用通配符放宽调用方，返回值用精确类型保护内部。

**容易答错的地方**：以为 `List<Object>` 能接收 `List<String>`——泛型不变，必须 `List<?>` 或 `List<? extends Object>`；把 extends/super 的读写方向记反——记「取出来用 extends，放进去用 super」；以为通配符能写在类型声明处（`class Box<?>` 非法，通配符只能出现在使用点）。

> 权威内容：[Java 泛型与类型安全](../../01-Java核心/02-深度解析/Java泛型与类型安全.md)

### Q12：Java 异常体系是怎样的？ `P0` `高频`

**30 秒回答**：`Throwable` 分 `Error` 和 `Exception`；`Exception` 分受检异常（编译期强制处理，如 `IOException`）和非受检异常 `RuntimeException`（运行期抛出，如 NPE、越界）。工程分界是：受检异常表达「可预期的外部失败」，运行时异常表达「程序错误」。

**深入理解**：

```text
Throwable
├── Error              —— JVM 级故障，不应捕获（OOM、StackOverflowError）
└── Exception
    ├── 受检（checked）    —— IOException、SQLException、ClassNotFoundException
    └── 非受检（RuntimeException） —— NPE、越界、IllegalArgument、ClassCast
```

`finally` 几乎总是执行，三个例外：try 块里调 `System.exit()` 直接终止 JVM；所在线程被强制终止（`Thread.stop` 已废弃、守护线程随 JVM 退出被杀）；JVM 直接崩溃。另一个隐蔽坑：`finally` 里自己 `return`/`throw` 会覆盖 try 的返回值或异常——这是 bug 高发点。try-with-resources（JDK 7+）按声明逆序自动 `close`，且 close 抛出的异常被「抑制」（`addSuppressed`）保留，比 try-finally 更可靠；JDK 9 起支持在 try 前声明已 effectively-final 的资源变量。

```java
try (Connection c = dataSource.getConnection();   // 逆序关闭，异常不吞
     PreparedStatement ps = c.prepareStatement(sql)) {
    // ...
}   // c、ps 自动 close；close 抛的异常被 suppressed 附加到主异常
```

**为什么这样设计**：受检异常强制调用方显式处理「必然可能发生的外部失败」（文件不存在、网络断开），把错误路径写进类型系统；运行时异常不强制，因为对编程错误（空指针、下标越界）加强制处理只会逼出 catch 后什么都不做的噪声代码。代价是大量「吞异常」文化——现代实践是：受检异常在边界转换、运行时异常快速失败。

**面试官可能继续追问**：

- **finally 里 return 会发生什么？** —— 覆盖 try 里的 return：try 返回 1、finally 返回 2，方法返回 2；try 抛异常、finally 里 return，异常直接被吞。永远不要在 finally 里 return。
- **Error 能 catch 吗？** —— 语法上能（都是 `Throwable`），但工程上不应捕获 OOM/StackOverflow——此时 JVM 状态已不可信，正确做法是记录现场后终止。`catch (Throwable)` 只适合放在最外层兜底日志。
- **try-with-resources 的异常怎么处理？** —— close 抛出的异常通过 `addSuppressed` 挂到 try 的主异常上，`getSuppressed()` 能拿完整链路，不会像 try-finally 那样被 close 异常覆盖主异常。

**实际工程场景**：分层架构里，DAO 的 `SQLException` 在 Service 边界转换成统一业务异常（错误码体系），Controller 用全局异常处理器统一响应；禁止裸 `catch (Exception e)` 后只打日志不处理——要么恢复、要么转换、要么向上抛；线程池异步任务要把异常写进 `Future`，否则默认处理器只能打印到 stderr。

**容易答错的地方**：认为 finally 无条件执行（`System.exit`、线程被强制终止、JVM 崩溃都是反例）；`catch (Throwable)` 当通用兜底——会连 `Error` 一起吞；catch 块重新抛新异常不带 cause——丢失原始堆栈，排障时最致命；finally 里关流再抛一次异常会覆盖主异常——这正是 try-with-resources 要解决的问题。

> 权威内容：Java 异常体系与 try-with-resources 当前尚未在仓库建设，可参考 The Java Tutorials 的 Exceptions 章节与《Java 核心技术》（卷 I）异常处理部分。

### Q13：反射机制的原理和问题？ `P0` `高频`

**30 秒回答**：反射是在运行期读取类的元数据、调用方法/访问字段的机制，是 Spring、JUnit、Jackson 等框架的基石。核心代价是性能开销（相对直接调用高，但要说清量级）和封装破坏（可访问 private 成员）；JDK 9 模块化后深反射还受强封装限制。

**深入理解**：获取 `Class` 三种方式：字面量 `Foo.class`（编译期确定）、`obj.getClass()`（运行期）、`Class.forName("Foo")`（触发类初始化，可传 ClassLoader 控制）。实例化用 `getDeclaredConstructor().newInstance()`——JDK 9 起 `Class.newInstance()` 已废弃，因为它对构造器抛出的受检异常包装行为不一致、且访问检查难以预期。

```java
Class<?> c = Class.forName("com.example.Order");           // 触发类初始化
Constructor<?> ctor = c.getDeclaredConstructor(Long.class);
Order o = (Order) ctor.newInstance(1L);
Field f = c.getDeclaredField("price");
f.setAccessible(true);                                     // 深反射：JDK 9+ 受模块限制
f.set(o, BigDecimal.TEN);
```

性能开销的量级（随 JDK 版本和 JIT 状态浮动，别背死数字）：一次性冷调用比直接调用慢 1～2 个数量级；JDK 7 引入 `InflationThreshold`（默认 15 次）后，热点反射调用切换为生成字节码接入器，稳定态差距收敛到约一倍到几倍。剩余固定开销在三处：`setAccessible` 的访问检查、调用路径无法像虚方法那样内联、参数装箱与返回值拆箱。说「反射一定慢十倍」不准确。

JDK 9 模块化（JPMS）后，非导出包的 private 成员深反射抛 `InaccessibleObjectException`，需要 `--add-opens java.base/java.lang=ALL-UNNAMED` 之类参数开放；这也是 JDK 16 起强封装默认生效后，老框架（早期 Spring、部分字节码工具）启动报错、被迫升级的根因。反射破坏单例的典型手法：拿私有构造器 `newInstance` 造第二个实例，或反射改静态字段——防御靠枚举单例（JVM 保证）或在私有构造器里抛异常。

**为什么这样设计**：反射打破「编译期才知道类型」的假设，让框架能在运行期组合未知的用户代码——这是 IoC、依赖注入、插件化成立的前提。代价是：类型安全从编译期退到运行期（`ClassCastException` 晚来）、封装被合法穿透、调用难以被 JIT 充分优化。框架选择反射是「通用性优先」，业务代码用反射通常是设计信号。

**面试官可能继续追问**：

- **反射为什么慢？慢在哪？** —— 三块：访问检查（`setAccessible` 前的权限校验）、调用走 JNI/委托而非虚方法表内联、参数 `Object` 装箱与返回值拆箱。JIT 预热后的稳定调用只慢一倍左右；热路径另有 `MethodHandle`（JDK 7）和字节码生成（ASM）可选。
- **反射和 MethodHandle 怎么选？** —— 框架内部热路径（序列化、代理）优先 `MethodHandle` 或代码生成；反射用于低频、一次性的元数据操作。`LambdaMetafactory` 生成的实现内部也走 `MethodHandle`。
- **JDK 9 后 setAccessible(true) 失效怎么办？** —— 库方在 `module-info` 里 `opens` 目标包；运行方加 `--add-opens`；框架方（Spring 5.2+、Jackson）已内置兼容。这是 JDK 17/21 升级时的常见启动报错。

**实际工程场景**：反射只应出现在框架边界（Spring 的 Bean 扫描与注入、MyBatis 的接口代理、Jackson 的字段映射）；业务代码里见到 `Class.forName + newInstance`，先问「为什么不用接口或 SPI」；性能敏感的热路径用 `MethodHandle`、字节码生成或显式注册表替代。

**容易答错的地方**：说「反射一定慢几倍」——要区分一次性调用（慢 1～2 个数量级）和 JIT 预热后的稳定调用（约一倍到几倍）；答案停留在 JDK 8、忽略模块化对深反射的限制；以为反射能改运行期常量——被 JIT 内联的常量读取改不动，且会破坏 JVM 假设；分不清 `Class.forName`（触发初始化）和 `ClassLoader.loadClass`（不触发初始化）。

> 权威内容：反射与模块化访问控制当前尚未在仓库建设，可参考官方文档 java.lang.reflect 包说明、JEP 261 与 JEP 403（JDK 强封装）。

### Q14：注解的原理？ `P1` `中频`

**30 秒回答**：注解是承载元数据的特殊接口，编译后作为字节码属性保留。运行期注解靠 `@Retention(RUNTIME)` 保留 + 反射读取；编译期注解靠 APT（JSR 269）注解处理器在编译阶段生成代码或做校验——Lombok 就是用 JSR 269 修改编译器 AST 实现的。

**深入理解**：三个元注解决定行为：

- `@Retention`：`SOURCE`（编译期丢弃，如 `@Override`）、`CLASS`（进字节码，运行期不可见，默认值）、`RUNTIME`（运行期可反射读取，如 Spring 的 `@Autowired`）。
- `@Target`：注解能贴的位置（类型、字段、方法、参数等）。
- `@Documented` / `@Inherited`：是否进 Javadoc；类注解是否被继承。

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column { String value(); }   // 注解本质是继承 Annotation 的接口，元素即抽象方法

Field f = Order.class.getDeclaredField("price");
Column col = f.getAnnotation(Column.class);     // 运行期读取
String name = col.value();                      // 实际调用的是 JDK 动态代理生成的对象
```

运行期读取时，注解实例是 JDK 动态代理生成的实现类对象（`AnnotationInvocationHandler`），`getAnnotation` 的解析来源是 class 文件里的 Annotation 属性。APT 则在 javac 编译时扫描注解、生成新的源文件/字节码（Dagger、MapStruct）；Lombok 更进一步，直接修改编译器的 AST（语法树）来「改」你的类——所以源码里并不存在生成的 getter/setter，IDE 必须装插件才能识别。

**为什么这样设计**：注解把「元数据从 XML 配置和外置约定搬回代码旁边」，消灭重复描述；RUNTIME 保留让框架能在启动期统一扫描（Spring 的条件装配、JPA 的实体映射）。代价是「魔法」：一个 `@Transactional` 或 `@Data` 展开的行为远大于一行代码，黑盒化增加调试难度——这是注解被批评「过度设计」的由来。

**面试官可能继续追问**：

- **@Retention 三档分别决定什么？** —— `SOURCE` 编译后消失（只影响编译器，如 `@Override`、Lombok）；`CLASS` 是默认，留在 class 文件但 JVM 不加载为注解对象；`RUNTIME` 运行时反射可见。框架契约注解必须是 RUNTIME。
- **注解里的方法（元素）有什么限制？** —— 无参、返回值只能是基本类型/String/枚举/注解/Class 及它们的一维数组；可以有 default；不能 throws。`value` 是约定俗成的默认元素名，可省名传值 `@Column("price")`。
- **Lombok 为什么 IDE 要装插件？** —— 它修改的是编译期 AST，源码文件里并不存在生成的成员；IDE 的代码分析基于源码，插件重新实现了同样的 AST 变换才能补全和检查。

**实际工程场景**：业务里「自定义注解 + AOP」是横切关注点（权限校验、日志、幂等）的标准组合；注意注解只携带静态元数据，动态值要配 SpEL 或运行期解析。框架选型时警惕「注解驱动一切」的库——配置分散在代码里，排查时要跳多个文件才能还原真实行为。

**容易答错的地方**：以为注解「编译后一定存在」——`SOURCE` 档编译后即消失；以为注解能主动执行逻辑——它只是元数据，执行逻辑的是读它的框架（AOP/APT）；混淆注解继承——只有类注解加 `@Inherited` 才继承，方法/字段上的不继承；以为 `getAnnotation` 返回的是自己 new 的实例——是代理对象。

> 权威内容：注解与 APT 当前尚未在仓库建设，可参考 JSR 269（Pluggable Annotation Processing API）与 The Java Tutorials 的 Annotations 章节。

### Q15：SPI 机制的原理？ `P1` `中频`

**30 秒回答**：SPI（Service Provider Interface）是「接口与实现分离」的插件机制：接口方在 `META-INF/services/` 下放置以接口全限定名命名的文件，内容写实现类全限定名，`ServiceLoader` 在运行期读取并实例化所有实现——JDBC 的 `DriverManager` 就是这么发现驱动的。

**深入理解**：

```text
调用方只依赖接口
   ↓
classpath 下 META-INF/services/{接口全限定名}
   ↓ 内容：实现类全限定名（每行一个）
ServiceLoader.load(Driver.class) 遍历、类加载、实例化
   ↓
迭代器逐个返回实现实例（懒加载，next() 时才创建）
```

经典案例：JDBC 4.0+ 的驱动只要在 classpath 上、`META-INF/services/java.sql.Driver` 文件齐全，`DriverManager.getConnection` 就会自动发现——不再需要 `Class.forName("com.mysql.jdbc.Driver")`。Spring 的 `spring.factories` 是同一思想的 key-value 工厂版；Spring Boot 2.7 起改为 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，把自动配置的注册统一到 ServiceLoader 风格的「一接口一实现一行」。

SPI 与双亲委派的关系：`ServiceLoader` 用线程上下文类加载器（TCCL）加载实现类——接口由启动类加载器加载、实现由应用类加载器加载，标准的双亲委派下父加载器看不到子的类，TCCL 让父方代码能「借」子方的加载器反向找到实现，是打破单向委派的规范出口。Tomcat 的 war 类隔离也正是这一手法。

**为什么这样设计**：JDK 核心不可能依赖任何具体驱动，只能定义接口 + 一套「运行期发现实现」的协议；这同样解释了框架的插件化（SLF4J Provider、JNDI）。代价是：注册是隐式的（加了 jar 包行为就变，调试时「明明没引用却生效」）、一个接口多个实现时选择策略要自己写、启动期全量扫描有性能成本。

**面试官可能继续追问**：

- **ServiceLoader 和 Class.forName 的区别？** —— ServiceLoader 按约定文件批量发现实现，调用方零硬编码；`forName` 是显式按类名加载单个类。前者解耦调用方与实现，后者把实现类名写死。
- **为什么用线程上下文类加载器？** —— SPI 接口由启动类加载器加载、实现由应用类加载器加载，父加载器按委派规则看不到子的类；TCCL 是规范给出的反向查找出口。
- **Spring Boot 2.7 的 imports 文件解决了什么？** —— `spring.factories` 把所有扩展点塞在一个文件里，自动配置与其他扩展混在一起、加载顺序难控；imports 按接口一文件一路由，与 ServiceLoader 的注册形式对齐，一个自动配置类一行，条件求值也更可控。

**实际工程场景**：自研框架/中间件对外提供扩展点时用 SPI（自定义存储引擎、规则引擎实现）；注意实现类必须有无参构造器（`ServiceLoader` 反射实例化）。一个接口有多个实现且需要选择时，SPI 只负责「全部列出」，选择逻辑（按配置 key 匹配）要自己做——Dubbo 的 `ExtensionLoader` 就是在 SPI 上加命名、自适应和包装链。

**容易答错的地方**：以为 SPI 是 Java 独有概念——它是一种插件式发现的设计模式，各语言/框架都有实现；以为 `ServiceLoader` 会按优先级排序——它只按文件行顺序迭代，没有内建优先级；文件内容写错（多余空格、写成接口名）会静默失败或抛 `ServiceConfigurationError`——排障时第一个查 `META-INF/services` 路径。

> 权威内容：SPI 与 ServiceLoader 当前尚未在仓库建设，可参考官方文档 java.util.ServiceLoader 包说明与 JDBC 驱动加载规范。

### Q16：Java 8 及后续的重要语言特性？ `P0` `高频`

**30 秒回答**：Java 8 是分水岭：Lambda/Stream 把函数式数据处理带进主流，`Optional` 把「可能为空」写进类型系统，接口默认方法解决了接口演进。之后每个 LTS 都在补表达力：`var`（10）、switch 表达式（14）、文本块（15）、Records（16）、instanceof 模式匹配（16）、密封类（17）。

**深入理解**：每个特性对应一个真实痛点：

| 特性 | 版本 | 解决的痛点 |
| --- | --- | --- |
| Lambda / Stream | 8 | 匿名内部类噪声；集合数据管道化，声明式过滤/映射/聚合 |
| Optional | 8 | NPE 无处标注；让「返回值可能为空」显式化（别滥用为字段/参数） |
| 接口默认/静态方法 | 8 | 接口加方法就破坏所有实现方；`Collection.stream()` 靠它落脚 |
| var | 10 | 类型名噪声（尤其是泛型右侧）；只能用于局部变量 |
| switch 表达式 | 14 | 老 switch 贯穿 break 易漏、不能返回值；`->` 无贯穿、可作表达式 |
| 文本块 | 15 | 多行字符串（SQL/JSON）的转义地狱；`\` 续行与 `\s` 控制缩进 |
| Records | 16 | 纯数据载体要手写构造器/getter/equals/hashCode/toString |
| instanceof 模式匹配 | 16 | 「instanceof + 强转」两行样板；判断与绑定一步完成 |
| 密封类 | 17 | 继承被无限打开；`sealed` + `permits` 限定子类范围，配合模式匹配做穷尽检查 |

Records 的深意是「不可变数据载体」成为语言级概念——final 字段、全参构造、按组件生成的 `equals`/`hashCode`/`toString`，紧凑构造器可做校验。密封类则让编译器能验证 `switch`/`instanceof` 是否覆盖所有允许的子类型——两者一起把「代数数据类型」的工程价值带进了 Java。

**为什么这样设计**：Java 长期被诟病「样板代码多、表达力弱」，这些特性都在还债：不是引入新范式，而是把其他语言验证过的「减样板」能力逐个补齐，且全部保持兼容（默认方法、`var` 只是语法糖、Records 就是 final 类加语法）。每条都经过 JEP 公开流程，落地节奏保守——所以企业升级通常按 LTS（8 → 17 → 21）走，而不是追每个特性。

**面试官可能继续追问**：

- **Stream 和 for 循环怎么选？** —— 声明式数据管道（过滤+映射+聚合、并行）用 Stream；含 break/continue、索引依赖、复杂副作用用 for。Stream 不是「更先进的 for」，可读性与并行收益要匹配场景。
- **Records 能继承吗？能改字段吗？** —— 都不能。Record 隐式 final、不能 extends 别的类（可以实现接口），字段 final——它是纯数据载体，不是「简化版 JavaBean」；需要可变 JavaBean 就用普通类。
- **Optional 能作为字段或方法参数吗？** —— 都不推荐。它是返回值级别的「可能没有」声明；序列化场景不支持（不实现 `Serializable`），作为字段既费内存又没省掉 null 检查。

**实际工程场景**：升级策略按 LTS：JDK 8 的项目评估 17（Records + 密封类对 DTO 和状态机代码减重明显，但要评估反射/字节码框架兼容性——ASM、CGLIB、老 Spring 是坑）；新服务直接用 21。文本块在 SQL/JSON 模板里收益最大；`var` 只在「类型名比变量名还长」时用，滥用会降低可读性。

**容易答错的地方**：把 Records 当 JavaBean 用——没有 setter，不可变；`var` 不是动态类型，编译期类型仍确定，只是省略书写；以为密封类是「final 增强」——它是「有限开放」，允许受控的继承集合；以为默认方法是万能钥匙——它不能有实例状态，也解决不了需要访问 `this` 字段的场景。

> 权威内容：[Java 运行机制与版本边界](../../01-Java核心/01-面试速记/Java运行机制与版本边界.md)

### Q17：Stream 的常见陷阱？ `P1` `中频`

**30 秒回答**：流只能消费一次（再操作抛 `IllegalStateException`）；`parallelStream` 默认共用 `ForkJoinPool.commonPool`，阻塞操作会拖垮整个池；`Collectors.toMap` 的 value 为 null 会抛 NPE，要显式给 mergeFunction；`peek` 是调试工具不是业务操作；大量数据还要当心装箱开销。

**深入理解**：

```java
Stream<String> s = list.stream();
s.forEach(System.out::println);
s.count();   // IllegalStateException: stream has already been operated upon

// toMap 的 value 为 null：默认合并函数抛 NPE
Map<Long, String> m = orders.stream()
    .collect(Collectors.toMap(Order::getId, Order::getName));          // name 为 null 时炸
Map<Long, String> ok = orders.stream()
    .collect(Collectors.toMap(Order::getId, Order::getName, (a, b) -> a));  // 显式合并
```

并行流的陷阱集中在「共享资源」：`commonPool` 的并行度是 CPU 核数 - 1，IO 等待型任务放进并行流，几个慢请求就占满所有线程；且自定义的 `ForkJoinPool` 不被 `parallelStream` 使用——要隔离只能把任务提交到自己的池里再跑 stream。另一个隐性成本是装箱：`Stream<Integer>` 上做 sum 之类的聚合会大量拆装箱，`mapToInt`/`mapToLong` 到 `IntStream`/`LongStream` 特化流能避开。至于「流慢」的错觉，很多时候慢在底层集合——如 `IntStream.range(0, list.size()).map(list::get)` 在 `LinkedList` 上是 O(n²)，慢在索引访问不在流。分工上：一次性转换用 stream，需要回退/中断续作用迭代器。

**为什么这样设计**：Stream 是「惰性管道 + 短路求值」的函数式抽象，一次消费是管道有状态的自然结果（中间操作挂在下游上，消费后失效）；`commonPool` 作为 JVM 级公共资源，默认并行省去用户建池，代价是隔离性差——这是「默认便利 vs 可预测性」的经典取舍。

**面试官可能继续追问**：

- **什么是短路操作？** —— `findFirst`/`anyMatch`/`allMatch` 等遇到结果就停止求值，配合 `limit` 能让「无限流 + 短路」成立（`Stream.generate(...).limit(n)`），与 for 循环提前 break 等价。
- **map 和 flatMap 的区别？** —— map 一对一转换；flatMap 把每个元素变成流再拍平（一对多），如把「订单列表的订单项」拍平成订单项流。嵌套集合展平只用 flatMap。
- **并行流什么时候更快？** —— 数据量大（万级以上）、计算密集、源是数组/ArrayList（易拆分）、无共享可变状态、无阻塞。五个条件缺一就可能更慢——实际项目里先测量再并行。

**实际工程场景**：报表聚合、DTO 映射（stream + map）是舒适区；生产代码用 `parallelStream` 前必须做三件事：确认计算密集无阻塞、确认无共享可变状态、压测对比——大部分「并行加速」的预期会被数据量小和装箱吞掉。要并行时更可控的做法是 `CompletableFuture` + 自定义线程池，失败隔离和可观测性都更好。

**容易答错的地方**：把同一个流 forEach 之后再 count——运行期直接报错；在 map 里改外部变量（副作用）——并行流下数据竞争，串行流下只是坏味道；以为 `parallelStream` 用了业务线程池——它用 `commonPool`，和 Tomcat 线程池完全无关；以为流操作一定比 for 快——小数据量、含副作用的场景流更慢。

> 权威内容：Stream API 当前尚未在仓库建设，可参考官方文档 java.util.stream 包说明与 Oracle 的 Parallelism 教程。

### Q18：深拷贝和浅拷贝？ `P1` `中频`

**30 秒回答**：浅拷贝复制字段本身，引用字段仍指向同一对象；深拷贝对引用字段递归拷贝，拷贝后两份完全独立。`Object.clone` 默认就是浅拷贝；数组 `clone` 是一维浅拷贝（元素不拷）。

**深入理解**：

```java
public class Address { public String city; }
public class User implements Cloneable {
    public Address addr;
    @Override public User clone() throws CloneNotSupportedException {
        return (User) super.clone();   // 浅：addr 被两个 User 共享
    }
    // 深拷贝：clone 后递归处理引用字段
    // User c = (User) super.clone(); c.addr = addr.clone(); return c;
}
```

序列化深拷贝（写进 `ByteArrayOutputStream` 再读回）能处理任意对象图，但只适用于 `Serializable` 体系，且慢（约比 clone 慢 1～2 个数量级）、不拷贝 transient 字段、要求整个对象图可序列化——适合一次性深拷工具，不适合热路径。替代方案：拷贝构造器（`new User(other)`）、静态复制工厂（`User.copyOf(other)`）、with 方法（返回改了某个字段的新副本）——《Effective Java》明确建议优先拷贝构造器而非 `Cloneable`，因为 `clone` 绕过构造器、协变返回、受检异常三件事都很难用好。

不可变对象是「免拷贝」策略：字段全 final、无 setter、集合用 `List.copyOf` 包装——调用方拿到引用即可安全共享，这是防御性复制的最高性价比形态（Q03 的共享逻辑在对象图上的推广）。

**为什么这样设计**：`Cloneable` 是 JDK 1.0 的标记接口设计——`Object.clone` 原生分配内存并逐字段复制（比构造器快，且保留运行时类型），但它「标记 + protected + 受检异常」的组合几乎没有被后来的 API 借鉴，被公认为 Java 设计最差的接口之一。深拷贝困难是引用图的固有复杂度，语言没有、也不打算提供万能方案——所以工程上按「对象图深度 × 使用频率」选策略。

**面试官可能继续追问**：

- **数组 clone 是深拷贝吗？** —— 一维数组是「干净」的浅拷贝（元素按值复制，所以 `int[]` clone 完全独立）；多维数组只复制第一维，子数组仍共享。对象数组同理只复制引用。
- **不可变对象为什么不需要拷贝？** —— 没有任何修改入口（无 setter、集合只读视图），共享不存在「被改」风险——String 能进常量池正是这个逻辑。代价是「修改即新建」，高频改字段的场景（循环内更新）不适合。
- **防御性复制什么时候做？** —— 系统边界：构造器接收外部可变集合/数组、getter 返回内部可变状态、RPC/缓存读写。内部可信调用不复制——到处复制是把复杂度摊给所有调用方。

**实际工程场景**：DTO 跨层/跨服务传输用 MapStruct 等映射器（显式、可测试）；缓存快照用不可变值对象或深拷（防调用方改了缓存里的对象影响其他人）；实体转 VO 不要直接引用实体里的可变集合。持久层实体尤其注意：JPA/Hibernate 的实体有懒加载代理，`clone` 会破坏代理、丢失会话上下文。

**容易答错的地方**：以为 `clone` 会递归拷贝——默认浅拷，引用字段两个对象共享，改一个另一个跟着变；以为序列化深拷贝万能——transient 不拷、不可序列化字段直接炸、循环引用会抛异常；在 clone 里做「半深」拷贝漏掉某个引用字段——对象图越深，漏拷越难 review，这也是优先拷贝构造器的原因。

> 权威内容：[toString 与对象工具方法](../../01-Java核心/02-深度解析/toString与对象工具方法.md) · [参数传递机制](../../01-Java核心/02-深度解析/参数传递机制.md) · [参数传递与不可变对象](../../01-Java核心/01-面试速记/参数传递与不可变对象.md)

### Q19：final、finally、finalize 的区别？ `P0` `高频`

**30 秒回答**：`final` 是修饰符——类不可继承、方法不可重写、变量不可重新赋值（引用不可变但对象内容可变）；`finally` 是 try 的收尾块，几乎总是执行（`System.exit` 和 finally 里 return 是例外）；`finalize` 是 `Object` 的遗留方法，对象被 GC 前调用，因设计缺陷在 JDK 9 废弃、JDK 18 起标记待移除，新代码用 `Cleaner` 或虚引用替代。

**深入理解**：三个东西名字像、层面完全不同：`final` 是编译期/语言级约束，`finally` 是控制流结构，`finalize` 是运行期 GC 钩子。

`final` 变量分两种语义：编译期常量（`static final int MAX = 10`，字面量，会被内联进调用方字节码）和运行期常量（`final int x = compute()`，每次求值）。final 域的写在构造器结束前发生、读方无需同步即可见——这是 final 域安全发布的语义，也是「构造器里通过 final 字段安全发布对象」的依据。final 引用只禁止重新绑定：`final List l = new ArrayList(); l.add(1);` 合法。

`finally` 的坑集中在「覆盖」：try 里 `return x`、finally 里 `return y` → 返回 y；try 抛异常、finally 里 `return` → 异常被吞。finally 里也不要做可能再抛异常的 IO/关闭操作（会覆盖主异常），用 try-with-resources（见 Q12）。

`finalize` 的缺陷是系统性的：执行时机不确定（可能永不执行就 OOM）、每个实例只有一次、抛出的异常被忽略且不影响销毁、finalize 里重新赋值给静态字段能「复活」对象、每次执行都有额外 GC 成本。替代：`Cleaner`（JDK 9，内部基于虚引用 PhantomReference + 清理队列线程，显式注册清理动作）或显式 `close` + try-with-resources——对文件句柄、socket 这类真实资源，主路径必须是显式 close，Cleaner 只是兜底。

**为什么这样设计**：`final` 用编译期约束消灭一整类「意外被继承/改绑定」的 bug，代价小收益大；`finally` 保证清理路径，是「资源获取—初始化」模式的语法基础；`finalize` 诞生于「让 C++ 析构函数平移过来」的直觉，但 GC 管理的堆上「对象死亡」与「确定时刻清理」本质矛盾——它是 Java 早期设计里最著名的失败之一，官方文档直接写了「不要使用」。

**面试官可能继续追问**：

- **final 一定能提升性能吗？** —— 不一定直接提速，但给 JIT 更多确定性（常量折叠、内联判定、final 域读无需屏障）。final 的主要价值是可读性与安全语义，不是微优化。
- **finally 和 return 的执行顺序到底是什么？** —— try 的 return 表达式先求值、存局部变量，然后执行 finally，最后才真正 return——所以 finally 改不了已求值的返回值（除非 finally 自己 return 覆盖）。
- **Cleaner 比 finalize 好在哪？** —— 清理动作显式注册（虚引用入队后由线程执行），不复活对象、异常不吞、没有「对象复活」攻击面；代价是仍依赖 GC 触发，不能替代 try-with-resources 的确定性关闭。

**实际工程场景**：类设计上：不可变工具类、安全关键类标 `final`；常量、注入依赖用 final（配合构造器注入）；新代码不应再出现手写 finally close——一律 try-with-resources；持有本地资源（内存映射、直接缓冲区）的类用 `Cleaner` 兜底，但主路径必须显式 close。

**容易答错的地方**：以为 final 数组/集合不可变——`final int[] a = {1}` 之后 `a[0] = 2` 合法；以为 finally 无条件执行——`System.exit`、线程被强制终止、JVM 崩溃都不会执行；以为 finalize 像 C++ 析构函数「确定执行」——时机不确定且可能不执行；在 finally 里 return——被 SonarQube/IDEA 标红是有原因的。

> 权威内容：[final 与常量设计](../../01-Java核心/02-深度解析/final与常量设计.md) · [toString 与对象工具方法](../../01-Java核心/02-深度解析/toString与对象工具方法.md) · [流程控制](../../01-Java核心/02-深度解析/流程控制.md)

### Q20：序列化：serialVersionUID 和 transient？ `P1` `中频`

**30 秒回答**：`Serializable` 是标记接口，`ObjectOutputStream` 把对象图写成字节流、`ObjectInputStream` 读回；反序列化不走本类构造器（只调最近的非序列化父类的无参构造）。`serialVersionUID` 是版本号，显式声明才能控制兼容性；`transient` 标记不参与序列化的字段。

**深入理解**：不显式声明 `serialVersionUID` 时，它在运行期由类结构（类名、修饰符、接口、字段、方法）按规范算法计算——看似微小的结构变化（加个字段、改个方法签名）都可能改变它，反序列化旧数据直接抛 `InvalidClassException`。显式声明后，兼容与否由你自己掌控（配 `ObjectInputStream` 的读取侧一起演进）。

```java
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;   // 显式声明，结构小变化不断流
    private Long id;
    private transient String sessionToken;             // 不序列化，反序列化后为默认值 null
}
```

`transient` 字段反序列化后拿默认值（null/0/false），密码、token、连接句柄都应标 transient。`Externalizable` 是另一套协议：自己实现 `writeExternal`/`readExternal`，要求 public 无参构造器，适合精细控制格式。安全上，反序列化不可信输入是历史著名漏洞入口（Apache Commons Collections 的 gadget 链）——`readObject` 里必须校验不变量（私有 `void readObject(ObjectInputStream)` 做字段检查），JDK 9 起用 `ObjectInputFilter`/`jdk.serialFilter` 做类白名单过滤（JEP 290）。

替代方案的选型：跨语言、向前兼容用 Protobuf（RPC 首选）；人可读、Web API 用 JSON（Jackson/Gson）；Java 进程内高性能用 Kryo（注意它同样有反序列化安全问题，且不跨语言）。三者都避免「反序列化即执行任意代码」的 Java 原生序列化陷阱。

**为什么这样设计**：Java 原生序列化诞生于 RMI 时代，目标是「透明地跨 JVM 传对象」——内建协议、自动处理对象图与循环引用，使用成本极低。代价同样明显：格式二进制不可读、兼容性脆弱、安全模型形同虚设（反序列化可执行任意代码）。这也是它逐渐退出新系统核心路径的原因——「方便」换「可控」在分布式规模下不划算。

**面试官可能继续追问**：

- **反序列化真的完全不执行构造器吗？** —— Serializable 路径不执行本类构造器；但会执行「最近的非序列化父类」的无参构造器。Externalizable 路径则要求 public 无参构造器并执行它。
- **serialVersionUID 不指定还有什么风险？** —— 除结构变化导致 `InvalidClassException` 外，不同编译器/版本算出的值理论上可能不同，跨环境反序列化旧数据时是隐形炸弹。
- **transient 和 static 字段会序列化吗？** —— 都不会。`transient` 是显式声明；`static` 属于类不属于实例，序列化机制天然跳过。

**实际工程场景**：缓存（Redis/MQ）里存对象优先 JSON/Protobuf，Java 原生序列化只用于必须传递完整对象图的遗留 RMI 内部链路；`serialVersionUID` 在所有 `Serializable` 实体上显式声明并纳入 code review；任何接收外部字节流的反序列化点（RPC 框架、缓存读取）配 `ObjectInputFilter` 白名单。

**容易答错的地方**：以为反序列化会跑构造器——不会跑本类构造器，所以「构造器里的默认值和校验」在反序列化路径上全部失效，不变量要靠 `readObject` 补校验；以为 transient 字段反序列化后保持原值——是默认值；以为加上 `serialVersionUID` 就永远兼容——删字段、改字段类型仍可能因流格式不匹配失败。

> 权威内容：Java 序列化机制当前尚未在仓库建设，可参考《Effective Java》第 12 章与官方文档（`ObjectOutputStream`/`ObjectInputStream`、JEP 290 反序列化过滤器）。

## 本专题常见追问链

```text
链 1（相等性 → 容器）：
  == 与 equals 口径
    → 重写 equals 必须重写 hashCode（契约）
      → HashMap「先定位桶、再 equals」两段式查找
        → key 必须不可变 / 并发容器（转 02-集合框架、03-并发编程）

链 2（不可变 → 拷贝）：
  String 不可变（常量池 / hashCode 缓存 / 线程安全）
    → 循环拼接换 StringBuilder
      → 可变对象的深拷贝与防御性复制
        → 不可变值对象「免拷贝」设计

链 3（类型系统）：
  值传递（引用值的副本）
    → 泛型擦除（运行期无类型实参）
      → 通配符 PECS（生产者 extends / 消费者 super）
        → 集合 API 签名设计（转 02-集合框架）

链 4（分派 → 初始化 → 终结）：
  重载（编译期静态分派）vs 重写（运行期动态分派）
    → 虚方法表
    → static 初始化顺序 / 类加载时机
      → final 语义与 final 域安全发布
        → finalize 缺陷与 Cleaner（转 04-JVM）

链 5（框架底座）：
  反射（运行期操作类元数据）
    → 注解（RUNTIME 保留 + 反射读取 / APT 编译期生成）
      → SPI（META-INF/services 发现实现）
        → Spring IoC / JDBC 驱动加载（转 Spring 专题）
```

面试官为什么这样追：每条链上的相邻节点是**因果**关系而不是并列关系——答了 equals 契约就必然被问到它服务谁（HashMap），答了 String 不可变就必然被问到拼接选型和拷贝策略，答了反射就必然被问到模块化限制。追的不是记忆量，而是看应聘者是否知道每个结论「被谁依赖、约束了什么」。复习时按链过，比按题过更容易在追问中保持主动。

## 面试前速记

- Java 只有值传递：引用类型传的是引用值的副本；形参重新赋值不影响调用方，通过形参改对象内容调用方可观察。
- equals 相等 ⇒ hashCode 必须相等，反向不成立；HashMap 先 hash 定位桶、再 equals 比较，只重写 equals 会「写得进去、查不出来」。
- String 不可变换来安全共享：常量池复用、hashCode 惰性缓存、天然线程安全；一切「修改」返回新对象，循环拼接用 StringBuilder。
- 泛型编译期存在、运行期擦除成上界：不能 `new T[]`、`instanceof T`、类型参数做静态成员；`f(List<String>)` 与 `f(List<Integer>)` 无法重载。
- 类初始化：静态部分按源码顺序、父先子后、全局一次；实例化先父构造、再本类字段/实例块、后构造体；static final 编译期常量被内联，不触发初始化。
- 重载编译期按静态类型选，重写运行期按实际类型选；private/static/final 方法不进虚方法表，static 是隐藏不是重写。
- `==` 比同一性，equals 默认就是 `==`、重写后是类型自定义的相等；包装类 `[-128,127]` 走缓存，比较一律 equals；String 的 `==` 为 true 只可能是常量池折叠或 intern。
- StringBuilder 单线程首选，StringBuffer 的 synchronized 无竞争也付费；`+` 号只在单条语句内被编译优化，循环里拼接必须 StringBuilder/String.join。
- 反射是框架底座但穿透封装且有固定开销（冷调用慢 1～2 个数量级、JIT 预热后约一倍到几倍）；JDK 9+ 深反射需 `--add-opens`；热路径用 MethodHandle。
- 受检异常表达可预期的外部失败，运行时异常表达程序错误；finally 里 return 会覆盖 try 的返回，资源关闭一律 try-with-resources；不要吞异常。
- final 只禁止重新绑定、不禁止改对象内容；finalize 时机不确定且可复活对象，JDK 9 废弃、JDK 18 标记待移除，用 Cleaner（虚引用）或显式 close。
- 反序列化不走本类构造器；显式声明 serialVersionUID；敏感字段 transient；不可信输入反序列化是安全漏洞入口，JDK 9+ 用 ObjectInputFilter 过滤。
- Java 8 是分水岭（Lambda/Stream/Optional/默认方法）；var、switch 表达式、文本块、Records、instanceof 模式匹配、密封类逐个补表达力；企业升级按 LTS 走。
