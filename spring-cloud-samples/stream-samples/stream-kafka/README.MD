# Spring Cloud Stream with Kafka Binder

## 项目依赖

* Zookeeper
    * 版本：3.6.4 (Kafka 内置)
    * 启动命令
      ```sh
      # 假定 kafka 主目录为 kafka_home
      $kafka_home/bin/zookeeper-server-start.sh ./config/zookeeper.properties
      ```

* Kafka
  * 版本：kafka_2.13-3.4.1
  * 启动命令
      ```sh
      # 假定 kafka 主目录为 kafka_home（单实例启动）
      $kafka_home/bin/kafka-server-start.sh ../config/server.properties
      ```

* Kafka UI （一款 Kafka 可视化 Web 控制台，可选）

    * [下载地址](https://github.com/provectus/kafka-ui)

    * 版本：0.7.1

    * 启动命令

        ```sh
        java -Dspring.config.additional-location=kafka-ui.yml  -jar ./kafka-ui-api-v0.7.1.jar
        ```

        kafka-ui.yml 配置文件：

        ```yml
        kafka:
          clusters:
            -
              name: local
              bootstrapServers: localhost:9092
              #schemaRegistry: http://localhost:8085
              #schemaRegistryAuth:
                #username: username
                #password: password
        #     schemaNameTemplate: "%s-value"
              #metrics:
                #port: 9997
                #type: JMX
        ```

        该工具可以取代命令行形式查看 Kafka 中创建的主题及消费者分区等信息，不过下文还是使用命令行形式查看。

        启动成功后，访问：`http://localhost:8080`


## 项目介绍

* kafka-producer 消息生产者（上游服务）
  * 应用名称：kafka-producer
  * 应用端口：8082
* kafka-consumer 消息消费者（下游服务）
  * 应用名称：kafka-consumer
  * 应用端口：8081
* zookeeper 服务
  zookeeper 采用 kafka 
  * 端口：2181
* kafka 服务
  * 端口：9092

## 应用交互

```txt

  PRODUCER                              kafkaMQ                            CONSUMER
╔═══════════╗                        ╔═══════════╗                   ╔═══════════════════════════╗
║           ║   === OUTPUT ===>      ║           ║  === INPUT ===>   ║   createGrp1              ║
║           ╚════════════════════════╝           ╚═══════════════════╝╭->  consumer1InCreateGrp  ║
║                              order.create.msg.createGrp  -----------┤                          ║
║           ╔════════════════════════╗           ╔═══════════════════╗┆   createGrp2             ║
║           ║                        ║           ║                   ║╰->   consumer2InCreateGrp ║
║           ║                        ║           ║                   ║                           ║
║           ║                        ║           ║                   ║                           ║
║           ╚════════════════════════╝           ╚═══════════════════╝╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮   ║
║                               partitioned.msg.partGrp-0             ╯evenPartitionConsumer ║   ║
║           ╟┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╢   ║
║                               partitioned.msg.partGrp-1             ╮oddPartitionConsumer  ║   ║
║           ╔════════════════════════╗           ╔═══════════════════╗╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯   ║
║           ║                        ║           ║                   ║                           ║
║           ║                        ║           ║                   ║                           ║
║           ║                        ║           ║                   ║                           ║
║           ╚════════════════════════╝           ╚═══════════════════╝                           ║
║                                                              ╭----->  gateway|functionRouter   ║
║ functionRouter ---[routeKey]---> routed.msg -----------------╯                      ┆          ║
║                                          ┆                         [routeKey] <-----╯          ║
║                                          ┆ DLX   ╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮<╯ ╰>╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮  ║
║                                          ┆       ┆evenRoutedConsumer┆     ┆oddRoutedConsumer┆  ║
║                                          ↓       ╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯     ╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯  ║
║                                 routed.msg.dlq ---------------------------╮  ╭┈┈┈┈┈┈┈┈╮        ║
║           ╔════════════════════════╗           ╔═══════════════════╗      ╰> ┆ logDlq ┆        ║
║           ║                        ║           ║                   ║         ╰┈┈┈┈┈┈┈┈╯        ║
╚═══════════╝                        ╚═══════════╝                   ╚═══════════════════════════╝

                                Graph-1 Message Interactive @Reion
```



### 消息分组

#### 生产者端（kafka-producer）

1. 配置

   ```yaml
   spring:
     cloud:
       stream:
         bindings:
           # 分组输出绑定
           groupOut:
            # 绑定目标地址
            destination: order.create.msg
            producer:
              partition-count: 1
              # 支持多个分组，同一消息将会路由到所有分组
              #
              # 与 RabbitMQ 不同：
              #
              # 1. Kafka 同组消费者不能消费相同分区的消息
              #    强行将同组消费者设置相同分区，只会有一个消费者被启用
              #    如果该 Topic 有多分区，同组相同分区的消费者将被平衡分配到其他分区
              # 2. Kafka 消费者可以消费不同分区的消息
              #
              # 同一消息路由到不同分组，将被不同分组的消费者用来实现不同功能
              # 例如：createGrp1 用来处理订单创建
              #      createGrp2: 用来记录订单创建日志
              required-groups:
                - createGrp1
                - createGrp2
   ```

   &emsp;&emsp; 就 Kafka  而言，此配置将会创建出名为 `order.create.msg` 的 **Topic 主题**：

   ```sh
   # 显示主题命令
   kafka-topics.sh --bootstrap-server localhost:9092 --topic "order.create.msg" --describe
   
   Topic: order.create.msg	TopicId: _yIXi_R1RhGQ4eFedViqfQ	PartitionCount: 1	ReplicationFactor: 1	Configs: 
   	Topic: order.create.msg	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
   ```

   

2. 生产类

   ```java
   public class OrderController {
   
       @Resource
       private StreamBridge bridge;
   
       /**
        * 创建并发送订单创建消息
        */
       @PostMapping(path = "/create")
       public boolean createOrder(@RequestParam("name") @NotBlank(message = "name is blank!") String name) {
           log.info("Create {} ...", name);
           // 将 WebEndpoint 消息桥接到消息绑定 groupOut, 即：分株输出绑定名
           bridge.send("groupOut", name);
           return true;
       }
   }
   ```

   &emsp;&emsp; 这里没有采用 **Supplier** 类型的函数 Bean 声明生产者，而是使用 **StreamBridge** 将来自 **Controller** 的请求转化成消息发送到绑定名中。值得注意的是采取函数 Bean 声明的生产者，默认采取定时轮询的方式调用此函数产生消息并发送。

   &emsp;&emsp; Post 请求如下地址可生产消息：

   ```sh
   curl -X POST -d 'name=order' localhost:8080/order/create
   ```

#### 消费者端（kafka-consumer）

1. 配置

   ```yaml
   spring:
     cloud:
       stream:
         function:
           # --- 绑定别名映射 ---
           bindings:
             # 订单创建消费者函数映射别名
             consumer1InCreateGrp-in-0: orderCreate1In
             consumer2InCreateGrp-in-0: orderCreate2In
         # --- 绑定配置 ---
         bindings:
           # --- 不同分组的两个消费者 ---
           # 它们同时消费相同分区的消息，即同一份消息被两个消费组的消费者消费
           #
           # 与 RabbitMQ 不同：
           #
           # 1. Kafka 同组消费者不能消费相同分区的消息
           #    强行将同组消费者设置相同分区，只会有一个消费者被启用
           #    如果该 Topic 有多分区，同组相同分区的消费者将被平衡分配到其他分区
           # 2. Kafka 消费者可以消费不同分区的消息
           orderCreate1In:
             destination: order.create.msg
             group: createGrp1
           orderCreate2In:
             destination: order.create.msg
             group: createGrp2
   ```

   &emsp;&emsp; 这里配置在两个分组 `createGrp1`、 `createGrp2` 中的消费者：`orderCreate1In`、`orderCreate2In`, 它们对应下面消费类中的两个 **Consumer** 函数 Bean。它们消费来自于 `order.create.msg` 地址的消息（同一份消息被这两个消费者同时读取）。 值得注意的是，函数式消费者默认绑定名称格式为：`functionName-in-index`，为了更简洁的使用，使用了 `spring.cloud.stream.function.bindings` 属性配置对函数式的绑定进行别名设置。

   

   ```sh
   # 分组 createGrp1 详情
   ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group createGrp1 --describe --members --verbose
   GROUP           CONSUMER-ID                                                HOST            CLIENT-ID             #PARTITIONS     ASSIGNMENT
   createGrp1      consumer-createGrp1-2-3c727b9f-f163-4c99-bfb3-7df9138ddde4 /127.0.0.1      consumer-createGrp1-2 1               order.create.msg(0)
   
   # 分组 createGrp2 详情
   ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group createGrp2 --describe --members --verbose
   GROUP           CONSUMER-ID                                                HOST            CLIENT-ID             #PARTITIONS     ASSIGNMENT
   createGrp2      consumer-createGrp2-4-bf6f3aa8-dcf9-433e-a01d-5e4fabf181f0 /127.0.0.1      consumer-createGrp2-4 1               order.create.msg(0)
   ```

   

   

2. 消费类

   ```java
   @Configuration
   public class GroupedConsumer {
   
       /**
        * createGrp1 分组消费者，接收订单创建消息
        */
       @Bean
       public Consumer<String> consumer1InCreateGrp() {
           return msg -> log.info("Group one receive a order, name is : {}", msg);
       }
   
       /**
        * createGrp2 分组消费者，接收订单创建消息
        */
       @Bean
       public Consumer<String> consumer2InCreateGrp() {
           return msg -> log.info("Group two receive a order, name is : {}", msg);
       }
   }
   ```

   这两个消费者将同时消费主题 `order.create.msg` 下的所有消息。

### 消息分区

#### 生产者端（kafka-producer）

1. 配置

   ```yaml
   spring:
     cloud:
       stream:
         function:
           # --- 绑定别名映射 ---
           # 将函数式 Bean 输出输入通道默认绑定名映射成简单的名称
           # 例如：foo-in-0: fooIn
           #      bar-out-0: barOut
           bindings:
             # 分区的绑定名称包含 '.'，使用 "[]" 转义
             "[partitioned.msg]": partOut
         # --- 绑定配置 ---
         bindings:
           # 分区输出绑定
           partOut:
             # 不设置，默认与绑定名称一致
             destination: partitioned.msg
             # 设置生产者分区属性
             producer:
               # 分区数量
               partition-count: 2
               # 分区表达式
               partition-key-expression: headers['partitionKey'] % 2
               # 所需分区组
               required-groups: partGrp
   ```

   &emsp;&emsp;此处配置名称为 `partOut` 的绑定，它绑定名称为 `partitioned.msg` 的 **Topic 主题**。在生产者属性中设置 2 个分区，分区 SpEL 表达式：`headers['partitionKey'] % 2`，即根据消息头中的 `partitioniKey` 与 2 取模运算的结果当做分区索引。指定消费者需要属于 `partGrp` 分组才能消费分区队列中的消息。

   ```sh
   # 查看分区型主题，可以看到两个分区
   ./kafka-topics.sh --bootstrap-server localhost:9092 --topic "partitioned.msg" --describe
   Topic: partitioned.msg	TopicId: WDqlWwYGQU6Pj5vI9yMDTA	PartitionCount: 2	ReplicationFactor: 1	Configs: 
   	Topic: partitioned.msg	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
   	Topic: partitioned.msg	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
   ```

   

2. 生产类

   ```java
   public class OrderController {
   
       @Resource
       private StreamBridge bridge;
   
       @PostMapping(path = "/partitionedMsg")
       public boolean partitionedMsg(@RequestParam("msg") @NotBlank(message = "msg is blank!") String msg) {
           log.info("Send message: {}, message length: {}", msg, msg.length());
           Message<String> message = MessageBuilder.withPayload(msg).setHeader("partitionKey", msg.length()).build();
           // 将 WebEndpoint 消息桥接到消息绑定 partOut
           bridge.send("partOut", message);
           return true;
       }
   }
   ```

   &emsp;&emsp; 同样，此处还是通过 **Controller** 发送分区消息，具体请求方式：

   ```sh
   curl -X POST -d 'msg=hello' localhost:8080/order/partitionedMsg
   ```

   

#### 消费者端（kafka-consumer）

1. 配置

   ```yaml
   spring:
     cloud:
       stream:
         function:
           # --- 绑定别名映射 ---
           bindings:
             # 分区消费者函数映射别名
             evenPartitionConsumer-in-0: evenPart1In
             oddPartitionConsumer-in-0: oddPart1In
   
         # --- 绑定配置 ---
         bindings:
           # 偶数分区消费者1
           evenPart1In:
             destination: partitioned.msg
             group: partGrp
             consumer:
               # 启用分区
               partitioned: true
               # 分区实例索引，本实例消费该主题下的 0 号分区
               instance-index: 0
               # 服务实例数量，这里一定要设置该主题下实例的总数，每个分区1个实例，总共2分区，故为 2
               instance-count: 2
               
           # 奇数分区消费者2
           oddPart1In:
             destination: partitioned.msg
             group: partGrp
             consumer:
               partitioned: true
               # 分区实例索引，本实例消费该主题下的 1 号分区
               instance-index: 1
               # 服务实例数量，这里一定要设置该主题下实例的总数，每个分区1个实例，总共2分区，故为 2
               instance-count: 2
   
         # === Kafka 定制化配置 ===
         kafka:
           bindings:
             # --- Kafka 分区型消费者配置 ---
             evenPart1In:
               consumer:
                 # 关闭消费者自动平衡，激活 instance-index、instance-count 指定的分区消费者
                 auto-rebalance-enabled: false
             oddPart1In:
               consumer:
                 # 关闭消费者自动平衡，激活 instance-index、instance-count 指定的分区消费者
                 auto-rebalance-enabled: false
   ```

   &emsp;&emsp;定义两个分区消费者，它们分别消费消息长度为奇数、偶数的消息。注意消费属性配置 `partitioned: true` 将使得消费者成为分区消费者，而 `instance-index` 所设置的值将决定它所消费的目标分区。

   

   ```sh
   # 采用自定义指定分区时，此处会提示没有活动的 members, 但是不会影响程序的执行
   ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group partGrp --describe
   
   Consumer group 'partGrp' has no active members.
   
   GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
   partGrp         partitioned.msg 0          1               1               0               -               -               -
   partGrp         partitioned.msg 1          1               1               0               -               -               -
   ```

   

2. 消费类

   ```java
   @Configuration
   public class PartitionedConsumer {
       /**
        * 头部字段 partitionKey 进行分区
        * 消费 partitionKey=0 的偶数消息
        */
       @Bean
       public Consumer<Message<String>> oddPartitionConsumer() {
           return msg -> log.info("partitionKey: {} is odd，oddPartitionConsumer get msg body: {}",
                   msg.getHeaders().get("partitionKey"), msg.getPayload());
       }
   
       /**
        * 头部字段 partitionKey 进行分区
        * 消费 partitionKey=1 的奇数消息
        */
       @Bean
       public Consumer<Message<String>> evenPartitionConsumer() {
           return msg -> log.info("partitionKey: {} is even，evenPartitionConsumer get msg body: {}",
                   msg.getHeaders().get("partitionKey"), msg.getPayload());
       }
   }
   ```

   

### 消息路由

#### 生产者端（kafka-producer）

1. 配置

   ```yaml
   spring:
     cloud:
       function:
         # 路由表达式设置
         routing-expression: "headers['routeNum'] % 2 == 0 ? 'even2RouteOut' : 'odd2RouteOut'"
       stream:
         # === 绑定 ===
         bindings:
           # 路由输出绑定
           routeOut:
             destination: routed.msg
             producer:
               required-groups: downstreamGrp
   ```
   
   &emsp;&emsp; 此处通过属性 `spring.cloud.function.routing-expression` 激活 Spring Cloud Stream 内置的 **RoutingFunction** 具备网关或路由特性的函数式 Bean，它的其中一项能力就是将这个路由表达式解析后的返回值当做路由键值，从而将消息路由到与路由键匹配的消费者。此处的路由表达式：`headers['routeNum'] % 2 == 0 ? 'even2RouteOut' : 'odd2RouteOut'`，即解析消息头中的 `routeNum` 字段值的奇偶性动态路由到 `even2RouteOut` 或 `odd2RouteOut`, 而这两个字符串是定义在容器中的函数式消费者的 Bean 名称，由它即可关联对应的函数处理消息。
   
   &emsp;&emsp;在本示例中，经过路由的消息最终被路由到 `even2RouteOut` 或 `odd2RouteOut` 本地函数式消费者，由它们将消息通过 `routeOut` 输出绑定发送到名为 `routed.msg` 的主题，从而使得下游加入到 `downstreamGrp` 分组的消费者接收该消息。至于消息则是由下面的 **Controller** 请求产生后直接交给容器中的 **RoutingFunction** 进行路由。
   
   
   
   ```sh
   # routed.msg 主题
   ./kafka-topics.sh --bootstrap-server localhost:9092 --topic "routed.msg" --describe
   Topic: routed.msg	TopicId: mnyPVDFSRW6-lODH9np2kA	PartitionCount: 1	ReplicationFactor: 1	Configs: 
   	Topic: routed.msg	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
   ```

   

   &emsp;&emsp;此外，消息在上游发送到下游所关联的主题 `routed.msg` 增加了对 Dead Letter（死信，无法路由的消息） 的处理，而 kafka 对死信的一种方式是将它路由到 **DLQ 死信主题**中。该特性属于特定中间件独有的配置，Spring Cloud Stream 目前官方支持的两个消息中间件 kafkaMQ、Kafka 分别在属性 `spring.cloud.stream.rabbit`、`spring.cloud.stream.kafka` 中进行配置，具体参考消费端配置。

   

2. 生产类

   ```java
   public class OrderController {
   
       @Resource
       private RoutingFunction routingFunction;
   
       /**
        * 设置包含头属性的消息，将其交给路由函数进行消息路由
        *
        * 发送 routeNum=0 的消息，将引起下游消费者异常，
        * 将激发消息重试，然后将重试次数内还未成功的消息发送到 DLQ。
        */
       @PostMapping(path = "/routedMsg")
       public boolean routedMsg(@RequestParam("routeNum") @PositiveOrZero(message = "routeNum is negative!") Integer routeNum) {
           Message<String> message = MessageBuilder.withPayload("WebEndpoint send a Number: " + routeNum)
                   .setHeader("routeNum", routeNum).build();
           log.info("\n\nWEB-ENDPOINT[ routeNum:{} ] \n\t===> \nROUTER[ message.headers.routeNum:{} ]\n", routeNum, message.getHeaders().get("routeNum"));
           // 路由消息
           routingFunction.apply(message);
           return true;
       }
   }
   ```

   &emsp;&emsp;尤其注意，消息是通过直接调用 **RoutingFunction** 的 `apply(Message)` 方法进行消息路由，更普遍的做法是设置一个输入绑定，将绑定的 `destination` 设置为 **RoutingFunction** 的输入交换机 `functionRouter-in-0`, 那么所有发往该绑定的目的地的消息，都会经过该路由函数。

    

   向 Controller 发送如下请求地址即可产生路由消息：

   ```sh
   # routeNum 非 0 时，会被正确路由到消费者队列
   curl -X POST -d 'routeNum=1' localhost:8080/order/routedMsg
   
   # routeNum 为 0 时，下游消费者将会发生异常，用来模拟演示不同的异常处理机制
   curl -X POST -d 'routeNum=0' localhost:8080/order/routedMsg
   ```

   消息将被路由到下面定义的两个函数式消费 Bean 中的一个，然后通过它们继续发生到目标下游：

   ```java
   @Configuration
   public class RouteOutConsumer {
   
       @Resource
       private StreamBridge streamBridge;
   
       /**
        * 偶数消息路由目标消费者，且将消息发送到输出绑定
        */
       @Bean
       public Consumer<Message<String>> even2RouteOut() {
           return msg -> {
               log.info("\n\nROUTER[ message.headers.routeNum:{} ] \n\t===> \nCONSUMER[ even2RouteOut ] \n\t===> \nBINDING[ routeOut(routeKey:evenRoutedConsumer) ]\n",
                     msg.getHeaders().get("routeNum"));
               // 设置包含下游头部路由键值的消息
               Message<String> newMsg = MessageBuilder.withPayload(msg.getPayload())
                       .setHeader("routeNum", msg.getHeaders().get("routeNum"))
                       .setHeader("routeKey", "evenRoutedConsumer").build();
               // 发送新消息到输出绑定
               streamBridge.send("routeOut", newMsg);
           };
       }
   
       /**
        * 奇数消息路由目标消费者，且将消息发送到输出绑定
        */
       @Bean
       public Consumer<Message<String>> odd2RouteOut() {
           return msg -> {
               log.info("\n\nROUTER[ message.headers.routeNum:{} ] \n\t===> \nCONSUMER[ odd2RouteOut ] \n\t===> \nBINDING[ routeOut(routeKey:oddRoutedConsumer) ]\n",
                       msg.getHeaders().get("routeNum"));
               // 设置包含下游头部路由键值的消息
               Message<String> newMsg = MessageBuilder.withPayload(msg.getPayload())
                       .setHeader("routeNum", msg.getHeaders().get("routeNum"))
                       .setHeader("routeKey", "oddRoutedConsumer").build();
               // 发送新消息到输出绑定
               streamBridge.send("routeOut", newMsg);
           };
       }
   }
   ```

   

#### 消费者端（kafka-consumer）

1. 配置

   ```yaml
   spring:
     cloud:
       # --- 函数 Bean 定义 ---
       function:
         # 函数 Bean 定义
         # gateway|functionRouter 为复合函数
         definition: >
           gateway|functionRouter
         # 路由表达式设置
         routing-expression: headers['routeKey']
   
       # --- Spring Cloud Stream 配置 ---
       stream:
         function:
           # --- 绑定别名映射 ---
           bindings:
             # 将复合函数的输入绑定映射别名
             gatewayfunctionRouter-in-0: routeIn
   
         # --- 绑定配置 ---
         bindings:
           # --- 下游路由输入绑定 ---
           routeIn:
             # 绑定上游路由的输出地址
             destination: routed.msg
             group: downstreamGrp
             # 自定义异常处理（与 DLX/DLQ 机制互斥）
             #error-handler-definition: errorMessageConsumer
             consumer:
               retry-template-name: myRetryTemplate
   
         # === Kafka 定制化配置 ===
         kafka:
           bindings:
             routeIn:
               # Kafka 消费者可定制属性，参考：KafkaConsumerProperties
               consumer:
                 # 开启 DLQ 功能
                 enable-dlq: true
                 # 设置 DLQ 主题
                 dlq-name: routed.msg.dlq
                 # 设置 DLQ 主题分区数量
                 dlq-partitions: 1
   ```

   &emsp;&emsp;首先，该下游消费者也通过 `routing-expression` 属性配置激活 **RoutingFunction** 具备网关路由功能的函数式 Bean，它解析传递给它的消息中的消息头的 `routeKey` 的值，动态将消息路由到值对应的函数式 Bean 名称的方法处理。值得注意的是，该网关路由函数式 Bean 接收的消息不再像上游的生产端那样，直接通过调用它的消息处理方法，而是通过复合函数的形式 `gateway|functionRouter` 进行定义，即：该路由函数接收 `gateway` 函数的输出消息。

   &emsp;&emsp;其次，该复合函数的输入绑定到上游具备 DLQ 特性的交换机 `routed.msg` 上，从而接收来自上游的消息，并最后通过路由表达式 `headers['routeKey']` 提取消息头中的 `routeKey` 的值，将消息路由到该值对应的下游 Bean 名称相匹配的函数式消费者处理，本例中为：``、``两个函数式 Bean，参考下面消费类的代码定义。

   &emsp;&emsp;此外，在本复合函数的 `gateway` 函数中，模拟消息处理异常，来验证常见的两种异常处理配置：

   - 自定义错误处理器

     该方法需在绑定配置中通过 `error-handler-definition` 设置自定义异常消息处理器 Bean 的名称，如本例中所配置的 `errorMessageConsumer`。

   - 利用 DLQ 机制

     该机制需在 kafka  定制化属性配置中，将消费者关联的交换机启用自动绑定 DLQ 的属性 `auto-bind-dlq: true`，同时需要指定 DLQ 的主题名称及该主题所需的分区数量，否则 Spring Cloud Stream 将不会进行 DLQ 主题的创建。

   &emsp;&emsp;注意，这两种异常处理是互斥的，两个都配置时，只有一种会生效。

   &emsp;&emsp;最后，消费出现异常时默认会开启 3 次重试操作，如果想针对具体消费者单独定制重试配置，可以在其绑定设置的消费配置中设置

    `retry-template-name` 属性，它的值是被注解 **@StreamRetryTemplate** 修饰的自定义 **RetryTemplate** Bean 的名称，通过它可以定制重试次数、重试策略、注册自定义重试监听器等操作。

   

   ```sh
   # 下游消费者分组
   ./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group "downstreamGrp"  --describe
   GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                    HOST            CLIENT-ID
   downstreamGrp   routed.msg      0          2               2               0               consumer-downstreamGrp-10-86c7b759-a154-4650-968d-b185a708920f /127.0.0.1      consumer-downstreamGrp-10
   
   # routed.msg 主题对应的 DLQ routed.msg.dlq
   ./kafka-topics.sh --bootstrap-server localhost:9092 --topic "routed.msg.dlq" --describe                                                                                         130 ↵ ──(二, 130)─┘
   Topic: routed.msg.dlq	TopicId: ryZGuYv5R9qzXT29yjPCkw	PartitionCount: 1	ReplicationFactor: 1	Configs: 
   	Topic: routed.msg.dlq	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
   
   ```

   

   

2. 消费类

   ```java
   @Configuration
   public class RouteInConsumer {
   
       /**
        * 消费上游消息，通过复合函数将消息传递给本地 {@link RoutingFunction} 进行路由
        */
       @Bean
       public Function<Message<String>, Message<String>> gateway() {
           return msg -> {
               if(msg.getHeaders().get("routeNum").equals(0)) {
                   throw new RuntimeException("routeNum is 0");
               }
               log.info("\n\nBINDING[ routeIn(gateway|functionRouter) ] \n\t===> \nROUTER[ routeKey:{} ]\n",
                       msg.getHeaders().get("routeKey"));
               return msg;
           };
       }
   
       @Bean
       public Consumer<Message<String>> evenRoutedConsumer() {
           return msg -> {
               log.info("\n\nROUTER[ routeKey:{} ] \n\t===> \nCONSUMER[ evenRoutedConsumer ] \n\tMessageBody:{}\n",
                       msg.getHeaders().get("routeKey"), msg.getPayload());
           };
       }
   
       @Bean
       public Consumer<Message<String>> oddRoutedConsumer() {
           return msg -> {
               log.info("\n\nROUTER[ routeKey:{} ] \n\t===> \nCONSUMER[ oddRoutedConsumer ] \n\tMessageBody:{}\n",
                       msg.getHeaders().get("routeKey"), msg.getPayload());
           };
       }
   }
   ```

3. DLQ 异常处理类

   ```java
   @Configuration
   public class RouteInDlqConsumer {
   
       private static final String ORIGINAL_TOPIC = "routed.msg";
       private static final String DLQ = ORIGINAL_TOPIC + ".dlq";
   
       /**
        * 处理 Kafka DLQ 中的失败消息
        */
       @KafkaListener(topics = DLQ, groupId = "downstreamGrp")
       public void logDlq(Object failedMessage) {
           // 打印失败消息
           logMessage((ConsumerRecord) failedMessage, false);
       }
   
       private static void logMessage(ConsumerRecord<byte[], byte[]> message, boolean isPrintStacktrace) {
           try {
               RecordHeaders headers = (RecordHeaders) message.headers();
               String payload = new String(message.value(), Charset.defaultCharset());
               SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               String timestamp = formatter.format(new Date(message.timestamp()));
   
               StringBuffer strBuffer = new StringBuffer();
               strBuffer.append("\nMessage Properties:\n");
               strBuffer.append('\t').append("messageTopic").append(" : ").append(message.topic()).append("\n");
               strBuffer.append('\t').append("timestamp").append(" : ").append(timestamp).append("\n");
   
               strBuffer.append("Message Headers:\n");
               Iterator<Header> iterable = headers.iterator();
               while(iterable.hasNext()) {
                   Header header = iterable.next();
                   if (header.key().equals("x-exception-stacktrace") && !isPrintStacktrace) {
                       continue;
                   }
                   strBuffer.append('\t').append(header.key()).append(" : ").append(header.value()).append("\n");
               }
   
               strBuffer.append("Message Payload:\n\t" + payload + "\n");
   
               log.info("\n\n=== DEAD LETTER INFO ===\n{}", strBuffer.toString());
           } catch (Exception e) {
               log.error("Print Message info error:{}", e.getMessage());
           }
       }
   }
   ```
   
   &emsp;&emsp;读取 DLQ 中的死信，打印死信内容，实际应用中可以在此进行死信的补偿操作。
   
   
   
4. 自定义异常处理类

   ```java
   @Configuration
   public class ErrorConsumer {
       @Bean
       public Consumer<ErrorMessage> errorMessageConsumer() {
           return message -> {
               log.info("=== Error: {} ===", message.getPayload().getCause().getMessage());
           };
       }
   }
   ```

   

5. 自定义重试配置类

   ```java
   @Configuration
   public class RetryableConfiguration {
   
       /**
        * 自定义重试策略
        *
        * <pre>
        * 自定义重试策略生效原理：{@link AbstractBinder#buildRetryTemplate(ConsumerProperties)}
        * </pre>
        */
       @StreamRetryTemplate
       public RetryTemplate myRetryTemplate() {
           RetryTemplate rt = new RetryTemplate();
           SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(2);
           ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
           backOffPolicy.setInitialInterval(1000);
           backOffPolicy.setMultiplier(2.0);
           backOffPolicy.setMaxInterval(10000);
           rt.setRetryPolicy(retryPolicy);
           rt.setBackOffPolicy(backOffPolicy);
           rt.registerListener(myRetryListener());
   
           return rt;
       }
       private RetryListener myRetryListener() {
           RetryListenerSupport retryListenerSupport = new RetryListenerSupport() {
               @Override
               public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                   String queue = (String) ((MessageHandlingException) throwable).getFailedMessage().getHeaders().get("amqp_consumerQueue");
                   log.error("\n\nSend message to {} error, retry {} times...\n", queue, context.getRetryCount());
               }
           };
           return retryListenerSupport;
       }
   }
   ```

   设置自定义重试监听器后，每当发生重试时将会打印重试消息。

   
