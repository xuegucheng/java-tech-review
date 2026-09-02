# 类初始化与 final

> **P1 · JDK 8+ · 5 分钟复习**

## 面试结论

类初始化是由 JVM 按需触发的受控过程，静态字段、静态初始化块和父子类初始化有明确顺序；编译期常量可能在使用方被内联。`final` 只限制变量再次赋值，不等于对象不可变，也不自动等于线程安全。

## 30 秒回答

```text
加载/链接完成
    ↓
首次主动使用触发初始化
    ↓
父类初始化
    ↓
静态字段与静态初始化块按源代码顺序执行
```

实际回答时要区分：类初始化、实例初始化、字段可见性、编译期常量和对象内部状态。Holder 惯用法利用类初始化的线程安全边界，但缓存对象是否可变仍是另一个问题。

## 追问链

1. `static final int` 一定是编译期常量吗？只有满足常量变量条件才是；引用对象或运行时计算不是。
2. `final List` 能不能 add？可以，`final` 固定引用，不固定对象状态。
3. 静态初始化失败会怎样？初始化异常会影响后续主动使用，具体异常类型和类状态需要按 JVM 规则分析。
4. final 字段能否替代并发发布？不能把语言级 final 语义扩大成完整的共享可变状态安全策略。

## Deep Dive

- [static 与类初始化](../02-深度解析/static与类初始化.md)
- [final 与常量](../02-深度解析/final与常量设计.md)
- [对象创建与不可变设计](../02-深度解析/对象创建与不可变设计.md)

## Runnable Example

- [ClassInitializationDemo.java](../04-示例代码/src/main/java/com/xuegucheng/javatechreview/core/ClassInitializationDemo.java)
- [对应测试](../04-示例代码/src/test/java/com/xuegucheng/javatechreview/core/ClassInitializationDemoTest.java)

## 一句话复盘

> final 保护绑定，初始化保护类级顺序；对象是否可变、共享是否安全要另外证明。
