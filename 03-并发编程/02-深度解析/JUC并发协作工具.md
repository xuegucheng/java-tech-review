# JUC 并发协作工具

> 面试定位：CountDownLatch、CyclicBarrier、Semaphore 的问题模型
> Java 版本：API 语义按 JDK 8+，实现关系按 Java 21
> P0/P1：P1
> 前置知识：AQS、Condition、线程中断和超时
> 本文不负责：把所有 JUC 工具逐个列成 API 手册

## 先说结论

三个工具解决的问题不同：

- CountDownLatch：一个或多个线程等待一组任务完成，一次性打开；
- CyclicBarrier：一组线程互相等待，全部到达后一起进入下一阶段，可重复使用；
- Semaphore：限制同时持有某种资源许可的线程数量。

CountDownLatch 和 Semaphore 直接使用 AQS 的 shared 思路；CyclicBarrier 主要由 ReentrantLock + Condition 组织屏障状态，因此它间接建立在 AQS 之上。

## 30 秒回答

> CountDownLatch 的 state 可以理解为剩余计数，countDown 递减，归零后所有等待者通过；它不能 reset，重复阶段要创建新实例。CyclicBarrier 维护 parties、当前到达数和 generation，最后一个线程到达时唤醒同一代等待者，generation 切换后可以复用，超时或中断会打破这一代。Semaphore 的 state 表示 permits，acquire 递减、release 增加；公平模式主要影响排队获取顺序，不等于业务绝对公平。三者都要给 await/acquire 设置超时和取消边界。

## CountDownLatch

适合：

~~~text
主线程等待多个初始化任务结束
一个阶段等待 N 个 worker 完成
测试中等待多个线程准备好
~~~

抽象流程：

~~~text
state = count
countDown → state--
state == 0 → shared 获取全部通过
await → state != 0 时排队
~~~

它是单向门闩。countDown 只能减少计数，不能把已归零的 latch 重新恢复到初始值。需要循环阶段时，创建新 latch 或使用 CyclicBarrier / Phaser。

注意 countDown 不等于任务一定成功；任务异常、超时和部分失败要通过 Future、结果对象或错误计数单独表达。

## CyclicBarrier

CyclicBarrier 面向“所有参与者到达同一个阶段”的协作：

~~~text
线程 A ─┐
线程 B ─┼→ barrier.await()
线程 C ─┘
          ↓ 最后一个到达
       barrier action
          ↓
      下一代 generation
~~~

它内部使用显式锁和 Condition 管理等待线程、计数和 generation。最后到达者执行 barrier action，然后唤醒同一代的其他等待者。

可以复用的原因是：一代完成后重置 count 并创建新的 generation，而不是把一次性计数器简单加回去。

以下情况会让当前 generation 进入 broken：

- 某线程被中断；
- 某线程等待超时；
- barrier action 抛异常；
- 显式 reset。

其他等待线程应处理 BrokenBarrierException，而不是继续假设所有参与者已经同步。

## Semaphore

Semaphore 的 permits 表示可同时获得的资源数量：

~~~text
permits = 3
三个线程 acquire 成功
第四个线程等待
任意线程 release
第四个线程才有机会继续
~~~

它可以用来限制并发连接、并行任务或某类资源访问，但 permit 本身不等于真实资源对象；业务仍要保证 acquire/release 配对。

公平模式通过排队规则减少插队，但：

- 线程调度仍不是严格时间顺序；
- 超时、取消和直接 tryAcquire 可能改变观察；
- 公平会增加排队和调度成本。

## 三者比较

| 工具 | 等待关系 | 是否可复用 | state / 核心状态 | 常见错误 |
| --- | --- | --- | --- | --- |
| CountDownLatch | 等待别人完成 | 否 | 剩余计数 | 把异常吞掉后仍 countDown |
| CyclicBarrier | 互相等到齐 | 是 | parties、count、generation | 忽略 broken barrier |
| Semaphore | 等待许可 | 是 | permits | acquire 后未 finally release |

## 超时与中断

并发协作工具不能无限等待：

- await(timeout) 返回后检查是否真的完成；
- acquire(timeout) 失败要走降级或释放已持有资源；
- InterruptedException 不要随意吞掉，无法继续时恢复中断状态；
- 线程池关闭时要让等待者能退出。

## 源码路径

- CountDownLatch.Sync：AQS shared state 和归零传播；
- Semaphore.Sync：许可数的 shared 获取与释放；
- CyclicBarrier.Generation：代际和 broken 状态；
- CyclicBarrier.lock / trip：ReentrantLock 和 Condition 的屏障协作。

## 工程边界

- 工具只解决等待协议，不解决业务错误传播；
- 所有等待设置合理上限；
- 资源型 semaphore 必须在 finally 中 release；
- barrier 参与者数量必须和实际任务生命周期一致；
- 不用 CountDownLatch 伪装可重置状态；
- 对超时、取消、部分失败和重复执行设计明确结果。

## 高频追问

- CountDownLatch 为什么不能 reset？一次性计数器的语义，归零后不再恢复。
- CyclicBarrier 为什么可复用？通过 generation 切换和重新计数表示下一轮。
- Semaphore permits 是什么？允许同时通过的许可数量，不自动代表真实资源。
- 公平 Semaphore 是绝对公平吗？不是，主要是获取排队策略。
- CyclicBarrier 和 CountDownLatch 最大区别？前者是参与者互相等待并可分代复用，后者是等待一组计数单向归零。

## 一句话复盘

先判断等待模型是“等别人完成”“等大家到齐”还是“等资源许可”，再选工具；工具不会替你完成异常、超时和资源清理。
