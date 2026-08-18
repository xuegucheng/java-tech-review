# 集合框架体系与核心契约

> 本章从接口契约和数据语义出发建立集合框架全景，先回答“应该使用什么抽象”，再进入具体数据结构。

---

## 01.1 本章定位

集合框架不是若干容器类的清单，而是一套由接口、实现、算法和基础设施共同组成的统一架构。本章先建立抽象契约，再进入具体实现，避免只会背 `ArrayList`、`HashMap` 的局部结论。

学完本章，应能够准确回答：

- `Collection`、`Collections` 和 Collections Framework 有什么区别？
- 为什么 `Map` 不继承 `Collection`？
- `List`、`Set`、`Queue`、`Deque` 分别承诺什么语义？
- “有序”“排序”“遇到顺序”有什么区别？
- 可选操作为什么可能抛 `UnsupportedOperationException`？
- 接口复杂度是否能够代表所有实现？
- 什么是集合视图，为什么视图不是副本？
- 不可修改集合、不可变集合和只读引用有什么差异？
- fail-fast 是否是线程安全保证？
- 集合 API 的元素相等性由什么决定？
- Java 21 的 Sequenced Collections 解决了什么问题？
- 普通集合与并发集合的边界在哪里？

本章不深入各实现的内部算法；`ArrayList` 从下一章开始单独分析。

---

## 01.2 学习主线

```text
集合框架的目标
↓
核心接口层次
↓
List / Set / Queue / Deque / Map 契约
↓
实现类与抽象骨架
↓
顺序、重复、null、相等性
↓
迭代、结构性修改与 fail-fast
↓
批量算法与集合视图
↓
可修改性、不可变性与线程安全
↓
现代 Sequenced API
↓
工程选型
```

面对集合需求时，先确定语义，再选择实现：

1. 是一组元素，还是键值映射？
2. 是否允许重复？
3. 是否需要按位置访问、排序、插入顺序或访问顺序？
4. 主要操作是查找、追加、两端操作还是优先级出队？
5. 是否允许 `null`？
6. 是否需要线程安全、不可变快照或实时视图？

---

## 01.3 集合框架是什么

Java Collections Framework 由四部分构成：

| 组成 | 作用 |
|---|---|
| 核心接口 | 定义集合抽象与操作契约 |
| 通用实现 | 提供可直接使用的数据结构 |
| 算法 | 对集合执行排序、查找、复制等操作 |
| 基础设施 | 迭代器、比较器、包装器、视图等 |

它的价值在于：

- 统一不同数据结构的使用方式；
- 让算法面向接口工作；
- 降低自定义容器和算法的重复实现；
- 让 API 能以抽象类型传递数据；
- 在实现之间保留可替换空间。

---

## 01.4 Collection、Collections 与 Framework

三个名称经常混淆：

```text
Collection
→ 核心接口，表示一组元素

Collections
→ 工具类，提供排序、查找、包装等静态方法

Collections Framework
→ 完整集合架构
```

示例：

```java
Collection<String> values = new ArrayList<>();
Collections.sort((List<String>) values);
```

工程代码不应为了调用工具方法随意强转。应让变量类型与实际需要匹配，或使用接口本身提供的默认方法，例如 `List.sort()`。

---

## 01.5 核心接口层次

简化层次：

```text
Iterable<E>
└── Collection<E>
    ├── List<E>
    ├── Set<E>
    │   ├── SortedSet<E>
    │   └── NavigableSet<E>
    └── Queue<E>
        └── Deque<E>

Map<K,V>
├── SortedMap<K,V>
└── NavigableMap<K,V>
```

Java 21 起，具有明确遇到顺序的集合进一步接入：

```text
SequencedCollection<E>
SequencedSet<E>
SequencedMap<K,V>
```

具体继承关系应以当前 JDK API 为准。本模块采用 Java 8 作为基础兼容背景，同时明确标注 Java 21+ API。

---

## 01.6 Iterable 与增强 for

`Iterable<E>` 表示对象可以提供迭代器：

```java
public interface Iterable<T> {
    Iterator<T> iterator();
}
```

增强 for：

```java
for (String value : values) {
    System.out.println(value);
}
```

对 `Iterable` 的执行模型近似：

```java
Iterator<String> iterator = values.iterator();
while (iterator.hasNext()) {
    String value = iterator.next();
}
```

数组的增强 for 由编译器以数组索引方式处理，不要求数组实现 `Iterable`。

---

## 01.7 Collection 的最小语义

`Collection<E>` 表示一组元素，定义：

- 大小和空判断；
- 添加、删除、包含判断；
- 迭代；
- 批量操作；
- 数组转换；
- Stream 入口等。

它不承诺：

- 是否允许重复；
- 是否保持顺序；
- 是否支持 `null`；
- 是否支持所有修改；
- 是否线程安全；
- 每个操作的复杂度。

这些由子接口、具体实现和 API 文档共同确定。

---

## 01.8 为什么 Map 不属于 Collection

`Collection<E>` 的基本元素模型是单个 `E`，而 `Map<K,V>` 表达键到值的映射：

```text
Collection<E>
→ 一组元素

Map<K,V>
→ 一组键值关联
```

Map 有三个集合视图：

```java
map.keySet();
map.values();
map.entrySet();
```

这三个视图分别把映射投影为键集合、值集合和条目集合。Map 不继承 Collection 可以避免把“添加一个元素”等语义强行套到键值对结构上。

---

## 01.9 List 契约

`List<E>` 的核心语义：

- 元素具有位置索引；
- 通常具有明确遇到顺序；
- 允许重复元素；
- 可按索引读取、替换、插入和删除；
- `equals()` 通常按顺序逐元素比较。

典型实现：

- `ArrayList`：动态数组；
- `LinkedList`：双向链表，同时实现 Deque；
- 不可修改列表工厂返回的内部实现。

“List 有序”通常指保留列表顺序，不代表元素已经按大小排序。

---

## 01.10 Set 契约

`Set<E>` 表达不包含重复元素的集合。

重复判定取决于实现：

- HashSet 主要依赖 `hashCode()` 和 `equals()`；
- TreeSet 主要依赖自然顺序或 Comparator；
- EnumSet 基于枚举身份和位表示。

Set 的 `equals()` 与元素顺序无关，只要两个 Set 包含相同元素就相等。

业务“去重”不一定只靠 Set：数据库唯一约束、分布式幂等键和业务标识仍需在相应边界实现。

---

## 01.11 Queue 契约

Queue 通常表示等待处理的元素序列，但不同实现可以是 FIFO、优先级或其他顺序。

两组方法：

| 语义 | 抛异常 | 返回特殊值 |
|---|---|---|
| 插入 | `add(e)` | `offer(e)` |
| 删除队首 | `remove()` | `poll()` |
| 查看队首 | `element()` | `peek()` |

在容量受限或空队列属于正常状态时，`offer/poll/peek` 通常更自然。具体实现是否允许 `null` 必须查文档；很多队列禁止 `null`，以免与“无元素”的特殊返回值混淆。

---

## 01.12 Deque 契约

Deque 是双端队列，支持首尾两端插入、删除和查看。

它可以表达：

- FIFO 队列；
- LIFO 栈；
- 滑动窗口；
- 双端工作队列。

现代 Java 中栈通常优先使用：

```java
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
String top = stack.pop();
```

旧的 `Stack` 继承 `Vector`，API 和同步模型都带有历史包袱，不应作为新代码默认选择。

---

## 01.13 Sorted 与 Navigable 接口

排序集合依据自然顺序或 Comparator 维护键或元素顺序：

```text
SortedSet / SortedMap
→ 基本排序与范围视图

NavigableSet / NavigableMap
→ lower、floor、ceiling、higher 等导航能力
```

排序结构最重要的契约是比较器一致性。若 `compare(a,b) == 0`，TreeSet 或 TreeMap 通常把二者视为同一个排序位置，即使 `equals()` 返回 false。

因此参与排序的字段应稳定，比较器应满足反对称、传递和一致性要求。

---

## 01.14 Sequenced Collections

Java 21 引入 Sequenced Collections，统一具有确定遇到顺序的数据结构的首尾和反向操作。

核心能力包括：

```java
getFirst();
getLast();
addFirst(e);
addLast(e);
removeFirst();
removeLast();
reversed();
```

适用接口包括 `SequencedCollection`、`SequencedSet` 和 `SequencedMap`。

关键边界：

- `reversed()` 通常是反向视图，不是复制；
- 某些集合不支持首尾添加，可能抛 `UnsupportedOperationException`；
- Java 8 基线代码不能直接调用这些 Java 21+ 方法；
- 更完整的现代 API 在本模块后续专章展开。

---

## 01.15 接口变量与实现对象

推荐面向满足需求的最小接口编程：

```java
List<String> values = new ArrayList<>();
Map<String, Integer> counts = new HashMap<>();
Deque<String> tasks = new ArrayDeque<>();
```

价值：

- 调用方只依赖必要能力；
- 更容易替换实现；
- 避免无意使用实现类特有方法；
- API 边界更清晰。

但“永远只写接口”不是绝对规则。构造、配置或需要实现特有能力时，可以在局部明确使用实现类型，例如 `LinkedHashMap` 的访问顺序配置。

---

## 01.16 通用实现、专用实现与遗留实现

集合实现可以粗分为：

- 通用实现：ArrayList、HashSet、HashMap、ArrayDeque、TreeMap 等；
- 专用实现：EnumSet、EnumMap、WeakHashMap、IdentityHashMap 等；
- 并发实现：ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue 等；
- 遗留实现：Vector、Stack、Hashtable、Properties 等；
- 不可修改工厂实现：List.of、Set.of、Map.of 等。

选择时不要只看接口名称。专用实现往往有非常不同的相等性、生命周期或并发语义。

---

## 01.17 抽象骨架实现

JDK 提供 `AbstractCollection`、`AbstractList`、`AbstractSet`、`AbstractMap` 等骨架类，帮助自定义集合以少量核心方法获得其他默认行为。

例如只读集合可能只需实现：

```java
iterator();
size();
```

但继承骨架并不自动获得高性能。默认实现可能通过迭代器逐个执行，复杂度取决于你提供的基本操作。

自定义集合应优先确认是否真的需要；很多领域需求用组合现有集合并封装业务 API 更安全。

---

## 01.18 可选操作

集合接口中某些修改操作是可选的。实现不支持时可以抛：

```text
UnsupportedOperationException
```

例如：

```java
List<String> values = List.of("A", "B");
// values.add("C"); // 运行时抛异常
```

这意味着：

```text
静态类型包含 add 方法
≠
运行时实现一定支持 add
```

接口统一了操作词汇，但可修改能力仍需由具体对象契约决定。

---

## 01.19 结构性修改

结构性修改通常指改变集合大小或以可能影响迭代结果的方式改变底层结构，例如添加、删除元素。

仅替换某个位置元素是否算结构性修改，由具体实现定义。以 ArrayList 为例，`set()` 通常不改变 `modCount`，而 `add()`、`remove()` 会改变。

这个概念影响：

- fail-fast 迭代器；
- 子列表视图；
- 遍历期间修改；
- 并发访问的可观察行为。

---

## 01.20 遇到顺序、插入顺序与排序

三种概念：

```text
遇到顺序
→ 遍历和顺序操作观察元素的顺序

插入顺序
→ 常见的一种遇到顺序来源

排序顺序
→ 由自然顺序或 Comparator 决定
```

示例：

- ArrayList：遇到顺序通常等于列表位置顺序；
- LinkedHashSet：通常保持插入顺序；
- LinkedHashMap 可配置访问顺序；
- TreeSet：按排序顺序；
- HashSet：不承诺稳定遇到顺序；
- PriorityQueue：只承诺队首优先，迭代不保证全局排序。

文档必须使用准确术语，不能笼统写“有序”。

---

## 01.21 重复元素与相等性

是否允许重复以及如何判定重复由接口和实现共同决定：

| 结构 | 重复语义 |
|---|---|
| List | 允许重复，位置不同仍可共存 |
| Set | 不允许按其相等性定义重复 |
| Map key | 键唯一，重复 put 通常替换值 |
| Map value | 可以重复 |
| Queue | 通常可重复，具体实现另有约束 |

相等性依赖：

- 哈希结构：`hashCode()` 与 `equals()`；
- 排序结构：`compareTo()` 或 Comparator；
- IdentityHashMap：引用身份 `==`。

对象相等性设计错误会直接破坏集合行为。

---

## 01.22 null 支持不是统一规则

不同实现对 `null` 的支持不同：

- ArrayList、HashMap 等通常允许；
- `List.of`、`Set.of`、`Map.of` 禁止；
- ArrayDeque 禁止；
- TreeSet 是否允许取决于比较器和实现版本，不能依赖；
- ConcurrentHashMap 禁止 null 键和值。

即使实现允许，业务 API 也不一定应接受 null。null 策略应在领域边界明确，避免把“实现能存”误当成“业务应该存”。

---

## 01.23 集合相等性契约

常见规则：

```text
List.equals
→ 顺序和逐元素相等

Set.equals
→ 与顺序无关，元素集合相同

Map.equals
→ 键到值的映射相同
```

因此：

```java
List.of("A", "B").equals(List.of("B", "A")) // false
Set.of("A", "B").equals(Set.of("B", "A"))   // true
```

不同接口语义的集合通常不应被认为相等。自定义集合实现必须遵守对应接口契约，否则会破坏替换性。

---

## 01.24 复杂度属于实现而非接口

不能说“List 查询是 O(1)”：

- ArrayList 按索引通常 O(1)；
- LinkedList 按索引需要遍历，通常 O(n)。

也不能说“Map 查询永远 O(1)”：

- HashMap 平均查找接近常数，但受哈希质量、容量和碰撞影响；
- TreeMap 通常 O(log n)；
- 某些特殊 Map 语义完全不同。

复杂度表必须写清：

- 具体实现；
- 平均还是最坏；
- 操作类型；
- 前置条件；
- 是否忽略扩容、比较器和缓存局部性成本。

---

## 01.25 Iterator 契约

Iterator 的核心方法：

```java
boolean hasNext();
E next();
default void remove();
```

正确模式：

```java
Iterator<String> iterator = values.iterator();
while (iterator.hasNext()) {
    String value = iterator.next();
    if (value.isBlank()) {
        iterator.remove();
    }
}
```

不要在增强 for 中直接调用集合的 `remove()`：

```java
for (String value : values) {
    // values.remove(value); // 常触发并发修改异常
}
```

完整迭代器、ListIterator 和 Spliterator 在后续专章展开。

---

## 01.26 fail-fast 的真实含义

许多普通集合迭代器会尽力检测迭代期间的意外结构性修改，并抛出 `ConcurrentModificationException`。

必须明确：

- fail-fast 是错误检测机制，不是并发控制；
- 不保证在所有竞争条件下都能检测；
- 不能依赖该异常保证业务正确性；
- 单线程错误修改也会触发；
- 并发集合可能采用弱一致性等不同策略。

需要线程安全时应使用正确同步、快照或并发集合，而不是依赖异常。

---

## 01.27 批量操作

Collection 提供：

```java
addAll
removeAll
retainAll
containsAll
removeIf
```

批量操作的语义和复杂度取决于双方实现。例如 `removeAll` 在不同集合组合下可能采用不同遍历策略。

注意：

- 操作可能部分完成后抛异常；
- 自引用集合可能产生未定义或异常行为；
- 参数集合是否为视图会影响结果；
- 需要原子性时普通集合批量操作通常不提供跨线程事务保证。

---

## 01.28 集合视图

视图是对现有数据结构的另一种观察或操作窗口，不复制全部元素。

典型视图：

```java
map.keySet();
map.values();
map.entrySet();
list.subList(from, to);
sequenced.reversed();
```

特点可能包括：

- 修改视图影响原集合；
- 修改原集合反映到视图；
- 视图只支持部分操作；
- 原集合结构变化可能让视图失效；
- 视图生命周期可能意外保留大对象。

API 必须明确返回的是视图还是快照。

---

## 01.29 Map 的三个视图

```java
Set<K> keys = map.keySet();
Collection<V> values = map.values();
Set<Map.Entry<K,V>> entries = map.entrySet();
```

它们通常由 Map 支撑：

- 从 `keySet` 删除键会删除映射；
- 从 `values` 删除某值会删除一个对应条目；
- `entrySet` 可用于高效遍历键值；
- 通常不能通过 `keySet.add()` 新增键，因为缺少值；
- `Map.Entry.setValue()` 是否支持取决于实现和迭代上下文。

不要把它们当作独立副本。

---

## 01.30 数组与集合桥接

常见转换：

```java
String[] array = values.toArray(String[]::new);
List<String> list = Arrays.asList(array);
```

边界：

- `Arrays.asList` 返回由数组支撑的定长 List；
- 可以 `set`，但不能改变大小；
- 修改数组与列表元素会互相反映；
- 基本类型数组传给 `Arrays.asList` 会成为单个元素；
- 需要独立可变列表时使用 `new ArrayList<>(Arrays.asList(array))`。

不同 JDK 版本的 `toArray` 重载要标注基线。

---

## 01.31 不可修改、不可变与只读引用

三种概念：

```text
不可修改对象
→ 当前 API 不支持修改操作

不可变对象
→ 状态本身不会改变，通常还要求元素图满足相应约束

只读引用
→ 当前变量类型或封装不暴露修改路径，但对象可能被别处修改
```

例如：

```java
List<String> view =
        Collections.unmodifiableList(mutable);
```

`view` 不能修改，但 `mutable` 改变后 view 会反映变化。

而：

```java
List<String> snapshot = List.copyOf(mutable);
```

通常产生不可修改快照，但仍是浅层的，元素对象本身可能可变。

---

## 01.32 不可修改工厂

Java 9 起：

```java
List.of(...)
Set.of(...)
Map.of(...)
```

特点包括：

- 返回不可修改集合；
- 禁止 null；
- Set 和 Map 工厂拒绝重复元素或键；
- 实现类型未承诺；
- 不应依赖具体类名、序列化形式或内部布局。

`List.copyOf` 等复制工厂适合建立不可修改快照，但不会深拷贝元素。

---

## 01.33 同步包装与并发边界

传统同步包装：

```java
List<String> safe =
        Collections.synchronizedList(new ArrayList<>());
```

单个包装方法调用受同步保护，但复合操作仍需额外同步：

```java
synchronized (safe) {
    if (!safe.contains("A")) {
        safe.add("A");
    }
}
```

遍历也通常需要在包装对象上同步。

并发集合具有不同内部算法和一致性语义，统一放在 `1.2-并发编程` 模块。本章只建立边界：普通集合没有自动线程安全保证。

---

## 01.34 默认方法与集合演进

Java 8 之后集合接口通过默认方法增加能力，例如：

```java
removeIf
spliterator
stream
parallelStream
List.replaceAll
List.sort
Map.computeIfAbsent
Map.merge
```

默认方法支持接口演进，但调用者仍需检查：

- 实现是否支持修改；
- 方法是否调用用户提供的函数多次；
- 函数是否允许副作用；
- 并发实现是否覆盖了更强原子性；
- 计算函数能否返回 null；
- 结构性修改是否与迭代冲突。

---

## 01.35 特殊集合的语义提醒

一些实现不能按普通 HashMap/HashSet 心智模型使用：

- `IdentityHashMap` 用 `==` 判断键；
- `WeakHashMap` 的键可能因 GC 可达性消失；
- `EnumSet` 和 `EnumMap` 针对枚举优化；
- `PriorityQueue` 只保证队首优先；
- `LinkedHashMap` 可维护插入或访问顺序；
- `TreeMap` 用比较器定义键等价。

它们不是“性能更好或更差”的简单替代，而是语义不同。

---

## 01.36 API 参数与返回值设计

参数通常应使用满足操作的最小接口：

```java
void process(Collection<Order> orders)
void sort(List<Order> orders)
void enqueue(Queue<Task> tasks)
```

返回值要明确所有权和修改语义：

```text
返回内部实时视图
返回只读包装
返回不可修改快照
返回新建可变集合
```

不要默认返回具体实现类，也不要直接暴露内部可变集合。

当调用方必须依赖顺序时，使用 `List`、`SequencedCollection` 或明确排序类型，而不是模糊的 `Collection`。

---

## 01.37 WMS 场景中的集合选型

仓储系统示例：

| 需求 | 候选 |
|---|---|
| 按扫描顺序保存条码 | `ArrayList<String>` |
| 批次内去重且保留首次顺序 | `LinkedHashSet<String>` |
| 按库位编码查库存 | `HashMap<LocationCode, Stock>` |
| 按到期日排序 | `TreeMap<LocalDate, List<Lot>>` |
| 波次任务双端调度 | `ArrayDeque<Task>` |
| 按优先级取补货任务 | `PriorityQueue<Task>` |
| 枚举状态集合 | `EnumSet<OrderStatus>` |

选型必须同时说明：

- 是否需要顺序；
- 相等性或比较器；
- 主要操作；
- 数据规模；
- 是否跨线程；
- 是否需要快照。

---

## 01.38 本章决策清单

1. 需求是元素集合还是键值映射？
2. 是否允许重复，重复由 equals、hash 还是 comparator 判定？
3. 需要位置顺序、插入顺序、访问顺序还是排序顺序？
4. 是否需要两端操作或优先级出队？
5. 是否允许 null？
6. 接口是否包含可选修改操作？
7. 返回的是视图、包装还是快照？
8. 是否依赖特定复杂度和实现特性？
9. 是否跨线程访问？
10. Java 版本是否允许使用 Sequenced API？

---

## 01.39 可运行实验

以下实验均为独立 Java 文件。示例以 Java 21 为编译基线；涉及更高版本 API 时会明确标注。

### 实验1：Iterable 支持增强 for



```java
import java.util.Iterator;
import java.util.List;

public class IterableForEachDemo {
    public static void main(String[] args) {
        Iterable<String> values = List.of("A", "B");
        for (String value : values) {
            System.out.print(value);
        }
    }
}
```

预期输出：

```text
AB
```

### 实验2：List 保留位置和重复



```java
import java.util.ArrayList;
import java.util.List;

public class ListContractDemo {
    public static void main(String[] args) {
        List<String> values = new ArrayList<>();
        values.add("A");
        values.add("A");
        values.add(1, "B");
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, B, A]
```

### 实验3：Set 去重



```java
import java.util.HashSet;
import java.util.Set;

public class SetContractDemo {
    public static void main(String[] args) {
        Set<String> values = new HashSet<>();
        values.add("A");
        values.add("A");
        System.out.println(values.size());
    }
}
```

预期输出：

```text
1
```

### 实验4：Queue 特殊值方法



```java
import java.util.ArrayDeque;
import java.util.Queue;

public class QueueMethodPairsDemo {
    public static void main(String[] args) {
        Queue<String> queue = new ArrayDeque<>();
        System.out.println(queue.poll());
        queue.offer("A");
        System.out.println(queue.peek());
    }
}
```

预期输出：

```text
null
A
```

### 实验5：Deque 同时表达队列和栈



```java
import java.util.ArrayDeque;
import java.util.Deque;

public class DequeContractDemo {
    public static void main(String[] args) {
        Deque<String> values = new ArrayDeque<>();
        values.addLast("queue");
        values.push("stack");
        System.out.println(values.removeFirst());
        System.out.println(values.removeFirst());
    }
}
```

预期输出：

```text
stack
queue
```

### 实验6：Map 三个视图



```java
import java.util.LinkedHashMap;
import java.util.Map;

public class MapViewsDemo {
    public static void main(String[] args) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        System.out.println(map.keySet());
        System.out.println(map.values());
        System.out.println(map.entrySet());
    }
}
```

预期输出：

```text
[A, B]
[1, 2]
[A=1, B=2]
```

### 实验7：keySet 删除影响 Map



```java
import java.util.HashMap;
import java.util.Map;

public class BackedKeySetDemo {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.keySet().remove("A");
        System.out.println(map.isEmpty());
    }
}
```

预期输出：

```text
true
```

### 实验8：List 与 Set 相等性差异



```java
import java.util.List;
import java.util.Set;

public class CollectionEqualityDemo {
    public static void main(String[] args) {
        System.out.println(
                List.of("A", "B").equals(List.of("B", "A"))
        );
        System.out.println(
                Set.of("A", "B").equals(Set.of("B", "A"))
        );
    }
}
```

预期输出：

```text
false
true
```

### 实验9：TreeSet 用比较器判定等价



```java
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class SortedSetEqualityDemo {
    public static void main(String[] args) {
        Set<String> values =
                new TreeSet<>(Comparator.comparingInt(String::length));
        values.add("AA");
        values.add("BB");
        System.out.println(values.size());
    }
}
```

预期输出：

```text
1
```

### 实验10：Iterator 安全删除



```java
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IteratorRemoveDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "", "B"));
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isBlank()) {
                iterator.remove();
            }
        }
        System.out.println(values);
    }
}
```

预期输出：

```text
[A, B]
```

### 实验11：fail-fast 检测错误修改



```java
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class FailFastDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B"));
        try {
            for (String value : values) {
                values.add(value);
            }
        } catch (ConcurrentModificationException exception) {
            System.out.println(exception.getClass().getSimpleName());
        }
    }
}
```

预期输出：

```text
ConcurrentModificationException
```

### 实验12：不可修改工厂拒绝修改



```java
import java.util.List;

public class UnmodifiableFactoryDemo {
    public static void main(String[] args) {
        List<String> values = List.of("A", "B");
        try {
            values.add("C");
        } catch (UnsupportedOperationException exception) {
            System.out.println(exception.getClass().getSimpleName());
        }
    }
}
```

预期输出：

```text
UnsupportedOperationException
```

### 实验13：只读包装反映原集合变化



```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnmodifiableViewDemo {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>();
        List<String> view =
                Collections.unmodifiableList(source);
        source.add("A");
        System.out.println(view);
    }
}
```

预期输出：

```text
[A]
```

### 实验14：copyOf 建立快照



```java
import java.util.ArrayList;
import java.util.List;

public class CopyOfSnapshotDemo {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>();
        source.add("A");
        List<String> snapshot = List.copyOf(source);
        source.add("B");
        System.out.println(snapshot);
    }
}
```

预期输出：

```text
[A]
```

### 实验15：Arrays.asList 是数组视图



```java
import java.util.Arrays;
import java.util.List;

public class ArraysAsListViewDemo {
    public static void main(String[] args) {
        String[] array = {"A", "B"};
        List<String> view = Arrays.asList(array);
        view.set(0, "X");
        System.out.println(array[0]);
    }
}
```

预期输出：

```text
X
```

### 实验16：基本类型数组成为单个元素



```java
import java.util.Arrays;
import java.util.List;

public class PrimitiveArrayAsListDemo {
    public static void main(String[] args) {
        int[] array = {1, 2, 3};
        List<int[]> values = Arrays.asList(array);
        System.out.println(values.size());
        System.out.println(values.get(0).length);
    }
}
```

预期输出：

```text
1
3
```

### 实验17：subList 是支撑视图



```java
import java.util.ArrayList;
import java.util.List;

public class SubListViewDemo {
    public static void main(String[] args) {
        List<String> source =
                new ArrayList<>(List.of("A", "B", "C"));
        List<String> middle = source.subList(1, 3);
        middle.remove("B");
        System.out.println(source);
    }
}
```

预期输出：

```text
[A, C]
```

### 实验18：Map merge 聚合



```java
import java.util.HashMap;
import java.util.Map;

public class MapMergeDemo {
    public static void main(String[] args) {
        Map<String, Integer> counts = new HashMap<>();
        for (String value : new String[]{"A", "B", "A"}) {
            counts.merge(value, 1, Integer::sum);
        }
        System.out.println(counts.get("A"));
    }
}
```

预期输出：

```text
2
```

### 实验19：Java 21 SequencedCollection 首尾操作



```java
import java.util.ArrayList;
import java.util.List;

public class SequencedCollectionDemo {
    public static void main(String[] args) {
        List<String> values =
                new ArrayList<>(List.of("A", "B"));
        values.addFirst("START");
        values.addLast("END");
        System.out.println(values.getFirst());
        System.out.println(values.getLast());
    }
}
```

预期输出：

```text
START
END
```

### 实验20：Java 21 reversed 是视图



```java
import java.util.ArrayList;
import java.util.List;

public class ReversedViewDemo {
    public static void main(String[] args) {
        List<String> source =
                new ArrayList<>(List.of("A", "B", "C"));
        List<String> reversed = source.reversed();
        reversed.set(0, "X");
        System.out.println(source);
    }
}
```

预期输出：

```text
[A, B, X]
```

---

## 01.40 高频面试题

1. Java Collections Framework 由哪些部分组成？
2. Collection、Collections 和集合框架有什么区别？
3. 集合框架统一架构的主要价值是什么？
4. 为什么算法应尽量面向接口？
5. Iterable 与 Collection 有什么关系？
6. 增强 for 对 Iterable 如何工作？
7. 数组为什么能使用增强 for 却不实现 Iterable？
8. Collection 接口承诺哪些最小能力？
9. Collection 是否承诺允许重复？
10. Collection 是否承诺顺序？
11. Collection 是否承诺支持 null？
12. Collection 是否承诺所有修改操作？
13. 为什么 Map 不继承 Collection？
14. Map 的三个集合视图是什么？
15. keySet 是否是 Map 的副本？
16. 为什么 keySet 通常不能 add？
17. List 的核心语义是什么？
18. List 的有序是否等于已排序？
19. List.equals 如何比较？
20. List 是否允许重复元素？
21. Set 的核心语义是什么？
22. HashSet 和 TreeSet 如何判定重复？
23. Set.equals 是否关心遍历顺序？
24. 业务去重为什么不能只依赖 Set？
25. Queue 的 add 与 offer 有什么区别？
26. Queue 的 remove 与 poll 有什么区别？
27. Queue 的 element 与 peek 有什么区别？
28. PriorityQueue 是否是 FIFO 队列？
29. Deque 能表达哪些结构？
30. 为什么新代码通常用 Deque 替代 Stack？
31. ArrayDeque 是否允许 null？
32. 双端队列与普通队列的核心差异是什么？
33. SortedSet 与 NavigableSet 有什么区别？
34. TreeSet 中 compare 为 0 意味着什么？
35. 比较器与 equals 不一致会导致什么？
36. 排序字段可变为什么危险？
37. SequencedCollection 在哪个 Java 版本引入？
38. Sequenced API 统一了哪些能力？
39. reversed 返回视图还是副本？
40. Java 8 项目能否直接调用 getFirst？
41. 为什么变量通常声明为接口类型？
42. 什么时候局部使用具体实现类型是合理的？
43. 通用实现与专用实现有什么区别？
44. IdentityHashMap 与普通 Map 的相等性有什么不同？
45. 抽象骨架实现有什么价值？
46. 继承 AbstractList 是否自动获得高性能？
47. 什么是集合的可选操作？
48. 为什么有 add 方法仍可能运行期抛异常？
49. 什么是结构性修改？
50. set 是否一定属于结构性修改？
51. 结构性修改为什么影响迭代器？
52. fail-fast 与 modCount 有什么关系？
53. 遇到顺序、插入顺序和排序顺序有什么区别？
54. HashSet 是否保证稳定遍历顺序？
55. LinkedHashMap 的访问顺序是什么？
56. PriorityQueue 的迭代顺序是否排序？
57. 不同集合如何判定重复元素？
58. Map key 可变会有什么风险？
59. null 支持为什么必须查具体实现？
60. List.of 为什么拒绝 null？
61. 复杂度为什么不能只写在接口层？
62. ArrayList 与 LinkedList 的 get 复杂度有什么差异？
63. HashMap 查询是否永远 O(1)？
64. 复杂度分析还应考虑哪些工程成本？
65. Iterator.remove 的正确前置条件是什么？
66. 增强 for 中直接集合 remove 为什么危险？
67. fail-fast 是否保证一定抛异常？
68. ConcurrentModificationException 是否只会在多线程出现？
69. 什么是集合视图？
70. 视图与快照有什么区别？
71. subList 的修改如何影响原列表？
72. 视图为什么可能延长大对象生命周期？
73. 不可修改集合与不可变集合有什么区别？
74. unmodifiableList 与 List.copyOf 有什么区别？
75. 不可修改快照是否深度不可变？
76. Arrays.asList 为什么不能改变大小？
77. 同步包装是否让复合操作自动原子？
78. synchronizedList 遍历为什么还需同步？
79. 并发集合应该放在哪个模块学习？
80. 集合选型最先应该确认什么语义？

---

## 01.41 易错点

### 误区1：集合框架就是 ArrayList 和 HashMap

**错误。** 它是一套包含接口、实现、算法和基础设施的统一架构。

### 误区2：Collection 和 Collections 是同一个东西

**错误。** 前者是接口，后者是静态工具类。

### 误区3：Map 也是一种 Collection

**错误。** Map 表达键值映射，通过三个视图连接到集合接口。

### 误区4：所有 Collection 都允许重复

**错误。** Set 不允许按其相等性语义重复。

### 误区5：所有集合都有固定遍历顺序

**错误。** HashSet、HashMap 等通常不承诺稳定遇到顺序。

### 误区6：有序集合就是排序集合

**错误。** List 的位置顺序、LinkedHashSet 的插入顺序与 TreeSet 的排序不同。

### 误区7：List 接口意味着按索引 O(1)

**错误。** 复杂度由具体实现决定，LinkedList 按索引通常 O(n)。

### 误区8：Map 查询永远是 O(1)

**错误。** HashMap 是平均模型，TreeMap 通常 O(log n)，特殊实现另有语义。

### 误区9：Set 去重统一依赖 equals

**错误。** TreeSet 主要依赖比较器或自然顺序。

### 误区10：Set 能替代数据库唯一约束

**错误。** 内存集合不能保证跨事务、跨节点和持久化唯一性。

### 误区11：Queue 一定按 FIFO

**错误。** PriorityQueue 等实现使用优先级顺序。

### 误区12：Deque 只能作为队列

**错误。** 它也适合栈、双端窗口和两端调度。

### 误区13：Stack 是现代 Java 栈的首选

**错误。** 新代码通常优先 ArrayDeque。

### 误区14：Map keySet 是复制出来的集合

**错误。** 它通常是由 Map 支撑的实时视图。

### 误区15：从 keySet 删除不会影响 Map

**错误。** 删除键会删除对应映射。

### 误区16：values 视图允许随意 add 值

**错误。** 缺少键，通常不支持新增。

### 误区17：接口包含 add 就保证支持修改

**错误。** 修改是可选操作，实现可能抛 UnsupportedOperationException。

### 误区18：不可修改集合等于深度不可变

**错误。** 元素对象仍可能可变。

### 误区19：unmodifiableList 会复制原列表

**错误。** 它通常是包装视图，原列表变化仍可见。

### 误区20：List.copyOf 会深拷贝元素

**错误。** 它通常只建立浅层不可修改快照。

### 误区21：Arrays.asList 返回普通 ArrayList

**错误。** 它返回数组支撑的定长 List 实现。

### 误区22：Arrays.asList 可以 add/remove

**错误。** 改变大小通常抛 UnsupportedOperationException。

### 误区23：基本类型数组会被拆成多个列表元素

**错误。** 整个数组通常成为一个元素。

### 误区24：subList 是独立列表

**错误。** 它是由原列表支撑的范围视图。

### 误区25：视图比复制总是更安全

**错误。** 视图有耦合、失效和生命周期风险。

### 误区26：fail-fast 是并发安全机制

**错误。** 它只是尽力检测错误修改。

### 误区27：ConcurrentModificationException 只在多线程出现

**错误。** 单线程遍历时错误修改也常触发。

### 误区28：只要没有抛并发修改异常就安全

**错误。** 检测不是保证，数据竞争仍可能存在。

### 误区29：同步包装让任意复合操作都原子

**错误。** contains-then-add 等仍需外部同步。

### 误区30：synchronizedList 遍历无需同步

**错误。** 传统文档要求在包装对象上同步迭代。

### 误区31：null 支持由接口统一规定

**错误。** 不同实现和工厂差异很大。

### 误区32：TreeSet 只要元素 equals 不同就能共存

**错误。** 比较器返回 0 时通常视为重复。

### 误区33：PriorityQueue 迭代结果是排好序的

**错误。** 只保证队首最优，迭代顺序不保证全局排序。

### 误区34：SequencedCollection 从 Java 8 就存在

**错误。** 它在 Java 21 引入。

### 误区35：reversed 一定创建新集合

**错误。** 现代 Sequenced API 通常返回反向视图。

### 误区36：面向接口意味着永远不能写实现类

**错误。** 构造配置或实现特有能力在局部使用具体类型是合理的。

### 误区37：自定义集合应优先继承 AbstractList

**错误。** 多数领域需求更适合组合现有集合并封装行为。

### 误区38：集合允许 null 就应该存 null

**错误。** 实现能力不等于业务设计建议。

### 误区39：所有批量操作都是原子的

**错误。** 普通集合通常不提供跨线程事务语义。

### 误区40：选集合只看理论复杂度

**错误。** 还需考虑顺序、内存、局部性、相等性、线程和所有权。

---

## 01.42 工程实践建议

1. 从语义出发选接口：元素集合用 Collection，键值映射用 Map。
2. 变量和参数声明为满足需求的最小接口，避免不必要的实现耦合。
3. 需要位置语义时使用 List，不用模糊 Collection 代替。
4. 需要去重时先定义相等性，再选择 HashSet、TreeSet 或其他实现。
5. 需要队列正常空状态时优先使用 offer、poll、peek 方法族。
6. 新代码使用 Deque 表达栈，不默认选择遗留 Stack。
7. 文档明确顺序类型：位置、插入、访问、排序或不保证。
8. 任何复杂度结论都绑定具体实现、操作和平均/最坏条件。
9. 明确每个集合边界是否允许 null，不依赖团队猜测。
10. 公开 API 不直接暴露内部可变集合。
11. 返回集合时注明视图、包装、快照或新建可变副本。
12. 使用 Map 视图时记住删除会回写原 Map。
13. 需要独立数据时不要把 keySet、subList 当成副本。
14. 对长期保存的 subList 谨慎，避免保留不必要的大列表。
15. 不可修改包装用于限制访问，独立快照用于隔离后续修改。
16. 不要把浅层不可修改误写成深度不可变。
17. 使用 List.of、Set.of、Map.of 时明确 null 和重复限制。
18. 通过 Iterator.remove 在遍历期间安全删除当前元素。
19. 不要把 fail-fast 当作业务控制流或并发正确性机制。
20. 普通集合跨线程共享前明确同步、快照或并发集合策略。
21. 使用 synchronized 包装时对复合操作和遍历显式同步。
22. 并发集合的弱一致性和原子复合 API放到并发模块深入。
23. TreeSet、TreeMap 的比较器必须满足传递性和稳定性。
24. 避免把可变字段参与 Set 相等性或 Map key 哈希/排序。
25. PriorityQueue 使用前明确只保证队首，不保证迭代排序。
26. EnumSet、EnumMap 用于枚举域，表达力和效率通常更好。
27. IdentityHashMap 只用于确实需要引用身份语义的场景。
28. WeakHashMap 使用前理解 GC 可达性，不当作普通缓存万能方案。
29. 自定义集合优先组合，确有集合替换需求再实现接口或骨架类。
30. 可选操作应在 API 文档和测试中明确，不让调用方运行时猜测。
31. 批量操作前评估参数是否为原集合的视图或同一对象。
32. 数组与集合转换时区分视图、定长列表和独立副本。
33. 基本类型数组转 List 时显式处理，避免形成单元素列表误判。
34. Java 21+ 使用 Sequenced API 时在文档标记版本基线。
35. 使用 reversed 视图时说明修改是否回写原集合。
36. 领域集合可封装成专用类型，避免裸 List 到处传递。
37. 集合选型审查同时包含数据规模、读写比例和内存成本。
38. 单元测试覆盖重复、null、顺序、视图回写和不支持操作。
39. 不要依赖集合实现的未承诺类名、扩容策略或序列化布局。
40. 把接口契约与 OpenJDK 实现细节分层记录，防止背错边界。

---

## 01.43 本章小结

- 集合框架由接口、实现、算法和基础设施共同组成。
- Collection 表示元素集合，Map 表示键值映射并提供三个集合视图。
- List、Set、Queue、Deque、Sorted/Navigable 接口具有不同语义。
- 顺序、重复、null、复杂度和可修改性必须查具体契约。
- 视图不是副本；不可修改也不等于深度不可变。
- fail-fast 只用于尽力检测错误修改，不提供并发正确性。
- Java 21 的 Sequenced API 统一了首尾和反向视图能力。

---

## 01.44 面试口述版

Java 集合框架是一套统一架构，包括核心接口、通用和专用实现、算法以及迭代器、比较器、包装器和视图等基础设施。`Collection` 表示一组元素，下面有 List、Set、Queue 和 Deque；Map 表示键值映射，所以不继承 Collection，而是通过 keySet、values 和 entrySet 暴露集合视图。选型时应先确定是否允许重复、需要什么顺序、主要操作、null 策略和并发边界，再选择实现。接口不保证所有修改操作，也不统一承诺复杂度。集合视图通常与原结构联动，不是副本；不可修改包装也不等于深度不可变。普通集合的 fail-fast 只是错误检测，不是线程安全保证。Java 21 进一步引入 SequencedCollection、SequencedSet 和 SequencedMap，统一了首尾操作和反向视图。

---

## 01.45 参考资料

- Java SE 25 API：Java Collections Framework Overview。
- Java SE 25 API：Collection、List、Set、Queue、Deque、Map。
- Java SE 25 API：SortedSet、NavigableSet、SortedMap、NavigableMap。
- Java SE 25 API：Collections、Arrays、Iterator。
- JEP 431：Sequenced Collections（Java 21）。
