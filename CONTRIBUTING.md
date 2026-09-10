# 贡献指南

感谢关注 `java-tech-review`。这是一个面向有经验 Java 后端工程师的通用复习仓库，目标是帮助读者恢复面试表达、建立原理模型、定位关键源码，并通过小型示例验证结论。

## 快速开始

仓库默认使用 Java 21、JUnit 5 和 Maven Wrapper。

Windows PowerShell：

```powershell
.\mvnw.cmd test
```

macOS / Linux：

```bash
chmod +x mvnw
./mvnw test
```

检查 Markdown 链接和代码围栏：

```bash
python scripts/check_markdown.py
```

检查已知文本污染：

```bash
python scripts/check_text_sanity.py
```

## 内容约定

- 面向有经验的 Java 后端工程师，不写从零开始的语法教程或 API 清单。
- 每个主题优先给出一句话面试结论，再解释原理、关键源码和工程边界。
- 正文和默认示例使用通用领域模型；行业案例只能作为可选工程案例。
- 定义性内容只保留一个权威来源，其他文章解释消费关系并链接回去。
- 完整示例和测试放在所属模块的 `04-示例代码/`，不要把大段测试代码嵌入正文。
- Mermaid 用于流程、状态和决策；SVG 用于数组、桶、链表、队列和对象布局等结构图。
- 版本相关结论必须标明 JDK 版本，并区分语言规范、OpenJDK 实现和工程建议。

## 新增主题

1. 先确定技术域和唯一权威文章。
2. 在 `01-面试速记/` 提供可复述的面试入口。
3. 在 `02-深度解析/` 说明原理、源码路径和工程边界。
4. 只有能降低理解成本时，才增加 `03-图示/` 资产。
5. 需要可重复验证时，在 `04-示例代码/` 增加主类和测试。
6. 更新模块 README 的主题资源地图，并运行测试和 Markdown 检查。

## Pull Request 检查清单

- [ ] 变更是否保持通用定位，没有引入公司、系统或内部业务标识？
- [ ] 是否更新了对应模块 README 或主题资源地图？
- [ ] 是否避免创建第二个“唯一权威”解释？
- [ ] `python scripts/check_markdown.py` 通过。
- [ ] `python scripts/check_text_sanity.py` 通过。
- [ ] `./mvnw test` 或 Windows 下的 `.\mvnw.cmd test` 通过（涉及示例代码时）。
- [ ] 新增图示、代码和外部资料的许可证边界明确。

## 许可证

仓库作者创作的笔记、示例代码和图示按 MIT License 发布。`mvnw` 和 `mvnw.cmd` 保留 Maven Wrapper 项目自带的 Apache License 2.0 声明；外部链接指向的资料遵循各自许可证。
