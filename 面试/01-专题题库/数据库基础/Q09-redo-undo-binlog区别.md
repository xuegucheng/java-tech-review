# Q09. redo log、undo log、binlog 的区别？

> 数据库基础 · P0 · 高频

**30 秒回答**：redo log 是 InnoDB 层的重做日志，循环写、保证崩溃恢复；undo log 是回滚日志兼 MVCC 版本链，逻辑在引擎内；binlog 是 Server 层的归档日志，追加写、服务主从复制与数据恢复，三者层次和用途完全不同。

**深入理解**：对比着记四个维度。**层次**：redo/undo 只有 InnoDB 有，binlog 是所有引擎通用的 Server 层日志。**内容**：redo 是物理逻辑页修改（哪个页改了什么）；undo 是行旧值镜像；binlog 是逻辑事件（行变更或语句，取决于格式）。**写入方式**：redo 固定大小循环覆盖，binlog 追加写可归档。**用途**：redo 用于崩溃恢复（crash-safe），undo 用于回滚和 MVCC，binlog 用于主从复制、按时间点恢复（配合全量备份）。binlog 的三种格式要知道：statement 记录 SQL 语句（日志小但某些函数不确定）、row 记录行变更（数据可靠、日志大、现网主流）、mixed 折中。RC 隔离级别下必须用 row 格式，否则主从会因语句执行顺序不一致而数据不一致。

**面试官可能继续追问**：
- **redo log 写满了怎么办？** —— 写满会阻塞写入并触发 checkpoint，把脏页刷盘后腾出空间；所以 redo log 组容量要按写入峰值配置，太小会频繁 checkpoint。
- **只靠 binlog 能不能做崩溃恢复？** —— 不能。binlog 可能丢最后几笔（未刷盘），且恢复要重放全部事件太慢；crash-safe 靠的是 redo log，binlog 的价值在主从和时间点恢复。

**实际工程场景**：排查数据被误删时，恢复顺序是「全量备份 + 增量 binlog 重放到误操作前」，所以 binlog 保留天数（`expire_logs_days`）要和备份策略匹配；只留 3 天 binlog 却宣称可以恢复到一周前，是常见的运维幻觉。

**容易答错的地方**：把 redo log 和 binlog 都叫「二进制日志」。两者一个在引擎层管恢复、一个在 Server 层管复制，主从复制用的是 binlog，崩溃恢复用的是 redo log。

> 权威内容：MySQL 日志系统当前尚未在仓库建设，可参考 MySQL 官方文档「InnoDB Redo Log」「Undo Log」「The Binary Log」章节。

---

返回 [数据库基础 专题](../数据库基础.md) · [专题题库首页](../README.md)
