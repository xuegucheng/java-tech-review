# Collections 泛型 API 设计

> **本章定位：Collections 消费视角 · P1 · JDK 8+ / Java 21。**
>
> 本章不重新讲类型参数、参数化类型、不变性、通配符捕获、擦除或堆污染。那些语言规则唯一维护在 [Java 泛型：语言规则与类型安全](../../01-Java核心/02-深度解析/Java泛型与类型安全.md)。这里只回答一个问题：JDK 集合 API 为什么把泛型签名写成现在这样，以及调用方应该如何读懂和使用它们。

## 1. 先说结论

阅读集合泛型签名时，先看数据流方向，再看类型变量：

```text
source / producer  ──? extends E──>  target / collection
target / consumer  <──? super E──   value of E
```

- 集合向外提供元素，通常使用 `? extends E`；
- 集合接收元素，通常使用 `? super E`；
- 多个参数、返回值需要表达“同一个确定类型”时，使用类型参数 `<T>`；
- 接口类型应表达调用方真正需要的能力，不要为了“更灵活”把所有位置都写成通配符。

## 2. `Collection.addAll` 为什么使用 `? extends E`

简化签名：

```java
interface Collection<E> {
    boolean addAll(Collection<? extends E> source);
}
```

目标集合 `Collection<E>` 接收元素。对目标来说，`source` 是一个生产者：它里面的每个元素都至少可以当成 `E` 使用。

```java
List<Number> numbers = new ArrayList<>();
List<Integer> integers = List.of(1, 2, 3);

numbers.addAll(integers); // 安全：Integer 是 Number
```

如果参数写成 `Collection<E>`，上面的调用会被不必要地拒绝，因为 `Collection<Integer>` 不是 `Collection<Number>` 的子类型。`? extends E` 扩大了输入集合的安全范围，同时不允许方法向未知的具体子类型集合写入任意新元素。

这不是“extends 集合可以随便读写”的规则。它表达的是：

```text
source 能生产 E
source 不能被 add 一个任意 E
```

`addAll` 自己知道如何把读出的元素作为 `E` 加入目标集合，因此签名成立。

## 3. `Comparator<? super T>` 为什么使用 `super`

排序操作消费比较器：它把两个 `T` 交给比较器，让比较器返回顺序关系。因此比较器是 `T` 的消费者。

```java
List<String> names = new ArrayList<>(List.of("C", "A", "B"));
Comparator<Object> byText = Comparator.comparing(Object::toString);

names.sort(byText); // Comparator<Object> 可以消费 String
```

简化理解：

```java
default void sort(Comparator<? super E> comparator)
```

`Comparator<Object>` 可以比较任意 `Object`，当然也能比较 `String`；如果只接受 `Comparator<String>`，反而会拒绝这个更通用的比较器。`super` 不是为了表示“父类排序”，而是为了表达比较器能够消费 `E`。

## 4. `Iterator<E>` 为什么不使用通配符

`Iterator<E>` 表示迭代器每次产生与集合元素相同的确定类型：

```java
Collection<String> names = List.of("A", "B");
Iterator<String> iterator = names.iterator();
String first = iterator.next();
```

这里不需要把类型放宽成 `Iterator<? extends E>`：集合接口已经承诺了元素类型，迭代器和集合之间需要保持同一个 `E`。使用确定类型可以让 `next()` 的返回值直接进入调用方的静态类型。

`Iterator.remove()` 的语义属于集合修改契约，具体的 fail-fast、弱一致性和并发边界见 [集合框架体系与核心契约](./集合框架体系与核心契约.md)，不在本章重复展开。

## 5. `Map<K, V>` 的两个类型参数表达什么

`Map<K, V>` 同时表达 key 和 value 的角色，而不是表达两个必然不同的具体类型：

```java
Map<String, Integer> counts = new HashMap<>();
counts.put("java", 1);
Integer count = counts.get("java");
```

- `K` 约束 key 的静态类型；
- `V` 约束 value 的静态类型；
- `get` 可能返回 `null`，泛型不提供非空保证；
- key 的 equality/hash 语义由 [HashMap 如何消费 equals/hashCode 契约](./Hash集合与equals-hashCode契约.md) 负责；
- `Map` 的遍历顺序、视图和修改边界由集合契约负责。

读取 `Map.Entry<K, V>` 时，`K` 和 `V` 是同一个 Map 的类型关系；不应为了接收任意 Map 而在每个调用处制造未经证明的强制转换。

## 6. `Collections.copy` 为什么同时使用 `extends` 和 `super`

简化签名：

```java
static <T> void copy(
        List<? super T> destination,
        List<? extends T> source
)
```

它表达两条数据流：

```text
source       ── produces T ──>  destination
? extends T                  ? super T
```

因此下面的调用可以成立：

```java
List<Number> destination = new ArrayList<>(List.of(0, 0));
List<Integer> source = List.of(1, 2);

Collections.copy(destination, source);
```

`<T>` 负责把两个位置连接为同一个类型关系；`extends` 和 `super` 负责表达每个位置的读写方向。只有口诀而没有这条数据流，容易把 `copy` 的参数顺序写反。

## 7. PECS 在集合 API 中如何落地

PECS（Producer Extends, Consumer Super）是 API 设计时的起点，不是调用方的机械替换规则。

| API 角色 | 常见写法 | 调用方能做什么 | 典型例子 |
| --- | --- | --- | --- |
| 生产者 | `Collection<? extends E>` | 读取为 `E` | `addAll` 的 source |
| 消费者 | `Collection<? super E>` | 写入 `E` | `copy` 的 destination |
| 比较器消费者 | `Comparator<? super E>` | 让比较器消费 `E` | `List.sort` |
| 同一类型关系 | `<T>` | 连接多个位置 | `Collections.copy` |

遇到一个新签名时按四步走：

1. 哪个参数提供元素，哪个参数接收元素？
2. 调用方是否应该能传入子类型或父类型容器？
3. 多个位置是否必须是同一个确定类型？
4. 返回值是否应该暴露具体类型，而不是把未知类型负担转嫁给调用方？

## 8. 返回类型不要滥用通配符

输入参数使用通配符通常是在扩大安全的接受范围；返回类型则应优先返回调用方真正需要的稳定抽象：

```java
List<Number> loadNumbers();
```

通常比下面的返回类型更容易使用：

```java
List<? extends Number> loadNumbers();
```

如果实现确实需要隐藏具体子类型，并且调用方只读，返回通配符可以成立，但必须记录这个限制。不要把“通配符更多”误认为“API 更高级”。

## 9. `List<E>` 的泛型不保证这些事情

泛型只描述静态类型关系，不自动保证：

- 元素非空；
- 集合不可变；
- 集合线程安全；
- 元素对象深度不可变；
- key 的业务唯一性；
- 数据来源可信或业务状态合法。

例如 `List<String>` 仍可能允许 `null`，是否允许要看具体集合工厂、实现和业务契约。`List.copyOf` 的不可修改快照语义与泛型是两件事，不能混成一个结论。

## 10. 与 authoritative source 的边界

- 类型参数、类型实参、泛型类/方法、不变性、通配符、擦除和堆污染：见 [Java 泛型：语言规则与类型安全](../../01-Java核心/02-深度解析/Java泛型与类型安全.md)；
- 集合接口、视图、迭代器、fail-fast：见 [集合框架体系与核心契约](./集合框架体系与核心契约.md)；
- HashMap 如何消费 equality：见 [Hash 集合如何消费 equals/hashCode 契约](./Hash集合与equals-hashCode契约.md)；
- 完整集合实现细节：见 [ArrayList](./ArrayList原理与实现.md)、[LinkedList](./LinkedList原理与实现.md)、[HashMap](./HashMap原理与源码分析.md)、[HashSet 与 LinkedHashSet](./HashSet与LinkedHashSet.md) 和 [LinkedHashMap/LRU](./LinkedHashMap与LRU缓存.md)。

本页只维护“集合 API 为什么这样声明”的消费解释；当语言规则与 API 使用混在一起时，以 Java Core 泛型权威文档为准。

## 一句话复盘

> 集合泛型签名首先表达数据流：生产者用 `extends`，消费者用 `super`，多个位置需要同一类型时用 `<T>`；理解这条关系，就能读懂 `addAll`、`Comparator` 和 `Collections.copy`，而不需要背 API 口诀。
