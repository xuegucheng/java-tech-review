# Q09. `==` 和 equals 的区别？

> Java基础 · P0 · 高频

**30 秒回答**：`==` 对基本类型比值、对引用类型比「是不是同一个实例」；`equals` 是 `Object` 的方法，默认实现就是 `==`，但可以被重写成「值相等或逻辑相等」的比较——`String`、包装类、集合都重写了它。

**深入理解**：

| 场景 | `==` | `equals` |
| --- | --- | --- |
| 基本类型 | 比值 | 不适用 |
| 引用类型、未重写 equals | 同一引用才为 true | 等价于 `==` |
| `String` 字面量之间 | 常量池命中为 true | 逐字符比较 |
| `new String("a")` 与 `"a"` | false | true |
| `Integer` 128 与 128 | false（超出缓存） | true |

关键边界：`String` 的 `==` 结果取决于实例是否来自常量池——`"a" + "b"` 编译期折叠成字面量，`== "ab"` 为 true；但含变量的拼接（如 `String s = "a"; s + "b"`）运行期生成新对象，`== "ab"` 为 false。包装类的 `==` 还受缓存区间影响（见 Q07）。

阿里巴巴 Java 开发规约与静态检查插件（p3c）要求：`equals` 的调用方放常量或确定非空的对象（`"x".equals(y)` 优于 `y.equals("x")`）。另一个实操坑是代码生成：IDEA 生成的 `equals`/`hashCode` 模板在类新增字段后不会自动更新——字段加了、契约没重新生成，是生产 bug 的常见来源。

**为什么这样设计**：`==` 必须是最便宜的同一性判断，语言级语义不能给重写留口子；`equals` 留给类型自己定义「什么算相等」。代价是两套「相等」并存，工程师必须在每个比较点想清楚要哪一种——这也是这道题几乎每轮都出现的原因。

**面试官可能继续追问**：

- **Object.equals 的默认实现是什么？** —— `return this == obj;`，即同一性比较。不重写 `equals` 的类（`Object`、`Thread`、`StringBuilder` 等）用 `==` 和 `equals` 等价。
- **重写 equals 要遵守哪些约束？** —— 自反、对称、传递、一致、对 null 返回 false。违反对称性会让集合行为不可预测，如 `Timestamp` 继承 `Date` 后双向比较结果不一致（已知的 JDK 设计缺陷）。
- **hashCode 相等 equals 一定相等吗？** —— 不一定，哈希冲突合法；反过来 equals 相等则 hashCode 必须相等，否则哈希容器失效（Q02）。

**实际工程场景**：枚举比较用 `==`（JVM 保证单例）；实体对象进 `HashSet`/`HashMap` 前先确认 `equals`/`hashCode` 已按业务主键重写；DTO 跨服务传输用 `equals` 比字段，不要依赖 `==`；null 安全比较统一 `Objects.equals(a, b)`。

**容易答错的地方**：认为「String 内容一样 `==` 就为 true」——只有常量池折叠或 `intern()` 后才成立；认为 `equals` 就是「值比较」——它只是「对象自己定义的相等」，语义由类型决定；忽略数组要用 `Arrays.equals`——`Object.equals` 比两个数组永远是比引用。

> 权威内容：[运算符与表达式陷阱](../../../01-Java核心/02-深度解析/运算符与表达式陷阱.md) · [equals 与 hashCode 契约](../../../01-Java核心/02-深度解析/equals与hashCode契约.md) · [equals 与 hashCode 对象契约](../../../01-Java核心/01-面试速记/equals与hashCode对象契约.md)

---

返回 [Java基础 专题](../Java基础.md) · [专题题库首页](../README.md)
