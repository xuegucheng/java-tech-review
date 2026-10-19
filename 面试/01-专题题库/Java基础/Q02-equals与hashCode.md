# Q02. 为什么重写 equals 就必须重写 hashCode？

> Java基础 · P0 · 高频

**30 秒回答**：`hashCode` 是「先定位桶、再用 equals 比较」这个两段式查找的第一段，两者口径不一致，对象就会落进错误的桶，表现成「写得进去、查不出来」。

**深入理解**：以 `HashMap` 为例，`put` 先用 `hash(key)` 定位桶、桶内再用 `equals` 比较，`get` 走同样路径。只重写 `equals` 时，两个逻辑相等的对象会算出不同哈希、落到不同桶，于是 `get`/`containsKey` 返回不存在，而 `size()` 却显示两个元素都在。

```java
// 典型错误：只重写 equals
public boolean equals(Object o) {
    return o instanceof Order && ((Order) o).id == id;
}
// put 两个 id 相同的 Order，size() == 2，get(id) 却可能为 null
```

契约三条：`equals` 相等则 `hashCode` 必须相等；`hashCode` 相等不要求 `equals` 相等（哈希冲突合法）；对象参与哈希结构期间 `hashCode` 不能变。

**为什么这样设计**：这是两段式结构的必然要求，不是 Java 的额外约定。第一段用 `hashCode` 把查找降到平均 O(1)，第二段用 `equals` 消化冲突——两段必须对「相等」持同一套口径，否则整个结构不成立。代价是「相等」这个概念被拆成两份实现，忘记同步就产生隐蔽 bug。

**面试官可能继续追问**：

- **HashMap 定位桶时用的是 hashCode 还是 hash()？** —— 是扰动后的值：`h = key.hashCode(); h ^= (h >>> 16);`，再 `(n - 1) & h` 取桶。只答 hashCode 不算完整。
- **把可变字段放进 equals/hashCode 有什么后果？** —— 对象作为 key 存入 `HashMap` 后修改该字段，哈希变了就再也找不到这个 entry，形成「查不掉的脏数据」；这也是 key 推荐用不可变类型的原因。
- **hashCode 相同一定 equals 吗？** —— 不一定，哈希冲突是合法且必然存在的，桶内用 `equals` 逐个比较（JDK 8 起同桶元素超过 8 个树化为红黑树，低于 6 个再退化回链表）。

**实际工程场景**：任何放进 `HashSet`/`HashMap` 的领域对象都要成对重写；IDEA 生成模板默认用 `31 * ` 乘法与 `Objects.equals`，比手写安全。缓存场景注意：以可变实体做 key 前先取不可变快照（如取 id 构造只读 key 对象）。

**容易答错的地方**：认为「hashCode 相等则 equals 必须相等」——契约只要求单向；认为随便拼个字符串当哈希就合格——冲突过多会让结构退化成链表遍历；跨 JVM 的分布式缓存里，不同语言客户端的 equals/hashCode 实现不一致会直接导致缓存错乱。

> 权威内容：[equals 与 hashCode 对象契约](../../../01-Java核心/01-面试速记/equals与hashCode对象契约.md) · [equals 与 hashCode 契约](../../../01-Java核心/02-深度解析/equals与hashCode契约.md)

---

返回 [Java基础 专题](../Java基础.md) · [专题题库首页](../README.md)
