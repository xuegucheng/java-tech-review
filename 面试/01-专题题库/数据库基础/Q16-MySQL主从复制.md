# Q16. MySQL 主从复制原理？

> 数据库基础 · P1 · 原理

**30 秒回答**：主库把变更写进 binlog，从库的 IO 线程拉取 binlog 写入本地 relay log，SQL 线程再重放 relay log 应用到自己的数据——异步链路决定了主从延迟是常态，一致性要靠半同步或组复制补强。

**深入理解**：三个线程的分工要能说出来：主库 dump thread 推（或从库拉）binlog 事件；从库 IO thread 写 relay log；从库 SQL thread 按序应用。复制模式分三档：异步（默认，主库提交即返回、不等从库，宕机可能丢数据）、半同步（`semi-sync`，至少一个从库收到 relay log 才返回提交，MySQL 5.5+ 引入，5.7 改进为 after-commit 与 lossless 语义）、组复制（MGR，基于 Paxos 的多数派提交，强一致但部署复杂度高）。延迟的根因通常是：大事务（一条 DDL/大批量 UPDATE 让 SQL 线程单线程重放到天荒地旧）、从库硬件差、并行复制参数保守。GTID（全局事务 ID）让从库可以按事务号续传，跳过已应用的，切换主库时不用找 binlog 文件名和位点。

**面试官可能继续追问**：
- **主从延迟怎么排查和处理？** —— 看 `Seconds_Behind_Master`（不准，只反映 SQL 线程进度）不如直接对比 GTID 差距；处理上拆大事务、开并行复制（`slave_parallel_type=LOGICAL_CLOCK`）、从库升配；读请求按延迟容忍度路由。
- **为什么 MySQL 8.0 的 GTID 是标配推荐？** —— 切换、重建从库、failover 都以 GTID 为锚点，避免文件名+位点的脆弱性；8.0 下 GTID 模式还简化了复制拓扑管理。

**实际工程场景**：WMS 报表查询走从库，但「下单后立刻查到订单」这类刚写入就读的流量必须回主库——按「写后立即读」识别主库路由，其余报表类读从库，是延迟与负载的常规平衡。主从延迟告警要按 GTID 差而不是 `Seconds_Behind_Master`，后者在网络分区时会显示 0 造成假安全。

**容易答错的地方**：认为「半同步 = 不丢数据」。半同步只保证至少一个从库收到（未应用）relay log，从库应用前宕机、或 ACK 后主库崩溃的场景仍有窗口；真正强一致要上组复制。

> 权威内容：MySQL 主从复制当前尚未在仓库建设，可参考 MySQL 官方文档「Replication Implementation」「Semi-Synchronous Replication」章节。

---

返回 [数据库基础 专题](../数据库基础.md) · [专题题库首页](../README.md)
