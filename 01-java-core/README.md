# Java Core

> Java 语言、对象模型和运行边界。面向已经会写 Java 的后端工程师，优先恢复面试表达，再按需进入原理长文。

返回 [仓库首页](../README.md)。本模块不承担并发、JVM、Spring 或数据库的完整解释；那些主题进入各自模块后再维护唯一权威来源。

## 先从哪里开始

### 1 天突击

按这个顺序只读 Interview Review：

1. [参数传递与不可变对象](interview/parameter-passing-and-immutability.md)
2. [对象模型与多态](interview/object-model-and-polymorphism.md)
3. [相等性与对象契约](interview/equality-and-object-contract.md)
4. [类型设计与组合](interview/designing-types-with-composition.md)

### 7 天复习中的 Java Core

先完成上面的 P0，再补 [运行链路与版本边界](interview/runtime-and-version-boundaries.md)、[类型转换陷阱](interview/type-and-conversion-traps.md) 和 [类初始化与 final](interview/class-initialization-and-final.md)。需要证明结论时，直接跳到对应 Deep Dive 或 [examples](../examples/README.md)。

## Interview Review

| 优先级 | 主题 | 目标 |
| --- | --- | --- |
| P0 | [参数传递与不可变对象](interview/parameter-passing-and-immutability.md) | 讲清引用值、别名、副作用和防御性复制 |
| P0 | [对象模型与多态](interview/object-model-and-polymorphism.md) | 讲清编译时类型、运行时类型、重写和动态绑定 |
| P0 | [相等性与对象契约](interview/equality-and-object-contract.md) | 讲清 `equals/hashCode`，并连接 Hash 集合 |
| P1 | [类型设计与组合](interview/designing-types-with-composition.md) | 从继承、接口、组合和不变量回答设计题 |
| P1 | [运行链路与版本边界](interview/runtime-and-version-boundaries.md) | 区分 JDK/JVM/字节码与 JDK 8/17/21/25 |
| P1 | [类型转换陷阱](interview/type-and-conversion-traps.md) | 处理数值提升、溢出、浮点和金额问题 |
| P1 | [类初始化与 final](interview/class-initialization-and-final.md) | 解释初始化顺序、编译期常量与安全发布边界 |

## Deep Dive 资产

这些文章保留了原仓库的长文、规范边界、工程讨论、源码阅读清单和面试题；它们不是首页的必读顺序。

| 主题 | Deep Dive |
| --- | --- |
| 运行与工具链 | [platform-and-execution.md](deep-dive/platform-and-execution.md) | JDK/JRE/JVM、字节码、JIT |
| 类型与变量 | [types-and-variables.md](deep-dive/types-and-variables.md) | 静态类型、基本类型、引用、作用域与遮蔽 |
| 转换与数值精度 | [conversions-and-numeric-precision.md](deep-dive/conversions-and-numeric-precision.md) | 宽化/窄化、二元数值提升、溢出、浮点与 BigDecimal |
| 表达式与运算符 | [operators.md](deep-dive/operators.md) | 算术、逻辑、位运算、三元、instanceof 与优先级 |
| 控制流 | [control-flow.md](deep-dive/control-flow.md) | if/switch/循环、break/continue、标签与可达性 |
| 方法与重载 | [methods-and-overloading.md](deep-dive/methods-and-overloading.md) | 方法签名、重载、可变参数、递归与可达性 |
| 参数传递 | [parameter-passing.md](deep-dive/parameter-passing.md) | 值传递、引用副本、防御性复制与深浅拷贝 |
| 对象与类设计 | [object-model-and-class-design.md](deep-dive/object-model-and-class-design.md) | 类、对象、字段、实例行为与不变量 |
| 封装与构造器 | [encapsulation-and-constructors.md](deep-dive/encapsulation-and-constructors.md) | 构造器、this、封装与访问控制 |
| 对象创建与不可变设计 | [object-creation-and-immutability.md](deep-dive/object-creation-and-immutability.md) | 工厂、Builder、对象关系与不可变设计 |
| 继承与重写 | [inheritance-and-overriding.md](deep-dive/inheritance-and-overriding.md) | 继承、super、重写与父子类初始化 |
| 多态与动态绑定 | [polymorphism-and-dynamic-dispatch.md](deep-dive/polymorphism-and-dynamic-dispatch.md) | 编译时类型、运行时类型、动态绑定与转换 |
| 抽象类 | [abstract-classes.md](deep-dive/abstract-classes.md) | 抽象类、抽象方法与模板方法 |
| 接口 | [interfaces.md](deep-dive/interfaces.md) | 默认方法、静态方法、冲突处理与接口演进 |
| 组合与设计原则 | [composition-and-design-principles.md](deep-dive/composition-and-design-principles.md) | 组合、委托、接口隔离、DIP 与演进 |
| static 与类初始化 | [static-and-class-initialization.md](deep-dive/static-and-class-initialization.md) | static、初始化顺序与类加载边界 |
| final 与常量 | [final-and-constants.md](deep-dive/final-and-constants.md) | final、编译期常量、初始化安全与兼容性 |
| equals 与 hashCode | [equals-and-hashcode-contract.md](deep-dive/equals-and-hashcode-contract.md) | equals、hashCode 与对象相等性契约 |
| Object 方法与工具 | [object-methods-and-utilities.md](deep-dive/object-methods-and-utilities.md) | toString、getClass、clone 与 Object 方法 |

## 版本边界

- 经典语言结论默认按 `JDK 8+` 表达。
- `Java 17+`、`Java 21+`、`Java 25+` 的新语法或 API 必须在标题、段落或代码前显式标记。
- `record` 从 Java 16 正式可用，`sealed` 从 Java 17 正式可用，Sequenced Collections 从 Java 21 可用。
- Java 25 的新入口或构造器语法是补充，不改变企业工程中最常见的传统写法。

## One Source of Truth

本模块维护语言层契约：变量/引用、参数传递、对象不变量、相等性、继承/接口和初始化。集合如何消费 `equals/hashCode` 由 [02-collections](../02-collections/README.md) 解释；并发模块未来接管 `wait/notify` 的完整机制。

## 图示与实验

- mental model 总入口：[Java 复习闭环](../diagrams/java/review-loop.svg)
- 可运行验证：[examples/](../examples/README.md)
- 图示职责：[diagrams/README.md](../diagrams/README.md)
