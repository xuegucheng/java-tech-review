# 集合框架示例

本目录只放集合框架模块的完整可运行类和可重复测试。它验证数据结构和顺序语义，不承担 Java 语言契约的唯一权威解释。

## 运行

在仓库根目录执行：

```bash
# macOS / Linux
./mvnw -pl '02-集合框架/04-示例代码' test
```

Windows PowerShell：

```powershell
.\mvnw.cmd -pl '02-集合框架/04-示例代码' test
```

也可以执行整个仓库的统一验证：

```bash
./mvnw test
```

Windows PowerShell 使用 `.\mvnw.cmd test`。

## 示例索引

| 示例 | 验证的结论 | 主类 | 测试 |
| --- | --- | --- | --- |
| HashMap 冲突 | hash 冲突不等于 key 相等，最终还要使用 equals | [HashMapCollisionDemo.java](src/main/java/com/xuegucheng/javatechreview/collections/HashMapCollisionDemo.java) | [HashMapCollisionDemoTest.java](src/test/java/com/xuegucheng/javatechreview/collections/HashMapCollisionDemoTest.java) |
| LinkedHashMap LRU | access-order、访问后移动和 `removeEldestEntry` | [LinkedHashMapLruDemo.java](src/main/java/com/xuegucheng/javatechreview/collections/LinkedHashMapLruDemo.java) | [LinkedHashMapLruDemoTest.java](src/test/java/com/xuegucheng/javatechreview/collections/LinkedHashMapLruDemoTest.java) |

每个示例都必须有对应测试，并由 [集合框架模块 README](../README.md) 或 HashMap/LinkedHashMap 主题文章反向链接。
