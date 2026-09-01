# Diagram Rules

图示服务于 mental model，不重复正文。

## 选择格式

- **Mermaid**：流程、决策、生命周期、状态机和简单时序。它靠近文章，便于随着推理一起修改。
- **SVG**：数据结构、内存引用、指针、对象布局、桶和链表等需要精确定位的结构图。SVG 必须自包含、可缩放、无需外部字体/图片/base64 资源。
- **PNG/JPG**：只在已有位图确实提供不可替代的信息时保留；新建技术结构图默认优先 SVG。

## 质量检查

每张图都应有清晰标题、可读文字和一个明确问题。提交前检查 XML 合法、相对链接正确、GitHub 可直接渲染，并确认图没有把正文逐字再讲一遍。

## Java Core 图示索引

- [重载解析](java/overload-resolution.svg)：严格调用、宽松调用、可变参数和最具体匹配；
- [Java 参数传递](java/java-pass-by-value.svg)：引用值副本、对象修改与形参重绑；
- [多态分派](java/polymorphism-dispatch.svg)：编译时重载与运行时重写的两阶段关系；
- [类初始化](java/class-initialization.svg)：加载、链接、准备和初始化；
- [final 引用与不可变对象](java/final-reference-vs-immutable.svg)：引用槽约束与对象状态约束。
