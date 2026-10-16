# MyBatis

> 一句话定位：这一专题考「SQL 怎么从接口方法变成 JDBC 调用」——执行流程、预编译、缓存、插件四大件；与 [Spring](Spring.md) 的事务整合是压轴题。

返回 [专题题库首页](README.md) · [面试首页](../README.md)。

## 专题定位

- **为什么高频**：MyBatis 是国内 Java 后端的默认持久层框架，面试默认你在用；考点集中在「细节决定安全与性能」——`#{}` 还是 `${}`、一级缓存踩坑、分页插件原理，每问都能追到 JDBC 层。
- **面试官主要验证什么**：能否讲清「接口方法 → SQL → 结果映射」的完整链路，能否说清缓存的生命周期与关闭理由；分页与批量两题看的是你有没有被线上毒打过。
- **5 年以上工程师的深度**：能定位一级缓存导致的脏读、解释 PageHelper 的改写时机、按业务特征选 MyBatis / MyBatis-Plus / JPA，并讲清批量操作的优化路径。
- **本专题与 [Spring](Spring.md) 的分工**：这里讲持久层自身机制与 Spring 整合；事务失效等框架层问题在 Spring 篇，此处只链接不复述。

## 高频题目导航

| 编号 | 面试问题 | 重要程度 | 追问深度 |
| --- | --- | --- | --- |
| Q01 | MyBatis 的整体执行流程？ | P0 | 源码 |
| Q02 | #{} 和 ${} 的区别？ | P0 | 原理 |
| Q03 | MyBatis 一级缓存？ | P1 | 源码 |
| Q04 | MyBatis 二级缓存？ | P1 | 源码 |
| Q05 | MyBatis 的动态 SQL？ | P0 | 工程实践 |
| Q06 | Mapper 接口的代理机制？ | P0 | 源码 |
| Q07 | Executor 和 SqlSession？ | P1 | 源码 |
| Q08 | MyBatis 插件机制？ | P1 | 源码 |
| Q09 | MyBatis 的分页怎么做？ | P0 | 工程实践 |
| Q10 | MyBatis 批量操作怎么优化？ | P1 | 工程实践 |
| Q11 | MyBatis 和 Spring 事务怎么整合？ | P0 | 源码 |
| Q12 | resultType 和 resultMap？ | P1 | 工程实践 |
| Q13 | MyBatis、MyBatis-Plus、JPA 怎么选？ | P1 | 设计 |

## 题目正文

### Q01：MyBatis 的整体执行流程？ `P0` `高频`

**30 秒回答**：从 SqlSessionFactory 拿到 SqlSession，通过 Mapper 代理执行方法，依次经过 Executor、StatementHandler、ParameterHandler、ResultSetHandler 四层，最终把 SQL 与参数交给 JDBC 执行并把结果集映射成对象返回。

**深入理解**：启动期：SqlSessionFactoryBuilder 读配置文件生成 Configuration，build 出 SqlSessionFactory（重量级、全局一个）。运行期链路：`SqlSession.getMapper()` 返回 Mapper 接口的动态代理 → 调用方法时 MapperMethod 找到对应的 MappedStatement（namespace + 方法名定位）→ Executor 先查一级缓存，未命中则创建 StatementHandler → ParameterHandler 为 `#{}` 占位符绑定参数 → JDBC 执行 → ResultSetHandler 按 resultType/resultMap 把 ResultSet 映射成对象。这里最该讲清的一点：`#{}` 在流程中被解析成 `?` 占位符，参数由 PreparedStatement 的 setXxx 绑定——这是它与 `${}` 一切差异的根源。

**面试官可能继续追问**：
- **SqlSessionFactory 为什么是重量级对象？** —— 它持有整个 Configuration（所有映射语句、缓存配置、插件链），构建成本高且可被整个应用共享；每个线程/每次请求该建的是轻量的 SqlSession。
- **MyBatis 默认用的什么 Executor？** —— SimpleExecutor，每次执行都创建 Statement；开了二级缓存才换成 CachingExecutor 装饰；Batch 需要显式指定（`ExecutorType.BATCH`）。

**实际工程场景**：WMS 里一次出库要「查单据头 + 查单据行 + 扣库存」三次查询，全部在同一个 SqlSession/事务内完成；理解了这条链路才能解释为什么同一个事务里的查询能共享连接（见 Q11）以及为什么跨会话的缓存会失效（见 Q03）。

**容易答错的地方**：把「Mapper 接口直接执行 SQL」当成答案——接口没有实现类，背后是 JDK 动态代理（见 Q06）；另外默认 Executor 是 Simple 不是 Caching，只有开了二级缓存才会被装饰。

> 权威内容：MyBatis 执行流程当前尚未在仓库建设，可参考 MyBatis 官方文档的 Getting Started 与 Configuration 章节。

---

### Q02：#{} 和 ${} 的区别？ `P0` `高频`

**30 秒回答**：`#{}` 是占位符，编译期变成 `?`、参数走 PreparedStatement 预编译绑定，能防 SQL 注入；`${}` 是字符串原样拼接，拼什么执行什么，注入风险全开——只能用于表名、列名、排序方向这类无法参数化的位置，且必须配白名单。

**深入理解**：预编译的价值有两层：安全（参数永远被当作值，不可能改变 SQL 结构）与性能（同构 SQL 可以复用 PreparedStatement，数据库侧有执行计划缓存）。`${}` 的典型合法用途：动态表名（分表 `select * from t_${month}`）、动态列名（`order by ${column} ${direction}`）、IN 列表的某些数据库方言。这些场景共同特征是「数据库对象名不能当参数」——所以防御只能靠白名单：列名从固定枚举里校验、排序方向只允许 ASC/DESC，绝不能直接透传前端参数。反例代码一眼识破：`order by ${sortColumn}` 后面直接跟用户输入。

**面试官可能继续追问**：
- **`${}` 一定不安全吗？** —— 不一定，取决于拼接的内容是否完全由服务端控制。比如拼接的是代码里写死的枚举名，风险为零；但只要有一丝可能来自请求参数，就必须白名单。
- **为什么不用字符串过滤代替预编译？** —— 黑名单过滤永远追不完变形手法（宽字节、注释截断、编码绕过）；预编译是结构性地让「值不可能变成代码」，过滤只是概率性防御。

**实际工程场景**：WMS 报表模块支持「按任意列排序」，实现是后端维护「可排序列白名单」，命中后用 `${}` 拼列名与 ASC/DESC——既保留灵活性又不给注入面。面试讲这个案例时主动说白名单，比只说「${} 危险」完整得多。

**容易答错的地方**：说「`${}` 就是用来拼接的，用的时候小心点」——没有白名单意识的「小心」等于没有防御；另外别把两者和「动态 SQL 标签」混为一谈，`<if>` 是 XML 层的条件拼装，`#{}`/`${}`` 是参数化层的选择。

> 权威内容：MyBatis 参数绑定当前尚未在仓库建设，可参考 MyBatis 官方文档的 Mapper XML 文件章节。

---

### Q03：MyBatis 一级缓存？ `P1` `重点`

**30 秒回答**：一级缓存是 SqlSession 级别的本地缓存（默认开启、不可关闭），同一个会话里参数相同的相同查询会直接命中缓存；但任何增删改都会清空它，而且 Spring 集成下每次 DAO 调用都是新 SqlSession，一级缓存基本处于失效状态。

**深入理解**：缓存在 Executor 层（`PerpetualCache`），key 是「statement id + 参数 + 边界环境」。清空时机是 `update` 类操作执行时——全量清空而非按表精准失效，这是它有脏读风险的原因：同一会话里先 select 再 update 再 select，第二次 select 因为缓存被清了会重新查，没问题；真正危险的是「update 走的是另一个会话/直连数据库」时，本会话的缓存不知道数据已变。Spring + MyBatis 的标准集成（mybatis-spring）里，每次 Mapper 方法调用都会从 SqlSessionTemplate 取一个「与事务绑定」的 SqlSession，非事务方法每次都是新会话——所以一级缓存在多数 Spring 项目里形同虚设；只有在 `@Transactional` 方法内多次调用同一查询，才会命中同一会话的一级缓存，这时才要警惕读到本事务内的旧数据。

**面试官可能继续追问**：
- **一级缓存能关吗？** —— 配置 `localCacheScope=STATEMENT` 可以让缓存只活一条语句（等效关闭），但没有开关直接禁用；二级缓存才有 `cacheEnabled=false` 的总开关。
- **一级缓存的脏读怎么避免？** —— 要么确保所有写都经过同一个 SqlSession（同一事务内），要么 `localCacheScope=STATEMENT`；跨服务、跨缓存的场景本来也不该靠一级缓存。

**实际工程场景**：WMS 出库事务里「先查库存再扣减再查库存确认」，三步在同一会话，最后一步命中一级缓存拿到事务内的旧值——如果业务把最后一步当「数据库真值」做判断就会错。治理方式是把这类确认查询改为走新会话或直接信任影响行数。

**容易答错的地方**：把一级缓存描述成「应用级缓存，能显著提升性能」。它的作用域只有一个 SqlSession、一次会话，Spring 项目里大部分时间根本命中不到，把性能优化押在它上面是找错了对象。

> 权威内容：MyBatis 缓存机制当前尚未在仓库建设，可参考 MyBatis 官方文档的 Caches 章节。

---

### Q04：MyBatis 二级缓存？ `P1` `重点`

**30 秒回答**：二级缓存是 Mapper namespace 级别的缓存，跨 SqlSession 共享，默认关闭；开启后需要实体可序列化，且因为无法感知跨表变更，生产环境通常建议保持关闭。

**深入理解**：开启方式：`<cache/>` 标签或 `@CacheNamespace` 注解 + 全局 `cacheEnabled=true`（默认 true，但 mapper 上不声明就不启用）。结构上它是 CachingExecutor 对普通 Executor 的装饰：查询先查二级缓存，未命中查库并回填；任何 `update` 清空整个 namespace 的缓存。脏读的根源是「按 namespace 清空」太粗：A 表被 B 表 join 查询缓存住，另一个 namespace 改了 B 表，A 的缓存不会因此失效——分布式部署下更严重，每个实例各有一份缓存，改数据的实例清了、其他实例没清。实体必须实现 Serializable，因为缓存可能被写盘（`BlockingCache` + 自定义存储）。MyBatis 自带缓存接口可接 Redis，但那通常意味着直接用 Redis 更可控。

**面试官可能继续追问**：
- **什么场景下二级缓存利大于弊？** —— 读极多、写极少、数据全局一致的字典/配置表（如商品类目、行政区划），且部署单实例或接受短暂不一致；即便如此，多数团队仍选择交给 Redis 统一管。
- **二级缓存和一级缓存的执行顺序？** —— 先二级后一级：CachingExecutor 查二级缓存，未命中委托给简单 Executor，后者先查一级缓存再查库。

**实际工程场景**：WMS 的字典数据（仓库信息、货主信息）变更是运营操作、读是每次出库都要用，早期开过二级缓存，结果运营改了仓库名，三个应用节点有两个还返回旧值——原因就是跨实例失效不了。最后改成 Redis 缓存 + 主动失效，MyBatis 二级缓存保持关闭。这个案例几乎可以原样搬进面试。

**容易答错的地方**：只答「默认关闭，性能不好」。面试官要的是「为什么不好」——跨 namespace 失效粗、跨实例不一致、实体要序列化，三条理由缺一不可。

> 权威内容：MyBatis 二级缓存当前尚未在仓库建设，可参考 MyBatis 官方文档的 Caches 章节。

---

### Q05：MyBatis 的动态 SQL？ `P0` `高频`

**30 秒回答**：动态 SQL 是用 `<if>`、`<where>`、`<choose>`、`<foreach>`、`<set>` 等标签在 XML 层按条件拼装 SQL，配合 OGNL 表达式读参数；核心价值是「一份映射文件适配多种查询组合」，但它本质仍是字符串拼装，安全边界仍取决于参数用 `#{}` 还是 `${}`。

**深入理解**：常用标签逐个说：`<if>` 条件片段；`<where>` 自动去掉多余的 AND/OR 并补 WHERE（解决第一个 if 就成立时多出的连接词）；`<trim>` 是 `<where>`/`<set>` 的通用版（自定义前后缀与覆盖规则）；`<choose>/<when>/<otherwise>` 多选一，适合「按状态走不同条件」；`<foreach>` 遍历集合拼 IN 或批量插入（`collection`、`item`、`index`、`open`、`close`、`separator` 六个属性）；`<set>` 去掉 UPDATE 后多余的逗号；`<sql>/<include>` 复用片段；`<bind>` 做字符串拼接（如 LIKE 的通配符拼接）。两个高频坑：IN 列表用 `${}` 传集合（直接注入面）——用 `<foreach>` 生成多个 `#{}` 才是正道；批量插入的 SQL 过长触发 `max_allowed_packet`——要分批。动态 SQL 写完后建议在测试环境打开日志核对最终 SQL，`<if>` 嵌套三层以上基本就是维护性灾难，该考虑 Specification/Query Object 模式或干脆拆接口。

**面试官可能继续追问**：
- **`<where>` 怎么知道该去掉哪个 AND？** —— 它解析内容开头的 AND/OR，若以连接词开头就剥掉再拼 WHERE；底层是 XML 解析期的文本节点处理，可以理解为「有条件的字符串修剪」。
- **动态 SQL 有性能问题吗？** —— 预编译层面没有问题（`#{}` 仍是 `?`）；但「同一条映射语句生成过多种 SQL 形态」时，数据库执行计划缓存的命中率会下降，极端场景能感觉到——这属于二级效应，别夸大成主要矛盾。

**实际工程场景**：WMS 单据中心一个「综合查询」接口，前端十来个可选条件，全用 `<if>` 拼；同时约定「列表查询必须带时间范围」（否则全表扫），这个业务约束就写死在 `<if>` 外的时间条件上——动态 SQL 也承担了「防止无边界查询」的职责。

**容易答错的地方**：把动态 SQL 和 SQL 注入混为一谈。动态只是「按条件拼装」，安不安全取决于每个变量走 `#{}`（参数化）还是 `${}`（拼接）；另一个常见错误是说不出 `<foreach>` 的六个属性，一被追问就卡。

> 权威内容：MyBatis 动态 SQL 当前尚未在仓库建设，可参考 MyBatis 官方文档的 Dynamic SQL 章节。

---

### Q06：Mapper 接口的代理机制？ `P0` `源码`

**30 秒回答**：MyBatis 在启动时为每个 Mapper 接口生成 JDK 动态代理（MapperProxy），方法调用进来后按「接口全限定名 + 方法名」找到对应的 MappedStatement，再由 MapperMethod 编排执行方式（增删改查、以及参数如何转换），最终交给 SqlSession。

**深入理解**：链路：Configuration 解析 XML/注解 → `MapperRegistry` 把接口类与代理工厂绑定 → `getMapper()` 时 `MapperProxyFactory` 生成实现接口的代理类 → 调用方法进入 `MapperProxy.invoke` → 用 `MapperMethod` 缓存的方法对象判断 SQL 命令类型（SELECT/INSERT/UPDATE/DELETE）→ 参数经 ParamNameResolver 处理（@Param 注解或多参数打包成 Map，单参数直接透传）→ 调 SqlSession 的 selectList/insert 等 → 进入 Executor 链。设计上的关键点：接口方法不允许重载——因为定位 statement 只靠「接口名 + 方法名」，重载后两个方法共用一个 id 必然冲突，启动就报错。返回值也受约束：单条查询返回多个结果会抛 TooManyResultsException，这是「查询本意」的校验而不是 bug。

**面试官可能继续追问**：
- **MapperProxy 是 JDK 代理还是 CGLIB？** —— JDK 动态代理，因为 Mapper 一定是接口；它没法代理没有接口的类，也不需要。
- **多个参数不加 @Param 会怎样？** —— MyBatis 会把它们打成 `param1`、`param2`… 的 Map（或按 `arg0` 引用，版本间有差异），XML 里就得写 `#{param1}`——所以规范是「多参数一定加 @Param」，可读性和稳定性都好。

**实际工程场景**：排查「方法明明写了却报 Invalid bound statement (not found)」时，检查顺序是：namespace 和方法名是否对得上、XML 是否被 mybatis 扫描到（mapper-locations 配置）、是不是方法重载了。理解了代理机制，这三个怀疑点不用查文档就能列出来。

**容易答错的地方**：说「MyBatis 用 CGLIB 生成 Mapper 实现类」——是 JDK 动态代理，因为 Mapper 是接口；另外「一个 Mapper 接口可以有多个实现」也是错的，全局限唯一。

> 权威内容：MyBatis Mapper 代理当前尚未在仓库建设，可参考 MyBatis 官方文档的 Mapper Interfaces 章节。

---

### Q07：Executor 和 SqlSession？ `P1` `重点`

**30 秒回答**：SqlSession 是面向使用者的门面（增删改查 + 事务 + 获取 Mapper），Executor 是它背后的执行引擎，负责缓存管理、Statement 创建与执行调度；Executor 有 Simple、Reuse、Batch 三种，CachingExecutor 是叠在上面的缓存装饰器。

**深入理解**：三者对比：SimpleExecutor 每次执行都 `prepareStatement` 用完即关；ReuseExecutor 按 SQL 缓存 Statement 复用（同一会话重复相同 SQL 时省去预编译）；BatchExecutor 把更新类语句攒批执行（JDBC addBatch/executeBatch），需要手动 `flushStatements`，且查询场景不适用。DefaultSqlSession 是默认实现，**线程不安全**——它持有 Executor 与事务状态，所以规范是「每个请求/线程一个 SqlSession，用完必关」。二级缓存开启时 Configuration 用 CachingExecutor 包住基础 Executor，形成装饰器链：缓存优先、未命中下沉。事务边界由 SqlSession 控制 commit/rollback，它影响的是「这波 Statement 何时提交」，与 Spring 整合后提交权被 TransactionManager 接管（见 Q11）。

**面试官可能继续追问**：
- **DefaultSqlSession 为什么线程不安全？** —— 内部持有 Executor、localCache、事务资源且没有同步；两个线程共用一个 SqlSession，缓存与事务状态会互相污染，规范用法就是「用时获取、用完关闭」。
- **BatchExecutor 什么时候用、注意什么？** —— 大批量 INSERT/UPDATE 时用（`ExecutorType.BATCH`），能显著减少网络往返；但它要求「批内 SQL 同构」，且要记得 flush，混入 SELECT 时行为不可靠——批量插入优先考虑 foreach 分批 + `rewriteBatchedStatements`。

**实际工程场景**：WMS 库存对账任务一次性刷十万行差异，普通逐条更新要几分钟；改 Batch 模式 + 每 2000 条 flush + 分批提交，压到秒级——瓶颈从「逐条提交的往返」挪到「数据库批量写入」。面试讲这个案例时把「分批」和「事务大小」一起说，说明你知道批量与事务的耦合关系。

**容易答错的地方**：把 SqlSession 和 Executor 混为一谈（门面 vs 引擎），或者说「SqlSession 是线程安全的可以共享」——恰相反，它是明确非线程安全的，这是 MyBatis 官方反复强调的使用边界。

> 权威内容：MyBatis Executor 与 SqlSession 当前尚未在仓库建设，可参考 MyBatis 官方文档的 Configuration 与 Java API 章节。

---

### Q08：MyBatis 插件机制？ `P1` `源码`

**30 秒回答**：MyBatis 插件就是拦截器：用 @Intercepts + @Signature 声明要拦截四大对象（Executor、StatementHandler、ParameterHandler、ResultSetHandler）的哪个方法，MyBatis 启动时用 JDK 代理把拦截器串成责任链包住目标对象，调用时逐层经过。

**深入理解**：四大对象可拦截的时机各有用途：Executor 的 query/update（分页、多数据源、全量缓存——PageHelper 就在这里改写 SQL）；StatementHandler 的 prepare（SQL 日志、耗时统计、加密改写）；ParameterHandler 的 setParameters（参数脱敏）；ResultSetHandler 的 handleResultSets（结果集加工）。责任链顺序由插件声明顺序决定（后声明的包在外层）。分页插件原理值得展开：PageHelper 用 ThreadLocal 存分页参数，拦截 `Executor.query` 时先执行 count 查询拿总数，再把原 SQL 包装成 `select ... from (原SQL) limit ...` 或直接改写 limit 子句，最后清理 ThreadLocal——「ThreadLocal 传参 + 拦截改写」这个模式也可以迁移到自己的框架里。风险点：插件用错方法签名会静默不生效（@Signature 写错没有启动报错），以及插件会包住所有查询，逻辑必须足够轻。

**面试官可能继续追问**：
- **插件能拦截到私有方法吗？** —— 不能。四大对象都是接口，代理只能拦接口公开方法；MyBatis 内部类对私有逻辑没有拦截点，这是插件能力的硬边界。
- **为什么分页插件不直接用拦截 StatementHandler？** —— 也能（早期实现如此），但 Executor 层能拿到 MappedStatement 与边界参数，做 count 查询与「逻辑分页判断」更完整；PageHelper 后来选择 Executor 层，兼容性更好。

**实际工程场景**：自研 SQL 审计插件：拦截 StatementHandler.prepare，把最终 SQL 与耗时按 traceId 落日志——比在每个 DAO 里包一层干净。落地时的教训是插件逻辑要极简（它在每次数据库调用上都跑），且必须防住自己的异常把业务调用带崩（try-catch 兜底 + 开关降级）。

**容易答错的地方**：只说「PageHelper 用了插件」。面试官要的是机制：拦截哪个对象的哪个方法、ThreadLocal 怎么传参、count 查询怎么来的；以及插件对性能的影响（每次调用多一层代理，逻辑必须轻）。

> 权威内容：MyBatis 插件机制当前尚未在仓库建设，可参考 MyBatis 官方文档的 Plugins 章节。

---

### Q09：MyBatis 的分页怎么做？ `P0` `高频`

**30 秒回答**：物理分页优先——用 PageHelper 这类插件在 SQL 层加 limit，配合数据库索引；逻辑分页（查出全部再内存截断）在大表上是灾难；深分页的优化手段（延迟关联、游标分页）与数据库侧一致，见 [数据库基础](数据库基础.md) Q03。

**深入理解**：两条路线对比：逻辑分页是 `RowBounds` 传内存边界，MyBatis 仍把全量结果查出来再截断——省的是 Java 对象创建，省不了数据库扫描，大表场景直接把内存打爆；物理分页由插件改写 SQL（count + limit），数据库只返回一页数据。PageHelper 的标准用法：`PageHelper.startPage(pageNum, pageSize)` 紧跟查询方法，原理见 Q08。安全与性能细节：分页参数必须校验上界（pageSize 无上限时一次拉 10 万行照样拖垮接口）；`order by` 要有索引否则 filesort；深分页（pageNum 过万）即使物理分页也慢——因为 `limit 1000000, 20` 仍要扫过前 100 万行，此时换延迟关联或游标分页（`where id > ? order by id limit ?`）。count 查询本身在大表上也可能慢，可以考虑「不查总数只查下一页是否有数据」的滚动加载。

**面试官可能继续追问**：
- **PageHelper 的 ThreadLocal 会泄漏吗？** —— 正常情况下 startPage 后第一个查询消费掉参数，插件用完清理；但如果查询方法内部吞掉异常或发生方法没执行到查询逻辑，参数可能残留影响下一次查询——这是「偶发分页错乱」类诡异 bug 的源头，新版本已做防御，老项目值得排查。
- **分页 count 一定要查吗？** —— 看产品形态：传统分页器要总页数就得查；「加载更多/无限滚动」只需要「当前页数据 + 是否有下一页」，count 全省了。

**实际工程场景**：WMS 后台列表全部接 PageHelper，同时做了三件事：pageSize 服务端封顶 200、排序字段白名单（防注入 + 保证走索引）、pageNum 超过阈值自动转「按时间倒序 + 游标续查」。这三条在面试里讲出来，比单说「用 PageHelper」更体现工程闭环。

**容易答错的地方**：把逻辑分页和物理分页说反，或认为「PageHelper 万能」——它对深分页、无索引排序、超大 count 都无能为力，这些要回到数据库侧解决。

> 权威内容：MyBatis 分页插件当前尚未在仓库建设，可参考 PageHelper 官方文档与 [数据库基础](数据库基础.md) Q03。

---

### Q10：MyBatis 批量操作怎么优化？ `P1` `工程实践`

**30 秒回答**：批量优化的核心是减少往返与提交次数：SQL 用 foreach 多值插入或 BatchExecutor，JDBC 层开 `rewriteBatchedStatements`，应用层按批大小（千行级）拆分、一个事务一次批量提交；主键回填用 `useGeneratedKeys`。

**深入理解**：四条路径逐个说。**多值 INSERT**：`insert into t (a,b) values (?,?),(?,?),...`，一次网络往返插多行，MySQL 侧需要 `rewriteBatchedStatements=true` 才能真正在 JDBC 层合并执行（否则仍是逐条）；SQL 长度受 `max_allowed_packet` 限制，所以要分批（如每 500～1000 行）。**BatchExecutor**：`SqlSession` 以 `ExecutorType.BATCH` 打开，同一 session 内多次 update 自动攒批，`flushStatements` 触发执行——适合「同一会话内大量同类更新」。**事务合并**：一万条独立事务的插入远慢于一个事务里的一万条批量插入，提交次数（fsync）才是隐藏瓶颈。**主键回填**：批量插入后要自增 ID 时开 `useGeneratedKeys=true, keyProperty=id`，MyBatis 会把生成的主键填回对象——比插入后回查高效且安全。验证手段：看慢查询日志与接口耗时，别凭感觉。

**面试官可能继续追问**：
- **为什么一个事务批量插入会比逐条快一个数量级？** —— 逐条提交每次都要 redo/binlog 落盘（fsync），一个批量事务只付一次；批量还减少了往返与解析开销。瓶颈清单里，fsync 通常排第一。
- **批量插入部分失败怎么办？** —— 整批回滚（同事务）或按批记录成功位点做补偿；批量放大失败影响面，所以批大小要和重试成本一起权衡，别一味求大。

**实际工程场景**：WMS 接口批量回传出库结果，一次最多 500 单，实现是「foreach 多值插入流水 + 每批一个事务 + useGeneratedKeys 回填」；压测时按 100/500/1000 三档试出批大小拐点。面试把这个「试出来的拐点」讲出来，比背参数更能证明做过。

**容易答错的地方**：只说「用 BatchExecutor」。多值 INSERT 和 Batch 是两条不同的路，且不配 `rewriteBatchedStatements` 时多值插入也可能退化为逐条执行——参数不提，等于没答到点上。

> 权威内容：MyBatis 批量操作当前尚未在仓库建设，可参考 MySQL 官方文档的 INSERT 优化与 MyBatis 官方文档的 Java API 章节。

---

### Q11：MyBatis 和 Spring 事务怎么整合？ `P0` `源码`

**30 秒回答**：整合的关键是「同一个事务里，MyBatis 的 SqlSession 和 Spring 的 DataSource 用的是同一条 JDBC 连接」：SqlSessionTemplate 在每次调用时从 Spring 的 TransactionSynchronizationManager 取线程绑定的 ConnectionHolder，有就复用、没有才开新的——所以 @Transactional 方法与 DAO 调用天然共享连接，事务边界由 Spring 统一控制。

**深入理解**：机制三层：mybatis-spring 的 `SqlSessionTemplate` 是线程安全的 SqlSession 门面，它的 `getSqlSession` 逻辑是「先看当前线程有没有事务绑定的 SqlSessionHolder，有则复用（同一事务共享会话，一级缓存也共享），没有则开新会话用完关闭」；`TransactionSynchronizationManager` 用 ThreadLocal 绑定的 `ConnectionHolder`（内含 DataSource 到连接的映射）由 `DataSourceTransactionManager` 在开启事务时放入；MyBatis 的 `JdbcTransaction`/`SpringManagedTransaction` 在执行时同样从这个 ThreadLocal 取连接——两条线在同一个 ThreadLocal 上会合，这就是整合的全部秘密。由此推出两个推论：一级缓存在 Spring 非事务方法里每次都是新会话所以失效，在 @Transactional 方法内跨 DAO 调用共享；@Transactional 失效（自调用、异常类型等）时，MyBatis 侧每个语句各自自动提交，表现为「事务没生效」——框架层原因见 [Spring](Spring.md) Q02。

**面试官可能继续追问**：
- **为什么同一个事务里 JdbcTemplate 和 MyBatis 能共享连接？** —— 两者都从 TransactionSynchronizationManager 的 ThreadLocal 拿连接，不是靠「互相认识」，是靠「都尊重 Spring 的事务资源管理」——这也是任何数据访问框架接入 Spring 事务的标准姿势。
- **事务方法里调用异步线程执行 DAO，会怎样？** —— 新线程没有事务上下文，会从数据源取新连接独立提交；要传递事务得用事务感知的线程池，或者改用消息驱动最终一致。

**实际工程场景**：WMS 出库主流程 `@Transactional` 方法内依次调「单据 Service → 库存 DAO → 流水 DAO」，全程一条连接一个事务，任一环节抛运行时异常整体回滚。理解这条链路后才能解释两个线上怪象：「为什么事务里查到的库存没变」（一级缓存读到会话内旧值）和「为什么异步线程里的写入没被回滚」（ThreadLocal 不跨线程）。

**容易答错的地方**：只答「MyBatis 和 Spring 整合用 SqlSessionTemplate」。面试官要的是「怎么共享连接的」——ThreadLocal + ConnectionHolder；能画出「DataSourceTransactionManager 绑定 → SqlSessionTemplate 复用 → SpringManagedTransaction 取用」这条链，这题才拿满。

> 权威内容：MyBatis-Spring 整合当前尚未在仓库建设，可参考 mybatis-spring 官方文档与 Spring 官方文档的 Transaction Management 章节。

---

### Q12：resultType 和 resultMap？ `P1` `工程实践`

**30 秒回答**：resultType 是「列名/下划线自动映射到同名/驼峰属性」的一键模式，适合单表简单查询；resultMap 是手写映射规则，用于列名与属性名对不上、一对一/一对多嵌套、类型转换等场景，核心是「显式声明列与属性的对应关系」。

**深入理解**：resultType 的映射规则：列名下划线转驼峰需要开 `mapUnderscoreToCamelCase`（或 MyBatis-Plus 默认开启），开了之后 `order_no` 自动映射到 `orderNo`——这一个配置能消灭项目里大半的 resultMap。嵌套查询两种写法：`<association>`（一对一，如单据头带货主信息）和 `<collection>`（一对多，如单据头带行列表）；嵌套有「嵌套查询」（再发一条 SQL，N+1 风险）和「嵌套结果」（join 一次查出按列分组）两种模式，后者效率可控但映射配置复杂。`<discriminator>` 按某列值走不同映射（多态实体）。延迟加载可以配，但它把简单的 N+1 换成「用时才查」，在大多数在线场景是弊大于利，不建议开。另一个实用点是 `resultMap` 的 `<constructor>` 用构造器注入映射——配合不可变实体比 setter 更干净。

**面试官可能继续追问**：
- **一对多为什么容易踩坑？** —— 嵌套查询默认 N+1（查 100 个单据头发 100 次行查询）；嵌套结果一次 join 但要处理「结果集按主列分组去重」，配错就会出现「3 行数据映射出 3 个单据对象、每个对象只带 1 行」。
- **列名和属性名一直对不齐怎么办？** —— 首选 SQL 里起别名（`select order_no as orderNo`），其次 `mapUnderscoreToCamelCase`，最后才写 resultMap；把 resultMap 当默认手段，XML 会迅速膨胀到没法维护。

**实际工程场景**：WMS 单据查询早期全是 resultMap，一个 XML 上千行；治理时统一打开驼峰映射 + SQL 起别名，resultMap 只留给「join 出的一对多嵌套」和「枚举/类型转换」两类——XML 行数降了七成， review 时能真正看清映射关系。

**容易答错的地方**：说「resultMap 更规范所以都用它」。工程现实是能自动映射就别手写；另外别忽略 `mapUnderscoreToCamelCase` 这个配置——答不出它，说明没做过中大型项目的映射治理。

> 权威内容：MyBatis 结果映射当前尚未在仓库建设，可参考 MyBatis 官方文档的 Mapper XML 文件章节。

---

### Q13：MyBatis、MyBatis-Plus、JPA 怎么选？ `P1` `设计`

**30 秒回答**：选型看团队与场景：MyBatis 是 SQL 完全可控、适合复杂查询与需要 DBA review  SQL 的团队；MyBatis-Plus 在它之上加条件构造器、代码生成与常用 CRUD 封装，适合想省样板代码又保留 SQL 控制权的团队；JPA/Hibernate 是对象图优先的 ORM，适合领域模型复杂、希望完全面向对象操作的场景——代价是复杂 SQL 与性能调优时要和框架搏斗。

**深入理解**：三个维度对比。**SQL 控制权**：MyBatis 最强（手写 SQL，执行计划可控、可 review）；MP 折中（简单 CRUD 免写、复杂查询退回 XML）；JPA 最弱（JPQL/Criteria 生成 SQL，深水区要靠原生查询兜底）。**开发效率**：MP 的 LambdaQueryWrapper + 代码生成对中后台 CRUD 提效明显；JPA 的实体关系映射在领域模型稳定后很省事，但初期配置成本高。**可维护性与团队结构**：有专职 DBA、SQL 要进 review 流程的团队（金融、WMS 这类业务逻辑重的系统）通常选 MyBatis 系——SQL 看得见摸得着，出问题能直接定位；模型驱动、迭代快、查询相对简单的业务选 JPA 更顺。MyBatis-Plus 的注意点：条件构造器生成的 SQL 可读性随链式长度下降，动态拼接复杂条件时仍建议写 XML；它的「逻辑删除/自动填充/乐观锁」是便利也是隐式行为，团队要有约定。

**面试官可能继续追问**：
- **MyBatis-Plus 和 MyBatis 是替代关系吗？** —— 不是，MP 是增强：MyBatis 的技能全部适用，它在启动器、BaseMapper、条件构造器上做增量；迁移成本主要是习惯而非机制。
- **JPA 的复杂报表查询怎么写？** ——  JPQL 表达不了的用 `nativeQuery` 或 Specification + Projection，但写到这一步已经放弃了 ORM 的抽象红利——所以「重报表的系统选 JPA」通常不是好答案。

**实际工程场景**：WMS 这类系统选 MyBatis 系的理由可以直接用：库存扣减 SQL 要按执行计划反复调、慢 SQL 要能直接贴出来给 DBA、单据查询动辄五六张表 join——这些场景里「SQL 可见」就是最大的可维护性。反过来，内部运营工具的简单配置表，用 MP 的条件构造器一天能糊完一个模块，也别坚持手写。

**容易答错的地方**：把选型答成「哪个框架更先进」。技术选型没有银弹，面试官想听的是「你们的约束条件是什么（SQL review、复杂查询、团队结构），所以选了什么，代价是什么」。

> 权威内容：持久层框架选型当前尚未在仓库建设，可参考各框架官方文档与《企业应用架构模式》相关章节。

---

## 本专题常见追问链

```text
执行流程（接口方法 → 四层处理器 → JDBC）
  → 参数化（#{} 预编译 vs ${} 拼接）
  → 缓存（一级 SqlSession 级 → 二级 namespace 级）
  → 动态 SQL（XML 层拼装，安全仍看 #{}）
  → 插件（拦截四大对象 → 分页改写）
  → 事务整合（Spring ThreadLocal 绑定连接）
  → 批量优化（多值插入 / Batch / 事务合并）
```

这条链的走向是「从一次调用到一次事务」：前半段回答「SQL 怎么来」，后半段回答「SQL 怎么高效且正确地落库」。每往下一跳，问题就从「会不会用」变成「踩过什么坑」。

## 面试前速记

- 执行链路：SqlSession → Mapper 代理 → MappedStatement → Executor → StatementHandler → ParameterHandler → ResultSetHandler。
- `#{}` 编译成 `?` 走预编译，`${}` 原样拼接；`${}` 只能用于表名/列名/排序方向且必须白名单。
- 一级缓存 = SqlSession 级（默认开、增删改清空、Spring 下基本失效）；二级缓存 = namespace 级（默认关、按 namespace 粗粒度失效、跨实例不一致，生产建议关）。
- 动态 SQL：`<where>` 去连接词、`<foreach>` 拼 IN/批量、`<set>` 去逗号；IN 列表必须用 foreach 生成 `#{}`。
- Mapper 代理是 JDK 动态代理，按「接口全限定名 + 方法名」定位 statement——所以方法不能重载。
- Executor 三种：Simple/Reuse/Batch；DefaultSqlSession 线程不安全，每请求一个、用完必关。
- 插件 = @Intercepts + @Signature 拦四大对象，责任链；PageHelper = ThreadLocal 传参 + 拦 Executor.query 改写 SQL 做 count + limit。
- 分页：物理分页（插件改写 SQL）优先，逻辑分页（RowBounds 内存截断）禁用于大表；深分页回数据库侧优化（延迟关联/游标）。
- 批量：多值 INSERT + `rewriteBatchedStatements` + 分批（防 max_allowed_packet）+ 一个事务一次提交；`useGeneratedKeys` 回填主键。
- Spring 整合核心：SqlSessionTemplate 从 TransactionSynchronizationManager（ThreadLocal）取 ConnectionHolder，MyBatis 与 JdbcTemplate 因此共享连接。
- resultType 走自动映射（开 `mapUnderscoreToCamelCase`），resultMap 留给列名对不上、association/collection 嵌套、类型转换。
- 选型：SQL 要可控要 review（金融/WMS）选 MyBatis 系；省 CRUD 样板选 MyBatis-Plus；对象图驱动、查询简单选 JPA。
