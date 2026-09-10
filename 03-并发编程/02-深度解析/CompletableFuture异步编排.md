# CompletableFuture 异步编排

> 面试定位：异步依赖、并行聚合、异常和执行器隔离
> Java 版本：API 语义按 Java 8+，超时和实现观察按 Java 21
> P0/P1：P1
> 前置知识：线程池、Future、异常传播和取消
> 本文不负责：反应式框架和完整异步运行时设计

## 先说结论

CompletableFuture 的价值不是把同步代码改成 then 链，而是明确任务之间的依赖图：

- thenApply：同一个结果的同步转换；
- thenCompose：串行依赖并展开嵌套 Future；
- thenCombine：两个独立 Future 完成后合并；
- allOf：等待一组 Future 全部完成，但不直接返回结果数组；
- exceptionally / handle / whenComplete：分别表达恢复、转换和观察；
- orTimeout / completeOnTimeout：把超时纳入结果协议。

异步不会凭空减少计算量，默认执行器也不会自动解决线程池隔离、下游容量和取消问题。

## 30 秒回答

> CompletableFuture 是一个可完成结果和依赖阶段的编排模型。thenApply 处理 T 到 U 的同步转换，thenCompose 处理 T 到 CompletionStage<U> 的串行依赖，thenCombine 合并两个独立结果。非 async 阶段通常由完成前一阶段的线程执行；async 阶段在未显式传 Executor 时通常使用默认异步执行器，普通多核环境一般是 commonPool，但实现有退化边界。服务端应为不同下游和耗时模型显式隔离线程池。异常会沿阶段传播，exceptionally 可恢复，handle 同时观察结果和异常。allOf 只表达全部完成，结果需要从原 Future 收集。

## thenApply 与 thenCompose

~~~text
thenApply：T → U
future<T>.thenApply(this::convert) → future<U>

thenCompose：T → Future<U>
future<T>.thenCompose(this::loadNext) → future<U>
~~~

如果把返回 Future 的方法交给 thenApply，结果会变成嵌套 CompletionStage；需要串行依赖时用 thenCompose。

## 并行与聚合

独立任务可以并行启动，再使用 thenCombine 或 allOf：

~~~text
配置任务 ─┐
          ├→ allOf / thenCombine → 聚合结果
用户任务 ─┘
~~~

allOf 的结果类型是 CompletableFuture<Void>，它只表示所有阶段完成。需要结果时：

~~~java
CompletableFuture.allOf(first, second).join();
Result result = combine(first.join(), second.join());
~~~

join 放在 allOf 完成之后，等待成本已经被聚合边界控制；仍要处理异常 CompletionException。

并行不是越多越好：如果每个阶段都调用同一个数据库或远程服务，异步只会把瞬时并发推高。

## 哪个线程执行阶段

- thenApply、thenCompose、thenCombine 等非 async 方法，通常由完成前一阶段的线程继续执行；
- thenApplyAsync 等 async 方法，未指定 Executor 时使用默认异步执行器；
- 指定 Executor 后，执行边界由该 Executor 决定；
- 前一阶段已经完成时，非 async 阶段可能直接在调用线程执行。

不要把“异步”理解为一定创建新线程，也不要把 commonPool 当作服务端所有业务的默认隔离池。阻塞 I/O、CPU 计算和低延迟请求应该按资源模型分池或改用适合的并发模型。

## 异常传播

~~~text
阶段 A
  ↓ 正常
阶段 B
  ↓ 异常
exceptionally：把异常恢复成一个结果
handle：同时拿到结果或异常并转换
whenComplete：观察并记录，不负责改变结果语义
~~~

如果异常没有被任何终结操作观察，可能只在 join、get 或最终日志中暴露。业务要定义失败是降级、重试、取消整条链，还是把异常交给上层。

## 超时、取消与资源释放

Java 9+ 的 orTimeout 会让 Future 以 TimeoutException 失败，completeOnTimeout 会提供兜底值；它们不自动杀死已经发出的底层 I/O。cancel 通常完成 Future 的取消状态，也不保证底层任务已经停止。

真正的超时设计需要：

- 下游调用本身有连接和读取超时；
- 任务响应 interrupt 或取消信号；
- 资源在 finally 释放；
- 重试有预算，避免超时风暴；
- 结果注明部分成功还是整体失败。

## 线程池隔离

不要在业务代码中无脑使用 commonPool：

- 阻塞任务会占用共享 worker；
- 不同下游的故障会互相拖垮；
- CPU 任务和 I/O 任务需要不同容量模型；
- 队列和拒绝策略必须可观测。

优先显式传入有界 Executor，并把其生命周期交给应用容器统一管理。线程池设计见 [ThreadPoolExecutor 线程池](ThreadPoolExecutor线程池.md)。

## 关键源码路径

- CompletionStage：阶段依赖和组合；
- CompletableFuture Uni / Bi / OrCompletion：单输入、双输入和竞速组合；
- asyncSupplyStage / asyncRunStage：执行器选择；
- AltResult：正常结果和异常结果的统一存储；
- orTimeout、completeOnTimeout：超时完成；
- join、get：异常包装和调用方观察。

不需要逐行阅读数千行内部 Completion 节点；先画清依赖图、执行器边界和异常路径。

## 工程边界

- 异步编排不能取代限流、超时、熔断和幂等；
- allOf 不是事务，部分远程调用成功不等于整体业务成功；
- 不把阻塞代码无脑扔进 commonPool；
- 明确异常、取消和超时是结果的一部分；
- 对外部副作用使用幂等键和补偿，而不是只依赖 Future 链；
- 任务完成后及时释放上下文和资源。

## 高频追问

- thenApply 和 thenCompose 区别？一个做同步转换，一个展开异步依赖。
- 默认线程池是谁？async 阶段通常使用默认异步执行器，多核环境一般是 commonPool，但应显式传入服务端执行器。
- allOf 为什么不直接返回结果？它只统一表达全部完成，结果类型和顺序由调用方决定。
- 异步一定更快吗？不一定，只是改变等待和资源占用模型。
- cancel 会中断远程调用吗？不保证，底层资源必须有自己的取消和超时。

## 一句话复盘

CompletableFuture 的核心是依赖图、执行器和错误协议；异步只是重新安排等待，不能消除计算、容量和副作用。
