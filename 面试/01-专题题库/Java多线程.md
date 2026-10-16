# Java 多线程

> 一句话定位：这一专题考的是「共享状态怎么管」——从可见性、原子性，到锁、线程池与异步编排，面试官用它验证你能否写出正确的并发代码、能否在线上排查并发故障；与 JVM（内存结构与 GC）、集合框架（ConcurrentHashMap 等容器）、操作系统（调度与死锁）相邻，但本专题的关注点是 Java 语言与 JUC 层的语义边界、选型与排查。

返回 [专题题库首页](README.md) · [面试首页](../README.md)。

## 专题定位

- 高频原因：并发是后端系统正确性与稳定性的底座，5 年以上岗位几乎必问；且这是整个题库里追问链最长的专题——「是什么」通常只是起手，真正的分水岭在「边界在哪、什么时候不该用它、线上出了事怎么查」。
- 面试官主要验证三件事：概念边界是否精确（volatile 不保证原子性、RUNNABLE 同时覆盖就绪与运行、BLOCKED 等锁而 WAITING 等通知）；选型是否有理由（synchronized 还是 ReentrantLock、线程池参数怎么定、阻塞队列怎么挑）；排查是否有手段（jstack 看死锁与锁竞争、线程状态分布、拒绝计数）。
- 5 年以上工程师的答题标准：不停留在 API 和八股，要讲代价与边界（无界队列为什么是陷阱、ThreadLocal 为什么必须 remove、JDK 21 虚拟线程的 pinning 问题），并能落到真实业务（库存扣减、出库流程、MQ 幂等消费）的并发控制上。
- 本专题与知识层 03-并发编程 的分工：这里只给面试口径，机制解释全部用相对路径链接过去，不复制。
- 版本意识是硬要求：偏向锁 JDK 15 废弃、虚拟线程 JDK 21 才转正 LTS、CompletableFuture 超时 API 要 JDK 9+——不带版本号的结论一律按过时处理。

## 高频题目导航

| 编号 | 面试问题 | 重要程度 | 追问深度 |
| --- | --- | --- | --- |
| Q01 | volatile 保证了什么，没保证什么？ | P0 | 原理 |
| Q02 | JMM 是什么，为什么需要它？ | P0 | 原理 |
| Q03 | synchronized 的锁升级过程是怎样的？ | P0 | 源码 |
| Q04 | AQS 的 state 是怎么被各个同步器复用的？ | P0 | 源码 |
| Q05 | 线程池核心参数怎么定，任务提交后发生了什么？ | P0 | 工程实践 |
| Q06 | ThreadLocal 为什么会内存泄漏，JDK 做了什么补救？ | P0 | 排查 |
| Q07 | CAS 的 ABA 问题怎么解决？ | P1 | 原理 |
| Q08 | synchronized 和 ReentrantLock 怎么选？ | P0 | 工程实践 |
| Q09 | 线程的生命周期和状态转换是怎样的？ | P0 | 原理 |
| Q10 | sleep 和 wait 有什么区别？ | P0 | 排查 |
| Q11 | wait/notify 机制怎么用？ | P1 | 原理 |
| Q12 | Java 里创建线程的方式有哪些？ | P1 | 原理 |
| Q13 | 死锁怎么产生、怎么排查？ | P0 | 排查 |
| Q14 | 线程池的拒绝策略有哪些，怎么选？ | P1 | 工程实践 |
| Q15 | CompletableFuture 怎么用？ | P1 | 工程实践 |
| Q16 | 阻塞队列和并发容器怎么选？ | P1 | 工程实践 |
| Q17 | Java 21 虚拟线程是什么，适合什么场景？ | P1 | 工程实践 |

（P0=优先复习，P1=常见，P2=了解即可；追问深度取 原理/源码/工程实践/排查 之一）

## 题目正文

### Q01：volatile 保证了什么，没保证什么？ `P0` `高频`

**30 秒回答**：volatile 保证可见性和有序性，不保证原子性——所以它能安全地做状态标志位，不能安全地做计数器。

**深入理解**：可见性靠写时插入内存屏障把工作内存的值刷回主内存、读时重新从主内存加载；有序性靠禁止特定重排序。不保证原子性是因为 `i++` 是「读—改—写」三步，volatile 只保证每一步读到的都是最新值，不保证三步整体不被穿插，两个线程可能都读到 5 再都写 6。修法是换 `AtomicInteger`；纯统计且竞争激烈时 `LongAdder` 的分段累加更划算。还要主动补一句：volatile 不互斥，它替代不了锁。

**为什么这样设计**：如果 volatile 连带保证原子性，就必须引入互斥语义，那它与 `synchronized` 的区别就消失了，读路径的开销也会上升。JMM 的做法是把「可见性/有序性」与「原子性」拆成两个正交保证，让调用方按需选最便宜的那个——代价是这两个概念常被混为一谈，这恰好也是这道题高频的原因。

**面试官可能继续追问**：
- **volatile 能保证 `long`/`double` 读写的原子性吗？** —— 能，但这是 JMM 对 volatile long/double 的特例规定（对它们的读写必须是原子的），不是「volatile 让普通运算变成原子」；非 volatile 的 long/double 在规范上允许被拆成两次 32 位写，只是 64 位 JVM 上一般不发生，别把话题带偏成内存结构。
- **「双重检查锁」里的单例字段为什么要加 volatile？** —— 防止 `instance = new Singleton()` 的三步（分配内存、初始化、赋引用）被重排成「赋引用」先于「初始化」执行，另一个线程可能拿到非 null 但字段还是默认值的半成品对象；volatile 的写屏障禁止这种重排。
- **一个 volatile 标志位能保证「看到」它之前的所有写入吗？** —— 能。volatile 写 happens-before 于后续对同一变量的读，借传递性，写之前的普通写在读之后也可见——这才是它做「停止标志」真正依赖的性质，比「可见」两个字更进一层。

**实际工程场景**：典型用法是「停止标志」与「配置热切换」——volatile boolean running 让工作线程在一轮循环后退出；volatile 引用指向不可变配置对象，发布时换引用即可，读者无需加锁。WMS 里理货任务的取消标志可以用它；但库存扣减这类「读—改—写」共享状态，volatile 不够，要么原子类要么锁。

**容易答错的地方**：把 volatile 说成「轻量级锁」；认为 `volatile int i; i++` 安全；认为有了 volatile 就不需要 synchronized。另外 JMM 里没有「工作内存」这块物理区域，它是抽象模型，别按 JVM 内存结构图回答。

> 权威内容：[volatile 原理与使用边界](../../03-并发编程/02-深度解析/volatile原理与使用边界.md) · [JMM 与 volatile 速记](../../03-并发编程/01-面试速记/JMM与volatile速记.md)

### Q02：JMM 是什么，为什么需要它？ `P0` `高频`

**30 秒回答**：JMM 是 Java 对「多线程读写共享变量」的一套抽象规则，定义主内存与工作内存的关系以及哪些重排序被允许，目的是让并发行为可预测且不绑定具体 CPU 架构。

**深入理解**：它的实际形态是一组 happens-before 规则：程序顺序、监视器锁、volatile 变量、线程启动与终止、传递性。只要读写能被这些规则串起来，就能得到确定的可见性与有序性。JMM 允许编译器、CPU、缓存做重排序，但要求「在本线程语义内不可观测」，这就是 `as-if-serial`。被追问时的落点通常是「happens-before 与内存屏障的关系」——屏障是实现手段，happens-before 是规范层面的保证。

**为什么这样设计**：硬件层面 x86 是 TSO、ARM 是更弱的内存模型，若直接把硬件模型暴露给程序员，Java 程序就不可移植。JMM 给出「最小保证」：只承诺满足规则时的行为，比硬件更弱的部分由程序员用同步原语补齐——这样既能在弱内存模型平台运行，也不必为可移植性牺牲全部优化空间。

**面试官可能继续追问**：
- **happens-before 和「时间先后」是一回事吗？** —— 不是。happens-before 是可见性保证的偏序关系：a happens-before b 意味着 a 的结果对 b 可见。两个操作时间上先发生但不在任何规则里，仍然可能看不到彼此。
- **as-if-serial 只保证单线程语义，那多线程的乱序从哪来？** —— 来自「没有 happens-before 关系约束的跨线程读写」。编译器和 CPU 的优化都以单线程不可观测为底线，跨线程没有同步时，观察顺序可以不同于程序顺序。
- **final 字段的初始化安全保证是什么？** —— JMM 保证对象正确构造后（构造过程中没有 this 逃逸），final 字段的值对所有线程可见且无需同步——这是 freeze 动作在规范层的体现，也是「不可变对象天然线程安全」的依据。

**实际工程场景**：JMM 不是背规则，是排查工具——线上遇到「明明赋值了另一个线程却读到 null/旧值」，先问「这两个操作之间有没有 happens-before 边」：没有 volatile、锁、线程启动/join 串起来，就是数据竞争，解法是补同步而不是加打印。安全发布对象（volatile、final、ConcurrentHashMap、线程池 submit）都是同一思想的具体化。

**容易答错的地方**：把 happens-before 说成「执行顺序保证」（它不保证执行顺序，只保证可见性）；把 JMM 等同于「Java 内存区域划分」（那是 JVM 内存结构，两回事）；以为 x86 强内存模型下 JMM 无所谓——写正确代码不能假设目标平台。

> 权威内容：[Java 内存模型与 happens-before](../../03-并发编程/02-深度解析/Java内存模型与happens-before.md)

### Q03：synchronized 的锁升级过程是怎样的？ `P0` `高频`

**30 秒回答**：路径是「无锁 → 偏向锁 → 轻量级锁 → 重量级锁」，按竞争程度逐级上升且不可逆；但**偏向锁自 JDK 15 起已被标记废弃并默认关闭（JEP 374）**，讲这条路径时必须说明版本。

**深入理解**：偏向锁把线程 ID 记在对象头 Mark Word 里，同一线程再次进入只做一次 ID 比较，几乎零开销，适合「一个线程反复进入」的场景；出现第二个线程竞争就撤销偏向、升级为轻量级锁——用 CAS 把 Mark Word 指向线程栈中的锁记录，靠自旋等待。自旋失败或持锁时间长就升级为重量级锁，依赖操作系统互斥量，未获取到的线程真正阻塞（不占 CPU，但唤醒有开销）。Mark Word 是承载这一切的字段，它的位模式随锁状态变化。

**为什么这样设计**：这是「按竞争程度付费」的典型设计——无竞争时几乎免费，轻度竞争用自旋避免内核态切换，重度竞争退回阻塞避免 CPU 空转。偏向锁被废弃的原因也在此：它依赖「同一线程反复进入」这一前提，而线程池让这个前提越来越不常见，撤销偏向所需的 safepoint 停顿代价却依然存在。

**面试官可能继续追问**：
- **锁升级能降级吗？** —— 不能。一旦升到重量级锁不会降回来（重入是同一线程重新进入，不是锁状态降级）。所以一次短时间的高竞争脉冲也可能把对象锁永久推到重量级。
- **轻量级锁的自旋会无限进行吗？** —— 不会。自旋次数有上限且是自适应的（按历史持锁时间调整），失败或对手持锁时间长就升级为重量级锁；自旋参数应用层无法直接控制，能控制的是临界区时长。
- **锁消除、锁粗化是什么？** —— JIT 的两种优化：锁消除证明锁对象不会逃逸出线程（如方法内 new 的 StringBuffer），直接去掉 monitor；锁粗化把相邻临界区对同一把锁的反复加解锁合并成一次。二者都是编译期优化，只对 synchronized 生效。
- **Mark Word 里都存了什么？** —— 哈希码、GC 年龄、锁标志位、偏向线程 ID、指向锁记录的指针、指向 Monitor 的指针——同一块存储按锁状态复用，这就是「锁状态编码在对象头」的具体含义。

**实际工程场景**：这条升级路径的实践含义是「热点对象上的临界区要短」。WMS 库存扣减如果在 synchronized 方法里查库、算金额、写流水，持有时间从微秒涨到毫秒，锁会迅速升级为重量级，吞吐被拖垮——正确做法是只在改内存计数或单行 DB 更新时持锁，IO 放到临界区外。

**容易答错的地方**：把「不可逆」答错成「竞争消失后降级」；只说四级不说 JDK 15 废弃偏向锁；把自旋说成「一直 CPU 空转」——自旋有上限，失败即升级阻塞。另外 JDK 15+ 默认配置下直接从无锁进轻量级，别默认偏向锁还存在。

> 权威内容：[synchronized 原理与锁实现](../../03-并发编程/02-深度解析/synchronized原理与锁实现.md) · [synchronized 与 CAS 速记](../../03-并发编程/01-面试速记/synchronized与CAS速记.md)

### Q04：AQS 的 state 是怎么被各个同步器复用的？ `P0` `高频`

**30 秒回答**：`state` 是一个 `volatile int`，语义完全由子类定义，AQS 只负责「用 CAS 改它 + 把它与一个 FIFO 等待队列绑定」，所以同一个字段既能表示重入次数，也能表示许可数或读写计数。

**深入理解**：`ReentrantLock` 把 `state` 当重入计数（0 空闲、n 表示被同一线程重入 n 次）；`Semaphore` 当剩余许可数；`CountDownLatch` 当倒计数，减到 0 放行所有等待者；`ReentrantReadWriteLock` 把 32 位拆成高 16 位写计数、低 16 位读计数（因此读锁最多 65535 个）。共享与独占两种模式靠 `tryAcquire` 与 `tryAcquireShared` 区分；等待队列是 CLH 变体的双向队列，节点里的 `waitStatus` 记录后续节点的等待状态；`Condition` 另起条件队列，`await` 时把节点从同步队列转到条件队列。

**为什么这样设计**：把「排队、阻塞、唤醒」这套最容易出错的逻辑收进 AQS，子类只需回答「当前状态下能否获取、怎么改 state」两个问题——这是模板方法把复杂度上移的典型。代价是 `state` 语义完全由子类维护，读源码时必须先确认是哪个同步器，否则 `state` 是什么含义无从判断。

**面试官可能继续追问**：
- **AQS 怎么判断该不该阻塞当前线程？** —— 独占模式下 `tryAcquire` 返回 false 就入队并 `park`；醒来后先看前驱是不是 HEAD（有没有轮到自己）、再 `tryAcquire`，失败继续 park——这就是「唤醒后重新竞争」而不是「醒来即获得」。
- **公平锁和非公平锁在 AQS 层的差别？** —— 只在获取时多一次 `hasQueuedPredecessors()` 判断：非公平锁先直接 CAS 抢一次再排队，公平锁先看队列有没有前驱。差别就这一行，但吞吐和饥饿表现差很多。
- **`state` 为什么用 int 而不是 long，读写锁拆位不会溢出吗？** —— 读写锁把 state 按 16 位拆段，读写各自最多 65535，超过就是实现极限；这是「一个字段复用全部状态」换来的简洁性代价，绝大多数场景碰不到。

**实际工程场景**：理解 AQS 后，Semaphore 做限流（如 WMS 出库接口同时最多 200 个在执行）、CountDownLatch 等一批子任务（批量理货并行后汇总）都是「state 语义 + 队列」的组合。自己写同步器几乎不该做——用现成的，把精力放在参数与监控上。

**容易答错的地方**：说 AQS「用 state 加锁」——state 只是被争抢的变量，互斥由 CAS + 队列 + park/unpark 完成；把条件队列和同步队列混为一谈（await 先释放锁、节点转移到条件队列，signal 再转回同步队列）；以为公平锁绝对公平——只是按 FIFO 尝试，park 唤醒到真正获取之间仍可能被插队。

> 权威内容：[AQS 核心原理](../../03-并发编程/02-深度解析/AQS核心原理.md) · [AQS 与锁速记](../../03-并发编程/01-面试速记/AQS与锁速记.md)

### Q05：线程池核心参数怎么定？任务提交后发生了什么？ `P0` `高频`

**30 秒回答**：核心参数没有公式解，只有「起点 + 压测校准」；任务提交后是三步决策——核心线程未满就建核心线程，核心满了入队，队列满了才建非核心线程，再满则执行拒绝策略。

**深入理解**：CPU 密集型的起点是「核数 + 1」，IO 密集型按「核数 × 目标利用率 × (1 + 等待时间 / 计算时间)」估算，但这只是起点。三步决策的顺序有个重要后果：若用了无界队列（`LinkedBlockingQueue` 默认容量近于无限），第二步永远不会满，`maximumPoolSize` 形同虚设，任务全堆在队列里，延迟和内存一起失控。正确做法是有界队列 + 明确的拒绝策略 + 队列水位监控。真实值必须由压测和线上监控校准——例如「8 核初设核心 16、最大 32、队列 500，后来发现瓶颈在下游连接池只给了 20，于是把最大线程数压到 24 并加了队列水位告警」。

**为什么这样设计**：这个「核心 → 队列 → 非核心」的顺序与很多人的直觉相反（直觉是先扩大线程再排队），它表达的是「优先用线程数吸收突发、把队列当缓冲而非主要承载」。理解了这一点，才能理解为什么无界队列是设计陷阱而不是配置疏漏。

**面试官可能继续追问**：
- **`Executors.newFixedThreadPool`/`newCachedThreadPool` 为什么生产上不推荐？** —— 前者默认用近乎无界的 LinkedBlockingQueue，maximumPoolSize 失效，任务堆积拖垮内存；后者核心 0、队列 SynchronousQueue、maximum 无限，线程数爆炸或 OOM 的风险都在。要的就是「显式 ThreadPoolExecutor + 有界队列」。
- **核心线程数会被回收吗？** —— `allowCoreThreadTimeOut(true)` 后核心线程也受 keepAlive 约束，适合流量有明显峰谷的场景；默认 false，核心线程长期驻留。
- **任务在队列里还没执行，怎么取消？** —— `remove(Runnable)` 对未开始的任务有效；已开始的中断才是 cancel 的领域。提交时保留 Future 是运维兜底手段，别指望事后能精确取消。
- **线程池的线程是怎么复用的？** —— worker 线程跑完一个 task 后在 `runWorker` 里循环从队列取下一个；`getTask()` 队列空且超过 maximum 就返回 null 退出。所以「线程复用」本质是 worker 循环 + 阻塞取任务。

**实际工程场景**：WMS 出库流程拆成「下单—分配库存—波次下发—回传」几段，每段一个独立线程池 + 有界队列 + 明确的拒绝策略，关键是监控队列水位和拒绝计数——水位持续上涨说明容量不够或下游变慢，此时扩容线程通常没用，先查下游（连接池、慢 SQL）。线程隔离比调大参数重要：慢任务和快任务混在一个池，快任务会被拖死。

**容易答错的地方**：报「CPU 核数 × 2」当标准答案——没有放之四海的比例，IO 密集度、下游容量、业务 SLA 都要进公式；说「队列满了先扩线程」——顺序恰好相反；以为 maximumPoolSize 总能生效——无界队列下它永远不生效。另外 keepAlive 对核心线程默认无效这一点也常被答错。

> 权威内容：[ThreadPoolExecutor 线程池](../../03-并发编程/02-深度解析/ThreadPoolExecutor线程池.md) · [线程池速记](../../03-并发编程/01-面试速记/线程池速记.md)

### Q06：ThreadLocal 为什么会内存泄漏？JDK 做了什么补救？ `P0` `高频`

**30 秒回答**：`ThreadLocalMap` 的 key 是弱引用、value 是强引用，key 被回收后 value 仍被 `Entry` 强引用着、而 `Entry` 又被线程持有，于是 value 随线程一起存活——JDK 的补救是 `get`/`set` 时顺带清理 key 为 null 的 `Entry`，但不保证清到，所以必须自己 `remove`。

**深入理解**：结构是 `Thread → ThreadLocalMap → Entry(ThreadLocal 弱引用, value 强引用)`。key 用弱引用是为了让 `ThreadLocal` 对象本身能被回收，避免 key 永远存活；但 value 若也用弱引用，正常使用时就会被回收掉，所以只能强引用。代价就是「key 为 null 但 value 还在」的滞留条目。线程池场景是重灾区：线程长期存活，不 `remove` 则 value 一直在，而且下一个任务可能读到上一个任务的残留数据——这比内存泄漏更危险。

**为什么这样设计**：这是「弱引用只能保护 key、保护不了 value」的结构性结果，不是可以简单修掉的缺陷——真正的解法只有「用完显式 remove」，所以规范写法总是把 `try { ... } finally { tl.remove(); }` 写在一起。JDK 的惰性清理只是减慢泄漏速度，不能当解决方案。

**面试官可能继续追问**：
- **ThreadLocalMap 怎么解决哈希冲突，和 key 弱引用有关系吗？** —— 用开放地址法（线性探测）而非链表；key 过期后探测链可能断，所以 `expungeStaleEntry` 会主动重排后续条目——这是它不用链表的代价之一。
- **`InheritableThreadLocal` 是什么，有什么问题？** —— 创建线程时把父线程的值复制给子线程，适合传递 traceId 这类上下文；但用线程池时「父子」关系不确定（任务提交线程 ≠ 执行线程），传递会失效或串味，需要装饰线程池（如 TTL 的 TransmittableThreadLocal）才行。
- **除了内存泄漏，ThreadLocal 更大的风险是什么？** —— 线程池下的数据串味：上一个请求的 userId、租户 ID 残留在线程变量里，下一个请求读到别人的数据——这是安全 bug，不是性能问题。面试里主动讲这个，层次立刻不一样。

**实际工程场景**：WMS 里常见用法是传递当前操作人、仓库编码、traceId。规范是「入口 set、出口 finally remove」；请求级上下文优先考虑随任务参数显式传递，能不用 ThreadLocal 就不用——它在过滤器拦截器里最自然，在异步线程池里最容易出事。监控上可以定期 dump 线程的 ThreadLocalMap 大小做趋势观察。

**容易答错的地方**：说 value 也是弱引用；以为 JDK 会在 GC 时自动清掉 value；只答内存泄漏不答数据串味。还有「每个线程一个副本所以线程安全」是常见误读——ThreadLocal 保证的是隔离，不是共享变量的正确性，该同步的还是要同步。

> 权威内容：[ThreadLocal 原理与内存泄漏](../../03-并发编程/02-深度解析/ThreadLocal原理与内存泄漏.md) · [ThreadLocal 与并发容器速记](../../03-并发编程/01-面试速记/ThreadLocal与并发容器速记.md)

### Q07：CAS 的 ABA 问题怎么解决？ `P1` `中频`

**30 秒回答**：ABA 是「值从 A 变 B 又变回 A，CAS 检查不出中间发生过变化」，解法是给状态带上版本号，把比较对象从「一个值」扩成「值 + 版本」。

**深入理解**：典型受害场景是链表/栈的 CAS 操作——线程读到栈顶 A 且 A.next = B，准备把栈顶改成 B；期间别的线程弹出 A、又压入一个新 A，CAS 看到栈顶仍是 A 就成功，但此时 A.next 已经不是 B，结构被破坏。`AtomicStampedReference` 用 `Pair(reference, stamp)` 打包值与版本；`AtomicMarkableReference` 简化为「值 + 一个布尔标记」，适合只关心「有没有被改过」的场景。也可以退化成「不做物理删除、只做逻辑删除」绕开。注意 ABA 在纯数值累加场景里无害——`AtomicInteger.incrementAndGet` 不关心中间值，绝大多数计数场景不需要处理它。

**为什么这样设计**：CAS 的原语只能「比较并交换一个机器字」，硬件层面没有版本号概念，版本必须由使用者在软件层带上——这就把「每次读都要取版本、每次写都要带版本」的成本转嫁给了调用方。判断要不要付这个成本的依据是「中间状态是否会改变后续操作的正确性」。

**面试官可能继续追问**：
- **`AtomicStampedReference` 的 stamp 怎么推进，代价是什么？** —— 每次 CAS 成功把 stamp +1，用 int 计数；理论上会绕回，但 2^31 次修改在真实系统里不现实。代价是每次读取都要读 Pair 两个字段，且 JDK 内部是「CAS 改 Pair 引用」间接实现引用与版本的原子性，比裸 `AtomicReference` 重。
- **除了 ABA，CAS 还有什么问题？** —— 自旋开销（高竞争下反复失败空转 CPU，适合低并发短临界区）；只能保护一个变量（多变量要用锁或合并成一个引用）；「比较—使用」之间仍可能被穿插（ABA 就是特例）。
- **`LongAdder` 为什么在高竞争下比 AtomicLong 快？** —— 分散成多个 cell 分别累加，读时求和，把对单个变量的竞争摊平；代价是统计有轻微延迟、占用更多内存。这是「用空间换竞争」，和版本号一样都是对 CAS 短板的软件层补救。

**实际工程场景**：WMS 的库存扣减如果做乐观锁（版本号 CAS 更新），天然就是「值 + 版本」模式，不需要额外套 AtomicStampedReference；真正会手写 CAS 结构的是无锁队列、连接池状态机这类组件级代码。日常业务计数用 AtomicInteger/LongAdder 就够，别为想象中的 ABA 付出复杂度。

**容易答错的地方**：把 ABA 说成「并发安全的通用问题」——纯累加场景无害；只会说「加版本号」但不知道 `AtomicStampedReference` 就是 JDK 的实现；以为时间戳能替代版本号——系统时钟会回拨，版本号是计数器，二者不同。

> 权威内容：[CAS 与原子类](../../03-并发编程/02-深度解析/CAS与原子类.md)

### Q08：synchronized 和 ReentrantLock 怎么选？ `P0` `高频`

**30 秒回答**：默认用 `synchronized`；只有需要「可中断获取、可超时、公平锁、多个条件队列」这四项独有能力时才用 `ReentrantLock`。

**深入理解**：`synchronized` 是语言关键字，进入与退出由编译器生成 `monitorenter`/`monitorexit`，异常时自动释放，不会因忘记解锁而死锁；`ReentrantLock` 是 API，必须在 `finally` 里 `unlock`，漏了会永久死锁——这是它最常见的线上事故来源。四项差异的用法：`lockInterruptibly()` 让等待线程响应中断；`tryLock(timeout)` 给等待设上限，是避免线程被无限阻塞的关键；构造时传 `true` 得到公平锁（吞吐下降但避免饥饿）；`newCondition()` 可建多个条件队列，实现按条件分别唤醒，而 `synchronized` 只有一个等待集合、只能 `notifyAll`。此外 `ReentrantLock` 能通过 `getHoldCount`、`getQueueLength` 观测，`synchronized` 不能。

**为什么这样设计**：`synchronized` 的不可中断、无超时正是「简单性」的代价，而绝大多数代码确实不需要那些能力；语言层面还能额外享受 JVM 的锁优化（锁消除、锁粗化），API 层做不到。所以正确的默认是 `synchronized`——「选了 `ReentrantLock` 却说不出为什么需要它的独有能力」是常见减分点。

**面试官可能继续追问**：
- **什么场景必须用 `ReentrantLock`？** —— 三个典型：等待获取锁时要能响应中断/超时（如调用方带超时的资源争抢）；需要「不满则退让」而不是死等（tryLock 失败走降级逻辑）；需要多个 Condition 分区唤醒（如有界缓冲区的 notFull/notEmpty）。占其一的才是充分理由，一个都没有就该回到 synchronized。
- **`tryLock()` 无参版本和带超时版本差别？** —— 无参只试一次、绝不阻塞，适合「抢不到就撤」；带超时在等待期间阻塞但可被中断，适合「最多等 200ms」。两者失败后的分支处理才是业务逻辑的重点。
- **公平锁为什么吞吐更低，还什么时候值得用？** —— 公平锁要维护 FIFO 顺序、唤醒开销集中，上下文切换更多；当系统里已经有明显饥饿现象（某些请求长期抢不到锁）或对尾延迟敏感时才值得，多数场景非公平反而整体更快。

**实际工程场景**：WMS 库存扣减这类短临界区用 synchronized 就够；跨系统的资源争抢（如定时任务抢占）常用 tryLock(timeout)，拿不到就放弃本轮。选了 ReentrantLock 却到处 `lock()` 不带超时、不加中断处理，等于把它的优势全丢了还多担了忘记 unlock 的风险。

**容易答错的地方**：说 ReentrantLock「性能更好」——JDK 6 之后两者吞吐在同一量级，选型理由是能力不是性能；说 synchronized「不能重入」——两者都可重入；忘说 ReentrantLock 必须 finally unlock；把「可中断」和「可超时」混成一个能力。

> 权威内容：[ReentrantLock 与 Condition](../../03-并发编程/02-深度解析/ReentrantLock与Condition.md) · [并发编程面试主线](../../03-并发编程/01-面试速记/并发编程面试主线.md)

### Q09：线程的生命周期和状态转换是怎样的？ `P0` `高频`

**30 秒回答**：`Thread.State` 有六个状态：NEW（创建未 start）、RUNNABLE（可运行，含操作系统的就绪和运行）、BLOCKED（等待进入 synchronized/monitor）、WAITING（无限等待通知）、TIMED_WAITING（限时等待）、TERMINATED（执行结束）。核心转换：start 后进入 RUNNABLE；抢不到 monitor 进 BLOCKED；wait/join 进 WAITING；sleep(带时间)/wait(带时间) 进 TIMED_WAITING；run 结束或抛异常进 TERMINATED。

**深入理解**：RUNNABLE 是 Java 的抽象——它把 OS 的 ready（等 CPU）和 running（占 CPU）合并了，所以 jstack 看到 RUNNABLE 不代表一定在跑，也可能在等时间片。BLOCKED 与 WAITING 的区别是「等什么」：BLOCKED 在等一把锁（进入 synchronized 临界区失败），WAITING 在等一个通知或另一个线程结束（Object.wait 无超时、join 无超时、LockSupport.park）。TIMED_WAITING 是两者的限时版：

```java
synchronized (lock) {
    lock.wait();          // WAITING，释放锁
    lock.wait(1000);      // TIMED_WAITING，释放锁
    Thread.sleep(1000);   // TIMED_WAITING，不释放锁
}
```

jstack 解读要点：大量 BLOCKED 且指向同一个锁地址 → 锁竞争，查临界区时长；大量 WAITING on condition（park）→ 可能在等队列/IO，结合业务栈看；RUNNABLE 但 CPU 不高 → 可能在阻塞系统调用里，别被状态名骗了。

**为什么这样设计**：把 OS 调度状态压缩成六个面向 Java 语义的状态，是为了让「等待的原因」可见——BLOCKED/WAITING/TIMED_WAITING 直接告诉你线程卡在锁上、通知上还是时间上，这是 jstack 排查的基础。合并 ready/running 则是因为 Java 层拿不到也不该依赖 OS 调度细节。

**面试官可能继续追问**：
- **`Thread.sleep(0)` 有意义吗？** —— 会让出 CPU 给同优先级线程（取决于 OS 实现），但状态仍是 TIMED_WAITING；生产中不靠它做调度。
- **`interrupt()` 让线程进入什么状态？** —— 本身不改状态，只设中断标志；若线程正在 sleep/wait/join，会抛 InterruptedException 并转入 catch 分支（回到 RUNNABLE 执行）；park 的线程会被唤醒。中断是协作式的，线程不响应标志就什么也不会发生。
- **`stop()`/`suspend()` 为什么废弃？** —— stop 强制终止可能留下半解锁状态、破坏共享数据一致性；suspend 持锁挂起易死锁。都被中断机制取代。
- **JDK 21 之后这套状态图还好用吗？** —— 状态语义本身没变，但虚拟线程的栈默认不出现在 jstack 里，要用 `jcmd Thread.dump_to_file -format=json` 导出，且十万级线程的解读逻辑与平台线程时代完全不同（见 Q17）。

**实际工程场景**：WMS 出库高峰期的线程 dump 分析：几十个 BLOCKED 堆在同一个库存行锁对象上，说明临界区太粗或热点行锁粒度过细；WAITING 集中在 MQ 消费确认，说明消费速率跟不上。状态分布是定位方向的第一步，看的是「哪一类状态异常多」。

**容易答错的地方**：把 RUNNABLE 说成「正在运行」；把 BLOCKED 和 WAITING 混为一谈（一个等锁一个等通知）；说 wait 进 BLOCKED；认为 interrupt 是强制停止。

> 权威内容：[线程模型与生命周期](../../03-并发编程/02-深度解析/线程模型与生命周期.md)

### Q10：sleep 和 wait 有什么区别？ `P0` `高频`

**30 秒回答**：核心区别三条：sleep 属于 Thread、不释放锁；wait 属于 Object、必须在 synchronized 里调用、会释放锁；sleep 到时间自动醒，wait 要被 notify/notifyAll 唤醒或等超时。衍生差异是 sleep 不会被虚假唤醒，wait 用于线程间协作。

**深入理解**：

| 维度 | sleep | wait |
| --- | --- | --- |
| 归属 | Thread 的静态方法 | Object 的实例方法 |
| 锁 | 不释放已持有的 monitor | 释放 monitor，被唤醒后重新获取 |
| 调用前提 | 无 | 必须持有该对象的 monitor |
| 唤醒 | 超时自动 | notify/notifyAll 或超时 |
| 中断 | 抛 InterruptedException | 抛 InterruptedException |
| 状态 | TIMED_WAITING | 无参 WAITING / 带参 TIMED_WAITING |

sleep 不释放 synchronized 锁是经典线上事故：A 线程持锁 sleep，B 线程阻塞在锁上干等——排查时 jstack 会看到 B 是 BLOCKED、A 是 TIMED_WAITING 且持有 B 等待的锁。修法通常是「把 sleep 移出临界区」或用条件等待替代。

**为什么这样设计**：sleep 是「本线程让出 CPU 一段时间」的调度原语，和锁正交，所以没有释放锁的义务；wait 是「条件未满足、先把锁让给别人」的协作原语，释放 monitor 是它的语义本身——不然别的线程永远无法改变那个条件，协作直接死锁。

**面试官可能继续追问**：
- **wait 一定要在 synchronized 里吗，为什么？** —— 必须。wait 的语义是「释放当前 monitor 并等待」——不持有 monitor 就无从释放；JVM 会抛 IllegalMonitorStateException 保护这个前提。同理 notify/notifyAll 也必须在 synchronized 块内。
- **sleep 和 wait 都会抛 InterruptedException，处理上有什么区别？** —— 语义上一样（响应中断），但 wait 被中断后 monitor 已释放；sleep 被中断后仍持锁——catch 块里的补偿逻辑要想清楚自己处于哪种状态。
- **`Thread.yield()` 和它们什么关系？** —— yield 只是 hint，让出当前 CPU 但不进等待状态、不保证让出成功，生产中基本不用。

**实际工程场景**：WMS 场景里最常见的误用是「持锁轮询」——在 synchronized 里 `while (!ready) { sleep(100); }`，既持着锁又睡，别人没法推进 ready。正确做法是 wait/notify（或 Condition、BlockingQueue）：「生产者改完状态 notify，消费者 wait」——把锁在等待期间交出去。

**容易答错的地方**：说 wait 释放「所有锁」——只释放当前对象的 monitor，其他锁不释放；说 sleep 释放锁；说 wait 到时间自动返回而不需要重新获取锁——返回前要重新竞争 monitor；把 IllegalMonitorStateException 和 InterruptedException 搞混。

> 权威内容：[线程模型与生命周期](../../03-并发编程/02-深度解析/线程模型与生命周期.md)

### Q11：wait/notify 机制怎么用？ `P1` `中频`

**30 秒回答**：wait/notify 是 monitor 内的协作原语：线程在 synchronized 块内发现条件不满足就 wait（释放锁、进入该对象的等待集合），别的线程在 synchronized 块内改变条件后 notify/notifyAll 唤醒它；标准写法是 while 循环检查条件防止虚假唤醒，notify 只唤醒一个等待者，notifyAll 唤醒全部。

**深入理解**：生产者—消费者（有界缓冲区）的标准骨架：

```java
// 生产者
synchronized (lock) {
    while (queue.isFull()) {
        lock.wait();               // 满则等待，释放锁
    }
    queue.offer(item);
    lock.notifyAll();              // 通知可能等待的消费者
}
// 消费者
synchronized (lock) {
    while (queue.isEmpty()) {
        lock.wait();
    }
    T item = queue.poll();
    lock.notifyAll();
}
```

三个易错点：条件必须用 while 检查而非 if——被唤醒时条件可能又变了或被别的线程抢先（虚假唤醒虽然罕见但规范要求循环）；notify 唤醒的是任意一个等待者，若它条件仍不满足就继续 wait，其他本可运行的线程却没被叫醒——多条件场景下 notifyAll 更安全，代价是惊群；wait 会释放当前 monitor 并在返回前重新获取，所以临界区里 wait 返回后要重新校验一切共享状态。

**为什么这样设计**：JDK 5 之后官方推荐用 `java.util.concurrent.BlockingQueue`（如 ArrayBlockingQueue）替代手写 wait/notify：BlockingQueue 把「条件判断、等待、唤醒、锁」全部封装并经过充分测试，还支持有界、超时（offer(E, time)/poll(time)）和中断；手写 wait/notify 的错误面太大（if 误用、notify 选错人、单一条件队列）。只有特殊协作逻辑（多条件分区、自定义同步器）才值得自己写，那时也优先用 ReentrantLock 的 Condition。

**面试官可能继续追问**：
- **notify 和 notifyAll 怎么选？** —— 只有一个等待条件、且每个等待者被唤醒后都能有所进展时用 notify 省一点开销；多条件（非空/非满）或无法保证被唤醒者一定可运行时必须 notifyAll。生产中默认 notifyAll，把惊群留给 profile 说话。
- **虚假唤醒真实存在吗？** —— 规范明确允许（无通知的唤醒合法），HotSpot 上极少发生，但「while 检查条件」是必须养成的习惯，不是防御虚构问题。
- **wait 等待的是哪个对象的锁？** —— 调用 wait 的那个对象的 monitor。`synchronized (a) { b.wait(); }` 会抛 IllegalMonitorStateException——wait 的对象必须和持有的锁一致。

**实际工程场景**：WMS 波次下发里「下发线程池等出库结果齐了再回传」可以用 CountDownLatch 或 BlockingQueue 表达，不必手写 wait/notify；真遇到自定义协作（如限流器的「额度恢复」等待），优先 Condition.await/signal。看到生产代码里裸 wait/notify，第一反应是查 while 和 notifyAll。

**容易答错的地方**：用 if 而不是 while 检查条件；随便用 notify；在非 synchronized 上下文调用；以为 wait/notify 过时了没用——JUC 的 Condition 底层仍是这一套，面试官问的是理解不是复古。

> 权威内容：[线程模型与生命周期](../../03-并发编程/02-深度解析/线程模型与生命周期.md) · [ReentrantLock 与 Condition](../../03-并发编程/02-深度解析/ReentrantLock与Condition.md)

### Q12：Java 里创建线程的方式有哪些？ `P1` `中频`

**30 秒回答**：四种：Thread/Runnable、Callable + FutureTask、线程池、JDK 21 的 `Thread.ofVirtual()` 虚拟线程。生产默认线程池；需要结果和异常传播用 Callable；IO 密集型高并发场景虚拟线程是新选项。

**深入理解**：

```java
// 1. Runnable——无返回值、不能抛受检异常
new Thread(() -> { /* ... */ }).start();

// 2. Callable + FutureTask——有返回值、能抛异常
FutureTask<Integer> task = new FutureTask<>(() -> 1 + 1);
new Thread(task).start();
task.get();  // 阻塞拿结果；业务异常被包装成 ExecutionException

// 3. 线程池——复用线程，生产默认
Executors.newFixedThreadPool(8).submit(() -> { /* ... */ });

// 4. 虚拟线程（JDK 21）
Thread.ofVirtual().start(() -> { /* ... */ });
```

Runnable 的局限：`run()` 没有返回值、不能抛受检异常——需要结果或异常传播时用 Callable，`Future.get()` 会把业务异常包成 ExecutionException 抛出来。Future 的局限是 `get()` 阻塞、组合困难——编排场景用 CompletableFuture（Q15）。直接 new Thread 的问题是「每任务一线程」成本高：线程栈默认 1MB 左右（可调）、创建销毁有内核态开销，几千个任务就会压垮机器——这正是线程池存在的理由，也是虚拟线程要解决的问题。

**为什么这样设计**：语言提供了越来越高的抽象层次：Thread 暴露 OS 线程、Runnable 解耦任务与执行、线程池把「创建/复用/排队/拒绝」变成策略、虚拟线程把「阻塞不占 OS 线程」做成运行时能力。每一层都把上一层的成本问题推到下面——选哪层取决于任务规模和 IO/CPU 画像。

**面试官可能继续追问**：
- **start() 和 run() 的区别？** —— start() 才向 JVM 请求新执行路径（然后异步执行 run）；直接 run() 只是在当前线程顺序调用，不会创建线程。这是高频送分/送命题。
- **实现 Runnable 和继承 Thread 哪个好？** —— 实现 Runnable（或传 lambda）。继承受单继承限制，且把「任务」和「执行机制」耦在一起；Java 8 后 lambda 让 `new Thread(runnable)` 更自然。
- **Callable 怎么提交给线程池，结果怎么拿？** —— `submit(callable)` 返回 Future；`invokeAll`/`invokeAny` 批量。注意 Future.get() 阻塞调用线程，别在业务主链路里同步 get。
- **虚拟线程和线程池冲突吗？** —— 不冲突但通常二选一：虚拟线程的定位是「每任务一线程」，本身极轻量，不再需要池化复用；需要限流（同时最多 N 个执行）时用 Semaphore 而不是线程池。

**实际工程场景**：WMS 批处理（批量理货、批量对账）用固定大小线程池 + 有界队列即可；高并发网关型 IO（调用下游 ERP/OMS）如果升级到 JDK 21，虚拟线程能让「一个请求一线程」的直白写法重新可行。还在 JDK 8/11 的系统别硬造轮子，线程池 + 参数调优仍是主线。

**容易答错的地方**：只说「Thread 和 Runnable」两种——漏掉线程池和 Callable，5 年经验只答两种是减分项；说 Runnable 能抛异常；说 `submit(Runnable)` 有返回值（返回 `Future<?>`，get 得 null，除非用 `submit(Runnable, T result)`）；分不清 start/run。

> 权威内容：[线程模型与生命周期](../../03-并发编程/02-深度解析/线程模型与生命周期.md) · [ThreadPoolExecutor 线程池](../../03-并发编程/02-深度解析/ThreadPoolExecutor线程池.md)

### Q13：死锁怎么产生、怎么排查？ `P0` `高频`

**30 秒回答**：死锁是两个以上线程互相持有对方要的锁且互不让步，产生需要四个必要条件——互斥、持有并等待、不可剥夺、循环等待；排查靠 jstack（或 `kill -3`/`jcmd Thread.print`），输出里出现 "Found one Java-level deadlock" 即确认；修复靠统一加锁顺序、tryLock 超时、缩小临界区。

**深入理解**：最小复现代码——两个线程按相反顺序加锁 A、B：

```java
// 线程1
synchronized (lockA) {
    Thread.sleep(100);              // 放大窗口，保证线程2先拿到 B
    synchronized (lockB) { /* ... */ }
}
// 线程2
synchronized (lockB) {
    Thread.sleep(100);
    synchronized (lockA) { /* ... */ }
}
```

jstack 会直接给出结论段：

```text
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x... (object 0x..., a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x... (object 0x...),
  which is held by "Thread-1"
```

四个必要条件的含义：互斥（锁本身排他）；持有并等待（拿着 A 等 B）；不可剥夺（只能主动释放）；循环等待（T1→T2→T1）。打破任意一个即可预防，工程上最实用的是「破坏循环等待」——全局统一加锁顺序（如永远按 A→B，或按业务主键 ID 升序）；其次是 `tryLock(timeout)` 拿不到就回退重试，把「不可剥夺」变成「可超时放弃」。数据库层还有 `innodb_lock_wait_timeout` 与死锁检测兜底。

**为什么这样设计**：也可以直接用一把全局锁消灭死锁——循环等待被彻底破坏，但所有临界区串行，并发度归零。统一加锁顺序是「破坏循环等待」里代价最小的做法：锁的数量和粒度不变，只约束获取次序。`tryLock` 超时则从另一侧拆「不可剥夺」：把「拿不到就死等」改成「拿不到就退」，代价是调用方必须写好失败分支。

**面试官可能继续追问**：
- **jstack 没打印 "Found one Java-level deadlock" 就没有死锁吗？** —— 不一定。JVM 的死锁检测基于 ThreadMXBean.findDeadlockedThreads()，覆盖 monitor 与 ReentrantLock 且线程处于阻塞态；「活锁」、锁 + IO 混合的卡死、分布式场景（等下游返回）它都看不到，那要靠线程栈 + 业务日志人工判断。
- **怎么在代码里预防而不靠事后排查？** —— 三板斧：约定锁顺序（文档 + Code Review + 静态检查）；多锁场景一律 tryLock 超时；尽量缩小临界区、少用嵌套锁。线上再加兜底：jstack 定时 dump + ThreadMXBean 死锁扫描告警。
- **哲学家就餐问题和它什么关系？** —— 就是死锁的形式化模型：五个哲学家五根筷子，四个条件俱全。奇数哲学家先拿左筷、偶数先拿右筷（破坏循环等待）或限制同时就餐人数（破坏持有并等待）都是标准解法。

**实际工程场景**：WMS 出库流程涉及「库存行锁 + 波次锁 + 容器锁」多把锁时，必须约定全局顺序（如库存行 → 波次 → 容器，同层按 ID 升序）；跨服务时本地锁 + 分布式锁叠加更容易出现循环等待，此时超时和重试策略比「绝对不死锁」更现实。监控上把 ThreadMXBean 死锁检测做成定时任务，发现即 dump 现场——这是事后复盘唯一的素材。

**容易答错的地方**：四个条件背不全或说错（「不可剥夺」常被漏）；只说 jstack 不说它看不到活锁与分布式死锁；只答排查不答预防——面试官要的是「怎么让它在代码里不发生」；把「嵌套 synchronized」一律当死锁——只有顺序相反才死，同序嵌套是正常写法。

> 权威内容：死锁排查与预防当前尚未在仓库建设，可参考《Java 并发编程实战》死锁章节与 Oracle 官方 Troubleshooting Guide 的 deadlock 一节。

### Q14：线程池的拒绝策略有哪些，怎么选？ `P1` `中频`

**30 秒回答**：JDK 内置四种：AbortPolicy（默认，抛 RejectedExecutionException）、CallerRunsPolicy（调用线程自己执行）、DiscardPolicy（静默丢弃）、DiscardOldestPolicy（丢队列里最老的）；选型看业务能不能丢、要不要背压，还可以自定义策略做告警/落库补录。

**深入理解**：

| 策略 | 行为 | 适用 | 风险 |
| --- | --- | --- | --- |
| AbortPolicy | 抛 RejectedExecutionException | 关键任务不允许静默丢失 | 异常沿调用链上抛，需要上层处理 |
| CallerRunsPolicy | 提交线程（如 Tomcat 工作线程）自己跑 | 要求背压、整体放缓 | 拖慢甚至阻塞提交方，可能引发连锁 |
| DiscardPolicy | 直接丢弃新任务 | 可丢的监控/日志类任务 | 静默丢失，无感知 |
| DiscardOldestPolicy | 丢弃队列头部最老任务，再提交 | 旧数据无价值、追新场景 | 丢的恰可能是关键任务 |

选择逻辑：不能丢且调用方能处理异常 → Abort（最常用，配合告警）；希望「源头减速」→ CallerRuns，但要评估提交线程被占用的后果——Web 容器里 CallerRuns 会把压力回传给 HTTP 线程，请求越积越慢；监控埋点类可丢 → Discard。自定义策略常见做法：拒绝时计数 + 打点告警 + 落本地队列/DB 稍后补录，把「抛异常或丢弃」变成「降级 + 可恢复」。

**为什么这样设计**：拒绝策略的本质是「系统过载时的价值观」——没有完美默认，只有「此刻哪类损失更可接受」。JDK 把四种价值观做成可插拔（RejectedExecutionHandler 接口），让容量规划失控时系统的行为仍然可控，而不是 OOM 或无限堆积。

**面试官可能继续追问**：
- **拒绝发生在什么时候？** —— 走完 `corePoolSize → queue → maximumPoolSize` 三步都失败才触发；所以「调大 maximum」常被用来「消灭拒绝」，那是把容量问题藏起来，真过载时只是死得晚一点。
- **怎么监控拒绝？** —— 埋三个指标：activeCount、queue.size()/remainingCapacity、以及自定义拒绝策略里的 rejected 计数。队列水位持续高位 + rejected 增长 = 容量不足或下游变慢，先查下游再考虑扩容。
- **`shutdown()` 后提交任务会怎样？** —— 抛 RejectedExecutionException（走拒绝策略）。优雅停机用 shutdown()（不接新任务、等存量完成）+ awaitTermination(timeout)，超时再 shutdownNow() 中断。

**实际工程场景**：WMS 出库接口的异步处理池：Abort + 告警（任务不能丢，接口层同步返回失败让上游重试）；理货上报埋点池：Discard + 计数（可丢，不影响业务）；对账补偿任务：自定义策略落 DB 补录表，后台 Job 重扫。三种池三种策略，依据就是「这个任务丢了谁买单」。

**容易答错的地方**：四种策略张冠李戴（DiscardOldest 丢的是队列里最老的，不是最新的）；只会背名字说不出选型逻辑；说 CallerRuns「保证不丢」——它保证执行不保证及时，极端情况把提交方堵死；不知道可以自定义。

> 权威内容：[ThreadPoolExecutor 线程池](../../03-并发编程/02-深度解析/ThreadPoolExecutor线程池.md) · [线程池速记](../../03-并发编程/01-面试速记/线程池速记.md)

### Q15：CompletableFuture 怎么用？ `P1` `中频`

**30 秒回答**：CompletableFuture 是 JDK 8 的异步编排工具：thenApply 做同步转换、thenCompose 串行展开嵌套 Future、thenCombine 合并两个独立 Future；allOf/anyOf 做多任务聚合；exceptionally/handle/whenComplete 处理异常；生产中必须指定自己的线程池别占 ForkJoinPool.commonPool，超时 API orTimeout/completeOnTimeout 要 JDK 9+。

**深入理解**：

```java
CompletableFuture
    .supplyAsync(() -> queryStock(sku), bizPool)          // 异步执行，指定池
    .thenApply(stock -> calcAvailable(stock))             // 同步转换，同线程接着跑
    .thenCompose(r -> saveAsync(r))                       // 展开嵌套 Future
    .exceptionally(ex -> { log.error("fail", ex); return fallback; })
    .whenComplete((r, ex) -> metric.report(ex == null));  // 完成回调，不改结果
```

thenApply vs thenCompose 是最高频追问：thenApply 的返回值是普通值（`Function<T,U>`），thenCompose 的返回值是另一个 CompletableFuture（`Function<T, CompletableFuture<U>>`），用来「串行依赖且不嵌套」——thenApply 里返回 Future 会得到 `CompletableFuture<CompletableFuture<U>>` 的套娃。带 Async 后缀的版本（thenApplyAsync）会把该阶段丢进池里，无后缀则沿用上一阶段的线程——需要控制线程切换点就显式传 executor。

异常处理三兄弟：exceptionally 只处理异常分支并返回兜底值；handle 正常和异常都过（`(r, ex) -> ...`）；whenComplete 是纯副作用回调（能拿到结果和异常但不改结果，类似 finally）。聚合用 allOf（全部完成，注意它返回 `CompletableFuture<Void>`，要自己 join 各个子 Future 收集结果）和 anyOf（任一完成即返回其结果）。

**为什么这样设计**：它把回调地狱改成声明式依赖图，同时保持「编排」与「执行」分离——每个阶段可以指定执行者，纯计算阶段沿用当前线程、阻塞 IO 阶段丢进业务池。代价是链上任何一个节点抛异常且没人处理，后续阶段全部以相同异常短路完成——错误处理必须显式设计，这也是它比回调难调试的地方。

**面试官可能继续追问**：
- **不指定线程池用什么，有什么坑？** —— 默认 ForkJoinPool.commonPool()：并行度是 CPU 核数 - 1，且是全局共享的。链路上一个阻塞 IO 阶段会占满 commonPool，拖垮同 JVM 里其他用默认池的异步任务——生产中所有阶段显式传业务线程池是铁律。
- **`get()` 和 `join()` 区别？** —— get 抛受检的 InterruptedException/ExecutionException；join 抛未受检 CompletionException（同样包装原异常）。链式代码里用 join 更干净，但两者都阻塞调用线程——别在主链路同步 join。
- **超时怎么控制？** —— JDK 9+ 的 `orTimeout(duration)`（超时后以 TimeoutException 完成）和 `completeOnTimeout(value, duration)`（超时后给个默认值完成）。JDK 8 只能自己套 `ScheduledExecutorService` 触发 `completeExceptionally`。
- **CompletableFuture 和 RxJava/Reactor 怎么取舍？** —— 编排复杂度低（几步依赖、几个并行）用 CF 足够；需要背压、流式、复杂操作符才上反应式框架——引入后者的学习成本和调试成本要高一个量级。

**实际工程场景**：WMS 出库「并行查库存 + 查容器 + 查波次，三者都回来再组装下发」：三个 supplyAsync 丢业务池 + allOf + join 汇合，比串行三次 RPC 快得多；任一步失败用 exceptionally 走「回滚预占 + 返回可重试错误」。注意每个阶段指定线程池、给整体加 orTimeout——不然一个下游卡住会把汇合点永久挂起。

**容易答错的地方**：thenApply/thenCompose 混淆；以为 allOf 直接返回各子结果；不知道 commonPool 共享导致相互影响的坑；忽略异常短路传播（中间某步抛异常，后面 whenComplete 拿到的 ex 非 null 而结果是 null，直接 get 会炸）；在 JDK 8 环境写 orTimeout。

> 权威内容：[CompletableFuture 异步编排](../../03-并发编程/02-深度解析/CompletableFuture异步编排.md)

### Q16：阻塞队列和并发容器怎么选？ `P1` `中频`

**30 秒回答**：阻塞队列按场景选：有界缓冲 ArrayBlockingQueue、高吞吐 LinkedBlockingQueue、直接交接 SynchronousQueue、延迟任务 DelayQueue、带优先级 PriorityBlockingQueue；并发容器里 ConcurrentHashMap 是并发 Map 默认解，ConcurrentLinkedQueue 是无界非阻塞队列，CopyOnWriteArrayList 只适合读多写极少——各有明确代价。

**深入理解**：

| 容器 | 结构 | 特点 | 典型场景 |
| --- | --- | --- | --- |
| ArrayBlockingQueue | 数组 + 一把锁 | 有界、公平可选、put/take 阻塞 | 有界缓冲区，生产者消费者 |
| LinkedBlockingQueue | 链表 + put/take 两把锁 | 吞吐高，默认「无界」（Integer.MAX_VALUE） | 线程池默认队列（但建议显式给容量） |
| SynchronousQueue | 无存储 | 每个 put 等一个 take，直接交接 | CachedThreadPool、任务直达线程 |
| DelayQueue | 优先级堆 | 元素到期才能取 | 延迟任务、超时关闭 |
| PriorityBlockingQueue | 优先级堆 | 无界，按 comparator 出队 | 任务优先级调度 |

生产者—消费者用阻塞队列的写法，wait/notify 的全部复杂度（条件循环、notifyAll）都被收进 put/take：

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);
// 生产者：满则阻塞（或 offer(E, 100, MILLISECONDS) 超时退让）
queue.put(task);
// 消费者：空则阻塞（或 poll(100, MILLISECONDS)）
Task t = queue.take();
```

ConcurrentHashMap：JDK 8+ 放弃分段锁，改「空 bin 用 CAS、非空 bin 局部 synchronized 树头、size 用 CounterCell 分散计数」；`computeIfAbsent` 等复合操作是原子的，但「先 get 再 put」整体不是——复合业务判断仍要额外同步。CopyOnWriteArrayList：读完全无锁，写时复制整个数组——适合「配置列表、监听器列表」这种读千万次写几次的场景，写频繁或数组大时内存和延迟都不可接受。

**为什么这样设计**：这一族容器遵循同一取舍——「把锁的粒度从整表缩小到冲突点，或用写时复制把读路径成本降到零」，分别服务「读写都并发」和「读绝对主导」两个不同画像。选错容器的代价不是性能差一点，而是语义错误（CopyOnWrite 上的「读—改—写」仍是竞态）。

**面试官可能继续追问**：
- **LinkedBlockingQueue 为什么两把锁？** —— put 锁和 take 锁分离，生产和消费互不阻塞，只有双方同时操作同一节点时才间接相关；数组版 ArrayBlockingQueue 是一把锁，实现简单但并发度低。
- **ConcurrentHashMap 的 size() 为什么只是估计值？** —— 计数分散在 CounterCell，求和是快照式汇总，并发写入时返回近似值；要精确计数得自己维护或用 LongAdder。
- **ConcurrentHashMap 的 key/value 能为 null 吗？** —— 都不能。null 会与「不存在」混淆，并发场景下无法区分「没这个键」和「值是 null」，这是对 HashMap 单线程语义的有意取舍。
- **ConcurrentSkipListMap 和 ConcurrentHashMap 怎么选？** —— 需要有序遍历、范围查询用前者（跳表，O(log n) 操作）；纯 KV 随机访问用后者。排序需求是分水岭。

**实际工程场景**：WMS 波次下发用 ArrayBlockingQueue 做「下发任务缓冲」——有界 + 拒绝策略可控，队列水位直接就是积压指标；理货任务状态缓存用 ConcurrentHashMap（computeIfAbsent 初始化）；仓库-库位映射这类极少变更的配置读用 CopyOnWriteArrayList 或不可变 Map。注意别把 ConcurrentHashMap 当「万能并发安全」——它对复合操作（check-then-act）的保证止于单次调用。

**容易答错的地方**：说 LinkedBlockingQueue 无界所以线程池安全——恰恰相反（Q05 的陷阱）；说 CopyOnWriteArrayList 适合写多场景；以为 ConcurrentHashMap 的 get 后 put 是原子的；分不清 SynchronousQueue「不存元素」的语义。

> 权威内容：[ConcurrentHashMap 并发容器](../../03-并发编程/02-深度解析/ConcurrentHashMap并发容器.md) · [ThreadLocal 与并发容器速记](../../03-并发编程/01-面试速记/ThreadLocal与并发容器速记.md)

### Q17：Java 21 虚拟线程是什么，适合什么场景？ `P1` `中频`

**30 秒回答**：虚拟线程（JEP 444，JDK 21 正式，LTS）是 JDK 用户态调度的轻量线程：不绑定 OS 线程，阻塞时把载体线程让给别的虚拟线程，所以「一个请求一线程」的阻塞式写法能支撑十万级并发；适合 IO 密集型高并发，不适合 CPU 密集或临界区持锁阻塞的场景。

**深入理解**：核心机制是「载体线程（carrier）+ 挂载/卸载」：虚拟线程由 JDK 调度器挂到少量载体线程（默认就是 ForkJoinPool）上运行；遇到阻塞 IO（Socket、LockSupport.park）时，JVM 把虚拟线程从载体上卸载、栈帧保存到堆里，载体线程立刻去跑下一个虚拟线程——阻塞的成本从「占一个 OS 线程」变成「存一坨堆内存」。创建用 `Thread.ofVirtual().start(task)` 或 `ExecutorService auto = Executors.newVirtualThreadPerTaskExecutor()`；栈是堆上的可变结构，平台线程 1MB 起步的栈限制不复存在。

已知问题与边界：虚拟线程中执行 `synchronized` 阻塞时，虚拟线程会被「固定（pinning）」在载体线程上无法卸载——因为 JVM 记录的是载体线程持有 monitor，卸载会让互斥失效。JDK 21 的应对是「ReentrantLock 不 pinning、synchronized pinning」——高并发虚拟线程 + synchronized 长临界区 = 载体被占死，官方建议改用 ReentrantLock（JDK 24 的 JEP 491 已着手解决 synchronized 的 pinning，原生代码、类初始化等窄场景仍会 pin）。其他边界：CPU 密集任务没有收益（载体就那几个，等于没变）；`ThreadLocal` 模式要重审（线程数暴涨后每线程副本的累计内存可能反超）；依赖「线程数上限」的隐式限流失效，连接池、下游 QPS 变成新瓶颈。

**为什么这样设计**：同步阻塞 API（BIO、JDBC、传统 HTTP 客户端）写起来简单但每个阻塞占一个 OS 线程；异步非阻塞（NIO/回调/Reactor）不占线程但代码复杂。虚拟线程取两边：保留阻塞式写法，把「阻塞不占 OS 线程」做成运行时能力——JVM 负责在阻塞点保存/恢复栈。这是对「线程池缓解阻塞成本」这个旧问题的根治，不是改良。

**面试官可能继续追问**：
- **有了虚拟线程还要线程池吗？** —— 不再需要「池化复用」来省创建成本——虚拟线程便宜到可以每任务一个。但池还承担别的职责：限流（信号量控制并发数）、隔离（关键任务独立池）、监控（命名与指标）。`Executors.newVirtualThreadPerTaskExecutor()` 就是为这些场景保留的壳。
- **JDK 21 上 pinning 具体怎么解？** —— 把虚拟线程里的 synchronized 改成 ReentrantLock；用 JFR 的 jdk.VirtualThreadPinned 事件定位 pinning 点（JDK 21 也可用 -Djdk.tracePinnedThreads 排查）。答题时点明「JDK 21 LTS 上建议 Lock、JDK 24 起 JEP 491 改进」，说明你有版本概念。
- **虚拟线程怎么排查，jstack 还管用吗？** —— 传统 jstack 只看平台（载体）线程，虚拟线程的栈要用 `jcmd Thread.dump_to_file -format=json` 导出，且十万级线程的 dump 规模本身就需要工具链适配。
- **线程数从几千变成十万，哪些旧假设会崩？** —— 依赖线程数上限的隐式限流失效；ThreadLocal 累计内存；日志量；监控基数——迁移前先做容量评估，这就是「不是银弹」的实证。

**实际工程场景**：WMS 与 ERP/OMS 的网关型同步调用（一次请求串行调三四个下游）是最典型受益场景：JDK 21 上用虚拟线程 + 阻塞式客户端，代码回到直白写法，并发度从「线程池几百」到「数万」，但要用 Semaphore 给下游限流。升级路径要评估：先在不碰 synchronized 长临界区的新服务试点，压测对比「虚拟线程 + 阻塞 IO」与「原线程池 + 异步」的尾延迟和下游压力。还在 JDK 8/11 的系统，了解即可，别硬上。

**容易答错的地方**：说虚拟线程「更快」——它省的是线程资源与阻塞成本，单任务执行速度不变；说它适合 CPU 密集——那是负收益；不知道 pinning 直接答「synchronized 也能用」；把 JDK 21 和预览版混为一谈——19/20 是预览，21 才是正式 LTS；以为有了它异步代码全淘汰——存量异步栈和反应式仍有位置。

> 权威内容：[Java21 虚拟线程](../../03-并发编程/02-深度解析/Java21虚拟线程.md)

## 本专题常见追问链

面试官通常从一个很泛的问题起手，一层层往下逼。每一跳的意图不同，答不上来就停在那一层：

```text
线程安全问题
  「多个线程读写同一状态，怎么保证正确？」
  想验证：能不能说清竞态条件（check-then-act、读-改-写），
          而不是只会喊「加锁」。
     │
     ▼
JMM
  「为什么会有可见性问题？」
  想验证：主内存/工作内存抽象、happens-before 规则，
          是否理解「没有同步就没有可见性保证」。
     │
     ▼
volatile
  「它保证了什么、没保证什么？」
  想验证：可见性+有序性 yes、原子性 no；屏障与 happens-before
          的关系；为什么不能拿它做计数器。
     │
     ▼
CAS
  「不阻塞怎么保证原子？」
  想验证：比较并交换的语义、自旋代价、ABA 与版本号、
          原子类家族（AtomicInteger/LongAdder）。
     │
     ▼
AQS
  「阻塞等待怎么组织？」
  想验证：state 语义复用、CLH 等待队列、独占/共享模式、
          park/unpark、Condition 条件队列。
     │
     ▼
ReentrantLock
  「锁的具体形态与选型？」
  想验证：可中断/可超时/公平/多 Condition 四项独有能力，
          以及与 synchronized 的默认选型逻辑。
     │
     ▼
线程池
  「成千上万任务谁来执行？」
  想验证：七参数、提交三步决策、有界队列+拒绝策略、
          容量评估与水位监控（开始考工程实践）。
     │
     ▼
高并发工程实践
  「线上出了并发问题怎么收场？」
  想验证：jstack 死锁与锁竞争定位、ThreadLocal 数据串味、
          CompletableFuture 编排与超时、阻塞队列/虚拟线程等
          选项的适用边界——从「会写」走到「能扛」。
```

## 面试前速记

- volatile 保证可见性、有序性，不保证原子性；计数器用 AtomicInteger，高竞争纯统计用 LongAdder。
- JMM 的实操形态是 happens-before：程序顺序、监视器锁、volatile、线程 start/join、传递性；它保证可见性，不保证执行顺序。
- synchronized 锁升级「无锁→偏向→轻量→重量」不可逆；偏向锁 JDK 15 起废弃默认关（JEP 374）。
- AQS = volatile state（语义子类定）+ CLH 队列 + park/unpark；ReentrantLock 记重入次数、Semaphore 记许可、读写锁高低 16 位拆分。
- 线程池提交顺序：核心线程 → 队列 → 非核心线程 → 拒绝策略（Abort 默认、CallerRuns 背压、Discard 可丢、DiscardOldest 追新）；无界队列让 maximumPoolSize 失效，必须有界 + 水位监控。
- ThreadLocal：key 弱引用、value 强引用，必须 finally remove；比内存泄漏更危险的是线程池下的数据串味。
- CAS 只能比较一个机器字；ABA 用 AtomicStampedReference 加版本号，纯累加场景不用管。
- 默认 synchronized；只有可中断、可超时、公平锁、多 Condition 四项需求才上 ReentrantLock，且必须 finally unlock。
- 线程六状态：RUNNABLE 含就绪和运行；BLOCKED 等锁、WAITING 等通知；sleep 不释放锁，wait 释放锁且必须持有 monitor。
- 死锁四条件：互斥、持有并等待、不可剥夺、循环等待；jstack 看 "Found one Java-level deadlock"；预防靠统一加锁顺序 + tryLock 超时。
- CompletableFuture：thenApply 转换、thenCompose 展嵌套、allOf/anyOf 聚合；必须指定线程池别占 commonPool；orTimeout 要 JDK 9+。
- 虚拟线程 JDK 21 正式（LTS）：阻塞时卸载到堆栈，适合 IO 密集；JDK 21 上 synchronized 会 pinning 载体线程，建议用 ReentrantLock（JDK 24 JEP 491 改进）。
