# ConcurrentHashMap 并发容器

> 面试定位：Java 8+ / Java 21 的局部并发、原子复合操作和协作扩容
> Java 版本：Java 8+ 主线，字段和实现观察按 Java 21
> P0/P1：P1
> 前置知识：HashMap、equals/hashCode、CAS、volatile 和 synchronized
> 本文不负责：重新解释 HashMap 的 hash 扰动、树化和 resize 基础

## 先说结论

ConcurrentHashMap 不是给整个 Map 加一把 synchronized，而是让不同 bin 尽量并行：空 bin 的首次写入可用 CAS，非空 bin 的更新通常在 bin 级 synchronized 内完成，读取大多数路径不需要显式锁，扩容时多个线程可以协作搬迁。

Java 8 之后的主线不再是 Segment。Segment 是 JDK 7 及更早实现的历史背景；Java 8+ / Java 21 的实现围绕 Node 数组、CAS、bin 锁、TreeBin、ForwardingNode、sizeCtl 和 transferIndex 组织。

## 30 秒回答

> ConcurrentHashMap 在 Node 数组上做局部并发。get 通常读取 volatile table、Node.value 和 next，不需要锁；put 先初始化 table，空桶用 CAS 放入，非空桶在桶头节点上 synchronized 更新链表或红黑树。冲突严重时使用 TreeBin。扩容时一个线程创建 nextTable，其他线程遇到 ForwardingNode 可以 helpTransfer，协作迁移不同区间。它不允许 null，因为并发 get 返回 null 时无法区分没有映射和映射值为 null，也会让 computeIfAbsent 等原子 API 的语义变得含糊。它仍然不提供跨多个 key 的事务一致性。

## 与 HashMap 的边界

HashMap 的数组、链表、树化、hash 和 resize 基础由 [集合框架 HashMap 深度解析](../../02-集合框架/02-深度解析/HashMap原理与源码分析.md)维护。本章只回答：

- 多线程读写怎样降低共享竞争；
- 为什么局部 bin 可以用 CAS 和 synchronized；
- get 为什么大多数时候不用锁；
- 扩容为什么能协作；
- 原子复合操作和 null 禁止带来什么工程边界。

不要在本章复制 HashMap 的完整 put 流程。

## Java 21 的结构入口

Java 21 的 ConcurrentHashMap 仍能从这些结构理解：

- volatile Node<K,V>[] table；
- Node 的 key、value 和 next 访问；
- TreeBin 负责树化 bin 的组织；
- ForwardingNode 表示某个 bin 已经转移；
- volatile sizeCtl 参与初始化、扩容阈值和协作状态；
- transferIndex 记录扩容任务分配位置；
- nextTable 作为扩容期间的新表。

字段的具体修饰和辅助方法以目标 JDK 源码为准，不把 JDK 8 的字段名和 Java 21 的字段名混写。

## get 为什么通常不需要锁

get 的基本路径可以抽象为：

~~~text
读取 table
→ 根据 hash 定位 bin
→ 读取 bin 头节点
→ 链表 / TreeBin 中查找
→ 读取 volatile value / next
~~~

写入方通过合适的发布和更新方式让读线程看到节点链路。读路径不需要为了每次查找锁住整个 Map，但“无锁读取”不代表可以得到跨多次读取的一致快照，也不代表 value 对象内部状态自动线程安全。

## put 如何保证并发安全

putVal 的主线可以抽象成：

1. table 未初始化时初始化；
2. 目标 bin 为空时 CAS 放入新节点；
3. 目标 bin 非空时，以 bin 头为 monitor 做局部互斥；
4. 链表中查找相同 key，找到则覆盖或按条件拒绝；
5. 未找到则追加节点；
6. bin 过长时考虑树化；
7. 更新计数并判断是否需要扩容。

为什么可以 synchronized 在 Node 上：锁的粒度是发生冲突的 bin，而不是整张 Map；不同 bin 的更新可以并行，冲突热点仍然会集中在同一个 bin，这是结构真实的竞争边界。

## computeIfAbsent 的价值和边界

普通的 get 后 put：

~~~java
V value = map.get(key);
if (value == null) {
    value = create();
    map.put(key, value);
}
~~~

多线程下可能重复创建或覆盖。computeIfAbsent 把“没有映射时计算并安装”作为 Map 对这个 key 的原子复合操作，但 mapping function 仍要遵守：

- 不返回 null；
- 尽量短小，不执行长时间阻塞；
- 不递归修改同一个 Map 的冲突 key；
- 不把它当成跨多个 key 的事务。

原子性覆盖的是该方法定义的 Map 操作，不是 create 过程涉及的外部系统副作用。

## resize 与协作迁移

扩容期间可以理解为：

~~~text
旧 table
  ↓ 某个 bin 被转移
ForwardingNode
  ↓ 其他线程遇到后
helpTransfer
  ↓
共同处理 nextTable 的迁移区间
~~~

sizeCtl 和 transferIndex 等状态用于协调初始化、阈值和迁移分工。协作扩容提高了扩容期间的进度，但不会让 resize 变成零成本；容量规划、hash 质量和热点 key 仍然重要。

## 为什么不允许 null

HashMap 可以把 null 当成一个合法 key/value，但 ConcurrentHashMap 的并发查询需要把 null 保留为“没有映射”的信号。否则：

- get 返回 null 无法区分缺失和显式 null；
- computeIfAbsent、merge 等复合 API 的语义难以定义；
- 并发结果判断需要额外的 containsKey，反而增加竞态窗口。

因此禁止 null 是并发语义设计，而不是单纯 API 限制。

## size 和弱一致遍历

ConcurrentHashMap 的 size、mappingCount 和遍历不能被描述成普通锁保护下的全局瞬时快照。并发更新时，读取到的是某个合法观察时刻附近的结果，迭代器通常是弱一致的：

- 不抛出普通 fail-fast 异常；
- 可能看到创建时或遍历期间的部分更新；
- 不保证遍历结果代表一个单一事务快照。

如果业务需要快照、一致统计或跨 key 原子性，应该使用更高层协议。

## 源码路径

- get：table、bin、Node.value/next 的读取；
- putVal：CAS 空 bin、bin 级 synchronized 和树化；
- initTable：初始化与 sizeCtl；
- treeifyBin、TreeBin：冲突结构；
- ForwardingNode、helpTransfer、transfer：协作扩容；
- computeIfAbsent、merge：原子复合方法。

## 工程边界

- 单 key 原子复合不等于多 key 事务；
- value 对象自身仍需不可变或单独同步；
- 热点 key 仍会竞争同一个 bin；
- computeIfAbsent 不应承载长时间远程调用；
- 需要顺序、过期、权重和高阶缓存能力时评估专业缓存，而不是把 ConcurrentHashMap 当缓存产品；
- 读多写多场景要结合容量、hash、热点和 GC 做压测。

## Runnable Example

[ConcurrentMapDemo](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/concurrency/ConcurrentMapDemo.java) 使用 computeIfAbsent 和 merge 验证单 key 初始化与计数复合操作。

## 高频追问

- Java 8 之后为什么没有 Segment？降低分段锁结构的固定分区限制，改为 bin 级并发。
- 为什么仍使用 synchronized？锁住的是冲突 bin，不是整个 Map。
- resize 为什么多个线程协作？ForwardingNode 和迁移索引让其他线程帮助搬迁。
- get 为什么大多数不加锁？通过可见字段和节点链路读取已发布结构。
- 为什么不允许 null？null 必须保留为缺失信号，避免并发查询歧义。

## 一句话复盘

ConcurrentHashMap 的核心是局部并发和原子复合方法，不是“所有操作都无锁”或“天然提供业务事务”。
