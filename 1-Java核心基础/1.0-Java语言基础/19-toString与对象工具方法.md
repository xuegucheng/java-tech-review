# toString 与对象工具方法

## 19.1 本章定位

本章讨论 Object 中不属于 equals/hashCode 主线的基础方法及相关工程风险。

主要问题：

- toString 默认输出什么？
- 如何安全重写 toString？
- getClass 返回什么？
- clone 为什么默认是浅拷贝？
- 为什么通常推荐拷贝构造方法而不是 clone？
- wait/notify 为什么定义在 Object？
- 为什么不应依赖对象终结机制？
- identityHashCode 有什么用途？

## 19.2 toString()

Object 默认的 toString() 通常生成类似：

```
com.example.User@1a2b3c
```

形式通常包含：

```
类名
@
十六进制形式的哈希相关值
```

但不应依赖默认格式作为稳定协议。

**19.2.1 为什么重写 toString**

默认输出缺乏业务信息：

```java
System.out.println(user);
```

可能只看到：

```
com.example.User@5f184fc6
```

重写后：

```java
@Override
public String toString() {
    return "User{"
    + "userId='" + userId + '\''
    + ", name='" + name + '\''
    + '}';
}
```

输出：

```
User{userId='U001', name='Java'}
```

有利于：

- 调试
- 日志
- 测试失败分析
- IDE 查看对象
- 问题定位

**19.2.2 toString 不应泄露敏感信息**

不应输出：

- 密码
- Token
- API Key
- 身份证号
- 银行卡号
- 完整手机号
- 私钥
- Cookie
- Authorization Header
- 错误：

```java
@Override
public String toString() {
    return "User{"
    + "password='" + password + '\''
    + ", token='" + token + '\''
    + '}';
}
```

日志可能长期保存并被多人访问。

**19.2.3 toString 不应承担业务序列化协议**

不建议把 toString() 当成：

- JSON 序列化
- 数据库存储格式
- 网络传输协议
- 缓存 Key 协议
- 签名原文

因为 toString 主要用于人类可读描述，格式可能随代码调整。

需要稳定格式时，应使用明确的：

- JSON 序列化
- DTO
- 协议对象
- 专用 format 方法

**19.2.4 toString 应避免复杂副作用**

不推荐在 toString 中：

- 查询数据库
- 发起网络请求
- 修改对象状态
- 执行耗时计算
- 抛出业务异常
- 访问可能未初始化的懒加载属性

日志框架、IDE 或调试器可能自动调用 toString。
因此应保持：

```
轻量
稳定
无副作用
```

## 19.3 getClass()

Object 定义：

```java
public final native Class<?> getClass();
```

用于获取对象的运行时类型：

```
User user = new User();
Class<?> type = user.getClass();
```

例如：

```java
System.out.println(
user.getClass().getName()
);
```

可能输出：

```
com.example.User
```

**getClass 返回运行时类型**

```java
Animal animal = new Dog();
System.out.println(
animal.getClass()
);
```

返回的是 Dog 的 Class 对象，而不是 Animal。
因为：

```
编译时类型：Animal
运行时类型：Dog
```

getClass() 获取运行时类型。

**getClass() 与 instanceof**

```
animal instanceof Animal
```

判断对象是否属于某类型或其子类型。

```
animal.getClass() == Animal.class
```

判断运行时类型是否精确等于 Animal。
例如 Dog：

```
dog instanceof Animal // true
dog.getClass() == Animal.class // false
```

**getClass() 不能被重写**

- getClass() 是 final 方法。
- 对象不能伪造自己的实际运行时 Class。
- Class 与反射机制后续单独展开。

## 19.4 clone()

Object 提供：

```java
protected native Object clone()
throws CloneNotSupportedException;
```

clone 用于复制对象，但其设计存在较多限制。

**19.4.1 Cloneable 标记接口**

要正常调用 Object.clone()，类通常需要实现：

```
Cloneable
```

示例：

```java
public class User implements Cloneable {
    private String name;
    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (
        CloneNotSupportedException exception
        ) {
            throw new AssertionError(exception);
        }
    }
}
```

Cloneable 本身没有声明 clone 方法，它只是告诉 Object.clone：

```
允许进行字段级复制
```

这是一种标记接口。

**19.4.2 clone 默认是浅拷贝**

```java
public class Order implements Cloneable {
    private String orderNo;
    private List<String> items;
}
```

执行 clone 后：

```bash
原 Order 对象
└── items ──┐
├──→ 同一个 List
克隆 Order 对象
└── items ──┘
```

外层对象被复制，但内部引用字段仍然共享。
因此：

```
clonedOrder.getItems().add("B");
```

可能影响原对象。

**19.4.3 为什么通常不推荐 clone**

clone 的问题包括：

- Cloneable 接口没有定义 clone 方法
- Object.clone 是 protected
- 默认只做浅拷贝
- 构造方法通常不会按普通方式执行
- 深拷贝需要手工处理
- 继承体系下容易出错
- 可变字段复制语义不明确
- 异常处理不自然
- 更推荐：
- 拷贝构造方法

```java
public Order(Order source) {
    this.orderNo = source.orderNo;
    this.items =
    new ArrayList<>(source.items);
}
```

静态复制工厂

```java
public static Order copyOf(Order source) {
    return new Order(
    source.orderNo,
    source.items
    );
}
```

显式映射

```
OrderCopyMapper.copy(source);
```

这些方式的复制语义更清晰。

## 19.5 wait()、notify() 与 notifyAll()

Object 定义了：

```
wait()
notify()
notifyAll()
```

原因是：
Java 中任何对象都可以作为监视器锁对象。
例如：

```
synchronized (lock) {
lock.wait();
}
```

唤醒：

```
synchronized (lock) {
lock.notifyAll();
}
```

这些方法必须在持有对象监视器时调用，否则会抛出：

```
IllegalMonitorStateException
```

完整机制包括：

- Monitor
- Wait Set
- 锁释放
- 虚假唤醒
- 条件循环
- notify 与 notifyAll
- 已放在并发编程笔记中，本章只理解：

它们属于 Object，是因为任意对象都可以充当同步监视器。

## 19.6 对象终结机制

历史上 Object 提供过对象终结相关机制，但它存在：

- 执行时间不确定
- 不保证及时执行
- 可能永远不执行
- 性能开销较高
- 容易导致对象复活
- 异常处理困难
- 资源释放不可靠
- 因此不能依赖对象被垃圾回收来关闭：
- 文件
- 数据库连接
- Socket
- 线程池
- 锁
- 本地资源
- 资源管理应使用：

```
try-with-resources
```

例如：

```
try (
InputStream input =
Files.newInputStream(path)
) {
// 使用资源
}
```

对象终结机制只作为历史知识了解，不应作为现代资源管理方案。

## 19.7 identityHashCode()

System 提供：

```
System.identityHashCode(object)
```

它用于获得基于对象身份的哈希值，不受类重写 hashCode 影响。
例如：

```java
public class User {
    @Override
    public int hashCode() {
        return 1;
    }
}
```

调用：

```java
User user = new User();
System.out.println(user.hashCode());
System.out.println(
System.identityHashCode(user)
);
```

两个结果可能不同。

适合：

- 调试对象身份
- 分析引用关系
- 身份型数据结构
- 排查对象重复创建

不应把它作为业务唯一 ID。

## 19.8 建议实验

实验八：toString 敏感信息泄露

```java
public class ToStringDemo {
    static class User {
        private String username;
        private String password;
        private String token;
        @Override
        public String toString() {
            return "User{"
            + "username='" + username + '\''
            + ", password='" + password + '\''
            + ", token='" + token + '\''
            + '}';
        }
    }
}
```

分析该实现为什么不适合生产日志。
改为：

```java
@Override
public String toString() {
    return "User{"
    + "username='" + username + '\''
    + '}';
}
```

实验九：浅克隆共享内部对象

```java
public class CloneDemo {
    public static void main(String[] args) {
        Order source = new Order();
        source.items.add("A");
        Order copy = source.clone();
        copy.items.add("B");
        System.out.println(source.items);
        System.out.println(copy.items);
    }
    static class Order implements Cloneable {
        private List<String> items =
        new ArrayList<>();
        @Override
        public Order clone() {
            try {
                return (Order) super.clone();
            } catch (
            CloneNotSupportedException exception
            ) {
                throw new AssertionError(exception);
            }
        }
    }
}
```

预期两个对象都看到：

```
[A, B]
```

说明默认 clone 是浅拷贝。
## 19.9 高频面试题
- 34. toString() 的默认格式是什么？
- 35.为什么需要重写 toString？
- 36.toString 中为什么不能输出密码和 Token？
- 37.toString 能否作为稳定序列化协议？
- 38.getClass() 返回编译时类型还是运行时类型？
- 39. instanceof 与 getClass 精确判断有什么区别？
- 40.clone 默认是深拷贝还是浅拷贝？
- 41.Cloneable 接口中是否定义了 clone 方法？
- 42.为什么通常不推荐使用 clone？
- 43.wait、notify 为什么定义在 Object 中？
- 44.调用 wait、notify 为什么必须持有对象锁？
- 45.System.identityHashCode 有什么作用？
## 19.10 易错点

- 误区十三：toString 可以随意打印所有字段
- 错误。
- 敏感字段和大型对象图不应输出。
- 误区十四：toString 适合作为业务数据协议
- 错误。
- toString 主要用于人类可读描述，格式不应视为稳定协议。
- 误区十五：clone 会自动深拷贝
- 错误。
- Object.clone 默认更接近字段级浅拷贝。
- 误区十六：Cloneable 定义了 clone 方法
- 错误。
- Cloneable 是标记接口，本身没有声明方法。
- 误区十七：getClass 返回变量声明类型
- 错误。
- getClass 返回对象的运行时类型。
- 误区十八：wait 和 notify 属于 Thread
- 错误。
- 误区十九：System.identityHashCode 是业务唯一 ID
- 错误。
- 它只适合对象身份相关的底层或调试场景，不保证全局唯一。
## 19.11 工程实践建议

**toString 只输出定位所需信息**

推荐输出：

- 业务 ID
- 类型
- 关键状态
- 少量必要字段
- 避免输出：
- 密码
- Token
- 大型集合
- 二进制内容
- 完整对象图
- 懒加载关联

- toString
- Setter

的注解。

应明确控制：

- 相等字段
- 日志字段
- 状态修改入口
- 关联对象

**复制对象优先使用明确 API**

推荐：

```
Order copy = Order.copyOf(source);
```

或者：

```
Order copy = new Order(source);
```

优于依赖 clone 的隐式复制语义。
## 19.12 本章总结

```
Object 提供基础对象方法
↓
toString 提供人类可读描述
↓
getClass 返回运行时类型
↓
clone 默认浅拷贝
↓
wait/notify 实现对象级线程协作
↓
identityHashCode 提供身份哈希
```

面试口述版：

- Object 除了 equals 和 hashCode 外，还提供了 toString、getClass、clone 以及线程协作方法。toString 默认输出类名@哈希，重写时应注意脱敏和避免递归。getClass 返回运行时类型，不能被重写。clone 默认是浅拷贝，工程上通常更推荐拷贝构造方法或复制工厂。wait 和 notify 定义在 Object 中是因为任意对象都可以作为监视器。identityHashCode 不受重写的 hashCode 影响，适合对象身份相关的调试场景。
