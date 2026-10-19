# Q06. explain 里你看哪些字段？

> 数据库基础 · P0 · 高频

**30 秒回答**：我先看 `type`、`key`、`key_len` 判断索引有没有走对、用到了几列，再看 `rows` 和 `filtered` 判断预估代价，最后看 `Extra` 判断有没有额外排序、临时表或回表。

**深入理解**：`type` 从好到差大致是 system > const > eq_ref > ref > range > index > ALL，其中 `ref` 是走索引但非唯一匹配，`index` 是扫完整棵索引树但只读索引，`ALL` 是全表扫。`key` 是实际选用的索引，可能和 `possible_keys` 不一致，不一致本身就是「优化器认为别的索引更便宜」的信号；`key_len` 可以反推联合索引用到了前几列。`rows` 是预估扫描行数，乘上 `filtered` 能估出实际返回行数，两个数比单看一个更有意义。`Extra` 里 `Using index` 是覆盖索引不用回表，`Using where` 是回表后再过滤，`Using filesort` 是多了一次排序，`Using temporary` 说明用了临时表，`Using index condition` 表示索引下推生效。MySQL 8.0.18 起可以用 `EXPLAIN ANALYZE` 看到实测耗时，比纯预估更可信。

**面试官可能继续追问**：
- **`type=index` 和 `type=ALL` 哪个好？** —— `index` 是扫完整索引树但只读索引列，通常比 `ALL`（扫聚簇索引即全表）代价低，但两者都不算好，出现时都该继续优化。
- **`possible_keys` 有索引但 `key` 是 NULL，说明什么？** —— 优化器估算后认为走索引不划算（如数据量太小或选择性太差），这时要么改 SQL 让索引可用，要么接受全表扫，但不能假装索引生效了。

**实际工程场景**：WMS 慢 SQL 治理的标准动作就是「先 explain 再改」：`type` 掉到 `ALL` 且 `rows` 百万级 → 查索引是否被函数/隐式转换废掉；`Extra` 出现 `Using filesort` → 检查排序字段是否进索引；`Using temporary` → 考虑改写 group by 或加索引。

**容易答错的地方**：只看 `key` 不为空就认为没问题。`key` 只说明用了某个索引，`key_len` 才说明用了几列、`rows × filtered` 才说明扫描量——联合索引只用上前导列时，`key` 照样非空。

> 权威内容：MySQL 执行计划与 EXPLAIN 当前尚未在仓库建设，可参考 MySQL 官方文档「EXPLAIN Output Format」章节。

---

返回 [数据库基础 专题](../数据库基础.md) · [专题题库首页](../README.md)
