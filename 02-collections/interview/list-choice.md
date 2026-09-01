# List 选型：ArrayList、LinkedList 与 ArrayDeque

> **P1 · JDK 8+；Sequenced Collections 标记 Java 21+ · 5 分钟复习**

## 面试结论

普通业务 List 通常优先考虑 ArrayList：连续引用数组、索引访问和较低节点开销更适合常见读多写少场景。LinkedList 只有在已知节点位置、且确实需要链表/Deque 语义时才有理由；如果主要是两端入队出队，ArrayDeque 往往更直接。

## 真实复杂度

| 操作 | ArrayList | LinkedList |
| --- | --- | --- |
| 按索引读取 | 通常 O(1) | 需要定位节点，通常 O(n) |
| 尾部追加 | 均摊 O(1)，扩容时复制 | O(1) |
| 中间插入/删除 | 搬移数组区间 | 找节点的成本可能已是 O(n) |
| 内存局部性 | 连续引用数组，通常较好 | 节点对象和指针更多 |

## 追问链

1. 为什么不能只背“LinkedList 增删快”？因为忽略了定位节点的成本。
2. `remove(1)` 和 `remove(Integer.valueOf(1))` 有什么不同？前者可能选择索引重载，后者明确按对象删除。
3. `subList` 是副本吗？通常是与原 List 联动的范围视图，生命周期和结构修改要谨慎。
4. fail-fast 是线程安全吗？不是；它只能尽力发现结构性修改。

## Deep Dive

- [ArrayList](../deep-dive/arraylist.md)
- [LinkedList](../deep-dive/linkedlist.md)
- [集合接口与核心契约](../deep-dive/collection-contracts.md)

## 一句话复盘

> 复杂度要包含“找到操作位置”的成本；默认选 ArrayList，队列优先考虑 ArrayDeque。
