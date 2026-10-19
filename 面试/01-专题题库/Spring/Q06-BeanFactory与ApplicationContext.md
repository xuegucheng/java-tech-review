# Q06. BeanFactory 和 ApplicationContext 的区别？

> Spring · P1 · 重点

**30 秒回答**：ApplicationContext 是 BeanFactory 的子接口，在「Bean 工厂」之上加了国际化、事件发布、资源加载、AOP 自动代理等一整套企业级能力；行为上最直观的差异是 BeanFactory 默认懒加载单例，ApplicationContext 启动时就把非懒加载的单例全部实例化。

**深入理解**：能力差异逐个说：事件体系（ApplicationEvent + ApplicationListener，@EventListener 也依赖它）；国际化（MessageSource，按 Locale 取消息）；资源访问（ResourceLoader，classpath:/file:/http: 统一抽象）；环境抽象（Environment + @Profile，按环境激活不同 Bean）；AOP 与事务的自动织入（AnnotationAwareAspectJAutoProxyCreator 这类 BeanPostProcessor 由 ApplicationContext 的 `invokeBeanFactoryPostProcessors` 阶段注册）。BeanFactory 能做的事 ApplicationContext 都能做（它继承了 BeanFactory），但 BeanFactory 通常只出现在「嵌入在框架内部按需取 Bean」的场景，比如某些 RPC 框架自己管生命周期时只需要最小容器。

**面试官可能继续追问**：
- **BeanFactoryPostProcessor 在生命周期哪个点执行？** —— 在所有 BeanDefinition 注册完、实例化任何 Bean 之前，它拿到的是 BeanDefinition 本身，可以改属性值、改作用域，甚至把某个 Bean 标记为懒加载。
- **refresh() 方法的主干流程？** —— 准备环境 → 刷新 BeanFactory 生成 BeanDefinition → 执行 BeanFactoryPostProcessor → 注册 BeanPostProcessor → 初始化消息源/事件多播器 → 实例化剩余单例 → 发布 ContextRefreshedEvent。能画出这条主干，容器就通了。

**实际工程场景**：启动慢的优化对象之一就是启动期实例化的单例——本地缓存预加载、健康检查客户端这些非必需 Bean 标 @Lazy 或用 `SmartInitializingSingleton` 延后；先看启动火焰图再决定懒加载谁，比一刀切更稳。

**容易答错的地方**：只答「功能多」。最关键的行为差异是初始化时机：ApplicationContext 预实例化单例，配置错误在启动期就爆炸；BeanFactory 懒加载，错误被推迟到第一次取 Bean 时。

> 权威内容：Spring 容器家族当前尚未在仓库建设，可参考 Spring 官方文档的 The IoC Container 与 ApplicationContext 章节。

---

返回 [Spring 专题](../Spring.md) · [专题题库首页](../README.md)
