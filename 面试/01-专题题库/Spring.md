# Spring

> 一句话定位：这一专题考「容器怎么管理 Bean、增强怎么织入」——从 IOC 的生命周期到 AOP 的代理机制，再到 Spring Boot 的自动配置，是一条完整的追问链。

返回 [专题题库首页](README.md) · [面试首页](../README.md)。

## 专题定位

- **为什么高频**：Spring 是 Java 后端的默认技术栈，面试官默认你会用，考点全在「为什么这样设计」——循环依赖、事务失效、自动配置，每题都能从使用层追到容器层。
- **面试官主要验证什么**：能否把「用了三级缓存」讲成「为什么不能是两级」，能否把「事务没生效」归因到代理机制而不是玄学；追到底都是代理与生命周期。
- **5 年以上工程师的深度**：能独立排查线上的事务失效、循环依赖启动失败、自动配置不生效，讲得清 Starter 封装与自定义，重要版本差异（Spring 5.x/6.0、Boot 2.x/3.x）不含糊。
- **本专题与 [MyBatis](MyBatis.md) 的分工**：这里讲容器、代理、事务框架层；MyBatis 篇讲 SQL 映射层与两者的事务整合。

## 高频题目导航

| 编号 | 面试问题 | 重要程度 | 追问深度 |
| --- | --- | --- | --- |
| Q01 | Spring 怎么解决循环依赖？为什么需要三级缓存？ | P0 | 源码 |
| Q02 | Spring 事务在什么情况下会失效？ | P0 | 工程实践 |
| Q03 | AOP 的实现原理，JDK 代理和 CGLIB 怎么选？ | P0 | 源码 |
| Q04 | Spring Bean 的生命周期是怎样的？ | P0 | 源码 |
| Q05 | IOC 和 DI 是什么？ | P0 | 原理 |
| Q06 | BeanFactory 和 ApplicationContext 的区别？ | P1 | 原理 |
| Q07 | Spring 事务的传播行为？ | P0 | 工程实践 |
| Q08 | @Transactional 的实现原理？ | P0 | 源码 |
| Q09 | Spring MVC 的请求流程？ | P0 | 原理 |
| Q10 | Spring Boot 自动配置原理？ | P0 | 源码 |
| Q11 | Spring Boot Starter 怎么自定义？ | P1 | 工程实践 |
| Q12 | Spring 里常见的设计模式？ | P1 | 设计 |
| Q13 | Spring 的线程安全问题？ | P1 | 工程实践 |
| Q14 | @Component 和 @Bean 的区别？ | P1 | 工程实践 |
| Q15 | Spring 的事件机制？ | P2 | 工程实践 |
| Q16 | AOP 是什么，解决了什么问题？ | P0 | 原理 |

## 题目正文

### Q01：Spring 怎么解决循环依赖？为什么需要三级缓存？ `P0` `高频`

**30 秒回答**：Spring 靠提前暴露「半成品」Bean 的引用，让 A 填充属性时能拿到尚未初始化完成的 B，从而打破 A→B→A 的相互等待；三级缓存的作用是把「是否要生成代理对象」推迟到真正被引用时才决定。

**深入理解**：三级缓存分别是 singletonObjects（成品）、earlySingletonObjects（早期引用）、singletonFactories（ObjectFactory）。创建 A 时先实例化，再把 A 的工厂放进三级缓存；填充属性发现依赖 B，转去创建 B；B 填充属性又发现依赖 A，此时从三级缓存取出工厂，调用 getEarlyBeanReference 得到 A 的早期引用（必要时代理）并放入二级缓存；B 完成后 A 拿到 B 继续初始化，最后进入一级缓存。边界很重要：它只解决**单例**且**非构造器注入**的字段/Setter 循环依赖，构造器注入的循环依赖会直接抛 BeanCurrentlyInCreationException，prototype 作用域也不支持（可用 @Lazy 打破）。

**为什么这样设计**：关键变量是 AOP。如果只有两级缓存，提前暴露的必然是「原始对象引用」，而 A 最终可能被包装成代理，于是 B 拿到原始对象、容器里却是代理对象，两者不一致。三级缓存的 ObjectFactory 把「要不要生成代理」延迟到被引用的那一刻，保证 B 拿到的是最终对象，同时在没有循环依赖时不会白白提前生成代理。

**面试官可能继续追问**：
- **构造器注入的循环依赖为什么救不了？** —— 构造器注入发生在实例化之前，此时对象还不存在，没有任何引用可以提前暴露；Spring 只能直接抛异常。这也是「能用 Setter/字段注入就别用构造器注入循环依赖」的原因，虽然更根本的解法是重新审视依赖方向。
- **二级缓存什么时候会被用到？** —— 多个 Bean 同时循环引用 A 的早期引用时，第一次从三级缓存生成后放进二级缓存，后续直接从二级取，避免同一个 ObjectFactory 被重复执行（重复执行可能生成多个代理对象）。

**实际工程场景**：微服务模块合包后启动报循环依赖，处理顺序是——先判断依赖方向是否合理（A 依赖 B 的同时 B 又依赖 A，通常说明职责划分有问题），能用 @Lazy 或事件机制解耦就不要硬吃三级缓存；把「启动能过」当唯一目标，会在运行期遇到代理对象不一致的诡异 bug。

**容易答错的地方**：只背「三级缓存」三个字，答不出「为什么不能是二级」；或者以为三级缓存解决了所有循环依赖——构造器注入和 prototype 都不在解决范围内。

> 权威内容：Spring 循环依赖与容器机制当前尚未在仓库建设，可参考 Spring 官方文档的 The IoC Container 章节。

---

### Q02：Spring 事务在什么情况下会失效？ `P0` `高频`

**30 秒回答**：事务失效基本可归为三类——代理没生效、异常没触发回滚、事务边界没覆盖到真正执行的代码。

**深入理解**：代理没生效最常见于自调用，同类内部用 this 调另一个 @Transactional 方法绕过了代理；final 方法无法被 CGLIB 覆写，同样失效（Spring 5.x 只支持代理类上的 public 方法，Spring Framework 6.0 起 protected 和包级可见方法也可生效，这是明确的版本差异）。异常没回滚，是因为默认只对 RuntimeException 和 Error 回滚，受检异常需要显式写 rollbackFor = Exception.class；异常被自己 try/catch 吞掉同样不会回滚。边界没覆盖，包括传播行为用错（嵌套事务里内层用默认 REQUIRED 并自行捕获异常、或外层用了 REQUIRES_NEW 提前提交）、数据表引擎不支持事务、多数据源没有纳入同一个事务管理器；异步方法或新起的线程也不在原事务里。

**面试官可能继续追问**：
- **自调用为什么一定失效？** —— 事务是代理对象在方法调用前后开启/提交/回滚的，同类内 this 调用走的是原始对象，根本没有经过代理那一层；解法是注入自身代理（`AopContext.currentProxy()`）或拆分到另一个 Bean。
- **怎么确认事务到底生没生效？** —— 看日志里连接的开市（`begin`）与提交（`commit`）时机是否包住了目标代码；或者临时打开事务管理器的 DEBUG 日志，比猜快得多。

**实际工程场景**：WMS 出库扣减 + 流水写入要求同事务，早期把两个操作写在同一个 public 方法里没问题；后来抽成私有方法被同类调用，事务静默失效，超卖风险就是这么来的。治理约定：事务注解只标在入口 public 方法上，私有方法靠调用链继承事务；受检异常统一配 `rollbackFor = Exception.class`。

**容易答错的地方**：列不全。及格线是五类：自调用、非 public/final 方法、异常类型不匹配、异常被吞、传播行为/引擎/多数据源问题——只答前两类会被认为没踩过坑。

> 权威内容：Spring 事务机制当前尚未在仓库建设，可参考 Spring 官方文档的 Transaction Management 章节。

---

### Q03：AOP 的实现原理，JDK 代理和 CGLIB 怎么选？ `P0` `高频`

**30 秒回答**：Spring AOP 在运行时为目标 Bean 生成代理对象，拦截方法调用后按通知链织入增强；目标类实现了接口时可以用 JDK 动态代理，否则用 CGLIB 生成子类代理。

**深入理解**：JDK 动态代理基于接口，用 Proxy.newProxyInstance 生成一个实现同一接口的代理类，方法调用转发给 InvocationHandler，所以目标类必须有接口。CGLIB 通过字节码生成目标类的子类并覆写方法，因此 final 类、final 方法和 private 方法都无法被增强（Spring 5.2 起 CGLIB 用的是 Spring 自行 repackage 的版本）。选择上，Spring Boot 2.x 起 spring.aop.proxy-target-class 默认为 true，即使有接口也优先用 CGLIB，Spring 5.x 之前则默认优先 JDK 代理。两者都只能拦截 Spring 容器里 Bean 的外部方法调用，同类自调用无效；要增强非容器对象、构造器或 final 方法，只能改用 AspectJ 的编译期或加载期织入。

**为什么这样设计**：选运行时代理而不是 AspectJ 编译期织入，是为了零侵入——业务代码不用改、不需要特殊编译器，代价就是拦不到自调用和私有方法，这也正是上一题「自调用导致事务失效」的根因。

**面试官可能继续追问**：
- **通知的执行顺序怎么定？** —— 同一个切面的多个增强按声明顺序（或 @Order）组织成责任链，环绕通知最外层；@Order 值小的优先级高，异常路径上顺序会反转，这题容易被现场追问「异常时还倒着走吗」。
- **为什么 CGLIB 代理要求类不能 final？** —— 它靠生成子类并覆写方法实现增强，final 类无法被继承、final 方法无法被覆写，增强点就漏掉了。

**实际工程场景**：统一日志、鉴权、耗时统计这类横切关注点用 AOP 收尾，比散落在业务代码里的 try-finally 更可控；但要注意别在环绕通知里做重逻辑——通知链上的每一毫秒都乘在调用量上，高接口会把「顺手打个日志」做成热点。

**容易答错的地方**：说「Spring Boot 一定用 CGLIB」。配置项 `spring.aop.proxy-target-class` 可以改回 JDK 代理；另外引入 AspectJ  Weaver（`spring-boot-starter-aop` 之外的 load-time/compile-time weaving）后，能力边界就和运行时代理不同了。

> 权威内容：Spring AOP 当前尚未在仓库建设，可参考 Spring 官方文档的 Aspect Oriented Programming with Spring 章节。

---

### Q04：Spring Bean 的生命周期是怎样的？ `P0` `高频`

**30 秒回答**：单例 Bean 的生命周期是「实例化 → 属性填充 → 初始化 → 使用 → 销毁」，Spring 在初始化前后留出了 BeanPostProcessor 这类扩展点，AOP 代理就是在其中生成的。

**深入理解**：顺序是：实例化（构造器或工厂方法）→ 属性填充 populateBean（注入依赖，循环依赖就发生在这里）→ Aware 回调（BeanNameAware、BeanClassLoaderAware、BeanFactoryAware，ApplicationContext 下还有 ApplicationContextAware）→ BeanPostProcessor 的 postProcessBeforeInitialization → 初始化（@PostConstruct、InitializingBean.afterPropertiesSet、@Bean(initMethod)，执行顺序即此顺序）→ BeanPostProcessor 的 postProcessAfterInitialization → 使用。容器关闭时按 @PreDestroy、DisposableBean.destroy、@Bean(destroyMethod) 的顺序回调。原型（prototype）作用域的 Bean 不经历销毁回调——容器不跟踪它们的生命周期，这是常被追问的边界。

**面试官可能继续追问**：
- **AOP 代理具体在哪一步生成？** —— 在 postProcessAfterInitialization 里，由 AbstractAutoProxyCreator 完成：初始化后的原始对象传进来，包上代理再返回，所以容器最终持有的是代理。这也是循环依赖里「要不要提前代理」问题的来处。
- **BeanPostProcessor 和 BeanFactoryPostProcessor 的区别？** —— 后者在 Bean 实例化之前工作，处理的是 BeanDefinition（比如占位符替换、修改属性值）；前者作用于 Bean 实例本身。两个接口名字像，工作时点完全不同。

**实际工程场景**：需要「依赖注入完成后执行一段初始化逻辑」时，用 @PostConstruct 而不是构造器——构造器执行时依赖还没注入，写初始化逻辑会拿到 null。销毁回调用于释放连接池、注销监听器，别依赖它做关键清理（容器异常关闭时回调不保证执行）。

**容易答错的地方**：把生命周期背成「实例化-初始化」两步。属性填充、Aware、两个后置处理器、三种初始化回调的先后顺序，才是面试官要的细节；另外原型 Bean 没有销毁阶段，答错说明只知道单例。

> 权威内容：Spring Bean 生命周期当前尚未在仓库建设，可参考 Spring 官方文档的 Lifecycle Callbacks 章节。

---

### Q05：IOC 和 DI 是什么？ `P0` `高频`

**30 秒回答**：IOC 是把创建和管理对象的控制权从代码里交到容器，DI 是它的具体形式——容器在初始化时把依赖注入到 Bean，而不是 Bean 自己去 new；两者是同一件事的思想与实现。

**深入理解**：没有容器时，A 要用 B 就自己 `new B()`，依赖关系硬编码在 A 内部；IOC 之后 A 只声明「我需要一个 B」，由容器在启动时装配。三种注入方式：构造器注入（依赖不可变、必需依赖显式、循环依赖在启动期就失败——Spring 4.3 起单构造器可以省略 @Autowired）；Setter 注入（适合可选依赖）；字段注入（写起来短，但隐藏依赖、不利于测试、无法用于 final 字段）。@Autowired 的解析流程是先按类型在 BeanFactory 里找候选，找到多个再按参数名/ @Qualifier 匹配，仍有多个则要求 @Primary 或报错。选型结论：框架类代码用构造器注入，业务代码跟随团队规范，但别用字段注入掩盖「这个类依赖了八个东西」的设计问题。

**面试官可能继续追问**：
- **IOC 容器启动时做了什么？** —— 读取配置（注解/XML）生成 BeanDefinition、执行 BeanFactoryPostProcessor、实例化并装配单例（默认）、发布刷新完成事件；非懒加载的单例都在启动期建好。
- **依赖注入的循环和启动失败是一回事吗？** —— 字段/Setter 循环依赖能被三级缓存接住（见 Q01），构造器循环依赖会直接在启动期抛异常——后者其实是好事，它逼你在编译期就发现依赖环。

**实际工程场景**：把「一个大 Service 注入了十几个 Repository」当信号——要么是职责过多该拆，要么是缺一个领域服务层。面试里讲这层判断，比背注入方式对比更能体现设计意识。

**容易答错的地方**：把 IOC 说成「 Spring 特有的新技术」。它是依赖倒置原则（DIP）的工程化实现，思想比框架老得多；另外「控制反转」反的是「获取依赖的方式」，不是「控制流」。

> 权威内容：Spring IOC 容器当前尚未在仓库建设，可参考 Spring 官方文档的 The IoC Container 章节。

---

### Q06：BeanFactory 和 ApplicationContext 的区别？ `P1` `重点`

**30 秒回答**：ApplicationContext 是 BeanFactory 的子接口，在「Bean 工厂」之上加了国际化、事件发布、资源加载、AOP 自动代理等一整套企业级能力；行为上最直观的差异是 BeanFactory 默认懒加载单例，ApplicationContext 启动时就把非懒加载的单例全部实例化。

**深入理解**：能力差异逐个说：事件体系（ApplicationEvent + ApplicationListener，@EventListener 也依赖它）；国际化（MessageSource，按 Locale 取消息）；资源访问（ResourceLoader，classpath:/file:/http: 统一抽象）；环境抽象（Environment + @Profile，按环境激活不同 Bean）；AOP 与事务的自动织入（AnnotationAwareAspectJAutoProxyCreator 这类 BeanPostProcessor 由 ApplicationContext 的 `invokeBeanFactoryPostProcessors` 阶段注册）。BeanFactory 能做的事 ApplicationContext 都能做（它继承了 BeanFactory），但 BeanFactory 通常只出现在「嵌入在框架内部按需取 Bean」的场景，比如某些 RPC 框架自己管生命周期时只需要最小容器。

**面试官可能继续追问**：
- **BeanFactoryPostProcessor 在生命周期哪个点执行？** —— 在所有 BeanDefinition 注册完、实例化任何 Bean 之前，它拿到的是 BeanDefinition 本身，可以改属性值、改作用域，甚至把某个 Bean 标记为懒加载。
- **refresh() 方法的主干流程？** —— 准备环境 → 刷新 BeanFactory 生成 BeanDefinition → 执行 BeanFactoryPostProcessor → 注册 BeanPostProcessor → 初始化消息源/事件多播器 → 实例化剩余单例 → 发布 ContextRefreshedEvent。能画出这条主干，容器就通了。

**实际工程场景**：启动慢的优化对象之一就是启动期实例化的单例——本地缓存预加载、健康检查客户端这些非必需 Bean 标 @Lazy 或用 `SmartInitializingSingleton` 延后；先看启动火焰图再决定懒加载谁，比一刀切更稳。

**容易答错的地方**：只答「功能多」。最关键的行为差异是初始化时机：ApplicationContext 预实例化单例，配置错误在启动期就爆炸；BeanFactory 懒加载，错误被推迟到第一次取 Bean 时。

> 权威内容：Spring 容器家族当前尚未在仓库建设，可参考 Spring 官方文档的 The IoC Container 与 ApplicationContext 章节。

---

### Q07：Spring 事务的传播行为？ `P0` `高频`

**30 秒回答**：传播行为回答「方法被调用时，周围已经有一个事务该怎么办」——默认 REQUIRED 是「有就加入、没有就新建」；REQUIRES_NEW 是「挂起当前的，自己开一个」；NESTED 是「加入但设保存点，内层回滚不拖垮外层」。

**深入理解**：七种逐个过：REQUIRED（默认，加入或新建）；REQUIRES_NEW（总是新建，外层挂起，内外互不影响——外层回滚不影响内层已提交的）；NESTED（加入当前事务，用 savepoint 标记回滚点，内层回滚到保存点，外层可继续）；MANDATORY（必须有，否则抛异常）；SUPPORTS（有就加入，没有就非事务运行）；NOT_SUPPORTED（挂起当前事务，非事务运行）；NEVER（有事务就抛异常）。最常用的坑是 REQUIRES_NEW：内层提交后外层才回滚，内层数据已经落库——对账类、审计类操作常故意用它，但误用会把「本该一起回滚」的操作拆开。NESTED 和 REQUIRES_NEW 的区别要能一句话说清：前者共享连接设保存点，后者拿新连接独立提交。

**面试官可能继续追问**：
- **REQUIRES_NEW 内层提交、外层回滚，数据会怎样？** —— 内层提交的数据保留，外层回滚不了它。所以「发通知 + 记账」这类操作故意用 REQUIRES_NEW 保证通知不随业务回滚丢失；但误用在扣库存场景就是资损。
- **NESTED 依赖什么实现？** —— 依赖 JDBC 的 Savepoint，所以要求同一个连接；配合 REQUIRED 使用，回滚时只撤销内层操作，外层继续。

**实际工程场景**：WMS 出库主流程里「扣库存 + 写流水」必须同生共死用默认 REQUIRED；而「记录操作日志」反而适合 REQUIRES_NEW——主流程失败回滚了，日志也应该留着供排查。区分标准是「这个操作该不该跟着主业务一起回滚」，先回答这个再选传播行为。

**容易答错的地方**：把 NESTED 和 REQUIRES_NEW 混为一谈（一个共享连接设保存点、一个独立事务）；以及以为传播行为能救自调用——代理都没走，传播行为根本不在场。

> 权威内容：Spring 事务传播行为当前尚未在仓库建设，可参考 Spring 官方文档的 Transaction Management 章节。

---

### Q08：@Transactional 的实现原理？ `P0` `源码`

**30 秒回答**：@Transactional 是声明式事务，本质是 AOP：Spring 为标了注解的 Bean 生成代理，方法调用进来时 TransactionInterceptor 先开启事务，方法正常返回则提交、抛运行时异常则回滚；事务资源（连接）通过 TransactionSynchronizationManager 以 ThreadLocal 绑定在当前线程上。

**深入理解**：链路是：容器启动时 AnnotationAwareAspectJAutoProxyCreator 这类后置处理器识别 @Transactional，为 Bean 生成代理；调用时代理链上的 TransactionInterceptor 执行——先拿事务属性（传播行为、隔离级别、超时、只读），从 TransactionSynchronizationManager 拿当前线程绑定的 ConnectionHolder，没有就开新事务并从数据源取连接绑上去；目标方法执行；正常走完提交（清理同步状态、释放连接），抛异常按 rollbackFor 规则决定回滚还是提交。MyBatis 侧每次 SqlSession 会从 TransactionSynchronizationManager 取同一个连接，所以 DAO 操作和事务用的是同一条连接——这就是「Spring 事务内多次查询为什么在同一连接上」的答案（详见 [MyBatis](MyBatis.md) Q11）。自调用失效的原因也在这条链上：this 调用不过代理，拦截器没执行，事务根本没开。

**面试官可能继续追问**：
- **事务绑定为什么用 ThreadLocal？** —— 事务必须跟随调用线程：同一线程内的多次 DAO 调用要共享连接与事务状态，ThreadLocal 是最直接的线程封闭手段；代价是线程池场景必须确保清理（ThreadLocal 泄漏在事务里同样存在）。
- **事务方法里开异步线程，新线程有事务吗？** —— 没有。ThreadLocal 不跨线程传递，新线程从数据源取的是新连接；要传递事务得用 TransactionAwareThreadPoolTaskExecutor 之类的支持，或者干脆拆成消息驱动。

**实际工程场景**：排查「事务内查不到刚插入的数据」这类问题，先确认是不是异步/新线程、是不是自调用、是不是 REQUIRES_NEW 换了连接——三个怀疑点按概率排完再翻源码。理解 ThreadLocal 绑定后，这类问题不用猜。

**容易答错的地方**：把 @Transactional 说成「 Spring 帮你在数据库开了事务」。准确说法是「Spring 管理事务边界与连接」，真正开事务的是底层 JDBC/连接池；隔离级别、传播行为这些属性最终都要翻译成对连接的操作。

> 权威内容：Spring 声明式事务当前尚未在仓库建设，可参考 Spring 官方文档的 Transaction Management 章节。

---

### Q09：Spring MVC 的请求流程？ `P0` `高频`

**30 秒回答**：请求先到 DispatcherServlet，它按 HandlerMapping 找到能处理这个 URL 的处理器（控制器方法），再用 HandlerAdapter 适配执行，拿到 ModelAndView 或响应体，最后由 ViewResolver 渲染视图返回——整个过程 DispatcherServlet 是唯一入口。

**深入理解**：主干七步：请求到达 DispatcherServlet（前端控制器）→ HandlerMapping 查询映射（@RequestMapping 注册成的 MappingRegistry）→ 选定 HandlerAdapter（支持 @RequestMapping 的 RequestMappingHandlerAdapter、支持函数式的 RouterFunctionMapping 等）→ 执行处理器：参数解析（@RequestBody/@RequestParam 由 HandlerMethodArgumentResolver 完成）、调用目标方法、返回值处理（@ResponseBody 时 HttpMessageConverter 把对象序列化成 JSON）→ 有视图名则 ViewResolver 解析 → 渲染视图 → 返回响应。拦截器链（HandlerInterceptor）在处理器执行前后介入，preHandle 返回 false 直接短路；过滤器（Filter）在 Servlet 容器层更靠外，见的是原始请求，两者层次不同。异常处理由 @ExceptionHandler + @ControllerAdvice 收敛，DispatcherServlet 解析不到处理器时按 404 走。

**面试官可能继续追问**：
- **拦截器和过滤器的区别？** —— 过滤器属于 Servlet 规范，在 DispatcherServlet 之前，拿不到 Spring 上下文与处理器信息；拦截器属于 Spring MVC，能拿到 HandlerMethod，可以做方法级鉴权；AOP 则更靠内，拦的是 Bean 方法调用。
- **一个 REST 接口的返回值怎么变成 JSON 的？** —— @ResponseBody 让返回值交给 RequestResponseBodyMethodProcessor，它按请求头 Accept 与类路径上的 Jackson 等转换器协商，用 HttpMessageConverter 序列化。

**实际工程场景**：统一响应体、鉴权、链路 traceId 这类横切逻辑放过滤器（最早拿请求）；需要「按接口维度限流/鉴权」的放拦截器（拿得到 HandlerMethod 上的注解）；两者别互相抄作业。WMS 的接口耗时统计放在拦截器，正好能拿到控制器与方法的元信息。

**容易答错的地方**：把流程背串（HandlerMapping 和 HandlerAdapter 的顺序颠倒），或者分不清 Filter、Interceptor、AOP 三者的层次——这题一半考流程，一半考对 Web 容器与 Spring 分层是否清楚。

> 权威内容：Spring MVC 请求处理当前尚未在仓库建设，可参考 Spring 官方文档的 Web Servlet 章节。

---

### Q10：Spring Boot 自动配置原理？ `P0` `源码`

**30 秒回答**：@SpringBootApplication 组合了 @ComponentScan 和 @EnableAutoConfiguration，后者读取所有 starter 里注册的自动配置类，按 @Conditional 条件判断——类路径有相关依赖、容器里没有同名 Bean、属性配了值——满足就生效；条件不满足整条配置静默跳过，这就是「引入依赖即拥有默认实现」的来源。

**深入理解**：拆开 @SpringBootApplication：@Configuration（标记配置类）、@EnableAutoConfiguration（关键）、@ComponentScan（扫描主类所在包）。@EnableAutoConfiguration 内部通过 AutoConfigurationImportSelector 读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件里列出的全限定名——这个文件是 Spring Boot 2.7 引入的新格式，2.7 之前是写在各 jar 的 `META-INF/spring.factories` 里，Spring Boot 3.x 已全面切换到 imports 文件（面试答「spring.factories」会被追问版本）。每个自动配置类上挂一串 @Conditional：@ConditionalOnClass（类路径有才生效，如 classpath 有 DataSource 才配数据源）、@ConditionalOnMissingBean（用户自己定义了就不覆盖）、@ConditionalOnProperty（开关或前缀匹配）。配置绑定靠 @ConfigurationProperties + 松弛绑定（ relaxed binding，`server.port`、`SERVER_PORT`、`server.port` 都能认）。@EnableConfigurationProperties 或自动配置类上的 @EnableConfigurationProperties 把属性类注册进容器。

**面试官可能继续追问**：
- **自动配置类什么时候被加载，顺序怎么控？** —— 在 refresh() 的 `invokeBeanFactoryPostProcessors` 阶段由 ConfigurationClassPostProcessor 解析；顺序靠 @AutoConfigureOrder、@Order 以及 `AutoConfiguration.imports` 中 before/after 声明，典型如数据源自动配置必须在 JdbcTemplate 之前。
- **我想改 starter 的默认行为怎么办？** —— 优先级从低到高：改 application.yml 覆盖属性 → 定义同名 Bean 覆盖（@ConditionalOnMissingBean 会让位）→ 排除该自动配置类（`spring.autoconfigure.exclude`）→ 干脆不引那个 starter。多数场景前两级就够。

**实际工程场景**：自研中间件接入公司环境时，把「默认配置 + 条件装配 + 属性前缀」三件套做进 starter，业务方引入依赖、在 yml 里改个地址就能用——这比让每个业务方复制十行 @Bean 配置可靠得多。踩过的经典坑是自定义 Bean 名字和自动配置里的不一致，结果两个实例并存，排查时看 Bean 定义一眼能识破。

**容易答错的地方**：把自动配置文件答成 `spring.factories`（2.7+ 已是 `AutoConfiguration.imports`，3.x 全面切换）；以及说不出 @Conditional 的具体条件类型，只背「按条件加载」。

> 权威内容：Spring Boot 自动配置当前尚未在仓库建设，可参考 Spring Boot 官方文档的 Auto-configuration 与 Customizing Spring Boot Starters 章节。

---

### Q11：Spring Boot Starter 怎么自定义？ `P1` `工程实践`

**30 秒回答**：一个自定义 starter = 自动配置类（@Configuration + @ConditionalOnClass）+ 属性绑定类（@ConfigurationProperties）+ 资源文件注册（`META-INF/spring/...AutoConfiguration.imports`）三件套，再按命名规范打成独立 jar，业务方引入依赖、改 yml 即可用。

**深入理解**：命名上官方 starter 是 `spring-boot-starter-*`，第三方 starter 推荐 `*-spring-boot-starter`（Spring Boot 2.x 官方建议，避免与官方命名空间冲突）。自动配置类上要标 @ConditionalOnClass——依赖不存在时整个配置类跳过，这是 starter 不会因为「缺依赖启动报错」的关键；默认 Bean 的方法上加 @ConditionalOnMissingBean，用户自定义优先。属性类用 @ConfigurationProperties(prefix = "xxx")，配合 `spring-configuration-metadata.json`（由注解处理器生成）让 IDE 有提示。别忘了 starter 的传递依赖要克制——把可选的依赖放 optional，否则业务方类路径被悄悄塞进一堆东西。

**面试官可能继续追问**：
- **starter 之间怎么保证加载顺序？** —— 在 imports 文件里用 `com.x.A,com.x.B` 声明基本顺序，或用 @AutoConfigureBefore/@AutoConfigureAfter 精确控制，比如你的配置要在 DataSourceAutoConfiguration 之后。
- **用户怎么关掉你的自动配置？** —— `spring.autoconfigure.exclude=com.x.YourAutoConfiguration`，这也是所有 starter 必须留的逃生门；更友好的做法是加一个 `xxx.enabled=false` 的 @ConditionalOnProperty。

**实际工程场景**：把公司统一的「链路追踪 + 配置中心客户端 + 监控上报」打成一个基础 starter，所有服务零代码接入；每个能力都带开关，出问题能单独关。面试讲这个案例时强调「条件装配让默认行为不打扰业务」，比罗列 starter 结构更打动人。

**容易答错的地方**：把 starter 说成「特殊依赖包」。它没有任何魔法格式，本质是「约定位置的配置类 + 属性元数据」；少了 imports 文件注册，@Configuration 根本不会被扫描到。

> 权威内容：自定义 Starter 当前尚未在仓库建设，可参考 Spring Boot 官方文档的 Customizing Spring Boot Starters 章节。

---

### Q12：Spring 里常见的设计模式？ `P1` `设计`

**30 秒回答**：至少能说六个：工厂（BeanFactory/FactoryBean）、单例（默认作用域）、代理（AOP）、模板方法（JdbcTemplate/RestTemplate）、策略（Resource/PlatformTransactionManager）、观察者（事件机制）；再往细了还有适配器（HandlerAdapter）、建造者、装饰器。

**深入理解**：逐个说「解决什么问题」比报名字重要。工厂：把「用什么实现」的决策推迟到容器，调用方只依赖接口；FactoryBean 则用于「创建过程本身很复杂」的 Bean（如 MyBatis 的 SqlSessionFactory）。单例：容器默认作用域，一个 BeanDefinition 一个实例，由容器保证而不是 double-check 单例模式。代理：AOP 的落地手段，JDK/CGLIB 二选一（见 Q03）。模板方法：JdbcTemplate 把「获取连接-执行-处理结果-释放连接」的骨架定死，把变化的部分（SQL、行映射）留给回调——JdbcTemplate 是理解 Spring 数据访问的钥匙。策略：同一接口多个实现按条件切换，如 Resource 对 classpath/file/url 的统一抽象背后是不同策略。观察者：ApplicationEvent 体系，发布订阅解耦。适配器：Spring MVC 用 HandlerAdapter 把「不同的处理器类型」适配成统一调用方式，是它支持多种控制器形态的原因。

**面试官可能继续追问**：
- **FactoryBean 和 BeanFactory 的区别？** —— 名字像、层次完全不同：BeanFactory 是容器本身，FactoryBean 是一个「生产 Bean 的 Bean」，容器遇到 FactoryBean 会调它的 getObject() 拿实例，getObject 返回的对象才进容器。
- **Spring 的单例和 GoF 单例模式一样吗？** —— 不一样。GoF 单例保证「一个 JVM 一个实例」，靠私有构造器；Spring 单例是「一个容器一个实例」，多个容器（多个 ApplicationContext）各有一份，也不阻止你手动 new。

**实际工程场景**：设计内部框架时，模板方法 + 策略是最常用的组合：把固定流程（参数校验→调用→重试→埋点）放抽象类，把变化点（序列化协议、目标地址）做成策略注入。面试里能把「报模式名字」升级成「我在哪用过、换掉了什么」，这题就从背诵题变成了设计题。

**容易答错的地方**：只报名字不说场景。面试官问设计模式通常是想听「你理解它解决什么问题」，答「Spring 用了单例、代理、工厂」三个词就停，等于把送分题答成扣分题。

> 权威内容：Spring 设计模式当前尚未在仓库建设，可参考《Spring 揭秘》与 Spring 官方文档相关章节。

---

### Q13：Spring 的线程安全问题？ `P1` `工程实践`

**30 秒回答**：Spring 的 Bean 默认单例且被所有请求共享，所以「Bean 有状态」就等于「状态被并发共享」——单例 Bean 里放可变成员变量是线程不安全的；正确做法是保持 Bean 无状态，请求级状态放方法局部变量或 ThreadLocal。

**深入理解**：Controller、Service、Repository 默认全是单例。判断标准很简单：这个类有没有「随请求变化的实例字段」——有就危险，没有（只依赖注入的其他无状态 Bean）就安全。常见的三个反模式：单例 Service 里用成员变量做累加/缓存（如把上一次查询结果存到字段供下次用）；单例里持有 SimpleDateFormat 这类非线程安全对象；把用户上下文存进单例 Bean 的字段。正确姿势分别是：计数器用原子类/Redis、日期格式化用 ThreadLocal 或 DateTimeFormatter（Java 8+ 的 DateTimeFormatter 本身线程安全）、用户上下文放 RequestContextHolder（它底层就是 ThreadLocal）。原型作用域（@Scope("prototype"）能每次取新实例，但用原型 Bean 存请求状态是「用作用域修设计问题」，真正该修的是「为什么需要按请求存状态」。线程池场景还要注意：ThreadLocal 里的用户上下文跨任务残留（见 [Java多线程](Java多线程.md) Q06 的内存泄漏与数据串味问题）。

**面试官可能继续追问**：
- **Spring 的 Bean 默认真的是单例吗？** —— 是，singleton 是默认作用域；但「单例」的边界是容器——一个 JVM 里跑两个 ApplicationContext（如 Spring Boot 多上下文场景），各容器有各自的单例。
- **Controller 里定义成员变量会怎样？** —— 所有并发请求共享这一个实例，成员变量就是共享可变状态：轻则读到别人的数据，重则状态机错乱。面试答「没有 @Scope 注解所以都是单例」时要能立刻接上这条。

**实际工程场景**：老代码里见过单例 Service 用成员变量做「上次查询结果缓存」，压测时串数据串得莫名其妙——这就是典型的有状态单例。改造方式是把状态挪进请求链路（方法参数）或显式缓存（Caffeine/Redis），Bean 回归无状态。这条经验面试讲出来，比背「单例线程不安全」有说服力。

**容易答错的地方**：把「Bean 是单例」等同于「一定有线程安全问题」。无状态单例完全安全；问题出在可变状态，不在单例本身。反过来说「加了 synchronized 就行」也不对——那是在用锁掩盖设计问题。

> 权威内容：Spring 作用域与并发当前尚未在仓库建设，可参考 Spring 官方文档的 Bean Scopes 章节。

---

### Q14：@Component 和 @Bean 的区别？ `P1` `工程实践`

**30 秒回答**：@Component 是类级别的「我是 Bean」声明，靠 @ComponentScan 扫描生效，用于自己写的类；@Bean 是方法级别的「这个方法的返回值是 Bean」，用在 @Configuration 类里，用于把第三方库的对象交给容器管理。

**深入理解**：目的差异是本质：@Component 只能标在你自己有源码的类上；@Bean 解决的是「这个类不是我的，但我要把它注册进容器」——第三方库的 Client、需要按条件装配的基础设施对象。粒度上 @Bean 更灵活：一个 @Configuration 类里可以按 profile、按属性决定 new 哪个实现。@Bean 的方法名默认就是 Bean 名，可以用 name 属性指定多个。还有一个易被追问的点：@Configuration 类本身会被 CGLIB 增强，保证同一个配置类里 @Bean 方法互调拿到的是同一个单例（容器里的那个）；如果把 @Configuration 换成 @Component，这个保证就没了（lite 模式），互调会生成新实例——这是「配置类为什么必须是 @Configuration」的标准答案。

**面试官可能继续追问**：
- **@Bean 方法可以私有/最终化吗？** —— 不能私有，CGLIB 代理要覆写它；final 同理不行（这也是为什么配置类不能是 final 类）。
- **@ComponentScan 的扫描范围怎么定？** —— 默认扫 @ComponentScan 标注类所在包及子包；Spring Boot 主类通常在根包，天然覆盖全部；多模块项目要小心扫描范围把别处的同名 Bean 扫进来。

**实际工程场景**：WMS 里封装 WMS 客户端 SDK 的 Bean：SDK 类不可改，用 @Configuration + @Bean 按环境（测试/生产）new 不同实现；业务 Service 全部 @Component/@Service。面试讲清这个分工，说明你真在搭过工程结构。

**容易答错的地方**：只答「一个标类一个标方法」。拉开差距的是两个点：@Bean 用于第三方类、@Configuration 的 CGLIB 增强保证 @Bean 互调单例。

> 权威内容：Spring Bean 定义当前尚未在仓库建设，可参考 Spring 官方文档的 Annotation-based Container Configuration 章节。

---

### Q15：Spring 的事件机制？ `P2` `工程实践`

**30 秒回答**：Spring 的事件是容器内的观察者模式：ApplicationEventPublisher 发布事件，ApplicationListener 或 @EventListener 标注的方法消费；@TransactionalEventListener 更进一步，只在事务提交成功后才触发。

**深入理解**：默认同步执行——publishEvent 返回时所有监听器都跑完了，监听器抛异常会影响发布方（可以用 ErrorHandler 隔离）。@Async 可以让监听异步，但要注意异步就丢了「事务后执行」的语义。@TransactionalEventListener 的 phase 有 BEFORE_COMMIT/AFTER_COMMIT/AFTER_ROLLBACK/AFTER_COMPLETTHREAD，常用 AFTER_COMMIT——「下单成功后发短信/推消息」这类「别在事务里做外部调用」的标准解法：事务提交前外部调用会把下游延迟算进事务时长，提交后做又可能事务回滚了通知已发。监听顺序用 @Order。注意事件只在同一个 ApplicationContext 内传播，多上下文场景拿不到。

**面试官可能继续追问**：
- **为什么不在事务里直接调外部接口？** —— 事务时长被外部接口的耗时拉长，连接与锁持有时间跟着涨；更糟的是事务最终回滚、通知已发出。AFTER_COMMIT 把这两件事解耦。
- **事件同步执行会有什么问题？** —— 一个慢监听器拖慢发布方，甚至发布方的异常被监听器异常污染；跨很长的链路时宁可上消息队列，事件机制只适合进程内轻量解耦。

**实际工程场景**：WMS 出库单审核通过后要「发短信 + 记操作日志 + 刷新缓存」，全部收进 AFTER_COMMIT 事件监听——主事务只干核心的事，外围动作失败也不回滚主流程（各自兜底）。这个模式比在主流程里顺序调三个服务干净得多。

**容易答错的地方**：把 Spring 事件当消息队列用。它是进程内同步（默认）机制，没有持久化、没有重试、跨上下文不传播——需要可靠投递时必须回到 MQ（见 [消息队列](消息队列.md)）。

> 权威内容：Spring 事件机制当前尚未在仓库建设，可参考 Spring 官方文档的 Application Events 章节。

---

### Q16：AOP 是什么，解决了什么问题？ `P0` `原理`

**30 秒回答**：AOP 是把散落在各处的**横切逻辑**——日志、事务、鉴权、耗时统计——从业务方法里抽出来，集中定义成切面，再在运行期织入到目标方法的调用前后。它解决两类耦合：业务与通用逻辑混在一个方法里，以及同一段通用逻辑在几十个方法里重复。Spring AOP 的实现方式就是运行时代理，代理机制本身见 [Q03](#q03aop-的实现原理jdk-代理和-cglib-怎么选-p0-高频)。

**深入理解**：日志、事务、安全、监控的共同特征是「每个业务方法都要有，但都不属于业务语义」。不抽出来的后果是可枚举的：代码侵入、重复、口径不一致、无法统一开关、单测里全是无关的 mock。

术语这一层要能对上号（面试官常用英文问）：

| 术语 | 含义 | Spring 里的落地 |
| --- | --- | --- |
| JoinPoint（连接点） | 程序执行中可以插入增强的点 | Spring AOP 只支持**方法执行**这一个维度 |
| Pointcut（切点） | 匹配连接点的表达式 | `@Pointcut("execution(* com..service.*.*(..))")` |
| Advice（通知） | 织入的动作与时机 | `@Before` / `@After` / `@AfterReturning` / `@AfterThrowing` / `@Around` |
| Aspect（切面） | 切点 + 通知的模块化单元 | `@Aspect` 标注的类 |
| Weaving（织入） | 把切面接进目标对象的过程 | Spring AOP 在**运行期**通过代理完成 |

织入时机决定了能力上限：

| 时机 | 代表 | 能拦什么 | 代价 |
| --- | --- | --- | --- |
| 编译期 | AspectJ `ajc` | 方法、字段、构造器、静态块，甚至 `final` 方法 | 换编译器，构建链要改 |
| 类加载期 | AspectJ LTW | 同上，不改编译器但要让 `javaagent` 介入 | 部署复杂度上升 |
| 运行期代理 | Spring AOP（默认） | **只能拦 Bean 的方法调用**，且必须是外部调用 | 零侵入，但自调用、private、final 全部失效 |

代理在 `BeanPostProcessor.postProcessAfterInitialization` 阶段生成（见 [Q04](#q04spring-bean-的生命周期是怎样的-p0-高频)）：容器最终持有的是代理对象而不是原始对象。

**为什么这样设计**：Spring 选运行期代理，本质是用「能力边界」换「零侵入」——业务代码不用改一行、不需要特殊编译器，代价就是上面那三条拦不到。需要更强能力时才叠加 AspectJ。

**面试官可能继续追问**：
- **为什么同类自调用会失效？** —— `this.b()` 用的是原始对象引用，不经过代理，通知链根本没进入。解法是注入自身代理、`AopContext.currentProxy()`（需 `exposeProxy`），或把方法拆到另一个 Bean。完整清单见 [Q02](#q02spring-事务在什么情况下会失效-p0-高频)。
- **多个切面的顺序怎么定？** —— `@Order` 或实现 `Ordered`，值小的优先级高；进入按序、退出逆序，形成栈式结构；异常路径上 `AfterThrowing` 与 `After` 的相对顺序会反转。
- **环绕通知里把异常吞掉会怎样？** —— 容器感知不到异常，事务不会回滚，调用方还会拿到「成功」的返回值。这不是 AOP 的问题，是通知里写了不该写的 `catch`。

**实际工程场景**：事务（`@Transactional` 本身就是基于 AOP 的增强，所以自调用失效是代理模式的必然结果）；统一日志与链路追踪（入口切面生成 traceId、出口清理）；鉴权与限流（切点落在 Controller 层，把校验前置到业务之外）。注意环绕通知里别做重逻辑——通知链上的每一毫秒都乘在调用量上。

**容易答错的地方**：把 Spring AOP 等同于 AspectJ（`spring-boot-starter-aop` 里带着 AspectJ 的注解和切点解析器，但**织入方式仍是代理**）；只说「AOP 用来做日志和事务」（这是用法不是理解）；忘了「只对 Bean 的外部方法调用生效」，于是把自调用失效解释成 Spring 的 bug；认为 JDK 代理一定比 CGLIB 慢（现代 JDK 上单次调用差距很小，主要成本在代理类生成）。

> 权威内容：Spring AOP 当前尚未在仓库建设，可参考 Spring 官方文档 *Aspect Oriented Programming with Spring* 章节；代理机制与选型见 [Q03](#q03aop-的实现原理jdk-代理和-cglib-怎么选-p0-高频)。

---

## 本专题常见追问链

```text
IOC 容器（Bean 从哪来、怎么装配）
  → Bean 生命周期（实例化 → 填充 → 初始化 → 销毁）
  → AOP 代理（增强在哪一步织入、JDK vs CGLIB）
  → 循环依赖（为什么要三级缓存）
  → 声明式事务（代理 + ThreadLocal 绑定 + 传播行为）
  → 事务失效（自调用、异常类型、边界没覆盖）
Spring Boot 自动配置（条件装配从哪来）
  → Starter 封装（自动配置 + 属性绑定 + 条件）
```

这条链的每一跳都在往「运行时」走：前四跳是启动期发生了什么，后两跳是调用期发生了什么。面试官从任意一点切入，最后都会落到「你在项目里见过什么异常、怎么定位的」。

## 面试前速记

- 循环依赖：三级缓存 = 成品/早期引用/ObjectFactory；只救单例非构造器注入；第三级的存在意义是把「是否代理」推迟到被引用时。
- 事务失效五类：自调用不过代理、非 public/final 方法、异常类型不匹配（默认只回滚 RuntimeException/Error）、异常被吞、传播行为/引擎/多数据源。
- AOP：JDK 代理要接口，CGLIB 生成子类不能碰 final；Boot 2.x 起默认 CGLIB；都拦不住自调用，要织入 final 方法得上 AspectJ。
- 生命周期：实例化 → populateBean → Aware → postProcessBeforeInitialization → @PostConstruct/afterPropertiesSet → postProcessAfterInitialization；AOP 代理在最后一个后置处理器里生成。
- 传播行为：REQUIRED 加入或新建、REQUIRES_NEW 挂起外层独立提交、NESTED savepoint 内层回滚不拖垮外层；「该不该跟着主业务回滚」是选型标准。
- @Transactional 本质：AOP 拦截 + TransactionSynchronizationManager 的 ThreadLocal 绑定连接；异步线程拿不到事务。
- 自动配置：@EnableAutoConfiguration → `META-INF/spring/...AutoConfiguration.imports`（2.7 前是 spring.factories，3.x 全面切换）+ @ConditionalOnClass/OnMissingBean/OnProperty。
- 自定义 starter 三件套：自动配置类、@ConfigurationProperties 属性类、imports 注册；别忘了 @ConditionalOnClass 与逃生开关。
- BeanFactory 是懒加载工厂，ApplicationContext 预实例化单例 + 事件/国际化/环境/AOP 自动代理。
- 单例 Bean 无状态就安全，有状态就是共享可变状态；用户上下文放 RequestContextHolder（ThreadLocal），注意线程池残留。
- @Component 标自己的类（靠扫描），@Bean 标方法（管第三方对象）；@Configuration 的 CGLIB 增强保证 @Bean 互调单例。
- @TransactionalEventListener 的 AFTER_COMMIT 是「事务里别调外部接口」的标准解法。
