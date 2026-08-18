# ArrayList 原理与工程实践

> 本章从 List 契约进入动态数组实现，区分规范保证与 OpenJDK 实现细节，并把扩容、视图、迭代和内存成本落到工程选择。

---

## 02.1 本章定位

`ArrayList` 是 Java 业务代码中最常用的 List 实现之一。本章不满足于“底层是数组、查询快、增删慢”的八股结论，而是建立从接口契约、对象布局、扩容、批量操作、迭代器、视图到工程选型的完整模型。

学完本章，应能够准确回答：

- `size` 与 `capacity` 有什么区别？
- 无参构造器是否立即分配长度 10 的数组？
- ArrayList 的扩容倍率是否属于 API 规范？
- `add`、`add(index)`、`remove` 分别移动哪些元素？
- 为什么 `remove(1)` 与 `remove(Integer.valueOf(1))` 语义不同？
- `clear()` 是否释放内部数组？
- `ensureCapacity()` 与 `trimToSize()` 适合什么场景？
- `clone()`、拷贝构造器、`List.copyOf()` 有什么差异？
- `subList()` 为什么是危险又高效的视图？
- fail-fast 能否保证并发正确性？
- `RandomAccess` 标记有什么意义？
- 为什么实际业务中 ArrayList 往往优先于 LinkedList？
- Java 21 的首尾和反向 API 如何作用于 ArrayList？

本章以 Java SE API 契约为主，对 OpenJDK 实现细节会明确标注，避免把当前实现背成永久规范。

---

## 02.2 学习主线

```text
List 契约
↓
连续引用数组 + size
↓
容量、延迟分配与构造器
↓
追加、插入、删除与数组搬移
↓
扩容和容量规划
↓
查询、遍历与 RandomAccess
↓
批量操作和数组桥接
↓
clone、copyOf 与 subList
↓
modCount、迭代器和 fail-fast
↓
内存、并发与工程选型
```

分析 ArrayList 操作时，先问：

1. 是否改变逻辑大小？
2. 是否需要移动连续区间？
3. 是否可能触发新数组分配和复制？
4. 返回的是独立对象还是支撑视图？
5. 当前结论来自 List 规范、ArrayList API 还是 OpenJDK 实现？

---

## 02.3 类型层次与定位

简化类型关系：

```text
Iterable<E>
└── Collection<E>
    └── List<E>
        └── ArrayList<E>

ArrayList<E>
├── RandomAccess
├── Cloneable
└── Serializable
```

它是：

- 可调整大小的数组实现；
- 允许重复元素；
- 允许 `null`；
- 保持列表位置顺序；
- 支持按索引随机访问；
- 非线程安全；
- 提供浅克隆；
- 支持 Java 序列化，但业务长期存储不应依赖其默认序列化形式。

---

## 02.4 核心数据模型

概念上可以理解为：

```text
ArrayList
├── Object[] elementData
└── int size
```

其中：

- `elementData.length` 表示当前内部容量；
- `size` 表示逻辑元素数量；
- `[0, size)` 是有效元素区间；
- `[size, capacity)` 是未使用槽位。

示意：

```text
elementData = [A, B, C, null, null]
size        = 3
capacity    = 5
```

源码字段名称属于 OpenJDK 实现细节，但“动态数组 + 逻辑大小”的模型是理解 API 行为的核心。

---

## 02.5 size 与 capacity

`size()` 是公开契约：

```java
list.size();
```

capacity 没有公开 getter，是实现内部资源状态。

二者关系：

```text
0 <= size <= capacity
```

常见误解：

- size 不是数组长度；
- capacity 不是可存储元素的最终上限；
- 扩容不改变 size；
- clear 把 size 变为 0，但通常不会把 capacity 变为 0；
- `trimToSize()` 尝试让容量贴近 size，但其效果仍属于实现层资源管理。

---

## 02.6 三个构造器

常见构造器：

```java
new ArrayList<>()
new ArrayList<>(initialCapacity)
new ArrayList<>(sourceCollection)
```

语义：

| 构造器 | 重点 |
|---|---|
| 无参 | 创建空列表，具体初始数组分配策略属于实现 |
| 指定容量 | 为预计元素数量预留容量 |
| Collection | 按源集合遇到顺序复制元素 |

指定负容量会抛 `IllegalArgumentException`。

集合构造器创建新的列表结构，但只是复制元素引用，不会深拷贝元素对象。

---

## 02.7 无参构造与延迟分配

ArrayList API 传统描述提到默认初始容量为 10，但现代 OpenJDK 实现通常让无参构造器先共享空数组，首次添加时再分配默认容量。

因此更准确地说：

```text
无参构造后 size = 0
首次添加时通常获得可容纳 10 个元素的数组
```

而不是：

```text
new ArrayList<>() 立即 new Object[10]
```

延迟分配可以避免大量从未写入的空列表立即占用数组空间。

这是当前 OpenJDK 实现策略，不应依赖内部共享数组身份。

---

## 02.8 显式容量为零的细微差异

在当前 OpenJDK 实现中：

```java
new ArrayList<>()
new ArrayList<>(0)
```

都表示空列表，但内部使用的空数组标记和首次扩容路径可能不同：

- 无参构造首次添加通常至少扩到默认容量；
- 显式 0 容量可能从较小容量开始增长。

业务代码不应依赖这种差异。只有在性能实验或源码分析中才有必要区分，并且应标注 JDK 版本。

---

## 02.9 追加元素 add(E)

追加的概念流程：

```text
检查是否有容量
↓
必要时扩容
↓
elementData[size] = value
↓
size++
```

当已有容量时，追加通常是常数时间；将偶尔扩容成本均摊后，API 文档将 `add` 描述为 amortized constant time。

“均摊 O(1)”不等于每次都 O(1)：触发扩容的一次调用需要分配新数组并复制已有元素。

---

## 02.10 按位置插入 add(index,E)

插入位置必须满足：

```text
0 <= index <= size
```

概念流程：

```text
检查索引
↓
必要时扩容
↓
把 [index, size) 整段右移一位
↓
写入新元素
↓
size++
```

移动通常通过高效数组复制实现，但移动元素数量仍与尾部长度成正比。

在头部频繁插入的场景中，ArrayList 通常需要移动几乎全部元素；这不代表 LinkedList 就一定更快，因为还要综合定位、对象分配和缓存局部性。

---

## 02.11 get 与 set

`get(index)`：

- 检查 `0 <= index < size`；
- 直接读取数组槽位；
- 通常为常数时间。

`set(index, value)`：

- 替换已有元素；
- 返回旧值；
- 不改变 size；
- 在当前 OpenJDK 中通常不属于结构性修改，因此既有迭代器一般不会仅因 set 失效。

set 不会自动调用元素的修改方法，它只是改变列表中保存的引用。

---

## 02.12 remove(index)

按索引删除：

```text
读取旧元素
↓
把 (index, size) 范围左移一位
↓
size--
↓
把原尾部槽位设为 null
```

尾部置 null 很重要，它解除 ArrayList 对已删除对象的引用，允许对象在无其他强引用时被 GC。

删除最后一个元素不需要搬移；删除靠前元素需要移动更多引用。

---

## 02.13 remove(Object)

`remove(Object target)` 删除遇到顺序中的第一个相等元素：

- target 为 null 时查找第一个 null；
- 否则通过 `equals()` 查找；
- 找到后执行位置删除；
- 返回是否删除成功。

因此其查找通常 O(n)，删除还可能搬移尾部元素。

如果列表保存的元素 `equals()` 设计不稳定，remove、contains、indexOf 都会表现异常。

---

## 02.14 整数列表的 remove 重载陷阱

```java
List<Integer> values =
        new ArrayList<>(List.of(10, 20, 30));

values.remove(1);
```

调用的是：

```java
remove(int index)
```

结果删除索引 1 的元素 20。

要按值删除：

```java
values.remove(Integer.valueOf(1));
```

或：

```java
values.removeIf(value -> value == 1);
```

这是自动装箱和重载共同造成的高频陷阱。

---

## 02.15 contains 与 indexOf

`contains(target)` 通常基于 `indexOf(target) >= 0`。

线性列表没有哈希索引，因此：

```text
contains
indexOf
lastIndexOf
remove(Object)
```

通常都需要顺序扫描，复杂度 O(n)。

ArrayList 适合按位置访问，不代表按值查找也为 O(1)。频繁按键查找应考虑 Map，频繁成员判断应考虑 Set，但必须先定义相等性和顺序需求。

---

## 02.16 扩容触发条件

当新增后的最小容量超过当前数组长度时，需要扩容。

概念上：

```text
minCapacity = size + incomingCount
if minCapacity > currentCapacity:
    allocate larger array
    copy existing references
```

单元素 add、批量 addAll、ensureCapacity 都可能触发容量增长。

扩容复制的是引用槽位，不会复制或克隆每个元素对象。

---

## 02.17 扩容倍率不是 API 规范

当前 OpenJDK 常见增长策略近似为：

```text
newCapacity = oldCapacity + oldCapacity / 2
```

也就是约 1.5 倍，并确保不小于所需最小容量。

但 API 只承诺容量会自动增长，并明确增长策略细节未被指定。因此：

- 1.5 倍适合用于理解当前源码；
- 不应写成所有 JVM、所有版本永久保证；
- 业务代码不能依赖某次扩容后的准确 capacity；
- 性能基准应记录 JDK 发行版和版本。

---

## 02.18 数组复制与浅层语义

扩容、构造复制、clone 等操作复制的是元素引用：

```text
旧数组 [refA, refB]
             ↓ 复制引用值
新数组 [refA, refB, null]
```

两个数组中的引用仍指向同一元素对象。

这就是浅层复制：

- 列表结构独立；
- 元素对象共享；
- 修改某一列表的元素位置不影响另一列表结构；
- 修改共享元素对象的内部状态会被双方观察到。

---

## 02.19 ensureCapacity

当预先知道大致元素数量时：

```java
ArrayList<Order> values = new ArrayList<>();
values.ensureCapacity(expectedCount);
```

它可以减少逐步增长造成的数组分配和复制。

适合：

- 批量导入已知行数；
- 根据响应头预知数量；
- 聚合结果上限较明确；
- 热点路径经基准验证存在扩容成本。

不适合盲目把容量设得很大，否则会增加未使用内存和长生命周期保留。

---

## 02.20 初始容量规划

最直接的方式：

```java
List<Order> values = new ArrayList<>(expectedCount);
```

容量规划原则：

- 估算可靠且列表确实会装满：预设容量；
- 数量波动大：保守估算，避免极端过配；
- 短命小列表：默认构造通常足够；
- 长命大列表：关注峰值后容量是否长期保留；
- 不要把“减少扩容次数”当作唯一目标，内存占用同样重要。

---

## 02.21 trimToSize

```java
list.trimToSize();
```

尝试把内部容量缩小到当前 size。

它可能：

- 分配更小数组；
- 复制现有元素引用；
- 降低长期闲置容量；
- 增加一次 CPU 和分配成本；
- 影响之后再次增长。

适合完成一次性构建后长期只读的大列表。对频繁增长和收缩的列表反复 trim 可能造成抖动，通常不建议。

---

## 02.22 clear 的真实效果

`clear()` 通常：

- 把有效槽位设为 null；
- size 设为 0；
- 保留内部数组容量。

因此它释放元素引用，但通常不立即释放大数组。

如果一个长生命周期对象曾持有百万级 ArrayList，clear 后容量仍可能长期占用。可根据场景：

- 复用容量；
- trimToSize；
- 用新小列表替换旧列表；
- 缩短整个容器生命周期。

选择取决于是否预计很快重新填充。

---

## 02.23 addAll 批量追加

```java
list.addAll(source);
list.addAll(index, source);
```

批量添加通常先获取源数据数组或迭代内容，确保一次满足所需容量，再执行连续复制。

优势：

- 相比逐个 add 可能减少边界检查和扩容；
- 保留源集合遇到顺序；
- 仍是浅层引用复制；
- 在指定位置插入时仍要搬移原尾部区间。

若 source 与目标存在特殊视图或自引用关系，应查看契约并避免未定义行为。

---

## 02.24 removeAll、retainAll 与批量删除

这些操作会按成员关系批量筛选：

```java
removeAll(other);
retainAll(other);
```

复杂度不仅由当前 ArrayList 大小决定，还取决于 `other.contains()` 的复杂度：

```text
other 是 HashSet
→ 成员测试平均较快

other 是 ArrayList
→ 每次 contains 可能线性扫描
```

因此大量批量差集或交集操作时，先把成员判断侧构造成合适 Set 往往更高效。

---

## 02.25 removeIf

```java
list.removeIf(order -> order.cancelled());
```

相比在增强 for 中直接 remove：

- 语义更清晰；
- 由实现协调遍历与结构修改；
- 可能采用批量标记和压缩；
- 谓词应无副作用、稳定且快速。

若谓词抛异常，列表是否已部分修改应查看具体实现契约，业务关键操作不要假设事务性。

---

## 02.26 replaceAll 与 sort

```java
list.replaceAll(String::trim);
list.sort(comparator);
```

`replaceAll` 原位替换每个位置元素；`sort` 原位重排列表。

注意：

- 都会修改列表内容；
- 比较器必须满足契约；
- 排序期间修改元素排序字段或列表结构会产生不可预测结果；
- 排序是稳定的 API 契约时，可依赖相等比较元素保持相对顺序；
- 对不可修改列表调用会抛 `UnsupportedOperationException`。

---

## 02.27 toArray

常见方式：

```java
Object[] raw = list.toArray();
String[] typed = list.toArray(String[]::new);
```

Java 8 常用：

```java
String[] typed = list.toArray(new String[0]);
```

边界：

- 返回新数组，结构与列表独立；
- 数组元素仍是共享引用；
- 生成器重载需要较新 Java 版本；
- 不应依赖传入“大数组复用”取得微优化，先以清晰和基准为准。

---

## 02.28 clone 的浅克隆

ArrayList 实现 Cloneable：

```java
@SuppressWarnings("unchecked")
ArrayList<Order> copy =
        (ArrayList<Order>) source.clone();
```

结果：

- ArrayList 对象独立；
- 内部数组结构独立；
- 元素引用共享；
- 返回类型在历史 API 中为 Object，需要转换；
- 不会把元素深克隆。

现代业务 API 通常更推荐：

```java
new ArrayList<>(source)
```

意图更清晰，也不把 clone 机制扩散到领域类型。

---

## 02.29 四种复制方式对比

| 方式 | 结构独立 | 可修改 | 元素深拷贝 | 典型用途 |
|---|---|---|---|---|
| `new ArrayList<>(source)` | 是 | 是 | 否 | 建立可变副本 |
| `source.clone()` | 是 | 是 | 否 | ArrayList 历史浅克隆 |
| `List.copyOf(source)` | 通常是快照语义 | 否 | 否 | 不可修改快照 |
| `Collections.unmodifiableList(source)` | 否，包装视图 | 通过包装否 | 否 | 限制当前访问路径 |

“通常是快照语义”强调调用方不应依赖是否复用已经不可修改的输入实例，只依赖结果不受源列表后续结构修改影响的契约。

---

## 02.30 subList 范围视图

```java
List<E> view = list.subList(from, to);
```

范围是：

```text
[from, to)
```

它通常由原列表支撑：

- view 的 set/remove/clear 影响原列表；
- 原列表通过非 view 路径结构性修改后，view 后续语义可能未定义并常抛异常；
- view 可能保留整个原列表引用；
- 适合局部批量操作；
- 需要独立结果时使用 `new ArrayList<>(view)`。

---

## 02.31 subList 的典型用途与风险

高价值用途：

```java
list.subList(from, to).clear();
```

可一次删除连续区间。

风险：

- 长期缓存很小 view 却保留大列表；
- 原列表和 view 在不同层级被修改；
- 把 view 作为独立分页结果返回；
- 并发访问时没有同步；
- 调用方不知道它是实时视图。

分页 API 通常返回独立快照或明确的分页对象，而不是直接暴露 subList。

---

## 02.32 modCount 与 fail-fast

OpenJDK 的 AbstractList/ArrayList 使用类似 `modCount` 的修改计数支持 fail-fast。

迭代器创建时记录预期计数；迭代过程中若发现不一致，通常抛：

```text
ConcurrentModificationException
```

这是实现机制，不是内存同步：

- 字段通常不是 volatile；
- 数据竞争下不保证及时观察；
- 检查可能在 next、remove、forEachRemaining 等位置发生；
- 不能把异常当作锁。

---

## 02.33 Iterator 与 ListIterator

Iterator 适合单向遍历和删除当前元素。

`ListIterator` 额外支持：

- 向前和向后遍历；
- 获取前后索引；
- `set()` 替换当前元素；
- `add()` 在迭代位置插入。

正确使用其修改方法，迭代器会同步自身预期修改计数。

不要同时混用列表直接结构修改和迭代器修改，除非 API 明确允许。

---

## 02.34 forEach、增强 for 与索引循环

ArrayList 常见遍历：

```java
for (E value : list) { }
list.forEach(value -> { });
for (int i = 0; i < list.size(); i++) { }
```

选择：

- 普通读取：增强 for 清晰；
- 需要索引：索引循环；
- 需要迭代器删除：显式 Iterator；
- 函数式副作用：谨慎使用 forEach；
- 热点代码性能差异应通过基准验证。

不要把 Stream 或 lambda 自动等同于更快。

---

## 02.35 RandomAccess 标记

ArrayList 实现 `RandomAccess`，这是无方法的标记接口，用于提示按索引访问通常高效。

通用算法可选择策略：

```java
if (list instanceof RandomAccess) {
    // 索引方式
} else {
    // 迭代器方式
}
```

标记不提供严格复杂度保证，但表达实现意图。

业务代码通常无需手工判断；编写通用集合算法时才更有价值。

---

## 02.36 时间复杂度与真实成本

| 操作 | 典型复杂度 | 主要成本 |
|---|---|---|
| `get/set` | O(1) | 边界检查和数组槽位访问 |
| 尾部 `add` | 均摊 O(1) | 偶尔扩容复制 |
| `add(index)` | O(n) | 尾部区间右移 |
| `remove(index)` | O(n) | 尾部区间左移 |
| `contains/indexOf` | O(n) | 逐个 equals |
| `clear` | O(n) | 清空有效引用槽位 |
| `sort` | O(n log n) | 比较、重排 |

理论复杂度之外还要考虑：

- 内存连续性和 CPU 缓存；
- 元素对象访问；
- 比较器或 equals 成本；
- 扩容导致的分配和 GC；
- 数据规模和读写比例。

---

## 02.37 为什么通常优先于 LinkedList

在很多业务列表中，ArrayList 更常作为默认选择：

- 按索引访问高效；
- 连续引用数组具有更好缓存局部性；
- 每个元素没有额外节点对象；
- 尾部追加均摊高效；
- 批量数组复制由底层优化；
- 实际“中间删除”往往仍需先查找位置。

LinkedList 只有在能够直接持有节点位置、频繁两端操作且经过验证时才可能合适；队列和栈场景通常又有 ArrayDeque 更优先。

---

## 02.38 内存与生命周期治理

ArrayList 的内存由两部分组成：

- 列表对象及其字段；
- 内部引用数组。

元素对象本身在列表外分配，列表保存引用。

风险：

- 预分配过大；
- 峰值后 clear 但数组长期保留；
- subList 保留大列表；
- 静态集合无限增长；
- 缓存元素对象图过大；
- 装箱值导致额外对象压力。

治理需要结合生命周期、上限、监控和容量策略，而不是只调用 trim。

---

## 02.39 并发访问边界

ArrayList 不是线程安全的。

并发读写可能造成：

- 数据竞争；
- 元素丢失或覆盖；
- 越界或状态不一致；
- 迭代异常；
- 读取陈旧值。

选择：

- 构建后安全发布的不可修改快照；
- 外部锁保护；
- `Collections.synchronizedList`；
- 读多写少时评估 CopyOnWriteArrayList；
- 重新设计所有权，避免共享。

具体并发实现放在 `1.2-并发编程`。

---

## 02.40 synchronizedList 的使用边界

```java
List<E> safe =
        Collections.synchronizedList(new ArrayList<>());
```

包装器同步单个方法，但：

```java
if (!safe.contains(value)) {
    safe.add(value);
}
```

不是自动原子操作。

遍历通常需要：

```java
synchronized (safe) {
    for (E value : safe) {
        // ...
    }
}
```

锁对象必须是包装后的 list，而不是原始 backing list。

---

## 02.41 Java 21 首尾操作

ArrayList 在 Java 21 的 List/SequencedCollection 体系中可使用：

```java
list.getFirst();
list.getLast();
list.addFirst(value);
list.addLast(value);
list.removeFirst();
list.removeLast();
```

复杂度要结合 ArrayList 结构：

- getFirst/getLast 通常 O(1)；
- addLast 均摊 O(1)；
- addFirst 需要移动现有元素，通常 O(n)；
- removeLast 通常 O(1)；
- removeFirst 需要左移，通常 O(n)。

统一 API 不会改变底层数据结构的成本。

---

## 02.42 reversed 反向视图

Java 21：

```java
List<E> reversed = list.reversed();
```

它是反向顺序视图：

- `reversed.get(0)` 对应原列表最后元素；
- 修改视图通常回写原列表；
- 对反向视图再 reversed 可回到正向视角；
- 不是一次性复制和反转；
- 结构修改规则和并发边界仍需遵守。

需要独立反向副本时：

```java
List<E> copy = new ArrayList<>(list.reversed());
```

---

## 02.43 序列化与跨边界传输

ArrayList 实现 Serializable，不代表适合把 Java 默认序列化作为长期协议。

风险：

- 类版本和实现演进；
- 安全问题；
- 语言和平台耦合；
- 元素必须可序列化；
- 数据格式不可读；
- 反序列化攻击面。

数据库、消息和 HTTP 边界应使用明确 DTO 与稳定协议。ArrayList 只是运行时容器，不应成为持久化协议本身。

---

## 02.44 WMS 批量扫描场景

条码扫描批次：

```java
List<Barcode> scanned =
        new ArrayList<>(expectedCount);
```

适合原因：

- 扫描顺序有意义；
- 主要是尾部追加；
- 后续可能按索引定位异常项；
- 批量提交可直接遍历；
- 容量上限可从波次任务估算。

若还需去重：

```text
ArrayList 保留全部扫描事件
LinkedHashSet 保存首次出现的唯一条码
Map 统计重复次数
```

不要为了去重直接把所有数据改成 HashSet，导致扫描顺序和重复审计信息丢失。

---

## 02.45 性能坏味道

典型坏味道：

- 循环中不断在索引 0 插入；
- 对大 ArrayList 反复 `contains` 做集合成员判断；
- 不知道数量却预分配极大容量；
- 每次删除后 trim；
- 把 subList 长期缓存；
- 在多线程下共享并依赖 fail-fast；
- 用 ArrayList 模拟高频队首队列；
- 为性能改成 LinkedList 却没有基准；
- 按值删除 Integer 时误调用索引重载；
- 依赖 1.5 倍扩容作为业务逻辑。

---

## 02.46 源码阅读清单

阅读 OpenJDK ArrayList 源码时建议按路径：

1. 字段与空数组常量；
2. 三个构造器；
3. add 与扩容入口；
4. grow 和 Arrays.copyOf；
5. fastRemove 与 shiftTailOverGap；
6. get、set、indexOf；
7. clear、removeAll、removeIf；
8. Itr、ListItr 与 modCount；
9. SubList；
10. reversed 相关默认或视图实现。

每个结论注明：

```text
API 规范保证
或
当前 OpenJDK 实现
```

---

## 02.47 本章决策清单

1. 是否真的需要按位置顺序？
2. 主要是尾部追加还是头部/中间插入？
3. 是否频繁按值查询，应该增加 Set/Map 索引？
4. 元素数量能否可靠估算？
5. 是否需要独立可变副本、不可修改快照或视图？
6. 是否可能长期保留大容量或 subList？
7. 是否跨线程共享？
8. Integer 删除是按索引还是按值？
9. Java 版本是否支持首尾与 reversed API？
10. 当前性能结论是否经过代表性基准验证？

---

## 02.48 可运行实验

以下实验均为独立 Java 文件。示例以 Java 21 为编译基线；涉及更高版本 API 时会明确标注。

### 实验1：size 与逻辑元素数量



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListSizeDemo {
    public static void main(String[] args) {
        List<String> values = new ArrayList<>(100);
        System.out.println(values.size());
        values.add("A");
        System.out.println(values.size());
    }
}
```

预期输出：

```text
0
1
```

### 实验2：尾部追加保持顺序



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListAppendDemo {
    public static void main(String[] args) {
        List<String> values = new ArrayList<>();
        values.add("A");
        values.add("B");
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, B]
```

### 实验3：中间插入移动尾部



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListIndexedAddDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "C"));
        values.add(1, "B");
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, B, C]
```

### 实验4：set 返回旧值



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListSetDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B"));
        String old = values.set(1, "X");
        System.out.println(old);
        System.out.println(values);
    }
}
```

预期输出：

```text
B
[A, X]
```

### 实验5：按索引删除



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListRemoveIndexDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B", "C"));
        String removed = values.remove(1);
        System.out.println(removed);
        System.out.println(values);
    }
}
```

预期输出：

```text
B
[A, C]
```

### 实验6：按对象删除第一个相等元素



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListRemoveObjectDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B", "A"));
        values.remove("A");
        System.out.println(values);
    }
}
```

预期输出：

```text
[B, A]
```

### 实验7：Integer remove 重载差异



```java
import java.util.ArrayList;
import java.util.List;

public class IntegerRemoveOverloadDemo {
    public static void main(String[] args) {
        List<Integer> values =
                new ArrayList<>(List.of(10, 20, 30));
        values.remove(1);
        values.remove(Integer.valueOf(30));
        System.out.println(values);
    }
}
```

预期输出：

```text
[10]
```

### 实验8：contains 基于 equals



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListContainsDemo {
    record Sku(String code) {}

    public static void main(String[] args) {
        List<Sku> values = new ArrayList<>();
        values.add(new Sku("A"));
        System.out.println(values.contains(new Sku("A")));
    }
}
```

预期输出：

```text
true
```

### 实验9：ensureCapacity 后正常追加



```java
import java.util.ArrayList;

public class EnsureCapacityDemo {
    public static void main(String[] args) {
        ArrayList<Integer> values = new ArrayList<>();
        values.ensureCapacity(100);
        for (int i = 0; i < 5; i++) {
            values.add(i);
        }
        System.out.println(values);
    }
}
```

预期输出：

```text
[0, 1, 2, 3, 4]
```

### 实验10：clear 解除逻辑元素



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListClearDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B"));
        values.clear();
        System.out.println(values.size());
        System.out.println(values.isEmpty());
    }
}
```

预期输出：

```text
0
true
```

### 实验11：批量 addAll



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListAddAllDemo {
    public static void main(String[] args) {
        List<Number> values = new ArrayList<>();
        values.addAll(List.of(1, 2, 3));
        System.out.println(values);
    }
}
```

预期输出：

```text
[1, 2, 3]
```

### 实验12：removeAll 使用成员集合



```java
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArrayListRemoveAllDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B", "C", "B"));
        values.removeAll(Set.of("B", "C"));
        System.out.println(values);
    }
}
```

预期输出：

```text
[A]
```

### 实验13：removeIf 安全批量删除



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListRemoveIfDemo {
    public static void main(String[] args) {
        List<Integer> values =
                new ArrayList<>(List.of(1, 2, 3, 4));
        values.removeIf(value -> value % 2 == 0);
        System.out.println(values);
    }
}
```

预期输出：

```text
[1, 3]
```

### 实验14：replaceAll 与 sort



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListReplaceSortDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of(" c ", "a", " b"));
        values.replaceAll(String::trim);
        values.sort(String::compareTo);
        System.out.println(values);
    }
}
```

预期输出：

```text
[a, b, c]
```

### 实验15：拷贝构造是浅复制



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListShallowCopyDemo {
    static final class Item {
        String name;
        Item(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        Item item = new Item("A");
        List<Item> source = new ArrayList<>(List.of(item));
        List<Item> copy = new ArrayList<>(source);
        copy.get(0).name = "X";
        System.out.println(source.get(0).name);
    }
}
```

预期输出：

```text
X
```

### 实验16：clone 结构独立元素共享



```java
import java.util.ArrayList;

public class ArrayListCloneDemo {
    public static void main(String[] args) {
        ArrayList<StringBuilder> source = new ArrayList<>();
        source.add(new StringBuilder("A"));

        @SuppressWarnings("unchecked")
        ArrayList<StringBuilder> copy =
                (ArrayList<StringBuilder>) source.clone();

        copy.add(new StringBuilder("B"));
        copy.get(0).append("X");

        System.out.println(source.size());
        System.out.println(source.get(0));
    }
}
```

预期输出：

```text
1
AX
```

### 实验17：subList 删除区间回写原列表



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListSubListDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B", "C", "D"));
        values.subList(1, 3).clear();
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, D]
```

### 实验18：ListIterator 双向和修改



```java
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ArrayListListIteratorDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "C"));
        ListIterator<String> iterator = values.listIterator(1);
        iterator.add("B");
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, B, C]
```

### 实验19：Java 21 首尾操作成本语义



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListSequencedDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("B", "C"));
        values.addFirst("A");
        values.addLast("D");
        System.out.println(values.getFirst());
        System.out.println(values.getLast());
        System.out.println(values);
    }
}
```

预期输出：

```text
A
D
[A, B, C, D]
```

### 实验20：Java 21 反向视图回写



```java
import java.util.ArrayList;
import java.util.List;

public class ArrayListReversedDemo {
    public static void main(String[] args) {
        List<String> source =
                new ArrayList<>(List.of("A", "B", "C"));
        List<String> reversed = source.reversed();
        reversed.removeFirst();
        System.out.println(source);
    }
}
```

预期输出：

```text
[A, B]
```

---

## 02.49 高频面试题

1. ArrayList 在类型层次中实现了哪些关键接口？
2. ArrayList 是否允许重复和 null？
3. ArrayList 是否线程安全？
4. RandomAccess 标记表达什么？
5. ArrayList 的核心数据模型是什么？
6. size 与 capacity 有什么区别？
7. capacity 是否有公开 getter？
8. 扩容是否改变 size？
9. 无参构造器是否立即分配长度 10 的数组？
10. 默认初始容量 10 应如何准确表述？
11. 无参构造与显式容量 0 在 OpenJDK 中可能有什么差异？
12. ArrayList 有哪些常用构造器？
13. Collection 构造器是否深拷贝元素？
14. 负初始容量会发生什么？
15. 尾部 add 的典型复杂度是什么？
16. 均摊 O(1) 是否表示每次 O(1)？
17. 触发扩容的一次 add 有哪些成本？
18. add 是否保留插入顺序？
19. `add(index,e)` 允许 index 等于 size 吗？
20. 中间插入为什么是 O(n)？
21. 插入头部需要移动哪些元素？
22. get 和 set 的典型复杂度是什么？
23. set 是否改变 size？
24. set 返回什么？
25. set 是否通常属于结构性修改？
26. `remove(index)` 如何处理尾部空槽？
27. 为什么删除后要把原尾部设为 null？
28. 删除最后元素是否需要搬移？
29. `remove(Object)` 删除所有匹配元素吗？
30. remove(Object) 如何处理 null？
31. contains 和 remove(Object) 依赖什么相等性？
32. 可变 equals 字段会带来什么风险？
33. `List<Integer>.remove(1)` 删除什么？
34. 如何按值删除 Integer 1？
35. remove 重载为什么容易与装箱混淆？
36. ArrayList 按值 contains 是否 O(1)？
37. indexOf 和 lastIndexOf 有什么差异？
38. 频繁按值查找为什么可能需要 Set 或 Map？
39. 建立额外索引会带来什么一致性成本？
40. 什么时候触发 ArrayList 扩容？
41. 扩容复制的是元素还是引用？
42. OpenJDK 常见扩容倍率是多少？
43. ensureCapacity 的作用是什么？
44. 何时适合预设初始容量？
45. 预设过大容量有什么风险？
46. 数量不确定时是否应盲目设大容量？
47. trimToSize 做什么？
48. trimToSize 是否一定值得调用？
49. 为什么频繁 trim 可能造成抖动？
50. clear 如何帮助元素被 GC？
51. clear 后大数组为什么可能仍占内存？
52. 何时适合替换成新列表？
53. 何时适合保留容量复用？
54. addAll 是否深拷贝元素？
55. addAll(index,source) 需要移动哪些数据？
56. removeAll 的复杂度为什么受参数集合影响？
57. removeIf 与遍历中直接 remove 有什么区别？
58. replaceAll 是否原位修改？
59. List.sort 是否稳定？
60. 比较器错误会如何影响排序？
61. toArray 返回的数组是否与列表结构联动？
62. 数组元素对象是否共享？
63. clone 是深拷贝还是浅拷贝？
64. 拷贝构造器与 clone 如何选择？
65. List.copyOf 与 new ArrayList 有什么区别？
66. unmodifiableList 与 copyOf 有什么区别？
67. subList 是视图还是副本？
68. 原列表结构修改为什么会让 subList 危险？
69. modCount 的作用是什么？
70. fail-fast 是否是线程安全保证？
71. ListIterator 比 Iterator 多哪些能力？
72. 遍历时应如何安全删除？
73. ArrayList 为什么通常比 LinkedList 更常用？
74. 连续引用数组有什么局部性优势？
75. 理论 O(1) 是否代表实际一定更快？
76. ArrayList 适合高频队首操作吗？
77. Java 21 为 List 增加了哪些首尾操作？
78. ArrayList.addFirst 的复杂度为什么仍是 O(n)？
79. reversed 返回视图还是复制？
80. 面试中如何概括 ArrayList 的核心取舍？

---

## 02.50 易错点

### 误区1：ArrayList 底层永远在构造时创建长度 10 的数组

**错误。** 现代 OpenJDK 无参构造通常延迟到首次添加才分配默认容量。

### 误区2：默认容量 10 是所有 JVM 的永久内部实现

**错误。** API 关注自动增长，精确分配和增长策略可能变化。

### 误区3：size 就是内部数组长度

**错误。** size 是逻辑元素数量，数组长度是 capacity。

### 误区4：指定初始容量 100 后 size 就是 100

**错误。** 列表仍为空，size 为 0。

### 误区5：capacity 是列表最大容量

**错误。** 容量可以继续自动增长。

### 误区6：ArrayList 每次 add 都是严格 O(1)

**错误。** 触发扩容时需要分配和复制，整体是均摊 O(1)。

### 误区7：ArrayList 扩容一定严格为 1.5 倍

**错误。** 这是常见 OpenJDK 实现策略，不是 API 保证。

### 误区8：扩容会克隆每个元素对象

**错误。** 只复制数组中的引用。

### 误区9：中间插入只修改一个数组槽位

**错误。** 需要把尾部连续区间右移。

### 误区10：删除元素后旧引用会自然立即消失

**错误。** 实现需要清空尾部槽位解除强引用。

### 误区11：remove(Object) 会删除所有相等元素

**错误。** 它通常只删除第一个匹配元素。

### 误区12：`List<Integer>.remove(1)` 按值删除 1

**错误。** 它优先匹配 int 索引重载。

### 误区13：ArrayList 的 contains 是 O(1)

**错误。** 它通常线性调用 equals。

### 误区14：ArrayList 查询快包括所有查询

**错误。** 按索引快，按值查找仍是 O(n)。

### 误区15：set 会改变列表大小

**错误。** 它只替换已有位置元素。

### 误区16：set 一定使所有迭代器失效

**错误。** 当前 OpenJDK 中通常不是结构性修改，但不要把细节扩大为其他实现保证。

### 误区17：clear 会释放内部数组

**错误。** 它通常清空元素引用并保留容量。

### 误区18：trimToSize 应在每次删除后调用

**错误。** 频繁缩容和再扩容可能增加分配与复制。

### 误区19：ensureCapacity 越大越好

**错误。** 过配会浪费内存并延长大数组生命周期。

### 误区20：初始容量越精确程序越快

**错误。** 收益取决于规模和热点，过早优化可能得不偿失。

### 误区21：addAll 会深拷贝源元素

**错误。** 它复制元素引用。

### 误区22：removeAll 只看当前列表大小

**错误。** 参数集合 contains 的复杂度也会影响总成本。

### 误区23：removeIf 一定具有事务原子性

**错误。** 谓词异常和实现细节下不能假设业务事务语义。

### 误区24：sort 创建新列表

**错误。** List.sort 原位重排。

### 误区25：clone 会深克隆元素

**错误。** ArrayList clone 是浅克隆。

### 误区26：clone 比拷贝构造器更现代

**错误。** 业务代码通常更推荐显式拷贝构造语义。

### 误区27：List.copyOf 返回可变 ArrayList

**错误。** 它返回不可修改结果，实现类型未承诺。

### 误区28：unmodifiableList 会隔离原列表变化

**错误。** 它通常是包装视图。

### 误区29：subList 是独立的小列表

**错误。** 它通常由原列表支撑。

### 误区30：小 subList 不会保留大列表

**错误。** 视图可能通过支撑链保留原列表。

### 误区31：fail-fast 能保证多线程一定抛异常

**错误。** 它是尽力检测，不是同步机制。

### 误区32：没有 ConcurrentModificationException 就线程安全

**错误。** 数据竞争可能未被检测。

### 误区33：RandomAccess 保证严格 O(1)

**错误。** 它是实现意图标记，不是数学复杂度合同。

### 误区34：索引 for 永远比 Iterator 快

**错误。** 需要结合实现、JIT、代码清晰度和基准。

### 误区35：ArrayList 中间删除一定比 LinkedList 慢

**错误。** LinkedList 还可能有定位、节点和缓存成本，需具体比较。

### 误区36：LinkedList 是所有频繁增删场景首选

**错误。** 只有特定访问模式下才可能合适。

### 误区37：ArrayList 适合作为高频队首队列

**错误。** 队首删除需要搬移，通常优先 ArrayDeque。

### 误区38：synchronizedList 让复合操作自动原子

**错误。** 多步操作仍需外部同步。

### 误区39：Java 21 addFirst 会神奇变成 O(1)

**错误。** 统一接口不改变 ArrayList 的数组搬移成本。

### 误区40：reversed 会复制并倒序所有元素

**错误。** 它通常是反向视图。

---

## 02.51 工程实践建议

1. 普通有序业务列表默认优先评估 ArrayList。
2. 已知预计数量时通过构造器或 ensureCapacity 合理预分配。
3. 预估不可靠时保守配置，避免大规模未使用容量。
4. 在文档中区分 size 与 capacity，不把容量暴露为业务含义。
5. 把默认容量和 1.5 倍增长明确标为当前 OpenJDK 实现细节。
6. 热点容量优化必须记录 JDK 版本并用基准验证。
7. 主要尾部追加时使用 add(E)，避免无意义的 add(size,E)。
8. 高频头部或两端操作优先评估 ArrayDeque。
9. 中间插入删除前同时考虑定位和搬移成本。
10. 频繁按值 contains 时评估维护 Set 或 Map 索引。
11. 额外索引必须与 List 修改保持一致，避免双结构漂移。
12. Integer 列表删除值时显式使用 Integer.valueOf 或 removeIf。
13. 集合元素的 equals 必须稳定并符合业务相等性。
14. 批量差集和交集操作让成员测试侧使用合适 Set。
15. 优先使用 addAll 完成已知批量追加，减少重复扩容机会。
16. 遍历删除使用 Iterator.remove 或 removeIf。
17. 不要在增强 for 中直接结构修改 ArrayList。
18. 不要依赖 fail-fast 保证并发正确性。
19. 跨线程共享时选择安全发布快照、外部锁或并发实现。
20. 使用 synchronizedList 时同步复合操作和遍历。
21. clear 后根据是否复用容量决定保留、trim 或替换列表。
22. 长生命周期大列表监控峰值容量和对象保留。
23. 避免反复 trimToSize 导致缩扩容抖动。
24. 使用 subList 完成短期范围操作后尽快释放。
25. 跨层返回分页结果时复制 subList，不暴露支撑视图。
26. 使用 new ArrayList<>(source) 表达独立可变浅副本。
27. 使用 List.copyOf(source) 表达不可修改浅快照。
28. 使用 unmodifiableList 前确认是否需要反映原列表变化。
29. 不要把任何浅层复制描述成元素深拷贝。
30. toArray 时选择与项目 Java 基线匹配的重载。
31. 排序前保证比较器传递、稳定且不依赖正在变化的字段。
32. replaceAll 和 removeIf 的函数保持无副作用并处理异常。
33. 大规模装箱数值列表评估数组或专用结构。
34. 不要依赖 ArrayList 默认序列化作为跨服务长期协议。
35. Java 21 首尾方法使用时注明 addFirst/removeFirst 的线性成本。
36. reversed 需要独立结果时再复制为新 ArrayList。
37. 代码评审中搜索循环头插、重复 contains 和长期 subList。
38. 源码阅读结论始终标注规范保证或实现细节。
39. 性能比较 ArrayList 与 LinkedList 时使用真实数据分布和操作比例。
40. 领域集合对外封装所有权、顺序、可变性和最大规模。

---

## 02.52 本章小结

- ArrayList 是动态引用数组，size 与内部 capacity 是两个概念。
- 尾部追加均摊 O(1)，中间插入删除需要连续区间搬移。
- 扩容复制引用，约 1.5 倍是常见 OpenJDK 实现而非 API 保证。
- clear 通常保留容量，ensureCapacity 和 trimToSize 需要基于生命周期使用。
- clone、拷贝构造和 copyOf 都不会自动深拷贝元素。
- subList 与 reversed 通常是视图，不是独立副本。
- fail-fast 不提供线程安全；ArrayList 跨线程需要额外策略。
- ArrayList 通常凭借随机访问、连续存储和低节点开销成为普通 List 首选。

---

## 02.53 面试口述版

ArrayList 是基于可调整大小引用数组的 List 实现，逻辑元素数量由 size 表示，内部数组长度是 capacity。按索引 get 和 set 通常是 O(1)，尾部 add 在均摊意义上是 O(1)，但触发扩容时要分配新数组并复制已有引用；中间插入和删除需要搬移尾部区间，所以通常是 O(n)。现代 OpenJDK 无参构造一般延迟到首次写入才分配默认容量，常见增长策略约为 1.5 倍，但这些都是实现细节，不是 API 永久保证。clear 通常释放元素引用但保留数组容量。拷贝构造、clone 和 List.copyOf 都是浅层元素语义，只是可修改性和结构隔离不同。subList 和 Java 21 的 reversed 通常是支撑视图。ArrayList 非线程安全，fail-fast 只能尽力发现错误修改。实际业务中，因为随机访问、连续引用存储和较低对象开销，ArrayList 通常比 LinkedList 更适合作为普通 List 默认实现。

---

## 02.54 参考资料

- Java SE 25 API：ArrayList。
- Java SE 25 API：List、RandomAccess、Iterator、ListIterator。
- Java SE 25 API：Collections.synchronizedList、List.copyOf。
- OpenJDK java.base 源码：java.util.ArrayList（实现细节需绑定版本）。
- JEP 431：Sequenced Collections（Java 21）。
