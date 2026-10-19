# Q03. AOP 的实现原理，JDK 代理和 CGLIB 怎么选？

> Spring · P0 · 高频

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

返回 [Spring 专题](../Spring.md) · [专题题库首页](../README.md)
