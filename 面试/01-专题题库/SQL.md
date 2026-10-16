# SQL

> 一句话定位：这一专题在面试里考的是「查询语法 + 执行计划 + 慢 SQL 治理 + 手写 SQL」四件事——语法是敲门砖，真正的区分度在能不能用 EXPLAIN 说话、把慢 SQL 定位治理闭环、把业务问题翻译成逻辑严密的 SQL；与数据库基础专题的分工是：索引结构、MVCC、锁、执行计划字段的底层机制在 [数据库基础](数据库基础.md)，这里只给面试口径的结论并回链。

返回 [专题题库首页](README.md) · [面试首页](../README.md)。

## 专题定位

- SQL 题是 Java 后端面试的「基础盘」：JOIN、GROUP BY、慢 SQL 几乎每轮都问，手写 SQL 是笔试与现场白板的高频形式，答不出来会直接暴露「平时只写 CRUD、没治理过数据库」。
- 面试官主要验证四件事：语法细节是否准确（ON 与 WHERE、HAVING 与 WHERE 的区别）；能否用 `EXPLAIN` 说话而不是背规则；慢 SQL 有没有完整的「采集 → 定位 → 治理 → 验证」闭环；手写 SQL 是否逻辑严密、能正确处理并列、重复、边界数据。
- 5 年以上工程师要答到「取舍」层面：EXISTS vs IN 要能说出数据量前提，深分页要能说出游标分页的代价，大数据量要能说出分区表、冷热分离、分库分表各自解决什么、不解决什么。
- 本专题与 [数据库基础](数据库基础.md) 的分界：索引与 B+ 树、MVCC、隔离级别、锁与死锁、执行计划字段详解在那边；这里只给面试口径的结论，需要深入时给出相对链接。
- 所有手写示例基于 MySQL 8.0（窗口函数、CTE 均为 8.0 特性）；涉及 5.7 与 8.0 行为差异的地方单独标注，如 ONLY_FULL_GROUP_BY 默认开启、Skip Scan（8.0.13）、隐藏索引（8.0）。

## 高频题目导航

| 编号 | 面试问题 | 重要程度 | 追问深度 |
| --- | --- | --- | --- |
| Q01 | SQL 有哪几类，DDL/DML/DQL/DCL/TCL 分别是什么？ | P2 | 原理 |
| Q02 | JOIN 有哪几种，ON 和 WHERE 有什么区别？ | P0 | 工程实践 |
| Q03 | GROUP BY 和 HAVING 有什么区别，ONLY_FULL_GROUP_BY 是什么？ | P0 | 工程实践 |
| Q04 | 子查询有哪几种，相关子查询为什么慢？ | P1 | 原理 |
| Q05 | EXISTS 和 IN 怎么选，NOT IN 有什么坑？ | P1 | 工程实践 |
| Q06 | 排序和分页怎么写，深分页怎么优化？ | P1 | 工程实践 |
| Q07 | 慢 SQL 怎么分析，完整的定位治理流程是什么？ | P0 | 排查 |
| Q08 | 哪些写法会让索引失效，怎么改？ | P0 | 排查 |
| Q09 | 大数据量表怎么查询和设计？ | P1 | 工程实践 |
| Q10 | 手写：怎么查连续登录 N 天的用户？ | P1 | 工程实践 |
| Q11 | 手写：每个部门薪资前 3 名？ | P0 | 工程实践 |
| Q12 | 手写：行转列与列转行？ | P1 | 工程实践 |
| Q13 | 手写：查找并删除重复数据保留一条？ | P1 | 工程实践 |
| Q14 | 手写：积分并列名次？ | P2 | 原理 |

## 题目正文

> 以下所有手写 SQL 示例基于 MySQL 8.0；需要 5.7 兼容写法时在对应题目内单独说明。

### Q01：SQL 有哪几类，DDL/DML/DQL/DCL/TCL 分别是什么？ `P2` `低频`

**30 秒回答**：按功能分五类：DDL 管结构定义，典型语句 CREATE/ALTER/DROP/TRUNCATE；DML 管行数据操作，INSERT/UPDATE/DELETE；DQL 管查询，就是 SELECT；DCL 管权限，GRANT/REVOKE；TCL 管事务控制，COMMIT/ROLLBACK/SAVEPOINT。面试里真正会展开的是边界：TRUNCATE 属于 DDL 所以不能回滚，SELECT 严格说不属于 DML。

**深入理解**：

| 类别 | 全称 | 典型语句 | 关键性质 |
| --- | --- | --- | --- |
| DDL | Data Definition Language | CREATE / ALTER / DROP / TRUNCATE / RENAME | 操作表结构，执行前后隐式提交，不能回滚 |
| DML | Data Manipulation Language | INSERT / UPDATE / DELETE | 操作行数据，在事务内可回滚 |
| DQL | Data Query Language | SELECT | 只读查询，日常使用频率最高的一类 |
| DCL | Data Control Language | GRANT / REVOKE / CREATE USER | 权限管理，不碰数据 |
| TCL | Transaction Control Language | COMMIT / ROLLBACK / SAVEPOINT | 控制事务边界 |

**面试官可能继续追问**：
- **TRUNCATE 和 DELETE 有什么区别？** —— TRUNCATE 是 DDL，内部近似「DROP 后按原结构重建」，不逐行删、不触发行触发器、不能带 WHERE、不能回滚，自增计数器重置；DELETE 是 DML，逐行删、可回滚、可带条件，已删除的自增值不复用（5.7 起明确、8.0 延续）。所以「清空一张大表」和「删掉一部分数据」是两条不同的路。
- **DDL 为什么会隐式提交？** —— 这是 MySQL 实现上的选择而非 SQL 标准要求：DDL 执行前后会自动提交当前事务。工程含义是「改表结构」和「改数据」不要放进同一个事务指望一起回滚；在线改大表还要单独评估 MDL 锁的影响（锁的部分详见 [数据库基础](数据库基础.md)）。
- **SAVEPOINT 解决什么问题？** —— 它让事务可以部分回滚：一个大事务里某一步失败时回滚到保存点而不是整个事务，代价是保存点过多时 undo 与日志管理变复杂，通常只用于「批量处理允许部分失败」的场景。

**容易答错的地方**：
- 把 SELECT 归进 DML（狭义 DML 只含 INSERT/UPDATE/DELETE，SELECT 单独归 DQL）。
- 说 TRUNCATE 可以回滚，或者说 TRUNCATE 比 DELETE 慢——真相相反，TRUNCATE 快得多且不可回滚。
- 把 GRANT 说成 DML；把 `SET autocommit` 说成 DCL——它属于 TCL 的事务控制范畴。

> 权威内容：SQL 语句分类当前尚未在仓库建设，可参考 MySQL 官方文档「SQL Statements」章节的分类目录。

---

### Q02：JOIN 有哪几种，ON 和 WHERE 有什么区别？ `P0` `高频`

**30 秒回答**：INNER JOIN 只保留两边匹配的行；LEFT JOIN 保留左表全部行、右表不匹配补 NULL；RIGHT JOIN 相反；FULL OUTER JOIN 两边都保留，但 MySQL 各版本都不支持这个关键字，要用 LEFT JOIN UNION RIGHT JOIN 模拟；CROSS JOIN 是笛卡尔积，一般要避免。ON 和 WHERE 的区别集中在外连接：LEFT JOIN 把右表的过滤条件写在 WHERE 里，会把补 NULL 的行全部滤掉，语义退化成内连接——这是最经典的坑。

**深入理解**：

```sql
-- 想查「所有用户及其已支付的订单」，没有支付订单的用户也要出现：
SELECT u.id, u.name, o.order_no
FROM user u
LEFT JOIN orders o ON o.user_id = u.id AND o.status = 'PAID';  -- 正确：没支付订单的用户仍保留，o 列为 NULL

SELECT u.id, u.name, o.order_no
FROM user u
LEFT JOIN orders o ON o.user_id = u.id
WHERE o.status = 'PAID';  -- 错误：没支付订单的用户被 WHERE 整行过滤，等价于 INNER JOIN
```

- **驱动表与被驱动表**：MySQL 的 JOIN 底层是嵌套循环——被驱动表的关联列有索引时逐键查找，8.0 起等值连接在关联列无可用索引时还可选 hash join。INNER JOIN 的驱动表由优化器按代价选；LEFT JOIN 的左表、RIGHT JOIN 的右表被语法固定为驱动表；`STRAIGHT_JOIN` 可强制连接顺序。
- **小表驱动大表**：嵌套循环里外层每行都要去内层查一次，所以外层行数越少总代价越低。准确说法是「过滤后行数少的表驱动行数多的表」，不是单纯看表大小。
- **三张表 JOIN 的驱动表选择**：先用 `EXPLAIN` 看最外层执行的是哪张表（简单连接里输出第一行即驱动表）；再确认每张被驱动表的 JOIN 列都有索引；过滤性最强的条件尽量前置，LEFT JOIN 场景把过滤后行数仍然最少的表放左边。
- **CROSS JOIN 与 USING/NATURAL JOIN**：MySQL 里 CROSS JOIN 等价于不带 ON 的 INNER JOIN，显式写出来是为了表达笛卡尔积意图；`USING(col)` 等价于两列等值连接且合并输出列，NATURAL JOIN 自动按同名列连接，可读性和可控性都差，生产 SQL 不建议用。

**为什么这样设计**：ON 是「连接条件」，在生成结果行的过程中生效，决定右表不匹配时补不补 NULL；WHERE 是「结果过滤」，在连接完成后生效，决定整行留不留。SQL 把这两个阶段分开，才能同时表达「保留左表全部」和「只要右表满足条件」两种语义；右表条件写进 ON 而不是 WHERE，用的就是「不匹配也保留左表行」这个语义，而不是写完 WHERE 再把行删掉。

**面试官可能继续追问**：
- **LEFT JOIN 的 ON 里放左表条件呢？** —— 语义基本等价于放 WHERE，因为左表是驱动表，它的条件本来就在连接过程中逐行判断；但按「连接条件归 ON、过滤条件归 WHERE」分工可读性更好，也方便以后改连接类型时不改变语义。
- **LEFT JOIN 一定比 INNER JOIN 慢吗？** —— 不一定，两者都走嵌套循环，代价取决于驱动表行数和被驱动表索引；LEFT JOIN 只是语法上固定了驱动表，限制了优化器选连接顺序的空间，多表复杂查询里 INNER JOIN 反而更容易选出好的顺序。
- **怎么让 MySQL 用上 hash join？** —— 关联列没有可用索引、且优化器预估连接结果集不大时会自动选；也可以用 `HASH_JOIN` 优化器 hint 显式指定。但生产上的常规顺序是先补索引，hash join 是没有索引时的兜底而不是首选。

**实际工程场景**：WMS 的出库查询是典型多表 JOIN：出库单主表关联出库明细、库位、SKU 档案。一类典型写法事故是「查所有出库单、有物流信息的带出物流状态」，写成 LEFT JOIN 之后又把 `WHERE logistics_no IS NOT NULL` 加在最后，结果只剩下有物流信息的单，业务侧看到「数据少了」——右表条件决定补不补 NULL，必须放 ON。另一类是把租户、仓库这类过滤条件随意散落在 ON 和 WHERE 里，后人难以分辨哪些条件会砍掉 NULL 行。团队 SQL 规范的常见约定就是：右表条件放 ON，纯粹过滤结果的放 WHERE。

**容易答错的地方**：
- 说 MySQL 支持 FULL OUTER JOIN 关键字——至今不支持，必须 LEFT JOIN UNION RIGHT JOIN 模拟（UNION 会去重，数据量大时注意代价；确认左右互补无重叠可用 UNION ALL）。
- 认为 ON 里的条件「不参与过滤」——ON 决定匹配关系，对右表当然是一种筛选，准确说法是「不过滤掉左表的行」。
- 说 RIGHT JOIN 只是 LEFT JOIN 的反写所以随便用——RIGHT JOIN 把驱动表固定在右侧，可读性差且限制优化器，生产上通常改写为 LEFT JOIN。
- 驱动表选择答成「永远小表驱动」而不提 LEFT JOIN 的驱动表是被语法固定的。

> 权威内容：JOIN 语法与优化器连接顺序当前尚未在仓库建设，可参考 MySQL 官方文档「JOIN Clause」「Nested-Loop Join Algorithms」「Hash Join」章节。

---

### Q03：GROUP BY 和 HAVING 有什么区别，ONLY_FULL_GROUP_BY 是什么？ `P0` `高频`

**30 秒回答**：GROUP BY 按列分组做聚合，WHERE 在分组前过滤行、HAVING 在分组后过滤分组，所以聚合条件必须写 HAVING，能提前到 WHERE 的条件不要拖到 HAVING。ONLY_FULL_GROUP_BY 是 MySQL 5.7 起默认开启的 sql_mode，要求 SELECT 里的非聚合列必须函数依赖于 GROUP BY 列（出现在分组列里，或能由分组列唯一决定，如 GROUP BY 主键），否则直接报错；5.7 之前默认关闭，那些 SQL 靠「随机取一个值」的宽松行为跑着。

**深入理解**：

- **逻辑执行顺序**：FROM → WHERE → GROUP BY → 聚合函数 → HAVING → SELECT（窗口函数在这一步计算）→ DISTINCT → ORDER BY → LIMIT。所以 WHERE 里不能用聚合函数（`WHERE COUNT(*) > 1` 直接报错），HAVING 里可以；WHERE 先执行，能被索引砍掉的行根本不进入分组。
- **HAVING 和 WHERE 的性能差别**：能放 WHERE 的条件放 WHERE——HAVING 过滤的是分组后的结果，而分组本身要读完参与的全部行；WHERE 提前砍行，聚合的输入集更小。
- **ONLY_FULL_GROUP_BY 的三个合法形态**：① 非聚合列出现在 GROUP BY 列表；② 非聚合列函数依赖于 GROUP BY 列（5.7 起引入函数依赖识别，GROUP BY 主键时可以 SELECT 同表其他列；8.0 延续该行为）；③ 被聚合函数包裹。
- **版本差异**：5.7 起默认开启 ONLY_FULL_GROUP_BY，老库升级时历史 SQL 集中报错是经典事故；另外 5.7 之前 GROUP BY 隐含按分组列排序，5.7 起取消隐式排序，要顺序必须显式 ORDER BY。8.0 在这两点上与 5.7 一致。
- **与去重的关系**：`SELECT DISTINCT a FROM t` 与 `SELECT a FROM t GROUP BY a` 结果等价，前者对所有 SELECT 列整体去重、后者按分组列去重且可同时带聚合；`COUNT(DISTINCT col)` 是分组内去重计数，且多个 DISTINCT 之间互相独立去重——这是「统计不同值组合数」类题目的坑。

**为什么这样设计**：GROUP BY 的语义是「每个分组产出一行」，如果不加约束，同组内非聚合列有多个候选值时「取哪个」没有定义——5.7 前的实现取扫描中先遇到的值，结果依赖执行计划，同样的 SQL 换台机器可能给出不同答案。默认开启 ONLY_FULL_GROUP_BY 是把未定义行为变成显式报错，逼写 SQL 的人决定取哪个值。

**面试官可能继续追问**：
- **`GROUP BY a ORDER BY b`（b 不在分组列里）会怎样？** —— 开启 ONLY_FULL_GROUP_BY 时报错，ORDER BY 里的非聚合列同样要满足函数依赖；要按非分组列排序必须显式聚合，如 `ORDER BY MAX(b)`。
- **HAVING 里能用 SELECT 的别名吗？** —— MySQL 扩展允许（HAVING 可引用 SELECT 别名），标准 SQL 不允许；依赖这个扩展的 SQL 换数据库会挂，跨库场景要避免。
- **`COUNT(*)`、`COUNT(1)`、`COUNT(列)` 有什么区别？** —— 前两者统计行数，语义相同、通常也几乎同速；`COUNT(列)` 只统计该列非 NULL 的行。三者在 InnoDB 上都是全量扫描计数，「COUNT(*) 更慢」是常见误解。

**实际工程场景**：WMS 的库存看板要按 SKU + 库区汇总可用数量、并过滤掉汇总后为 0 的分组：`WHERE ... GROUP BY sku_id, area_id HAVING SUM(available_qty) > 0`——单行为 0 的记录要先被 SUM 聚合成 0 才能判断，只能放 HAVING。MySQL 5.6 升 5.7 的场景里，运营后台十几个按 SKU 维度统计的 SQL 会因 ONLY_FULL_GROUP_BY 默认开启集体报错，稳妥的处理不是关掉 sql_mode（那是把风险留给下一次升级），而是逐条补分组列或给非分组列套聚合函数，顺便把「SELECT 里混非分组列」的习惯暴露出来。

**容易答错的地方**：
- 把「HAVING 一定比 WHERE 慢」当结论——HAVING 慢在「分组后过滤」这个位置，条件能提前到 WHERE 就应该提前，这是写法问题不是 HAVING 的问题。
- 说 5.7 之前的行为是「取每组第一个值」所以没关系——它取的是执行计划扫描到的第一个值，加 ORDER BY 或换索引都可能让它变，那套行为从来不该依赖。
- 升级 8.0 时以为 ONLY_FULL_GROUP_BY 行为有变化——默认开启这点 5.7 与 8.0 一致，8.0 增强的是函数依赖识别的覆盖面。
- 以为 GROUP BY 的结果天然有序——5.7 起没有隐式排序，不写 ORDER BY 就不保证顺序。

> 权威内容：GROUP BY 与 ONLY_FULL_GROUP_BY 当前尚未在仓库建设，可参考 MySQL 官方文档「GROUP BY Handling」与 sql_mode 章节。

---

### Q04：子查询有哪几种，相关子查询为什么慢？ `P1` `中频`

**30 秒回答**：按返回结果分四种：标量子查询返回单行单列，行子查询返回单行多列，列子查询返回单列多行（配 IN/ANY/ALL），表子查询返回多行多列（放 FROM 里当派生表，必须起别名）；按是否依赖外层分两种：相关子查询引用外层列、逐行执行，非相关子查询只算一次。相关子查询慢在「外层每行都要执行一次内层查询」，数据量大时优先改写成 JOIN。

**深入理解**：

- **标量子查询**：`SELECT name FROM emp WHERE salary = (SELECT MAX(salary) FROM emp)`，可用在任何期望单值的位置（包括 SELECT 列表）；返回多行直接报错「Subquery returns more than 1 row」。
- **行子查询**：`WHERE (dept, salary) = (SELECT dept, MAX(salary) FROM emp GROUP BY dept)` 这类多列等值比较，场景不多但要知道写法。
- **列子查询 + ANY/ALL**：`WHERE salary > ALL (SELECT salary FROM emp WHERE dept = '研发')` 表示大于研发全员（等价于大于 MAX）；`> ANY` 等价于大于 MIN；`= ANY` 等价于 IN。
- **派生表**：FROM 中的子查询，如 `FROM (SELECT dept, AVG(salary) avg_sal FROM emp GROUP BY dept) d`；MySQL 8.0 会把派生表合并（merge）或物化（materialize）——合并省掉一次物化代价，派生表被多次引用时物化反而划算。
- **相关子查询的执行模型**：外层每取一行，内层就带着外层的值执行一次，最坏 O(外层行数 × 内层单次代价)；内层关联列有索引时单次代价低、整体可接受，没索引就是外层行数次的全表扫。
- **优化方向**：能改 JOIN 的相关子查询优先改 JOIN（一次关联完成，优化器还能选连接顺序与算法）；IN/EXISTS 类子查询 5.6 起可被优化器改写为 semi-join（FirstMatch、Materialization、Duplicate Weedout、LooseScan 等策略），8.0 对部分相关子查询还能去关联化（decorrelation），但最终以 `EXPLAIN` 实测为准。

**为什么这样设计**：相关子查询提供的是「逐行判定」的表达能力，写起来直观、语义清晰，代价是把循环暴露给执行器；JOIN 提供的是「集合对集合的关联」，表达能力等价，但把循环怎么执行交给优化器去选算法。所以优化的本质不是「子查询不好」，而是「相关子查询把优化器能做的选择做没了」。

**面试官可能继续追问**：
- **派生表为什么必须起别名？** —— 派生表在语法上等价于一张临时表，后续引用它的列必须能限定来源，MySQL 规定派生表必须有别名，否则语法错误。
- **标量子查询查不到怎么办？** —— 结果是 NULL，而 `salary = NULL` 恒为 unknown、匹配不到任何行；要按「查不到就怎么算」补 `IS NULL` 判断或用 COALESCE 给默认值。
- **子查询里能查自己这张表吗？** —— 能，相关子查询本质上就是引用外层表的列；自连接与自相关子查询多数场景可互相改写，选哪种取决于哪种写法能让内层走索引。

**实际工程场景**：WMS 的「查出库单并带出该单最新物流状态」，早期写法是对每单跑一次相关子查询 `(SELECT status FROM logistics l WHERE l.order_id = o.id ORDER BY create_time DESC LIMIT 1)`，出库单量上来后整条 SQL 退化。改造分两步：先给 `logistics(order_id, create_time)` 建联合索引，再把相关子查询改写成 JOIN 派生表或用窗口函数取最新一条——逐单执行变成一次关联。这里有个容易引入的新 bug：物流表存在重复推送时，「最新一条」必须先按 create_time 去重，否则 JOIN 会把单行放大成多行，出库单在结果里重复出现。

**容易答错的地方**：
- 说「子查询一定要改成 JOIN」——非相关子查询只执行一次，多数场景没有改写必要；该改的是「外层行数多 × 内层无索引」的相关子查询。
- 把 `> ALL` 和 `> ANY` 的含义说反。
- 以为 MySQL 会把所有相关子查询自动去关联——8.0 只覆盖部分模式，`EXPLAIN` 里出现 `DEPENDENT SUBQUERY` 就是没有去关联成功。
- 改写 JOIN 后忘了去重，行放大导致统计数字虚高。

> 权威内容：MySQL 子查询优化与 semi-join 当前尚未在仓库建设，可参考 MySQL 官方文档「Subqueries」「Derived Tables」「Optimizing Subqueries」章节。

---

### Q05：EXISTS 和 IN 怎么选，NOT IN 有什么坑？ `P1` `中频`

**30 秒回答**：IN 先把子查询结果算出来再匹配外表，适合子查询结果集小、外表大的场景；EXISTS 对外表每行探测内表是否存在匹配、命中即停，适合外表小、内表大且关联列有索引的场景。最大的坑是 NOT IN 遇到子查询里的 NULL：`x NOT IN (1, NULL)` 永远不为真，整条查询返回空集，生产上一律用 NOT EXISTS 替代。

**深入理解**：

- **NOT IN 的 NULL 陷阱**：`x NOT IN (1, 2, NULL)` 展开是 `x <> 1 AND x <> 2 AND x <> NULL`，只要 x 不是 1 或 2，最后一项就是 unknown，整个 WHERE 为 unknown，行被过滤掉——表现为「SQL 不报错但查不出数据」。
- **替代写法**：`NOT EXISTS (SELECT 1 FROM b WHERE b.a_id = a.id)`，或确保子查询列非空 `NOT IN (SELECT col FROM b WHERE col IS NOT NULL)`；优先 NOT EXISTS，语义上还自带「命中即停」。
- **小表驱动大表**：EXISTS 的外表就是驱动表，外表每行带值去内表探测，内表关联列必须有索引，否则外表行数就是内表全表扫次数；IN 的子查询结果会被物化或转 semi-join，适合子查询本身结果集小的场景。
- **半连接语义**：IN 和 EXISTS 都是「外表行在内表至少一个匹配即保留」，内表有多个匹配也不会让外表行重复——这是它们相对 JOIN 的安全点，JOIN 一对多会把行放大。
- **优化器行为**：5.6 起 IN/EXISTS 可被改写为 semi-join，8.0 对相关子查询的去关联能力更强；写法和最终执行计划不一定一一对应，比较两者要看 `EXPLAIN` 而不是背结论。

**为什么这样设计**：IN 是「集合成员判断」，先把集合算出来再逐行查成员，集合小时这是最便宜的；EXISTS 是「存在性判断」，不需要完整集合、找到第一个匹配就停，内表有索引时单次探测接近 O(log n)。两个原语对应两种数据分布，没有普适最优，所以「怎么选」的答案一定是带前提的：看哪边小、内表有没有索引。

**面试官可能继续追问**：
- **`WHERE a.id IN (SELECT b.a_id FROM b)` 和 EXISTS 结果一定等价吗？** —— 子查询列无 NULL 时结果集等价，执行计划可能不同；还要注意 IN 子查询列含 NULL 时只是匹配不上、不影响正向查询，但 NOT IN 会被 NULL 拖垮，这种不对称性是两者的关键差异。
- **怎么确认优化器选了 semi-join？** —— `EXPLAIN` 里子查询不再以独立 `SUBQUERY` 块出现、而是并入连接行（Extra 可能出现 FirstMatch 之类信息），说明被改写了；看到 `DEPENDENT SUBQUERY` 则是相关子查询没有被去关联。
- **外表很大、内表也有索引，IN 和 EXISTS 谁快？** —— 仍是「外表小走 EXISTS、内表结果集小走 IN」的判断框架，但这种情况优化器很可能把两者优化成同一个 semi-join 计划，实测差距不大；纠结写法不如先看执行计划。

**实际工程场景**：WMS 的「查没有出库流水的入库单」，早期用 `WHERE id NOT IN (SELECT inbound_id FROM outbound_flow)`，某次上游同步把几行 inbound_id 写成了 NULL，整张报表空白——排查方向很容易被带偏到「数据是不是丢了」，根因是 NULL 让 NOT IN 恒不成立。改用 NOT EXISTS 后问题消失，团队随之把「SQL 规范禁用 NOT IN 子查询、统一 NOT EXISTS」写进 checklist。反向场景「查有库存变动的 SKU」：SKU 档案表小、库存流水表大且 `(sku_id, create_time)` 有索引，用 EXISTS 让外表 SKU 逐行探测流水索引，比先物化整个流水子查询再匹配便宜得多。

**容易答错的地方**：
- 脱离数据量和索引谈「EXISTS 快」或「IN 快」——正确答案必须带前提。
- 不知道 NOT IN 的 NULL 陷阱，或以为加了 `IS NOT NULL` 过滤是多余代码。
- 以为 IN 一定把子查询结果完整物化——5.6 起优化器可能改写为 semi-join 策略，不物化完整集合。
- 把 IN/EXISTS 说成「永远比 JOIN 快」——不放大行是优点，但代价与 JOIN 不可直接比较，要看执行计划。

> 权威内容：MySQL semi-join 与子查询执行当前尚未在仓库建设，可参考 MySQL 官方文档「Optimizing Subqueries」「Semi-join Strategies」章节。

---

### Q06：排序和分页怎么写，深分页怎么优化？ `P1` `中频`

**30 秒回答**：ORDER BY 多字段就是按逗号分隔的列依次排序；排序列没有索引时会多一次 filesort，MySQL 8.0 之前索引只按升序存储，升降序混合基本用不上索引。`LIMIT offset, n` 的 offset 越大越慢，因为数据库仍要扫过并丢弃前 offset 行；深分页的标准解法是延迟关联和游标分页（把 offset 换成上一页的位置条件），完整推演见 [数据库基础](数据库基础.md) 的分页专题。

**深入理解**：

- **filesort 的触发**：ORDER BY 的列和顺序与可用索引不一致时，MySQL 把结果集送排序缓冲区（放不下就落临时文件）排一遍，`EXPLAIN` 的 Extra 出现 `Using filesort` 就是这个信号。多字段排序要么完全走索引顺序、要么整体 filesort，不存在「前两列走索引、第三列排一下」的拼接。
- **混合方向排序**：`ORDER BY a ASC, b DESC` 在 8.0 之前无法用单个索引（索引按升序存储，反向读也只能整体反向），8.0 索引支持降序键后才可能走索引；混合方向加分页是最容易偷偷 filesort 的写法。
- **深分页的三个结论**（推演见 [数据库基础](数据库基础.md)）：
  1. **延迟关联**：`SELECT ... FROM t a JOIN (SELECT id FROM t ORDER BY id LIMIT 1000000, 20) b ON a.id = b.id`，把大 offset 的回表从 100 万次降到 20 次，前提是内层只读索引、走覆盖索引；
  2. **游标分页（keyset）**：`WHERE id > 上一页最后一个 id ORDER BY id LIMIT 20`，每页代价恒定，代价是只能顺序翻页、不能跳页；
  3. **业务侧限制**：限制最大可翻页深度，超阈值引导改选时间范围或走搜索引擎。
- **分页稳定性**：排序字段不唯一时翻页会重复或丢行（上一页最后一行与下一页第一行相同），分页排序必须带唯一键兜底；游标分页的位置条件也要求排序键稳定唯一。
- **COUNT 的代价**：分页组件通常还要总数，大表上 `COUNT(*)` 同样是全量扫描，可以用估算值、缓存精确 count，或限制最大页数后不再展示总数。

**为什么这样设计**：LIMIT 的语义是「跳过 offset 行再取 n 行」，实现上没有任何机制能凭空跳过未扫描的行——只能顺序扫（主键序或索引序），丢弃 offset 行的工作省不掉。延迟关联省的是「回表取全部列」的浪费，游标分页省的是「扫过前 offset 行」本身，两者优化的不是同一段代价，所以深分页场景经常两个一起用。

**面试官可能继续追问**：
- **延迟关联的第一步为什么强调覆盖索引？** —— 内层 `SELECT id ... ORDER BY id LIMIT offset, n` 只读主键索引、不回表，才能把大 offset 的代价压到「索引内顺序扫」；如果排序不是主键，就按排序列建覆盖索引。
- **游标分页怎么支持「上一页」？** —— 反向翻页用 `WHERE id < 第一行的 id ORDER BY id DESC LIMIT 20` 再把结果反序，本质是 keyset 的对称写法，仍然不能跳页。
- **`LIMIT 1000000, 20` 和 `LIMIT 20 OFFSET 1000000` 有区别吗？** —— 没有，同一种写法；MySQL 不支持 `OFFSET n ROWS FETCH NEXT m ROWS ONLY` 语法。

**实际工程场景**：WMS 的出库流水列表页按出库时间倒序分页，运营习惯翻到几千页后导出，`ORDER BY create_time DESC LIMIT 300000, 50` 会慢到十几秒。典型改造组合：`(create_time, id)` 联合索引让排序走索引；超过一定页数后必须选时间范围（时间范围查询天然是游标分页的变体）；「下一页」改传上一页最后一条的 `(create_time, id)` 做 keyset，翻页代价从「越翻越慢」变成恒定。同时把 `SELECT *` 收敛成列表真正需要的列——回表列少了，延迟关联内层的覆盖索引也更容易建。

**容易答错的地方**：
- 只说「加索引」——排序列有索引只能消掉 filesort，消不掉「扫过 offset 行」这个根本代价。
- 把游标分页说得一无是处——它代价恒定，限制只是不能跳页；选型看产品形态（feed 流/下一页 vs 页码跳转），不是技术优劣。
- 忽略翻页重复/丢行——排序键不唯一时这是必然出现的 bug，不是偶发。
- 说 MySQL 支持标准 `FETCH FIRST` 语法——那属于其他数据库，MySQL 只认 `LIMIT offset, n`。

> 权威内容：ORDER BY 与 LIMIT 优化的完整推演当前尚未在仓库建设，可参考 MySQL 官方文档「ORDER BY Optimization」「LIMIT Optimization」章节；面试口径的结论与延迟关联细节见 [数据库基础](数据库基础.md)。

---

### Q07：慢 SQL 怎么分析，完整的定位治理流程是什么？ `P0` `高频`

**30 秒回答**：五步闭环：① 采集——开慢查询日志（slow_query_log、long_query_time）或从监控拿样本；② 聚合——用 pt-query-digest 或 mysqldumpslow 按 SQL 指纹归一化，找总耗时最高的 Top SQL；③ 分析——EXPLAIN 看 type、key、rows、Extra；④ 治理——按「先 SQL 改写、再索引、后架构」的优先级处理；⑤ 验证——对比治理前后的执行计划和耗时。执行计划字段的详细含义见 [数据库基础](数据库基础.md)。

**深入理解**：

- **采集层**：`slow_query_log = ON`、`long_query_time`（生产常设 1～2 秒，默认 10 秒太宽，常态劣化的 1～3 秒高频 SQL 会全部漏掉）、`log_queries_not_using_indexes` 顺带抓没走索引的查询。8.0 可以把慢日志输出到表（`log_output = TABLE`，写 `mysql.slow_log`），performance_schema 的 statements_digest 与 sys 库的 statement_analysis 则适合常态化巡检。
- **聚合层**：同一条 SQL 不同参数会刷出几百条日志，必须先按指纹归一化再分析。pt-query-digest 输出按总耗时排序的报告，mysqldumpslow 是自带轻量替代。重点看三个指标：总耗时（影响面）、单次最大耗时（恶化程度）、扫描行数/返回行数比例（执行效率）。
- **分析层**：`EXPLAIN` 的四个关键信号——`type` 出现 `ALL`（全表扫）、`key` 为 NULL 或与 possible_keys 不一致（没用上预期的索引）、`rows` 远大于实际需要、`Extra` 有 `Using filesort` / `Using temporary`。8.0.18 起可以用 `EXPLAIN ANALYZE` 看实测行数和耗时，比纯预估可信，对「优化器统计信息过期导致选错索引」的场景几乎必需。
- **治理层优先级**：先 SQL 改写（不改索引、不动表，成本最低），再索引（有写入和空间代价），最后才是架构（缓存、读写分离、分库分表）。同一个 Top SQL 往往先改写法就能解决，不要一上来就加索引。
- **回归**：治理后重新 `EXPLAIN` 对比、灰度观察慢日志 Top 是否下降；索引不是免费的——每个索引都拖慢写入并占用 buffer pool，上线前要评估。

**为什么这样设计**：慢 SQL 治理的目标不是「把这一条改快」，而是「让数据库整体负载回到可控水位」。所以必须先聚合定位影响面最大的 SQL 再逐条治理——单看一条两秒的 SQL 没有意义，一秒钟被执行一万次的 SQL 才是优先项；这也是为什么慢日志一定要先归一化再分析，否则排查会被同一模式的海量日志带偏。

**面试官可能继续追问**：
- **EXPLAIN 的 rows 是精确值吗？** —— 是优化器基于索引统计信息的估算值，统计信息过期（大表长期没 ANALYZE、数据分布倾斜）时会错得离谱；可以 `ANALYZE TABLE` 重新统计，8.0 还支持直方图（histogram）纠正分布假设，极端场景用索引提示兜底。
- **EXPLAIN 和 EXPLAIN ANALYZE 有什么区别？** —— 前者只给优化器的预估执行计划，后者真正执行并给出每步实测行数和耗时；生产上用后者要注意它会真的执行，写操作用 EXPLAIN 而不是 EXPLAIN ANALYZE。
- **监控里 CPU 不高但接口超时，怎么查？** —— 先怀疑锁等待而不是慢 SQL：查 `information_schema.innodb_trx`、`performance_schema.data_lock_waits`（8.0；5.7 用 `INNODB_LOCKS`/`INNODB_LOCK_WAITS`）。慢 SQL 治理解决的是「执行慢」，锁等待要按死锁与锁竞争的思路查（见 [数据库基础](数据库基础.md)）。

**实际工程场景**：WMS 的出库流水导出功能上线后，周期性被反馈「导出转圈」。按流程走一遍的样子是：pt-query-digest 聚合慢日志，Top1 就是导出 SQL（单次十几秒、每天几百次），EXPLAIN 显示 `type=ALL` 且 `Extra=Using filesort`——出库时间列没走索引、查询还带着大 offset 分页。治理组合是补 `(warehouse_id, out_time)` 联合索引（查询必带仓库条件）、去掉深分页改为按出库时间范围分批拉取、导出改异步任务加结果文件下载。上线后的验证动作是重新 EXPLAIN 确认走索引、慢日志 Top10 按天汇总进值班看板——治理闭环的最后一步永远是验证和监控，避免同类问题再次靠投诉发现。

**容易答错的地方**：
- 只说「加索引」不给治理前后的执行计划/耗时对比——无法证明是索引起了作用还是数据量变化。
- 把 long_query_time 默认 10 秒当生产配置——默认值只能抓到极端 SQL，常态劣化的高频中速 SQL 全漏掉。
- 以为走了索引就没问题——走错索引（优化器估算错）和走对索引但 rows 巨大，都是「走了索引但慢」。
- 把锁等待、IO 打满、大事务回滚都归成「慢 SQL」——排查先分类，慢 SQL 治理只覆盖执行慢这一类。

> 权威内容：慢查询日志与 SQL 治理当前尚未在仓库建设，可参考 MySQL 官方文档「The Slow Query Log」「Query Optimization」章节与 pt-query-digest 文档；执行计划字段详解见 [数据库基础](数据库基础.md)。

---

### Q08：哪些写法会让索引失效，怎么改？ `P0` `高频`

**30 秒回答**：索引失效不是语法黑名单，而是优化器判断走索引的代价高于全表扫；最常见的触发写法有六类：索引列被函数或运算包裹、字符串列与数字比较的隐式转换、前导通配的 LIKE、or 混非索引列、联合索引跳过前导列、范围条件之后的列无法继续定位。标准答法是给「错误写法 vs 正确写法」对照表，并强调以 EXPLAIN 实测为准——8.0.13 起的 Skip Scan 等新行为会让静态规则过时。

**深入理解**：

| # | 错误写法 | 为什么失效 | 正确写法 |
| --- | --- | --- | --- |
| 1 | `WHERE DATE(create_time) = '2026-01-01'` | 索引存的是 create_time 原值，函数结果不再有序，只能逐行计算 | `WHERE create_time >= '2026-01-01' AND create_time < '2026-01-02'`（闭开区间） |
| 2 | `WHERE phone = 13800000001`（phone 是 varchar） | 字符串列与数字比较触发隐式转换，整列转数字，索引不可用 | `WHERE phone = '13800000001'` |
| 3 | `WHERE name LIKE '%abc'` | 前导通配无法定位扫描起点 | 改前缀匹配 `LIKE 'abc%'`，或用全文索引/搜索引擎 |
| 4 | `WHERE a = 1 OR b = 2`（b 无索引） | or 一侧无索引，整体退化为全表扫 | `UNION ALL` 两个索引查询，或给 b 补索引 |
| 5 | 联合索引 `(a, b, c)`，`WHERE b = 1` | 跳过前导列 a，不满足最左前缀 | 调整索引列顺序匹配查询条件，或新建以 b 前导的索引 |
| 6 | 联合索引 `(a, b, c)`，`WHERE a > 1 AND b = 2` | 范围条件后的 b 在引擎层无法继续定位，只能靠索引下推过滤 | 等值列前置、范围列后置；前导列基数低时可评估 Skip Scan（8.0.13+） |

- **不等于 / IS NULL / 大 IN 列表**：这些场景优化器可能走也可能不走索引（取决于选择性和行数估算），不要答成「一定不走」，以 `EXPLAIN` 为准；几千个值的大 IN 列表本身还有语句解析和网络开销，应拆批次。
- **隐式转换的方向性**：varchar 列与数字比较是「列转数字、索引失效」；整型列与字符串常量比较是「常量转整型、索引仍可用」。方向不要说反。
- **8.0 的新变量**：8.0.13 起 Skip Scan 会在联合索引前导列基数很低时跳过它直接用后续列，「跳过前导列必失效」在 8.0 上不再绝对；8.0 隐藏索引可以在不真正 DROP 的前提下让优化器忽略某个索引，是验证「索引是否被用上」的利器。
- **方法论**：任何「失效」结论都要在真实数据分布上用 `EXPLAIN` 验证——同样的写法在小表（优化器直接全表扫更快）和大表上结论可能相反。完整的机制分析（ICP、优化器代价模型）在 [数据库基础](数据库基础.md)。

**为什么这样设计**：B+ 树索引的有序性是「按索引列的原始值」组织的，函数、运算、隐式转换都让比较对象不再是原始值，有序性失效就只能全扫；范围条件截断则是因为存储引擎层范围扫描一旦开始就无法回头用后续列精确定位。所以这些规则的统一本质是「破坏了索引列值的有序性或可定位性」，不是 MySQL 的任意规定——答出这个统一解释，比背六条规则更能体现理解。

**面试官可能继续追问**：
- **怎么快速验证某个索引有没有被用上？** —— `EXPLAIN` 看 key 字段；8.0 可以先 `ALTER TABLE t ALTER INDEX idx INVISIBLE` 再对比执行计划和耗时，确认影响后再决定保留还是真正 DROP，避免删错。
- **函数包裹列一定要改写吗？** —— 如果查询频率极低、表极小，全表扫完全可接受，改写的意义就不大；索引治理要按查询频率和数据量分级，不是见到函数就改。
- **`LIKE '%abc%'` 有解吗？** —— B+ 树无解，双向通配本质是子串匹配，走全文索引（ngram 解析器支持中文）或搜索引擎；数据量小、并发低时全表扫也不是不能接受。

**实际工程场景**：WMS 的出库流水查询早期有个「按出库日期查」的写法 `WHERE DATE(out_time) = '2026-01-01'`，流水表千万行级，直接全表扫；改成闭开区间后走 `(warehouse_id, out_time)` 联合索引，耗时从秒级降到几十毫秒。另一个隐蔽坑是 SKU 编码：字段是 varchar，代码里用 Long 接收后直接拼进 SQL，触发隐式转换导致索引失效，压测时才暴露——修复除了改 SQL，还在 DAO 层加了类型断言，从入口堵住「用数字型 ID 查字符串列」这类写法。这类问题靠 code review 抓不完，接入 SQL 审核平台或上线前 EXPLAIN 卡点更稳。

**容易答错的地方**：
- 把索引失效答成「背黑名单」，讲不出「有序性/可定位性被破坏」这个统一原因。
- 说「不等于、IS NULL 一定不走索引」——优化器按代价选，可能走也可能不走。
- 只给错误写法不给改法——面试官问这题，重点在听「怎么改」和「你怎么验证改了有效」。
- 不提版本差异——Skip Scan（8.0.13+）让「跳过前导列」的绝对结论在 8.0 上不再成立。

> 权威内容：索引失效机制的完整分析（ICP、Skip Scan、优化器代价模型）当前尚未在仓库建设，可参考 MySQL 官方文档「Range Optimization」「Skip Scan」章节；面试口径的结论与索引结构见 [数据库基础](数据库基础.md)。

---

### Q09：大数据量表怎么查询和设计？ `P1` `中频`

**30 秒回答**：按代价从低到高分四层：单表层先靠「时间范围必带索引 + 覆盖索引 + 深分页改造」撑到千万级；再往上冷热分离归档，把历史数据挪到归档表或数仓，线上表只留热数据；读扩展用读写分离扛查询流量；最后才是分库分表，代价是分布式查询、跨分片 JOIN、全局 ID、扩容迁移一整套复杂度。分区表是单库内的折中：按时间 RANGE 分区让老分区可整片 DROP/归档，但单库容量和写入瓶颈没有解决。

**深入理解**：

- **分区表**：常用 RANGE 按时间分区（如按月），好处是历史分区可以 `DROP PARTITION` 秒级清理、比 DELETE 逐行删便宜得多，查询带分区键时还有分区裁剪；限制是仍是单库单表（只是物理分段）、分区键必须进查询条件才裁剪、分区表的所有索引都是局部索引（唯一约束必须包含分区键），8.0 之前分区表的在线 DDL 还有额外限制。
- **冷热分离**：最近 N 个月的热数据留在在线表，更早的走归档表或同步到数仓；业务查询默认只查热表，历史查询走归档通道。这是性价比最高的一步——大多数业务的查询天然集中在近期数据，归档后线上表行数直接降一个量级，索引和备份都受益。
- **读写分离**：主库写、从库读，报表和列表查询走从库；代价是主从延迟（秒级波动），「刚写完立刻读」和读己之写场景要有策略（写后强制读主、或加缓存）。它是读扩展，不解决写瓶颈和数据量问题。
- **分库分表**：垂直分库按业务域拆（订单库、库存库），水平分表按分片键打散（用户 ID 取模、时间范围）；代价立刻来一整套：跨分片分页与排序、不能跨片 JOIN、分布式 ID（自增不可用）、扩容时的数据迁移与双写、分布式事务。所以它是最后手段，前三层能解决就不要上。
- **查询侧的兜底**：无论哪一层，「按时间范围查询必须带索引」是底线——流水型表的时间列不进索引，任何架构层都救不了全表扫；先用 `EXPLAIN` 确认时间范围走了索引，比选哪种架构更优先。

**为什么这样设计**：数据量增长带来三种压力——单表扫描慢、单库容量/连接数上限、写入热点——分别对应不同的解，混淆它们就会过度设计：单表慢就上分库分表是拿核弹打蚊子，写瓶颈靠读写分离也治不好。正确的顺序是先让「查询少碰行」（索引 + 时间范围），再让「库少存行」（归档），最后才是「行分散放」（分片），每一层都用实际瓶颈验证后再升级。

**面试官可能继续追问**：
- **分区表和分表有什么区别？** —— 分区表是单库内单表的物理分段，对应用透明、SQL 基本不用改；分表是多张真实表，应用要感知分片键和路由。分区表解决「单表管理与清理」，分表解决「单库容量与写入分散」，两者不是一回事。
- **归档的数据还要能查怎么办？** —— 归档库保留查询能力（结构一致、只读），前端对超范围的时间查询引导走归档通道；或同步进数仓/ES 供分析，不在线提供 OLTP 查询。关键是查询入口要按时间范围分流，而不是让用户在一个入口里翻全量。
- **分片键怎么选？** —— 核心原则是「查询绝大多数带分片键」，否则退化成全分片扫描；按用户 ID 取模均匀但跨用户查询困难，按时间范围分片则热点集中在最新分片、要定期预分片；取模 + 时间的组合分片是常见折中。选之前先把高频查询列出来逐条核对。

**实际工程场景**：WMS 的出入库流水表（入库、出库、库存变动三类流水）涨到千万行级后，列表查询按业务时间倒序翻页越翻越慢。典型的治理顺序完全按四层走：第一层先补 `(warehouse_id, biz_time)` 联合索引、深分页改游标分页，接口耗时从秒级降到百毫秒级——这一步解决绝大部分查询投诉；第二层做冷热分离，一年前的流水同步到归档库，在线表行数降一个量级，备份和 DDL 都轻松；第三层把运营统计报表甩到只读从库，与交易流量隔离。分库分表通常评估但不上：按单号哈希可以散开流量，但「按 SKU 查流水」和跨仓统计会退化成广播查询，收益不抵复杂度，继续用归档 + 从库兜着。这个案例的结论是架构升级按实际瓶颈逐层验证，不要一步到位。

**容易答错的地方**：
- 上来就答分库分表——前两层（索引/归档）没做之前，分片解决不了「查询没走索引」这个高频根因。
- 说分区表能提高写入性能——分区主要是管理和清理（DROP PARTITION）的便利，写入瓶颈是单库的，分区表并不分散写入。
- 忽略读写分离的延迟代价——「刚下的单立刻查列表看不到」是主从延迟的经典客诉，要有读主策略或延迟监控的配套。
- 不追问就下结论——听到「数据量大」先问三个问题：慢在扫多少行、查得最多的是哪段时间、写入是否真的到上限。

> 权威内容：分区表、冷热分离与分库分表当前尚未在仓库建设，可参考 MySQL 官方文档「Partitioning」章节与《数据密集型应用系统》分区、复制章节。

---

### Q10：手写：怎么查连续登录 N 天的用户？ `P1` `中频`

**30 秒回答**：核心思路是把「连续日期」变成一个可分组的值：对去重后的登录记录按用户分区、按日期排序编号，用「日期减去序号」得到组内恒定的锚点，再按用户和锚点分组计数，组内天数大于等于 N 即命中。MySQL 8.0 用窗口函数 DENSE_RANK 一步完成，5.7 可以用用户变量或自连接模拟。

**深入理解**：

以下示例基于 MySQL 8.0。

① 建表与样例数据：

```sql
CREATE TABLE user_login (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  login_date DATE NOT NULL,
  KEY idx_user_date (user_id, login_date)
);

INSERT INTO user_login (user_id, login_date) VALUES
(1, '2026-01-01'), (1, '2026-01-02'), (1, '2026-01-03'),                          -- 连续 3 天，命中
(2, '2026-01-01'), (2, '2026-01-03'), (2, '2026-01-04'),                          -- 中间断 1 天，不命中
(3, '2026-01-01'), (3, '2026-01-02'), (3, '2026-01-03'), (3, '2026-01-05'),       -- 有一段连续 3 天，命中
(4, '2026-02-01'), (4, '2026-02-01'), (4, '2026-02-02'), (4, '2026-02-03');       -- 有重复记录，去重后连续 3 天，命中
```

② 窗口函数解法：

```sql
WITH dedup AS (
  SELECT DISTINCT user_id, login_date FROM user_login
),
ranked AS (
  SELECT user_id, login_date,
         DENSE_RANK() OVER (PARTITION BY user_id ORDER BY login_date) AS rn
  FROM dedup
),
tagged AS (
  SELECT user_id, login_date,
         DATE_SUB(login_date, INTERVAL rn DAY) AS grp
  FROM ranked
)
SELECT user_id,
       MIN(login_date) AS start_date,
       MAX(login_date) AS end_date,
       COUNT(*)        AS login_days
FROM tagged
GROUP BY user_id, grp
HAVING COUNT(*) >= 3
ORDER BY user_id;
```

③ 预期结果：

| user_id | start_date | end_date | login_days |
| --- | --- | --- | --- |
| 1 | 2026-01-01 | 2026-01-03 | 3 |
| 3 | 2026-01-01 | 2026-01-03 | 3 |
| 4 | 2026-02-01 | 2026-02-03 | 3 |

④ 原理：同一用户的连续日期段内，DENSE_RANK 每天 +1、日期也每天 +1，两者差值恒定，把这个差值当分组键，一个连续段就是一个组；断档处差值跳变，自然裂成新组。用 DENSE_RANK 而不是 ROW_NUMBER，是因为同一天可能有多条登录记录（见 user 4），ROW_NUMBER 会把重复日也计入序号、把连续段打散。

**为什么这样设计**：连续性的本质是「差值为固定步长的序列」，日期减序号把「判断连续」这个逐行比较的问题转化成「分组计数」这个集合问题，一次排序加一次分组就够；而不用去逐段自连接递推，也不会引入日期字符串拼接（跨月跨年必然算错）。

**面试官可能继续追问**：
- **换成 lag/lead 怎么写？** —— 用 LAG 取上一条登录日期，`DATEDIFF(login_date, LAG(login_date) OVER (PARTITION BY user_id ORDER BY login_date)) = 1` 标记「连续段起点」，再用 SUM 窗口对起点标记做累计得到组号，按用户和组号分组计数。两种写法等价；日期减序号更短，lag 法语义更直观，也好扩展成「间隔不超过 K 天算连续」。
- **MySQL 5.7 没有窗口函数怎么办？** —— 经典写法是用户变量：按用户和日期排序遍历，日期比上一条大 1 天则组号不变、否则组号 +1，再按组号聚合；或自连接 `a.login_date = DATE_ADD(b.login_date, INTERVAL 1 DAY)` 逐段递推。变量法依赖遍历顺序，可读性和稳定性都差，生产库建议推动升 8.0。
- **数据量很大（十亿行）怎么查？** —— 窗口函数要全量排序，代价高；按天预计算「每个用户当前的连续天数」增量表（今天连续 = 昨天连续 + 1，断档则重置为 1），查询直接 `WHERE streak >= 3`，把计算挪到写入侧，这是连续签到、留存类需求的常规做法。

**实际工程场景**：WMS 的仓内 PDA 作业有「连续 N 天无操作异常」的监控需求，本质是连续登录问题的反向版——找连续 N 天没有作业记录的作业员。实现在离线任务里按天物化每人每天的「无操作连续天数」，超阈值触发预警；在线侧查「本周连续登录 3 天以上的作业员」则走窗口函数。实践里的关键点是：登录/作业表必须按 (user_id, date) 去重（唯一约束或查询时 DISTINCT），否则窗口函数的序号会被重复记录打乱，统计口径忽大忽小——样例数据里 user 4 就是为暴露这个点设计的。

**容易答错的地方**：
- 直接用 ROW_NUMBER 而不处理重复日期——同一天多条记录会让「日期减序号」的差值漂移，连续段被错误打散。
- 手工拼日期字符串再减（如格式化成 YYYYMMDD 相减）——跨月跨年会算出 70、990 这种错误差值，必须用日期类型相减。
- 断档判断写错——「连续」要求差正好 1 天，不是「差 2 天以上」的反面表述。
- N 写死在逻辑里——N 应该是参数；另外别忘 `HAVING COUNT(*) >= N` 而不是 `=`。

> 权威内容：窗口函数与连续区间问题当前尚未在仓库建设，可参考 MySQL 官方文档「Window Functions」章节。

---

### Q11：手写：每个部门薪资前 3 名？ `P0` `高频`

**30 秒回答**：按部门分区、按薪资降序编号再过滤编号：要「前 3 个人」用 ROW_NUMBER，要「前 3 个薪资档、并列都保留」用 DENSE_RANK——并列时 DENSE_RANK 可能返回超过 3 行，ROW_NUMBER 永远只返回 3 行。窗口函数不能直接放 WHERE，必须套一层子查询或 CTE。MySQL 8.0 之前用相关子查询或用户变量模拟。

**深入理解**：

以下示例基于 MySQL 8.0。

① 建表与样例数据（研发部故意构造两轮并列，用于暴露 dense_rank 与 row_number 的差异）：

```sql
CREATE TABLE emp (
  id INT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  dept VARCHAR(50) NOT NULL,
  salary DECIMAL(10, 2) NOT NULL
);

INSERT INTO emp (id, name, dept, salary) VALUES
(1,  '张三', '研发', 30000),
(2,  '李四', '研发', 25000),
(3,  '王五', '研发', 25000),   -- 与李四并列
(4,  '赵六', '研发', 20000),
(5,  '孙七', '研发', 20000),   -- 与赵六并列
(6,  '周八', '研发', 15000),
(7,  '陈一', '产品', 28000),
(8,  '郑二', '产品', 22000),
(9,  '吴三', '产品', 22000),   -- 与郑二并列
(10, '钱四', '产品', 18000);
```

② DENSE_RANK 版（前 3 个薪资档，并列保留）：

```sql
SELECT id, name, dept, salary, rnk
FROM (
  SELECT id, name, dept, salary,
         DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rnk
  FROM emp
) t
WHERE rnk <= 3
ORDER BY dept, rnk, id;
```

③ 预期结果（研发 5 人、产品 4 人——并列档全部保留，所以可能超过 3 行）：

| id | name | dept | salary | rnk |
| --- | --- | --- | --- | --- |
| 1 | 张三 | 研发 | 30000.00 | 1 |
| 2 | 李四 | 研发 | 25000.00 | 2 |
| 3 | 王五 | 研发 | 25000.00 | 2 |
| 4 | 赵六 | 研发 | 20000.00 | 3 |
| 5 | 孙七 | 研发 | 20000.00 | 3 |
| 7 | 陈一 | 产品 | 28000.00 | 1 |
| 8 | 郑二 | 产品 | 22000.00 | 2 |
| 9 | 吴三 | 产品 | 22000.00 | 2 |
| 10 | 钱四 | 产品 | 18000.00 | 3 |

④ ROW_NUMBER 版（严格前 3 行，并列被强制拆开）：

```sql
SELECT id, name, dept, salary, rn
FROM (
  SELECT id, name, dept, salary,
         ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC, id) AS rn
  FROM emp
) t
WHERE rn <= 3
ORDER BY dept, rn;
```

预期结果：研发取张三（30000）、李四（25000）、王五（25000）；产品取陈一（28000）、郑二（22000）、吴三（22000）——每部门恰好 3 行，孙七和钱四被截掉。ROW_NUMBER 的 ORDER BY 必须带唯一键（这里是 id），否则并列行之间的行号不固定。

**为什么这样设计**：ROW_NUMBER 是「取前 N 行」的语义，适合限额发放、分页这类「数量说了算」的场景；DENSE_RANK 是「取前 N 档」的语义，适合排名次这类「名次说了算」的场景。同一个需求选错函数，表现就是「该并列的发不够、不该并列的超额」，所以这题的区分度不在会不会写窗口函数，而在能不能先问清楚「前 3 名是 3 个人还是 3 个薪资档」。

**面试官可能继续追问**：
- **窗口函数为什么不能直接写在 WHERE 里？** —— 窗口函数在逻辑执行顺序里位于 WHERE 之后（分组聚合之后、SELECT 阶段计算），WHERE 执行时窗口结果还不存在，所以 MySQL 直接报错，必须套子查询/CTE 把窗口结果物化成列再过滤。
- **5.7 没窗口函数怎么写？** —— 「前 3 个薪资档」用相关子查询：`WHERE (SELECT COUNT(DISTINCT e2.salary) FROM emp e2 WHERE e2.dept = e.dept AND e2.salary > e.salary) < 3`，即「薪资比你高的不同薪资值少于 3 个」；「前 3 行」用用户变量按部门分组计数。两种都啰嗦且易错，这也是推动升 8.0 的常见理由。
- **只按薪资排、不加 id 会怎样？** —— DENSE_RANK 不受影响（并列本来就要并列），但 ROW_NUMBER 在并列行之间的分配不确定——同样的 SQL 两次执行可能把 2、3 名换人，所以 ROW_NUMBER 的 ORDER BY 必须带稳定唯一键。

**实际工程场景**：WMS 的仓内绩效看板要按「出库单量」给作业员排名发日榜奖励，这里的取舍很典型：如果奖品是「前 3 名各一份」，用 ROW_NUMBER 严格取 3 人，并列时用业务 tiebreaker（如处理时效更短者胜）显式进 ORDER BY；如果只是「展示前 3 档作业量」，用 DENSE_RANK，并列的人都能展示。另有一个隐蔽点是分页 + 排名：榜单页按名次分页时，ROW_NUMBER 的名次跨页不稳定（翻页间隙数据变动会导致重复/跳号），对外展示名次要按排名函数算而不是按行号算。

**容易答错的地方**：
- 把 ROW_NUMBER、RANK、DENSE_RANK 三者的并列行为混着说（三者的完整对比见 Q14）。
- 窗口函数直接写进 WHERE，不知道要套一层。
- ROW_NUMBER 的 ORDER BY 不带唯一键，并列时结果不确定。
- 答不出「前 3 名」的业务歧义——不问清是 3 个人还是 3 个档，写对函数也可能答错需求。

> 权威内容：窗口函数当前尚未在仓库建设，可参考 MySQL 官方文档「Window Functions」章节。

---

### Q12：手写：行转列与列转行？ `P1` `中频`

**30 秒回答**：行转列就是 GROUP BY + CASE WHEN + 聚合（MAX/SUM）把多行值摊平成多个列；列转行 MySQL 没有 UNPIVOT 关键字，用 UNION ALL 从摊平结果拆回多行，或按需用 GROUP_CONCAT 把多行拼成一个列表。关键细节：行转列用 MAX 而不是 SUM（SUM 遇到同组多行会累加），聚合选 NULL 而非 0 时注意业务兜底。

**深入理解**：

以下示例基于 MySQL 8.0。

① 建表与样例数据：

```sql
CREATE TABLE score (
  student VARCHAR(50) NOT NULL,
  course  VARCHAR(50) NOT NULL,
  score   DECIMAL(5, 2) NOT NULL,
  PRIMARY KEY (student, course)
);

INSERT INTO score (student, course, score) VALUES
('小明', '语文', 85),
('小明', '数学', 92),
('小明', '英语', 78),
('小红', '语文', 90),
('小红', '数学', 88),
('小红', '英语', 95),
('小刚', '语文', 72),
('小刚', '数学', 60),
('小刚', '英语', 81);
```

② 行转列（每个学生一行、三科各一列）：

```sql
SELECT student,
       MAX(CASE WHEN course = '语文' THEN score END) AS chinese,
       MAX(CASE WHEN course = '数学' THEN score END) AS math,
       MAX(CASE WHEN course = '英语' THEN score END) AS english
FROM score
GROUP BY student
ORDER BY student;
```

③ 预期结果：

| student | chinese | math | english |
| --- | --- | --- | --- |
| 小明 | 85.00 | 92.00 | 78.00 |
| 小红 | 90.00 | 88.00 | 95.00 |
| 小刚 | 72.00 | 60.00 | 81.00 |

④ 列转行（GROUP_CONCAT 汇总每人的课程列表）：

```sql
SELECT student,
       GROUP_CONCAT(CONCAT(course, '=', score)
                    ORDER BY FIELD(course, '语文', '数学', '英语')) AS course_scores
FROM score
GROUP BY student
ORDER BY student;
```

⑤ 预期结果：

| student | course_scores |
| --- | --- |
| 小明 | 语文=85.00,数学=92.00,英语=78.00 |
| 小红 | 语文=90.00,数学=88.00,英语=95.00 |
| 小刚 | 语文=72.00,数学=60.00,英语=81.00 |

⑥ 列转行的另一种形态（UNION ALL 真拆行，从行转列结果拆回明细）：

```sql
WITH score_pivot AS (
  SELECT student,
         MAX(CASE WHEN course = '语文' THEN score END) AS chinese,
         MAX(CASE WHEN course = '数学' THEN score END) AS math,
         MAX(CASE WHEN course = '英语' THEN score END) AS english
  FROM score
  GROUP BY student
)
SELECT student, '语文' AS course, chinese AS score FROM score_pivot
UNION ALL
SELECT student, '数学', math FROM score_pivot
UNION ALL
SELECT student, '英语', english FROM score_pivot
ORDER BY student, FIELD(course, '语文', '数学', '英语');
```

预期结果即还原为 ① 的 9 行明细（student, course, score）。CASE WHEN 不带 ELSE 时未命中科目返回 NULL，拆行时该行仍会出现、score 为 NULL——需要「缺科不占位」就在 UNION ALL 外层加 `WHERE score IS NOT NULL`。

**为什么这样设计**：关系模型的「行」是同构的，把科目变成列本质是「为每个科目声明一列」，所以行转列的列数在写 SQL 时就是固定的，运行时才决定列序/列集合就得动态 SQL；反过来，列转行是把「列的信息」降级回「行的值」，UNION ALL 是对称的逆操作，GROUP_CONCAT 则是更进一步把多行压成一个字符串。两者没有优劣，取决于下游要「宽表一行」还是「明细多行」。

**面试官可能继续追问**：
- **为什么用 MAX 不用 SUM？** —— 每个 (student, course) 只有一行时两者结果相同；但一旦同组同科目出现多行（数据重复或补录），SUM 会累加成错误值，MAX 取其中之一。语义上「取某科分数」是取值不是求和，所以 MAX 更稳；要检测重复应该用 COUNT + HAVING，而不是靠聚合函数掩盖。
- **GROUP_CONCAT 有什么坑？** —— 默认 `group_concat_max_len` 只有 1024 字节，列表长会被静默截断，表现是「下拉选项少了几个」这种难查的 bug；需要 `SET SESSION group_concat_max_len` 调大，并显式 ORDER BY 保证顺序（不保证顺序时换台库结果都可能不同）。另外 GROUP_CONCAT 是字符串拼接，下游要再解析，性能和数据完整性都不如 UNION ALL。
- **科目不确定（动态列）怎么办？** —— 静态 SQL 做不到，要么在应用层按 DISTINCT course 动态拼 CASE WHEN 列（预编译 + 白名单校验科目），要么接受固定列集合。这是行转列类需求的固有边界，答不出这一点说明只写过固定报表。

**实际工程场景**：WMS 的库存报表要把「SKU × 库区」的长表摊平成一屏宽表：每行一个 SKU，列是各库区的可用数量，就是标准行转列（`MAX(CASE WHEN area = 'A' THEN qty END)`）；库区集合基本固定，写死列集合可接受。反向场景「汇总某个仓所有库位编码给下游系统」用 GROUP_CONCAT 拼串，这里踩过的典型坑是 `group_concat_max_len` 截断——库位多的仓拼出来的编码串被静默砍断，下游建账失败。另外中文列排序不稳定，生产上用 `ORDER BY FIELD(course, ...)` 固定业务列序，不依赖 collation 对中文的排序规则。

**容易答错的地方**：
- 行转列用 SUM——同组多行时累加成错误值，且掩盖了数据重复问题。
- 以为 MySQL 有 PIVOT/UNPIVOT 关键字（那是 Oracle/SQL Server 的语法）。
- 忽略缺科时 CASE WHEN 返回 NULL（不是 0），业务要 0 就得 COALESCE 兜底。
- GROUP_CONCAT 不写 ORDER BY、不关心 max_len 截断。
- 答不出动态列需要动态 SQL，把「科目固定」当成理所当然。

> 权威内容：行转列与 GROUP_CONCAT 当前尚未在仓库建设，可参考 MySQL 官方文档「GROUP_CONCAT」与聚合函数章节。

---

### Q13：手写：查找并删除重复数据保留一条？ `P1` `中频`

**30 秒回答**：先按重复键分组计数找出重复组，再删除每组里除了保留行之外的行。MySQL 8.0 用 ROW_NUMBER 按重复键分区编号、删掉编号大于 1 的；5.7 用「派生表包住 MIN(id) 分组」再 NOT IN 删除；多表删除语法自连接也行。保留规则（最小 id / 最新一条）是业务定的，删前必须备份或改软删，大批量还要分批。

**深入理解**：

以下示例基于 MySQL 8.0。

① 建表与样例数据（phone 列存在两组重复，其中一组重复三次）：

```sql
CREATE TABLE t (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL,
  name VARCHAR(50) NOT NULL,
  KEY idx_phone (phone)
);

INSERT INTO t (phone, name) VALUES
('13800000001', '张三'),
('13800000002', '李四'),
('13800000001', '张三丰'),   -- 与 id=1 手机号重复
('13800000003', '王五'),
('13800000002', '李四光'),   -- 与 id=2 手机号重复
('13800000001', '张小三');   -- 第三次重复
```

② 先查重复（确认范围和保留规则，删除前必做）：

```sql
SELECT phone, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
FROM t
GROUP BY phone
HAVING COUNT(*) > 1;
```

③ 预期结果：

| phone | cnt | ids |
| --- | --- | --- |
| 13800000001 | 3 | 1,3,6 |
| 13800000002 | 2 | 2,5 |

④ 删除（8.0，ROW_NUMBER 版，保留每组最小 id）：

```sql
DELETE FROM t
WHERE id IN (
  SELECT id FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY phone ORDER BY id) AS rn
    FROM t
  ) ranked
  WHERE rn > 1
);
```

⑤ 删除（5.7 兼容版，派生表 + NOT IN）：

```sql
DELETE FROM t
WHERE id NOT IN (
  SELECT id FROM (
    SELECT MIN(id) AS id FROM t GROUP BY phone
  ) keep
);
```

⑥ 删除（多表删除自连接版）：

```sql
DELETE t1 FROM t t1
JOIN t t2 ON t1.phone = t2.phone AND t1.id > t2.id;
```

⑦ 预期结果（三种写法结果一致，保留 id 最小的 1、2、4）：

| id | phone | name |
| --- | --- | --- |
| 1 | 13800000001 | 张三 |
| 2 | 13800000002 | 李四 |
| 4 | 13800000003 | 王五 |

**为什么这样设计**：删除「重复数据保留一条」的难点不在算法在工程：第一，MySQL 不允许在 DELETE 的子查询里直接引用被删的同一张表（报错 1093：You can't specify target table 't' for update in FROM clause），必须用派生表把子查询物化一次绕开——这是本题最常考的细节；第二，自连接版 `t1.id > t2.id` 的语义是「存在比我小的同键行」，天然保留最小值，不需要 MIN；第三，真正的风险是误删，所以保留规则必须先和业务确认，且大批量表必须分批删（单表 DELETE 支持 `LIMIT`，循环小批次），避免大事务和长锁。

**面试官可能继续追问**：
- **为什么子查询外面要再套一层 `SELECT id FROM (...)`？** —— MySQL 禁止 DELETE/UPDATE 的子查询里出现目标表本身，套一层派生表会让优化器先把子查询物化成临时结果，从而绕开 1093 限制；UPDATE 场景同理。这是写「同表子查询删除」时必须记住的固定套路。
- **想保留最新一条而不是最旧一条？** —— 把保留条件换成按更新时间取最新：8.0 版 `ROW_NUMBER() OVER (PARTITION BY phone ORDER BY update_time DESC, id DESC)`；5.7 版把 `MIN(id)` 换成先子查询查出每组的最大 update_time 对应的 id。规则本身是业务定的，SQL 只是实现。
- **数据量很大怎么安全删？** —— 先按保留规则选出待删 id 落临时表或加「待删」标记，再每批 `DELETE FROM t WHERE id IN (SELECT id FROM tmp LIMIT 5000)` 循环执行，避开大事务、长锁和大 undo；删完补唯一索引（`phone` 上加唯一键）从根上防止再重复，注意加唯一索引前要确认没有新重复写进来。

**实际工程场景**：WMS 的 SKU 档案同步场景里，上游重推或接口重试会让 (tenant_id, sku_code) 出现重复行，表现为「同一 SKU 在列表出现两遍、库存汇总翻倍」。清理动作的标准流程是：先用分组计数 SQL 圈定重复范围给业务确认（哪些行该留、按创建时间还是按数据完整度留），再选自连接或 ROW_NUMBER 版删除，删完在 (tenant_id, sku_code) 上补唯一索引，并在同步链路加幂等（存在则更新、不存在则插入）。只删不加唯一索引，下次重推还会重复——删除是止血，唯一约束和幂等才是根治。

**容易答错的地方**：
- 直接写 `DELETE FROM t WHERE id NOT IN (SELECT MIN(id) FROM t GROUP BY phone)`——撞 1093 报错，不知道要套派生表。
- 保留规则不问业务就拍脑袋「保留最小 id」——也可能是保留最新、保留信息最全的一条，规则错了删了也白删。
- 删完不补唯一索引/幂等，重复问题复发。
- 大表一把 DELETE 全删——大事务、长锁、undo 膨胀，直接把库拖住。

> 权威内容：同表删除与 1093 限制当前尚未在仓库建设，可参考 MySQL 官方文档「DELETE」语句与多表 DELETE 语法章节。

---

### Q14：手写：积分并列名次？ `P2` `低频`

**30 秒回答**：三个窗口函数对应三种名次语义：ROW_NUMBER 强制连续行号不管并列，RANK 并列后跳号（1、2、2、4），DENSE_RANK 并列不跳号（1、2、2、3）。积分并列场景展示名次用 RANK/DENSE_RANK，按名次发限量奖励用 ROW_NUMBER；注意窗口函数的「并列」是按整个 ORDER BY 列表判断的，所以 RANK 的 ORDER BY 不能加唯一 tiebreaker，ROW_NUMBER 反而必须加。

**深入理解**：

以下示例基于 MySQL 8.0。

① 建表与样例数据（三人积分并列 90，用于暴露三种函数的差异）：

```sql
CREATE TABLE points (
  player VARCHAR(50) PRIMARY KEY,
  score INT NOT NULL
);

INSERT INTO points (player, score) VALUES
('A', 100),
('B', 90),
('C', 90),
('D', 90),
('E', 80);
```

② 一条 SQL 同时算出三种名次：

```sql
SELECT player, score,
       ROW_NUMBER() OVER (ORDER BY score DESC, player) AS row_no,
       RANK()       OVER (ORDER BY score DESC)          AS rank_no,
       DENSE_RANK() OVER (ORDER BY score DESC)          AS dense_rank_no
FROM points
ORDER BY rank_no, player;
```

③ 预期结果：

| player | score | row_no | rank_no | dense_rank_no |
| --- | --- | --- | --- | --- |
| A | 100 | 1 | 1 | 1 |
| B | 90 | 2 | 2 | 2 |
| C | 90 | 3 | 2 | 2 |
| D | 90 | 4 | 2 | 2 |
| E | 80 | 5 | 5 | 3 |

④ 语义对比：

| 函数 | 并列行为 | 并列后下一名 | 典型用途 |
| --- | --- | --- | --- |
| ROW_NUMBER() | 不承认并列，强制 1、2、3 | 连续 | 分页、限量取 Top N（发 N 份奖品） |
| RANK() | 并列同名次 | 跳号（2、2 之后是 4） | 体育排名，「第 2 名有三人，下面没有第 3 名」 |
| DENSE_RANK() | 并列同名次 | 不跳号（2、2 之后是 3） | 档位排名，「前 3 档」类需求 |

**为什么这样设计**：三个函数分别抽象了「行序」「名次跳号」「名次连续」三种业务直觉，差异全部集中在「并列行算几个名次」这一个点上：ROW_NUMBER 认为并列也要分先后（适合限额发放），RANK 认为并列占掉后续名次（适合竞技排名），DENSE_RANK 认为并列不占名次（适合档位）。没有哪个更「正确」，选错函数的典型后果是该并列的发不够、不该并列的超额。

**面试官可能继续追问**：
- **RANK 的 ORDER BY 加上 player 会怎样？** —— 并列就消失了：窗口函数的「并列」按整个 ORDER BY 列表判断，加上唯一的 player 后每行都不是并列行，RANK 退化成 1、2、3、4、5。所以 RANK/DENSE_RANK 的 ORDER BY 只放排名字段，ROW_NUMBER 才需要追加唯一键保证输出稳定——这个「该加在哪」的区分是本题的深水区。
- **并列时怎么打破平局？** —— 业务 tiebreaker（如用时更短者胜、注册更早者胜）要显式进 ORDER BY，否则「谁是第二」不确定；但如果 tiebreaker 影响了对外名次，记得只放进 ROW_NUMBER，别污染 RANK 的并列语义。
- **MySQL 5.7 怎么写？** —— 用用户变量模拟：按分数降序遍历，分数与上一行相同则名次变量不变、否则名次变量更新为当前行号（RANK）或自增计数器（DENSE_RANK）。变量法依赖遍历顺序且可读性差，是「推动升 8.0」的常用论据之一。

**实际工程场景**：WMS 的仓内作业排行榜（按出库单量）对外展示名次时用 DENSE_RANK——并列的人共享名次，运营解释成本最低；而「日榜前 3 名发奖励」的筛选用 ROW_NUMBER，且 ORDER BY 显式带 tiebreaker（单量相同比处理时效），保证每次跑出来的获奖人稳定。另有一个常见坑是把「名次」和「行号」混用：榜单分页按 ROW_NUMBER 的行号对外展示，翻页间隙数据变动会让同一人名次跳号，对外名次要按排名函数的值算，不是按页内行号。

**容易答错的地方**：
- 把三者的并列后编号说混（RANK 跳号、DENSE_RANK 不跳号是核心差异）。
- 给 RANK/DENSE_RANK 的 ORDER BY 加唯一键，把并列语义悄悄抹掉。
- ROW_NUMBER 的 ORDER BY 不带唯一键，并列行之间行号不确定，限量发放结果不可复现。
- 只背定义、说不出「什么业务该用哪个」——函数选择本质是业务语义选择。

> 权威内容：窗口函数当前尚未在仓库建设，可参考 MySQL 官方文档「Window Functions」章节。

---

## 本专题常见追问链

```text
SQL 分类（Q01）
└─ TRUNCATE vs DELETE？→ DDL 为什么隐式提交 → SAVEPOINT 的用途
JOIN（Q02）
└─ ON vs WHERE？→ LEFT JOIN 右表条件下放为什么退化 → 驱动表怎么选 → 被驱动表没索引会怎样
GROUP BY / HAVING（Q03）
└─ 执行顺序？→ ONLY_FULL_GROUP_BY 是什么 → 5.7 升级为什么集中报错 → 和去重的关系
子查询（Q04）
└─ 相关子查询为什么慢 → 一定改 JOIN 吗 → semi-join 策略 → 派生表为什么要别名
EXISTS vs IN（Q05）
└─ 怎么选 → NOT IN 遇 NULL → 小表驱动大表 → 怎么确认走了 semi-join
排序分页（Q06）
└─ filesort 什么时候出现 → 深分页怎么优化 → 游标分页的代价（→ 数据库基础.md）
慢 SQL（Q07）
└─ 日志怎么采 → 怎么聚合 → EXPLAIN 看什么 → 治理优先级 → 怎么验证（→ 数据库基础.md）
索引失效（Q08）
└─ 六类写法 → 隐式转换的方向 → Skip Scan(8.0.13) → 隐藏索引验证（→ 数据库基础.md）
大数据量（Q09）
└─ 分区表 vs 分表 → 冷热分离 → 读写分离的延迟 → 分片键怎么选
手写 SQL（Q10-Q14）
├─ 连续登录：先去重 → 日期减序号分组 → lag 变体 → 5.7 兼容 → 增量预计算
├─ 部门前 3：dense_rank vs row_number → 并列怎么办 → 窗口函数不能放 WHERE
├─ 行转列：CASE WHEN + MAX → SUM 行不行 → 动态列 → GROUP_CONCAT 截断
├─ 删重复：错误 1093 → 三种写法 → 分批删除 → 补唯一索引
└─ 并列名次：rank vs dense_rank vs row_number → tiebreaker 该加在哪
```

## 面试前速记

- LEFT JOIN 的右表条件放 WHERE 会退化成内连接——右表过滤条件放 ON；ON 决定「补不补 NULL」，WHERE 决定「留不留行」。
- INNER JOIN 驱动表由优化器按代价选；LEFT/RIGHT JOIN 的驱动表被语法固定；小表驱动大表的准确说法是「过滤后行数少的驱动行数多的」。
- HAVING 在分组后过滤，能用 WHERE 的条件不要拖到 HAVING；ONLY_FULL_GROUP_BY 从 5.7 起默认开启，8.0 一致。
- NOT IN 遇 NULL 恒返回空集，一律用 NOT EXISTS；IN/EXISTS 的选择看哪边小、内表关联列有没有索引。
- 深分页 = 延迟关联 + 游标分页 + 业务限制翻页深度，机制与代价详见 [数据库基础](数据库基础.md)。
- 索引失效六类写法：函数包裹、隐式转换、前导通配、or 混非索引列、跳过前导列、范围后列截断——最终以 EXPLAIN 实测为准，8.0 还有 Skip Scan 变量。
- 慢 SQL 五步闭环：采集（慢日志）→ 聚合（pt-query-digest 按指纹）→ EXPLAIN/ANALYZE → 治理（先改写后索引）→ 回归验证。
- 大数据量四层顺序：索引 + 时间范围 → 冷热分离 → 读写分离 → 分库分表（最后手段，别跳级）。
- 连续登录 = 先 (user_id, date) 去重，再按「日期减 DENSE_RANK」差值分组计数；ROW_NUMBER 会被重复日期打散。
- Top N 行用 ROW_NUMBER（必须带唯一排序键），并列名次用 RANK/DENSE_RANK（不能加唯一键，否则并列消失）。
- 行转列 = GROUP BY + CASE WHEN + MAX；列转行 = UNION ALL 或 GROUP_CONCAT（注意 group_concat_max_len 截断和 ORDER BY）。
- 删重复保留一条：派生表包 MIN(id) 绕开 1093、或 ROW_NUMBER、或自连接 `t1.id > t2.id`；删前圈定范围、删后补唯一索引、大批量分批删。
