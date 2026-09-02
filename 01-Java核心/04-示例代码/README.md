# Java 核心示例

本目录只放 Java 核心模块的完整可运行类和可重复测试。正文保留推理与关键片段，具体验证通过本模块完成。

## 运行

在仓库根目录执行：

```bash
mvn -pl '01-Java核心/04-示例代码' test
```

也可以执行整个仓库的统一验证：

```bash
mvn test
```

## 示例索引

| 示例 | 验证的结论 | 主类 | 测试 |
| --- | --- | --- | --- |
| 参数传递 | 引用值按值传递；改对象可见，重绑参数不可见 | [ParameterPassingDemo.java](src/main/java/com/xuegucheng/javatechreview/core/ParameterPassingDemo.java) | [ParameterPassingDemoTest.java](src/test/java/com/xuegucheng/javatechreview/core/ParameterPassingDemoTest.java) |
| 多态分派 | 编译时重载选择与运行时重写分派 | [PolymorphismDispatchDemo.java](src/main/java/com/xuegucheng/javatechreview/core/PolymorphismDispatchDemo.java) | [PolymorphismDispatchDemoTest.java](src/test/java/com/xuegucheng/javatechreview/core/PolymorphismDispatchDemoTest.java) |
| 重载解析 | 严格转换、基本类型宽化与可变参数优先级 | [OverloadResolutionDemo.java](src/main/java/com/xuegucheng/javatechreview/core/OverloadResolutionDemo.java) | [OverloadResolutionDemoTest.java](src/test/java/com/xuegucheng/javatechreview/core/OverloadResolutionDemoTest.java) |
| 类初始化 | Holder 的延迟初始化和类初始化边界 | [ClassInitializationDemo.java](src/main/java/com/xuegucheng/javatechreview/core/ClassInitializationDemo.java) | [ClassInitializationDemoTest.java](src/test/java/com/xuegucheng/javatechreview/core/ClassInitializationDemoTest.java) |
| final 引用 | final 引用可变与不可变快照的差异 | [FinalReferenceDemo.java](src/main/java/com/xuegucheng/javatechreview/core/FinalReferenceDemo.java) | [FinalReferenceDemoTest.java](src/test/java/com/xuegucheng/javatechreview/core/FinalReferenceDemoTest.java) |

每个示例都必须有对应测试，并由 [Java 核心模块 README](../README.md) 或对应主题文章反向链接。
