# 相等性与对象契约

> **P0 · JDK 8+ · 5 分钟复习**

## 面试结论

`==` 对引用比较身份；`equals` 表达逻辑相等；`hashCode` 为哈希容器提供候选定位。若两个对象 `equals` 相等，它们必须拥有相同的 `hashCode`；反过来不成立。

## 30 秒回答

```text
先确定业务语义：值相等还是身份相等
        ↓
equals 满足自反、对称、传递、一致、非空
        ↓
相等对象必须有相同 hashCode
        ↓
HashMap/HashSet 先按 hash 定位，再用 equals 确认
```

实现前先决定类型策略：`getClass()` 更严格，`instanceof` 允许子类型参与；实体、值对象、ORM 代理和可变 key 没有一个通用模板。

## 追问链

1. 只重写 `equals` 会怎样？逻辑相等对象可能进入不同桶，HashMap/HashSet 行为异常。
2. hash 冲突会覆盖吗？不会只因 hash 相同就覆盖，还要继续用 `equals` 判断。
3. 为什么可变 key 危险？放入容器后参与相等性的字段改变，查找会按新 hash 去错误桶。
4. `compareTo() == 0` 等于 `equals()` 吗？不保证；排序集合的唯一性依据是比较器/自然顺序。
5. `BigDecimal` 有什么陷阱？`equals` 还比较 scale，数值业务常需要明确 `compareTo` 语义。

## One Source of Truth

本页只维护对象相等性的语言契约。HashMap 如何消费这个契约、HashSet 如何复用 HashMap，见 [Collections 的 Hash 集合复习](../../02-集合框架/01-面试速记/HashMap与Hash集合.md)。

## Deep Dive

- [equals 与 hashCode 契约](../02-深度解析/equals与hashCode契约.md)
- [Object 方法与工具](../02-深度解析/toString与对象工具方法.md)

## 一句话复盘

> equals 定义逻辑相等，hashCode 负责把相等对象送进同一候选范围，作为 key 的状态必须稳定。
