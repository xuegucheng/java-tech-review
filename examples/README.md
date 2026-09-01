# Runnable Examples

这个 Maven 工程只放验证机制所需的完整代码；正文保留关键片段和推理，避免把笔记变成源码仓库。

## 环境

- Java 21（`maven.compiler.release=21`）
- Maven 3.9+
- JUnit 5

## 运行

在仓库根目录执行：

```bash
mvn -f examples/pom.xml test
```

运行一个主类：

```bash
java -cp examples/target/classes com.xuegucheng.javatechreview.ParameterPassingDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.HashMapCollisionDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.LinkedHashMapLruDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.ClassInitializationDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.OverloadResolutionDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.PolymorphismDispatchDemo
java -cp examples/target/classes com.xuegucheng.javatechreview.FinalReferenceDemo
```

## 示例索引

| 示例 | 验证的结论 |
| --- | --- |
| `ParameterPassingDemo` | 引用值按值传递；改对象可见，重绑参数不可见 |
| `HashMapCollisionDemo` | hash 冲突不等于 key 相等，最终还要使用 equals |
| `LinkedHashMapLruDemo` | access-order、访问后移动和 `removeEldestEntry` |
| `ClassInitializationDemo` | Holder 的延迟初始化和类初始化边界 |
| `OverloadResolutionDemo` | 严格转换、基本类型宽化与可变参数优先级 |
| `PolymorphismDispatchDemo` | 编译时重载选择与运行时重写分派 |
| `FinalReferenceDemo` | final 引用可变与不可变快照的差异 |

新增示例时，必须同时增加可重复执行的测试，并从对应 Interview Review 或 Deep Dive 反向链接回来。
