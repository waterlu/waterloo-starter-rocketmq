## RocketMQ-Starter

整合Spring Boot和RocketMQ，通过注解方式快速实现生产者和消费者。

## 使用方法

### 1. 引入JAR包，选择合适的版本

```java
<dependency>
    <groupId>cn.waterlu</groupId>
    <artifactId>waterloo-starter-rocketmq</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 在Application上添加注解，启用RocketMQ

```java
@EnableRocketMQConfiguration
public class ServiceApplication {
    
}
```

### 3. 在application-${env}.properties配置文件中添加RocketMQ相关配置

```java
# RocketMQ 服务地址和端口 （必须）
rocket-mq.name-server=192.168.75.159:9876

# 生产者组名 (建议填写)
rocket-mq.producer-group-name=p_order

# 事务型生产者组名 (建议填写)
rocket-mq.producer-transaction-group-name=pt_order

# Redis配置
spring.redis.host=192.168.75.159
spring.redis.port=6379
```

### 4. 定义消息体结构

> 继承AbstractMQMessage类或者实现MQMessage接口，建议继承AbstractMQMessage

- 为避免重复消费，每个消息体都必须指定一个Key（MQ有messageID，但是重试时，每次messageID都会改变）
- MQMessage接口只有一个方法getKey()
- AbstractMQMessage抽象基类实现了MQMessage接口，getKey()方法默认返回随机的UUID
- 建议还是根据业务需求自己实现getKey()方法，实在没有合适的key再使用UUID，因为UUID没有逻辑含义，查问题的时候不如订单号、用户ID等方便
- AbstractMQMessage抽象基类中包含了三个时间createTime、bornTime和storeTime，这三个时间都是底层组件设置好的，业务层直接读取即可
  - createTime是生产者（例如交易服务）创建这个消息的时间
  - bornTime是RocketMQ记录的生产消息的时间
  - storeTime是RocketMQ记录的消息落盘的时间
  - createTime在消息体内，永远不变；storeTime和messageID一样，重试时每次都变；bornTime经测试，重试时有可能改变
  - 判断历史消息时，如果createTime有值，使用createTime判断，否则使用bornTime判断
  - 建议消息都继承AbstractMQMessage，这样可以自动生成createTime，判断历史时间更准确
  - 由于这里的时间都是服务器时间，理论上是同步的，但实际可能稍有偏差，所以时间对比精度请不要设置太高，以小时或天为宜，最多精细到分钟级别

### 5. 发送消息

> 继承类AbstractMQProducer，并添加注解@RocketMQProducer

- 组件会扫描@RocketMQProducer注解，根据注解配置生成生产者客户端
- 继承AbstractMQProducer是为了封装发送消息的send()方法，这样就不用自己写了

```java
@RocketMQProducer(topic = "my-topic", tag = "my-tag")
public class MyProducer extends AbstractMQProducer<MyMessage> {
}
```

> 注意：当前版本要求必须指定topic和tag，tag不可以为空，一定注意。

### 6. 接收消息

> 继承类AbstractMQConsumer，实现process()方法，并添加注解@RocketMQConsumer

@RocketMQConsumer注解属性

- topic: 主题（不能为空）
- tag：标签，一个主题下可以定义多个标签（不能为空）
- consumerGroup：消费组，非常重要（不能为空）
- 以上三个属性是必须有的，不能为空，否则无法创建出消费者
- duplicated：消息是否可重复，true-可以重复，false-不能重复，默认false
- maxHistoryTime：消息的最大时间间隔，单位：秒，接收到消息时组件会用当前时间与消息生成时间进行比较，如果差距大于maxHistoryTime，将认为是历史消息重复消费，直接抛弃掉；默认值为259200L，即超过3天的消息不再消费
- beginTime: 消息的开始时间，默认为空，不做控制；如果设置了beginTime，那么早于这个时间生成的消息将被抛弃掉，时间格式为"yyyy-MM-dd HH:mm:ss"，错误的时间格式将导致beginTime失效
- 以上三个属性可以不设置，duplicated和maxHistoryTime有默认值，默认不可重复消费，最大时间间隔为3天

```java
@RocketMQConsumer(topic = "my-topic", tag = "my-tag", consumerGroup = "c_my_group")
public class MyConsumer extends AbstractMQConsumer<MyMessage>{

    @Override
    public boolean process(MyMessage message) throws Exception {
        return true;
    }
}
```

实现AbstractMQConsumer的抽象方法process()

- 返回true证明消费成功
- 返回false证明消费失败，消息会被再次投递，直到成功，每次投递之间的时间间隔逐渐加大
- 抛出异常同上

注意:

- process()方法中的操作需要保证幂等性和事务性；
- 幂等性：组件底层会尽量避免同一个消息重复消费，但业务层也要做好幂等处理，保证即使出现消息重复消费的情况也不会出错，尤其是敏感操作；
- 事务性：如果process()方法中有多步操作，请保证事务性；例如：如果第一步成功，第二步抛异常，而且又没有事务控制，将导致第二次消费消息时重复执行第一步操作，引发错误；
- 建议不同类型的操作定义不同的consumerGroup，这样可以分开执行，互不影响。

### 7. 事务消息

- 事务消息的生产者请继承AbstractTransactionMQProducer，消费者与普通消费者没有区别
- 事务消息的生产者需要实现checkState()方法，检查本地事务是否执行成功

```java
@RocketMQTransactionProducer(topic = "order", tag = "paid")
public class OrderPaidProducer extends AbstractTransactionMQProducer<OrderPaidMessage> {

    @Autowired
    private OrderTransactionCheckService orderTransactionCheckService;

    @Override
    public boolean checkState(OrderPaidMessage orderPaidMessage) throws Exception {
        return orderTransactionCheckService.checkInvest(orderPaidMessage.getOrderBillCode());
    }
}
```

> 注意，V4.2.0版本没有实现事务消息，也就是说，调用接口不会返回错误，但是不起作用。

## 注意事项

在重复一遍注意事项：

- 如果发送消息失败，最大的可能是broker注册了内部IP到nameServer上（例如：127.0.0.1），导致远程连接不上；通过rocketmq-console可以查看broker地址进行验证；如果确认IP错误，请在启动broker时添加-c选项，显示指定IP地址；
- 1.0.0版本要求必须指定topic和tag，两者缺一不可，消费者必须指定消费组；
- 组件底层实现了消息幂等，每成功消费一个消息，就把key添加到redis中；所以必须配置redis地址，这里我使用的是Sorted Sets，key是消息主键，score是时间，如果数据量太大可以手工根据score删除。

> ZSET名是`message_consume_history:[topic]:[tag]:[consumerGroup]`
>
> `zrange message_consume_history:test:time:demo 0 -1 WITHSCORES`
>
> 上面指令可以查看全部数据（主题是test，标签是time，消费者是demo）

## 变更日志

### v1.0.0

* 实现注解方式使用消息生产者和消费者；
* 增加消息重复的判断（根据key），记录消息消费记录到redis中，记录消费时间，为删除做准备；
* 增加消息相对时间和绝对时间判断；
* 当消息实体是从抽象基类AbstractMQMessage继承时，已经包含生成时间，通过这个时间判断过期，不再通过bornTime判断过期（原因：测试时发现，%RETRY%消息中bornTime曾经改变过）；
* AbstractMQMessage添加UUID作为默认的Key（建议自定义key，方便查找）。



## 
