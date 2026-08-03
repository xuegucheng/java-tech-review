# equals 与 hashCode

> 本章把 Java 对象相等性视为一项长期 API 契约，而不是 IDE 自动生成的样板代码。重点是区分身份相等、值相等和业务身份，正确实现 `equals`/`hashCode`，理解继承、代理、可变字段和哈希容器中的边界，并能为 WMS 实体、值对象和复合键选择稳定语义。

---

## 18.1 本章定位

学完本章，应能够准确回答：

- 基本类型和引用类型的 `==` 分别比较什么？
- `Object.equals` 默认语义是什么？
- 值相等、对象身份和业务实体身份如何区分？
- `equals` 的自反、对称、传递、一致和非空契约分别意味着什么？
- 重写 `equals` 后为什么必须重写 `hashCode`？
- 哈希容器如何组合使用 hashCode 和 equals？
- unequal 对象是否必须拥有不同 hashCode？
- `getClass()` 与 `instanceof` 类型判断如何选择？
- 可扩展类为什么很难稳定增加值组件而保持 equals 契约？
- `canEqual` 能解决哪些问题，又不能解决哪些问题？
- 为什么组合通常比继承更适合值相等？
- 数组、浮点数、BigDecimal、枚举和 record 应如何比较？
- 可变字段参与 hashCode 后放入 HashMap 会发生什么？
- 实体对象的数据库 ID、自然键和生命周期状态如何影响 equals？
- ORM 代理为什么会让 `getClass()` 判断变复杂？
- `compareTo` 与 equals 不一致有什么集合行为差异？
- `Objects.equals`、`Objects.hash` 和 `Arrays.deepEquals` 如何正确使用？
- 何时可以缓存 hashCode？
- IdentityHashMap 与普通 HashMap 的相等性语义有何不同？
- 如何用属性测试和契约测试验证相等性实现？

`toString`、`clone`、`getClass` 的通用使用以及 `Objects` 其他工具方法见第 19 章；HashMap 数据结构和扩容算法见集合模块。

## 18.2 学习主线

```text
== 的两类语义
↓
Object 默认身份相等
↓
业务类型定义值或身份相等
↓
equals 五项契约
↓
hashCode 一致性契约
↓
哈希桶先散列、后 equals
↓
选择参与字段与类型边界
↓
继承、代理和可变对象风险
↓
值对象、实体、记录和复合键建模
↓
测试契约并治理集合行为
```

相等性设计的第一步不是生成代码，而是写出一句业务定义：

```text
两个该类型对象在什么条件下应被视为同一个值或同一个实体？
```

## 18.3 Object 与对象协议

所有普通类最终继承 `Object`。`equals` 和 `hashCode` 共同形成对象相等性与散列协议。

默认情况下，`Object.equals` 使用身份语义，近似等价于 `this == other`；`Object.hashCode` 提供与该身份语义一致的散列值。

## 18.4 基本类型的 ==

对基本类型，`==` 比较经过数值提升后的值：

```java
10 == 10L // true
```

浮点数需要额外注意 NaN、正负零和舍入误差。业务金额不应直接依赖 double 的 `==`。

## 18.5 引用类型的 ==

引用类型的 `==` 判断两个引用是否指向同一个对象，或是否都为 null。

```java
User a = new User("U1");
User b = a;
User c = new User("U1");

// a == b 为 true
// a == c 为 false
```

字段相同不会让两个独立对象通过 `==` 相等。

## 18.6 装箱与 ==

包装类型使用 `==` 时仍比较引用。缓存、常量折叠和装箱实现可能使部分小值引用相同，但不能把它作为数值比较规则。

```java
Integer a = 100;
Integer b = 100;
Integer c = 1000;
Integer d = 1000;
```

`a == b` 可能为 true，而 `c == d` 通常为 false。数值语义使用 `equals` 或拆箱比较。

## 18.7 身份相等、值相等与实体身份

```text
身份相等
→ 是否同一内存中的对象实例

值相等
→ 所有价值组件是否相同

实体身份
→ 是否表示同一个业务实体，即使属性发生变化
```

`Money(100,CNY)` 通常按值相等；订单实体通常按稳定订单号或持久化身份相等；连接、线程和锁对象往往保留身份相等。

## 18.8 何时重写 equals

适合重写：值对象、复合键、不可变 DTO、坐标、金额、范围和集合元素。

不一定适合：服务对象、连接、线程、资源句柄、生命周期组件和无稳定值语义的可变流程对象。

“所有类都重写 equals”与“所有类都不重写”同样错误。

## 18.9 equals 基本模板

```java
@Override
public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof SkuKey key)) return false;
    return Objects.equals(warehouseId, key.warehouseId)
            && Objects.equals(skuId, key.skuId);
}
```

步骤通常是：同一引用快速返回、检查兼容类型、比较相等性组件。

## 18.10 自反性

对任何非 null 引用 `x`：

```text
x.equals(x) 必须为 true
```

错误的浮动状态、随机值或代理委托都可能破坏自反性。集合实现依赖这一最基础假设。

## 18.11 对称性

若 `x.equals(y)` 为 true，则 `y.equals(x)` 也必须为 true。

父类允许子类参与比较、而子类又要求额外字段相同，是经典对称性破坏来源。

## 18.12 传递性

若 `x.equals(y)` 且 `y.equals(z)`，则 `x.equals(z)` 必须为 true。

在继承层次中试图让“无颜色点”和“有颜色点”部分相等，常会破坏传递性。

## 18.13 一致性

只要用于相等比较的信息未改变，多次调用结果必须一致。

不要把当前时间、随机数、远程查询、数据库状态或懒加载失败作为 equals 结果的一部分。

## 18.14 非空性

任何非 null 对象 `x` 都必须满足：

```text
x.equals(null) == false
```

使用模式匹配 `instanceof` 会自然处理 null；手工调用 `other.getClass()` 前必须先判 null。

## 18.15 hashCode 契约

核心要求：

1. 同一对象在参与 equals 的信息未变化时，多次调用结果一致；
2. equals 相等的两个对象必须具有相同 hashCode；
3. equals 不等的对象可以碰撞，但更好的分布有助于哈希容器性能。

逆命题“hashCode 相同则 equals 相等”不成立。

## 18.16 为什么必须同时重写

若只重写 equals，不重写 hashCode，两个逻辑相等对象可能进入不同桶，HashSet 无法去重，HashMap 也可能无法使用等价 key 查到值。

```text
相等对象 → 必须相同 hashCode
```

这是正确性的要求，不只是性能优化。

## 18.17 哈希容器查找流程

简化流程：

```text
计算 key.hashCode()
↓
扰动并定位桶
↓
比较 hash
↓
对候选 key 调用 equals
↓
确认是否同一个逻辑键
```

hashCode 用于缩小候选范围，equals 用于最终确认。

## 18.18 碰撞

不同对象产生相同 hashCode 称为碰撞，是允许且不可避免的。正确容器仍会使用 equals 区分它们。

所有对象都返回常量 hashCode 在正确性上可能仍工作，但会把哈希结构退化为近似线性查找。

## 18.19 选择相等性组件

参与 equals 的字段必须共同定义对象语义，并且 hashCode 使用同一组字段。

不要因为字段“目前方便”就纳入：更新时间、缓存、展示名称、懒加载关联、审计信息和可变统计通常不应决定身份。

## 18.20 Objects.equals

`Objects.equals(a, b)` 以 null 安全方式比较：

```text
a == b || (a != null && a.equals(b))
```

它适合引用字段；基本类型直接使用 `==`，数组需要 Arrays 系列方法。

## 18.21 Objects.hash 与手工散列

```java
return Objects.hash(warehouseId, skuId);
```

简洁但使用可变参数，会创建数组并装箱基本类型。普通业务对象通常足够；性能热点可使用 `31 * result + fieldHash` 或专用实现，并通过基准验证。

## 18.22 单字段 hashCode

单个引用字段可使用：

```java
return Objects.hashCode(value);
```

它对 null 返回 0。不要误用 `Objects.hash(value)` 替代单字段 hashCode；两者计算结果通常不同，因为后者按一个元素数组散列。

## 18.23 getClass 类型策略

```java
if (other == null || getClass() != other.getClass()) return false;
```

只允许完全相同运行时类型相等。优点是容易维护对称和传递；缺点是代理子类、合法子类型和值层次可能不能比较。

## 18.24 instanceof 类型策略

```java
if (!(other instanceof Money money)) return false;
```

允许子类型参与。适合 final 类、record、sealed 且相等性统一的层次，或接口定义了稳定值契约的情况。

对可扩展类使用时必须评估子类新增值组件是否破坏契约。

## 18.25 可扩展值类难题

一个可实例化父类使用坐标相等，子类增加颜色后，无法同时自然满足：

- 父类与子类按坐标相等；
- 子类之间还要考虑颜色；
- 对称性与传递性全部成立。

最佳设计通常是：让值类 final、使用组合，或把共同部分提取为值组件而不是可实例化父类。

## 18.26 canEqual 模式

`canEqual` 允许对象声明是否接受与另一类型比较，可帮助部分继承层次保持对称：

```java
protected boolean canEqual(Object other) {
    return other instanceof Point;
}
```

它增加复杂度，不能自动证明所有子类契约正确。除非框架或既有层次要求，优先 final 值类与组合。

## 18.27 组合替代值继承

```java
record Point(int x, int y) { }
record ColoredPoint(Point point, String color) { }
```

两个类型拥有明确、互不矛盾的相等性。组合避免父子对象之间“部分相等”的难题。

## 18.28 record 相等性

record 自动基于组件生成 equals 和 hashCode，且同一 record 类型的组件值相等时对象相等。

组件若为数组，自动生成逻辑仍使用数组自身的 equals，即身份语义；需要内容相等时不要直接把裸数组作为 record 组件，或手工定义协议。

## 18.29 数组相等性

数组继承 Object 的身份 equals：

```java
new int[]{1}.equals(new int[]{1}) // false
```

内容比较使用：

- `Arrays.equals`：一维元素比较；
- `Arrays.deepEquals`：嵌套对象数组递归比较；
- 对应 `Arrays.hashCode` / `deepHashCode` 保持散列一致。

## 18.30 浮点相等性

浮点数存在 NaN、正负零和舍入。自动生成实现通常使用 `Double.compare` 或位级规则，而业务近似相等不应直接放入 equals，因为带容差比较很难满足传递性。

近似比较应是明确方法：

```java
boolean approximatelyEquals(Measurement other, double epsilon)
```

## 18.31 BigDecimal 相等性

```java
new BigDecimal("1.0").equals(new BigDecimal("1.00")) // false
compareTo(...) == 0                                  // true
```

`equals` 同时考虑数值和 scale。若领域只关心数值，需要在构造时规范化 scale，或定义专用值对象；不要让 equals 和 hashCode 使用不同规范。

## 18.32 枚举相等性

枚举常量是受 JVM 管理的唯一实例，使用 `==` 比较最清晰且 null 安全：

```java
status == OrderStatus.CREATED
```

枚举的 equals 也采用身份语义且不可重写。

## 18.33 String 相等性

字符串内容比较使用 `equals`，不要依赖字符串池让 `==` 偶然为 true。忽略大小写比较应明确使用 `equalsIgnoreCase`，并注意区域和协议规则。

## 18.34 实体相等性

实体具有跨时间稳定身份。候选策略：

- 创建时即分配业务 ID；
- 使用不可变自然键；
- 使用数据库 ID，但必须处理持久化前 ID 为 null 的阶段；
- 在同一持久化上下文内保留身份相等。

没有适用于所有 ORM 和领域的统一模板。

## 18.35 数据库 ID 陷阱

若两个新实体的 ID 都是 null，不能因此认为相等。若对象加入 HashSet 后持久化生成 ID，hashCode 变化又会破坏容器定位。

更稳妥的方案是创建时分配稳定 UUID/业务键，或避免把生命周期中 hash 会变化的实体作为哈希 key。

## 18.36 自然键

自然键应满足稳定、唯一、非空和不会因普通业务修改而变化。手机号、邮箱、名称看似唯一但常可更改，不一定适合作为永久实体身份。

## 18.37 ORM 代理

ORM 可能使用运行时子类代理实体。严格 `getClass()` 判断会让实体与其代理不相等；宽泛 `instanceof` 又可能允许不合适子类比较。

应遵循所用 ORM 的代理类型解析和实体相等性建议，并保持对称性、稳定 hash 和会话边界一致。

## 18.38 可变 Hash Key

```java
Set<Key> set = new HashSet<>();
set.add(key);
key.changeWarehouse("W2");
set.contains(key); // 可能 false
```

对象进入桶时使用旧 hash，字段变化后计算新 hash，容器不会自动搬迁条目。参与 equals/hashCode 的字段应在作为 key 期间保持不变。

## 18.39 缓存 hashCode

真正不可变、计算昂贵且频繁散列的对象可以缓存 hashCode。必须处理计算结果为 0 的标志问题，并确保所有相等性组件绝不变化。

普通小对象无需过早缓存。

## 18.40 compareTo 与 equals

有序集合按比较器或 `compareTo` 判断等价；哈希集合按 equals/hashCode 判断。

若 `compareTo(a,b)==0` 但 `a.equals(b)==false`，TreeSet 可能只保留一个，而 HashSet 保留两个。BigDecimal 是经典例子。

## 18.41 IdentityHashMap

`IdentityHashMap` 使用引用身份 `==` 判断 key，而不是 equals。它适合对象图遍历、序列化内部表和身份拓扑算法，不适合普通业务 key。

## 18.42 null 与相等性

`Objects.equals(a,b)` 可安全处理 null。Map 是否允许 null 由具体实现决定：HashMap 允许一个 null key，ConcurrentHashMap 不允许 null key/value。

不要用 `x.equals(null)` 判断 null。

## 18.43 继承 equals 的风险

子类如果继承父类 equals，而新增字段不参与相等，可能是刻意的“非值扩展”，也可能是遗漏。若子类重写，则需要重新证明整个层次的对称和传递。

## 18.44 自动生成代码

IDE、record 和 Lombok 可以减少样板，但无法替你决定：

- 哪些字段是身份；
- 是否允许子类相等；
- 可变字段是否稳定；
- ORM 代理如何处理；
- 数组和 BigDecimal 的特殊语义。

生成后仍必须评审契约。

## 18.45 WMS 值对象

```java
public record StockKey(
        String warehouseId,
        String skuId,
        String batchNo
) {
    public StockKey {
        warehouseId = requireText(warehouseId);
        skuId = requireText(skuId);
        batchNo = requireText(batchNo);
    }
}
```

库存维度键适合不可变值相等，可安全用于 Map key；展示名称、可用库存数量和更新时间不应参与。

## 18.46 WMS 实体

出库单实体通常按稳定 `outboundOrderId` 识别。状态、优先级、操作人和时间会变化，不应参与实体身份。

若 ID 在持久化后才生成，应避免把暂态对象放入 HashSet，或改为创建时分配业务 ID。

## 18.47 测试相等性契约

至少验证：

- `x.equals(x)`；
- `x.equals(null)` 为 false；
- 对称性；
- 三对象传递性；
- 多次一致性；
- 相等对象 hash 相同；
- 非关键字段变化不影响相等；
- 关键字段变化产生不等；
- HashSet/HashMap 实际行为；
- 子类或代理边界。

## 18.48 属性测试

属性测试可以批量生成对象组合，验证 equals 的数学性质。对金额、范围、复合键和继承层次尤其有效。

测试不能证明所有语义正确，但能快速发现非对称、非传递和 hash 不一致。

## 18.49 设计决策清单

实现 equals 前回答：

1. 这是值对象、实体还是身份对象？
2. 哪些字段定义相等，是否稳定？
3. 类型是否 final 或允许继承？
4. 是否存在 ORM 代理？
5. 是否会作为 Map key 或 Set 元素？
6. 数组、BigDecimal、浮点或集合字段需要特殊规范吗？
7. compareTo 是否应与 equals 一致？
8. 对象生命周期中 hash 是否可能变化？

## 18.50 建议实验

### 实验1：引用 == 与逻辑 equals

**目标**：区分同一对象和相同内容。

```java
public class IdentityAndValueDemo {
    record User(String id) { }
    public static void main(String[] args) {
        User a = new User("U1");
        User b = new User("U1");
        System.out.println(a == b);
        System.out.println(a.equals(b));
    }
}
```

预期或观察重点：

```text
false
true
```

### 实验2：包装类型 == 缓存陷阱

**目标**：观察引用比较不能替代数值比较。

```java
public class BoxedEqualityDemo {
    public static void main(String[] args) {
        Integer a = 100, b = 100;
        Integer c = 1000, d = 1000;
        System.out.println(a == b);
        System.out.println(c == d);
        System.out.println(c.equals(d));
    }
}
```

预期或观察重点：

```text
true
false
true
```

### 实验3：只重写 equals 的 HashSet 问题

**目标**：展示 hashCode 不一致导致去重失败。

```java
import java.util.HashSet;
import java.util.Set;
public class EqualsOnlyDemo {
    static class Key {
        final String id; Key(String id) { this.id = id; }
        public boolean equals(Object o) { return o instanceof Key k && id.equals(k.id); }
    }
    public static void main(String[] args) {
        Set<Key> set = new HashSet<>();
        set.add(new Key("A")); set.add(new Key("A"));
        System.out.println(set.size());
    }
}
```

预期或观察重点：

```text
2
```

### 实验4：正确的值对象散列

**目标**：验证 equals/hashCode 同时重写后的去重。

```java
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
public class ValueKeyDemo {
    static final class Key {
        final String id; Key(String id) { this.id = id; }
        public boolean equals(Object o) { return this == o || o instanceof Key k && Objects.equals(id, k.id); }
        public int hashCode() { return Objects.hashCode(id); }
    }
    public static void main(String[] args) {
        Set<Key> set = new HashSet<>(); set.add(new Key("A")); set.add(new Key("A"));
        System.out.println(set.size());
    }
}
```

预期或观察重点：

```text
1
```

### 实验5：哈希碰撞仍由 equals 区分

**目标**：验证相同 hashCode 不代表相等。

```java
import java.util.HashSet;
import java.util.Set;
public class CollisionDemo {
    record Key(String id) { public int hashCode() { return 1; } }
    public static void main(String[] args) {
        Set<Key> set = new HashSet<>(); set.add(new Key("A")); set.add(new Key("B"));
        System.out.println(set.size());
    }
}
```

预期或观察重点：

```text
2
```

### 实验6：可变 Hash Key 失联

**目标**：观察修改散列字段后 contains 失败。

```java
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
public class MutableHashKeyDemo {
    static class Key {
        String id; Key(String id) { this.id = id; }
        public boolean equals(Object o) { return o instanceof Key k && Objects.equals(id, k.id); }
        public int hashCode() { return Objects.hashCode(id); }
    }
    public static void main(String[] args) {
        Key key = new Key("A"); Set<Key> set = new HashSet<>(); set.add(key);
        key.id = "B";
        System.out.println(set.contains(key));
    }
}
```

预期或观察重点：

```text
false
```

### 实验7：getClass 严格类型策略

**目标**：验证父子类型即使字段相同也不相等。

```java
public class GetClassEqualityDemo {
    static class Point {
        final int x; Point(int x) { this.x = x; }
        public boolean equals(Object o) { return o != null && getClass() == o.getClass() && x == ((Point)o).x; }
        public int hashCode() { return x; }
    }
    static class SpecialPoint extends Point { SpecialPoint(int x) { super(x); } }
    public static void main(String[] args) {
        System.out.println(new Point(1).equals(new SpecialPoint(1)));
    }
}
```

预期或观察重点：

```text
false
```

### 实验8：instanceof 与 final 值类

**目标**：展示 final 类中使用 instanceof 的安全边界。

```java
public class FinalValueEqualityDemo {
    static final class Code {
        final String value; Code(String value) { this.value = value; }
        public boolean equals(Object o) { return o instanceof Code c && value.equals(c.value); }
        public int hashCode() { return value.hashCode(); }
    }
    public static void main(String[] args) { System.out.println(new Code("A").equals(new Code("A"))); }
}
```

预期或观察重点：

```text
true
```

### 实验9：数组默认身份相等

**目标**：验证数组 equals 不比较内容。

```java
public class ArrayIdentityEqualsDemo {
    public static void main(String[] args) {
        int[] a = {1,2}; int[] b = {1,2};
        System.out.println(a.equals(b));
    }
}
```

预期或观察重点：

```text
false
```

### 实验10：Arrays.equals 内容相等

**目标**：使用匹配的数组比较和散列方法。

```java
import java.util.Arrays;
public class ArraysContentEqualsDemo {
    public static void main(String[] args) {
        int[] a = {1,2}; int[] b = {1,2};
        System.out.println(Arrays.equals(a,b));
        System.out.println(Arrays.hashCode(a) == Arrays.hashCode(b));
    }
}
```

预期或观察重点：

```text
true
true
```

### 实验11：深层数组比较

**目标**：比较嵌套对象数组。

```java
import java.util.Arrays;
public class DeepArrayEqualsDemo {
    public static void main(String[] args) {
        Object[] a = {new int[]{1,2}}; Object[] b = {new int[]{1,2}};
        System.out.println(Arrays.equals(a,b));
        System.out.println(Arrays.deepEquals(a,b));
    }
}
```

预期或观察重点：

```text
false
true
```

### 实验12：BigDecimal equals 与 compareTo

**目标**：观察 scale 造成的语义差异。

```java
import java.math.BigDecimal;
public class BigDecimalEqualityDemo {
    public static void main(String[] args) {
        BigDecimal a = new BigDecimal("1.0"); BigDecimal b = new BigDecimal("1.00");
        System.out.println(a.equals(b));
        System.out.println(a.compareTo(b) == 0);
    }
}
```

预期或观察重点：

```text
false
true
```

### 实验13：枚举使用 ==

**目标**：验证枚举常量身份唯一。

```java
public class EnumEqualityDemo {
    enum Status { CREATED, DONE }
    public static void main(String[] args) {
        Status a = Status.CREATED; Status b = Status.valueOf("CREATED");
        System.out.println(a == b);
    }
}
```

预期或观察重点：

```text
true
```

### 实验14：record 自动相等性

**目标**：验证 record 基于组件生成 equals/hashCode。

```java
public class RecordEqualityDemo {
    record StockKey(String warehouse, String sku) { }
    public static void main(String[] args) {
        StockKey a = new StockKey("W1","S1"); StockKey b = new StockKey("W1","S1");
        System.out.println(a.equals(b));
        System.out.println(a.hashCode() == b.hashCode());
    }
}
```

预期或观察重点：

```text
true
true
```

### 实验15：record 数组组件陷阱

**目标**：观察自动 equals 沿用数组身份语义。

```java
public class RecordArrayComponentDemo {
    record Payload(byte[] data) { }
    public static void main(String[] args) {
        System.out.println(new Payload(new byte[]{1}).equals(new Payload(new byte[]{1})));
    }
}
```

预期或观察重点：

```text
false
```

### 实验16：TreeSet 与 HashSet 语义差异

**目标**：观察 compareTo 与 equals 不一致的影响。

```java
import java.math.BigDecimal;
import java.util.*;
public class OrderedAndHashSetDemo {
    public static void main(String[] args) {
        BigDecimal a = new BigDecimal("1.0"), b = new BigDecimal("1.00");
        Set<BigDecimal> tree = new TreeSet<>(); tree.add(a); tree.add(b);
        Set<BigDecimal> hash = new HashSet<>(); hash.add(a); hash.add(b);
        System.out.println(tree.size()); System.out.println(hash.size());
    }
}
```

预期或观察重点：

```text
1
2
```

### 实验17：IdentityHashMap 身份语义

**目标**：验证逻辑相等对象仍作为不同 key。

```java
import java.util.IdentityHashMap;
import java.util.Map;
public class IdentityMapDemo {
    public static void main(String[] args) {
        Map<String,Integer> map = new IdentityHashMap<>();
        map.put(new String("A"),1); map.put(new String("A"),2);
        System.out.println(map.size());
    }
}
```

预期或观察重点：

```text
2
```

### 实验18：Objects.hashCode 与 Objects.hash

**目标**：展示单字段两种方法结果不同。

```java
import java.util.Objects;
public class ObjectsHashDifferenceDemo {
    public static void main(String[] args) {
        String value = "A";
        System.out.println(Objects.hashCode(value));
        System.out.println(Objects.hash(value));
        System.out.println(Objects.hashCode(value) == Objects.hash(value));
    }
}
```

预期或观察重点：

```text
65
96
false
```

### 实验19：null 安全比较

**目标**：验证 Objects.equals 的 null 语义。

```java
import java.util.Objects;
public class NullSafeEqualsDemo {
    public static void main(String[] args) {
        System.out.println(Objects.equals(null,null));
        System.out.println(Objects.equals(null,"A"));
    }
}
```

预期或观察重点：

```text
true
false
```

### 实验20：WMS 复合库存键

**目标**：验证稳定值对象可作为 HashMap key。

```java
import java.util.HashMap;
import java.util.Map;
public class StockKeyMapDemo {
    record StockKey(String warehouseId, String skuId, String batchNo) { }
    public static void main(String[] args) {
        Map<StockKey,Integer> stock = new HashMap<>();
        stock.put(new StockKey("W1","S1","B1"),10);
        System.out.println(stock.get(new StockKey("W1","S1","B1")));
    }
}
```

预期或观察重点：

```text
10
```

## 18.51 高频面试题

1. 基本类型的 `==` 比较什么？
2. 引用类型的 `==` 比较什么？
3. 两个 null 引用使用 `==` 的结果是什么？
4. 为什么包装类型不能普遍使用 `==` 比值？
5. Integer 缓存会造成什么错觉？
6. Object 默认 equals 的语义是什么？
7. 身份相等与值相等有什么区别？
8. 什么是业务实体身份？
9. 哪些类型通常适合值相等？
10. 哪些类型通常保留身份相等？
11. equals 方法的参数为什么是 Object？
12. equals 实现为什么先判断 `this == other`？
13. equals 如何安全处理 null？
14. equals 的自反性是什么？
15. equals 的对称性是什么？
16. equals 的传递性是什么？
17. equals 的一致性是什么？
18. equals 的非空性是什么？
19. 时间和随机数为什么不能参与 equals？
20. hashCode 的第一条一致性要求是什么？
21. equals 相等对象的 hashCode 必须怎样？
22. equals 不等对象必须不同 hashCode 吗？
23. hashCode 相同能否推出 equals 相等？
24. 为什么重写 equals 后必须重写 hashCode？
25. 只重写 hashCode 不重写 equals 是否有意义？
26. HashMap 查找 key 的简化流程是什么？
27. HashSet 如何判断重复元素？
28. 什么是哈希碰撞？
29. 所有对象返回同一 hashCode 是否仍可能正确？
30. 碰撞过多有什么性能影响？
31. equals 和 hashCode 应使用同一组字段吗？
32. 哪些字段通常不应参与相等性？
33. `Objects.equals` 的 null 语义是什么？
34. 基本类型字段是否需要 Objects.equals？
35. `Objects.hash` 有什么便利和开销？
36. `Objects.hashCode(x)` 与 `Objects.hash(x)` 有何区别？
37. 单字段 hashCode 如何实现？
38. `getClass()` 类型检查的特点是什么？
39. `instanceof` 类型检查的特点是什么？
40. final 值类中使用 instanceof 为什么较安全？
41. 可扩展值类为什么难以保持 equals 契约？
42. Point/ColorPoint 问题破坏哪项契约？
43. 什么是 canEqual 模式？
44. canEqual 是否自动解决所有继承相等性问题？
45. 为什么组合常比值继承更安全？
46. record 如何生成 equals/hashCode？
47. record 数组组件为何仍可能按身份比较？
48. 数组默认 equals 比较什么？
49. 一维数组内容比较使用什么？
50. 嵌套对象数组比较使用什么？
51. 数组内容 hashCode 应使用什么？
52. 浮点近似相等为什么不适合直接做 equals？
53. NaN 和正负零需要注意什么？
54. BigDecimal.equals 比较哪些信息？
55. BigDecimal.compareTo 与 equals 有何差异？
56. 金额值对象如何规范 BigDecimal scale？
57. 枚举为什么推荐使用 `==`？
58. String 为什么不能依赖 `==`？
59. 忽略大小写比较应注意什么？
60. 什么是实体相等性？
61. 数据库生成 ID 的实体有哪些生命周期问题？
62. 两个 ID 为 null 的新实体是否应相等？
63. 实体持久化后 ID 变化会怎样影响 HashSet？
64. 自然键需要满足哪些条件？
65. 手机号适合作为永久自然键吗？
66. ORM 代理为什么影响 getClass 判断？
67. 使用 instanceof 是否完全解决代理问题？
68. 可变对象为什么不适合做 HashMap key？
69. 修改 key 后为什么 contains 可能失败？
70. 容器会自动重新散列已修改 key 吗？
71. 什么对象可以缓存 hashCode？
72. 缓存 hashCode 需要处理什么边界？
73. compareTo 与 equals 不一致有何后果？
74. TreeSet 如何判断元素等价？
75. HashSet 如何判断元素等价？
76. BigDecimal 在 TreeSet 和 HashSet 中可能有什么不同？
77. IdentityHashMap 使用什么相等语义？
78. IdentityHashMap 适合什么场景？
79. IdentityHashMap 为什么不适合普通业务键？
80. HashMap 是否允许 null key？
81. ConcurrentHashMap 是否允许 null？
82. 子类继承父类 equals 时新增字段怎么办？
83. IDE 自动生成 equals 是否保证业务正确？
84. Lombok 生成 equals 仍需评审哪些问题？
85. WMS 库存复合键应包含哪些稳定字段？
86. 库存数量是否应参与 StockKey 相等性？
87. 订单状态是否应参与订单实体相等性？
88. 如何测试 equals 五项契约？
89. 相等性属性测试有什么价值？
90. 设计 equals 前第一句业务定义应该是什么？

## 18.52 易错点

### 易错点 1：用 == 比较字符串内容

`==` 比较引用，字符串池只能造成偶然相同。

### 易错点 2：用 == 比较包装数值

缓存范围外会得到不同结果。

### 易错点 3：认为 Object.equals 默认比较字段

默认主要是身份相等。

### 易错点 4：所有类都自动生成 equals

没有值语义的服务和资源对象可能只需要身份相等。

### 易错点 5：只重写 equals

哈希容器可能无法找到逻辑相等 key。

### 易错点 6：只重写 hashCode

默认 equals 仍按身份判断，通常没有形成值协议。

### 易错点 7：认为 hash 相同就是相等

碰撞是允许的，必须再用 equals。

### 易错点 8：要求不等对象 hash 必须不同

int 空间有限，契约不作此要求。

### 易错点 9：equals 与 hashCode 使用不同字段

会直接破坏相等对象同 hash 的要求。

### 易错点 10：把更新时间放入 equals

对象一更新就改变身份并破坏集合稳定性。

### 易错点 11：把懒加载关联放入 equals

可能触发 I/O、递归和会话异常。

### 易错点 12：equals 查询数据库

破坏一致性、性能和无副作用预期。

### 易错点 13：用随机值参与 hashCode

同一对象多次散列会落入不同桶。

### 易错点 14：对引用字段直接调用 equals

字段为 null 时会抛异常，使用 Objects.equals。

### 易错点 15：数组字段使用 Objects.equals

数组仍按身份比较，应使用 Arrays 方法。

### 易错点 16：record 数组组件自动内容相等

record 调用组件 equals，数组组件仍是身份语义。

### 易错点 17：用容差实现 equals

近似关系通常不具传递性。

### 易错点 18：BigDecimal 只看 compareTo 实现 equals，却沿用原 hashCode

数值规范和散列会不一致。

### 易错点 19：用 enum.ordinal 持久化并比较

顺序改变会破坏数据语义。

### 易错点 20：可扩展父类用 instanceof 比较全部子类

子类新增字段容易破坏对称和传递。

### 易错点 21：认为 getClass 永远最好

ORM 代理和值子类型可能需要不同策略。

### 易错点 22：认为 instanceof 永远最好

过宽类型兼容也可能破坏契约。

### 易错点 23：把 canEqual 当万能补丁

它仍需整个层次正确协作。

### 易错点 24：值对象使用继承扩展值组件

组合通常更清晰。

### 易错点 25：实体按所有当前字段相等

实体属性变化不应改变身份。

### 易错点 26：两个 null 数据库 ID 实体相等

会把所有暂态实体错误合并。

### 易错点 27：实体入 Set 后再生成 ID

hash 变化导致条目失联。

### 易错点 28：把邮箱当永不变自然键

业务通常允许修改，需要确认稳定承诺。

### 易错点 29：忽略 ORM 代理

实体与代理可能非对称或完全不等。

### 易错点 30：修改 HashMap key 后期待自动搬桶

容器不会监控 key 字段变化。

### 易错点 31：为可变对象缓存 hashCode

缓存会与当前 equals 字段失配。

### 易错点 32：Objects.hash 用于极致热点而不测量

可变参数和装箱可能产生额外分配。

### 易错点 33：把 compareTo==0 当成 equals

有序和哈希集合可能表现不同。

### 易错点 34：普通业务使用 IdentityHashMap

逻辑相等 key 会被视为不同。

### 易错点 35：用 x.equals(null) 判空

x 为 null 时抛 NPE。

### 易错点 36：让 equals 抛业务异常

容器和框架调用相等性时难以恢复。

### 易错点 37：toString 参与 equals

展示格式变化会改变身份。

### 易错点 38：序列化字符串参与 hashCode

协议格式和字段顺序改变会破坏稳定性。

### 易错点 39：只测试两个对象

传递性至少需要三个对象组合。

### 易错点 40：认为 IDE 生成即可定稿

工具不会理解领域身份和生命周期。

## 18.53 工程实践建议

### 工程实践 1：先定义相等性语义

写出对象何时代表同一个值或实体。

### 工程实践 2：值对象优先 final 或 record

减少继承对契约的破坏。

### 工程实践 3：实体使用稳定身份

优先创建时可用且不可变的业务 ID。

### 工程实践 4：参与字段保持稳定

作为哈希 key 期间绝不修改。

### 工程实践 5：equals/hashCode 同步维护

代码评审把两者视为一个修改单元。

### 工程实践 6：使用相同字段集合

顺序可以不同但语义必须一致。

### 工程实践 7：引用字段用 Objects.equals

获得清晰 null 语义。

### 工程实践 8：单字段 hash 用 Objects.hashCode

避免 Objects.hash 的数组规则差异。

### 工程实践 9：数组字段用 Arrays 配套方法

equals 与 hashCode 必须同时使用内容语义。

### 工程实践 10：BigDecimal 在构造边界规范化

明确 scale 和舍入策略。

### 工程实践 11：近似数值使用专用方法

不要污染 equals 数学契约。

### 工程实践 12：枚举用 ==

表达唯一常量身份且 null 安全。

### 工程实践 13：String 用 equals

需要忽略大小写时明确协议区域规则。

### 工程实践 14：可扩展值层次优先组合

避免 Point/ColorPoint 问题。

### 工程实践 15：ORM 实体遵循框架指南

统一处理代理、ID 和会话边界。

### 工程实践 16：暂态实体避免进入 HashSet

除非已有稳定业务身份。

### 工程实践 17：Map key 使用不可变专用类型

不要直接使用可变实体或 DTO。

### 工程实践 18：compareTo 尽量与 equals 一致

除非明确记录并测试差异。

### 工程实践 19：缓存 hash 只用于不可变热点

先测量计算成本。

### 工程实践 20：测试真实容器行为

不仅直接调用 equals，还要放入 Map/Set。

### 工程实践 21：覆盖契约五项

尤其是对称和传递。

### 工程实践 22：生成同值不同实例测试

验证值语义而非身份误判。

### 工程实践 23：覆盖 null 和其他类型

equals 应返回 false 而非异常。

### 工程实践 24：覆盖非关键字段变化

确认展示和审计字段不影响身份。

### 工程实践 25：覆盖关键字段变化

确认相等性组件确实生效。

### 工程实践 26：覆盖代理或子类边界

根据所选类型策略验证。

### 工程实践 27：使用属性测试扩展组合

批量验证数学契约。

### 工程实践 28：常量 hash 仅用于演示

生产应提供合理分布。

### 工程实践 29：不要依赖 hash 值跨进程稳定

契约只要求单次执行内的一致性条件。

### 工程实践 30：业务唯一 ID 不使用 identityHashCode

它不是持久化标识。

### 工程实践 31：IdentityHashMap 限制在算法内部

避免泄露为领域接口。

### 工程实践 32：日志中同时输出业务 ID

不要用 hashCode 作为对象唯一标识。

### 工程实践 33：不可变集合元素也检查深层状态

元素可变仍可能改变容器键语义。

### 工程实践 34：复合键做构造校验

拒绝 null、空白和未规范化字段。

### 工程实践 35：复合键包含完整一致性边界

仓库、SKU、批次等按业务准确选择。

### 工程实践 36：实体身份不包含状态

状态变化不应让实体变成另一个实体。

### 工程实践 37：避免 equals 触发外部资源

保持快速、确定、无副作用。

### 工程实践 38：记录相等性决策

尤其说明 getClass 或 instanceof 的原因。

### 工程实践 39：API 演进时保持相等性兼容

新增字段前评估历史集合、缓存和序列化行为。

### 工程实践 40：评审自动生成结果

领域语义永远优先于工具默认。

## 18.54 官方参考资料

- Java SE 26 `java.lang.Object`：equals 与 hashCode 契约；
- Java SE 26 `java.util.Objects`：equals、hash、hashCode；
- Java SE 26 `java.util.Arrays`：数组内容比较与散列；
- Java SE 26 `HashMap`、`HashSet`、`TreeSet`、`IdentityHashMap` API；
- Java Language Specification：数值比较、引用相等和 record 成员规则。

## 18.55 本章总结

```text
==：基本值或引用身份
↓
Object.equals 默认仍是身份语义
↓
业务类型明确值相等或实体身份
↓
equals 满足自反、对称、传递、一致、非空
↓
相等对象必须拥有相同 hashCode
↓
哈希容器先定位桶，再用 equals 确认
↓
参与字段必须稳定并与业务语义一致
↓
继承、代理、数组、BigDecimal 和可变 key 需要专门处理
↓
通过契约测试和真实集合行为验证实现
```

## 18.56 面试口述版

引用类型的 `==` 比较是否指向同一对象，`Object.equals` 默认也采用身份语义；值对象需要根据价值组件重写 equals，同时必须用相同字段重写 hashCode。equals 要满足自反、对称、传递、一致和对 null 返回 false；hashCode 要保证相等对象散列值相同，但不等对象允许碰撞。HashMap 和 HashSet 先用 hashCode 定位候选桶，再用 equals 确认逻辑键，所以只重写 equals 会导致逻辑相等对象落入不同桶。类型判断用 `getClass` 还是 `instanceof` 取决于是否允许子类型参与相等；可扩展值类很难在新增字段后保持契约，通常应使用 final、record 或组合。作为哈希 key 的对象，其相等字段在容器期间必须不可变，否则修改后会在旧桶中失联。实体相等还要处理数据库 ID、自然键和 ORM 代理，没有通用模板，核心是选择跨生命周期稳定的身份并通过契约测试验证。
