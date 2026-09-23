# AI 维护说明

本文件只约束 AI 助手在本仓库中的维护方式。内容架构和写作规则以现有文档为准：

- [仓库首页](README.md)
- [笔记编写规范](笔记编写规范.md)
- [仓库架构说明](项目文档/仓库架构说明.md)
- [贡献指南](CONTRIBUTING.md)

## 开工前

- 阅读上面的仓库契约；进入技术域前先读该模块的 `README.md`。
- 将 `项目文档/审计/` 下的报告视为历史快照，当前状态以实时 Git 和实际文件为准。
- 修改前检查当前分支、`HEAD`、`origin/main` 和 `git status --short`，保留已有改动。

## 修改边界

- 遵循技术域目录和架构说明中的 ownership matrix；定义只改唯一 Owner，Consumer 保留消费关系并链接回 Owner。
- 只做任务要求的最小改动；不批量重写文章、不批量改名、不为路线图创建空模块。
- 不执行 `reset --hard`、`clean -fd`、`checkout .`、`restore .`、历史重写或强制推送。
- Java 示例默认为 Java 21；区分规范、OpenJDK 实现和具体版本行为。

## 验证与提交

- 文档变更检查：`python scripts/check_markdown.py`、`python scripts/check_text_sanity.py`。
- 用户要求验证或任务授权验证时，Java 示例使用 `mvnw.cmd test`；根 reactor 已覆盖的模块无需重复运行。
- 提交前检查 `git diff --check`、`git status --short` 和 `git diff --stat`；只暂存本任务明确修改的路径。
- 仅在任务授权且变更全部属于本任务、验证完成时提交或推送。
