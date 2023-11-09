# Spring Cloud Stream with RabbitMQ Binder

## 项目介绍

* rabbit-producer 消息生产者（上游服务）
  * 应用名称：rabbit-producer
  * 应用端口：8080
* rabbit-consumer 消息消费者（下游服务）
  * 应用名称：rabbit-consumer
  * 应用端口：8081
* RabbitMQ 消息服务
  * 端口：5672
  * 账户及密码：guest/guest

## 应用交互

```txt

  PRODUCER                              RABBITMQ                            CONSUMER
╔═══════════╗                        ╔═══════════╗                   ╔═══════════════════════════╗
║           ║   === OUTPUT ===>      ║           ║  === INPUT ===>   ║╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮    ║
║           ╚════════════════════════╝           ╚═══════════════════╝╯consumer1InCreateGrp ┆    ║
║                              order.create.msg.createGrp                                   ┆    ║
║           ╔════════════════════════╗           ╔═══════════════════╗╮consumer1InCreateGrp ┆    ║
║           ║                        ║           ║                   ║╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯    ║
║           ║                        ║           ║                   ║       createGrp           ║
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
║ functionRouter ---[routeKey]---> routed.msg.downstreamGrp ---╯                      ┆          ║
║                                          ┆                         [routeKey] <-----╯          ║
║                                          ┆ DLX   ╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮<╯ ╰>╭┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╮  ║
║                                          ┆       ┆evenRoutedConsumer┆     ┆oddRoutedConsumer┆  ║
║                                          ↓       ╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯     ╰┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈╯  ║
║                            routed.msg.downstreamGrp.dlq ------------------╮  ╭┈┈┈┈┈┈┈┈╮        ║
║           ╔════════════════════════╗           ╔═══════════════════╗      ╰> ┆ logDlq ┆        ║
║           ║                        ║           ║                   ║         ╰┈┈┈┈┈┈┈┈╯        ║
╚═══════════╝                        ╚═══════════╝                   ╚═══════════════════════════╝

                                Graph-1 Message Interactive @Reion
```



### 消息分组

#### 生产者端（rabbit-producer）

1. 配置

   ```yaml
   spring:
     cloud:
       stream:
         bindings:
           # 分组输出绑定名
           groupOut:
             # 绑定目标地址
             destination: order.create.msg
             producer:
               # 支持多个分组，同一消息将会路由到所有分组
               # 同组的多个消费者共享同组的消息，一条消息只被该消费分组的一个消费者获取
               # 同一消息路由到不同分组，将被不同分组的消费者用来实现不同功能
               # 例如：此订单创建分组外，再设置一个 createLogGrp 分组
               #      除创建订单的消费者消费消息来创建订单外，另一分组获得该消息后只用作记录日志
               required-groups:
                 - createGrp
                 #- createLogGrp
   ```

   &emsp;&emsp; 就 RabbitMQ  而言，此配置将会创建出名为 `order.create.msg` 的 **Exchange 交换机**，并且将名为 `order.create.msg.createGrp` 的 **Queue 队列** 绑定到该交换机。生产类通过向绑定名为 `groupOut` 的 **Binding 绑定** 发送消息，将被交换机路由到绑定的队列中。默认情况下，交换机为 Topic 类型，绑定到该交换机的队列的 **BindingKey 绑定键** 为 **#**，即通配所有 **RoutingKey 路由键**。

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/exchanges-group.png?raw=true)

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/queue-group.png?raw=true)

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/binding-group.png?raw=true)

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

#### 消费者端（rabbit-consumer）

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
           # --- 相同分组的两个消费者 ---
           # 它们共享 createGrp 分组内的消息，默认是轮询方式轮流交替获得消息
           orderCreate1In:
             destination: order.create.msg
             group: createGrp
           orderCreate2In:
             destination: order.create.msg
             group: createGrp
   ```

   &emsp;&emsp; 这里配置在同一分组 `createGrp` 中的两个消费者：`orderCreate1In`、`orderCreate2In`, 它们对应下面消费类中的两个 **Consumer** 函数 Bean。它们消费来自于 `order.create.msg` 地址的消息。 值得注意的是，函数式消费者默认绑定名称格式为：`functionName-in-index`，为了更简洁的使用，使用了 `spring.cloud.stream.function.bindings` 属性配置对函数式的绑定进行别名设置。

   

2. 消费类

   ```java
   @Configuration
   public class GroupedConsumer {
   
       /**
        * createGrp 分组消费者 1，接收订单创建消息
        */
       @Bean
       public Consumer<String> consumer1InCreateGrp() {
           return name -> log.info("Consumer one receive a order, name is : {}", name);
       }
   
       /**
        * createGrp 分组消费者 2，接收订单创建消息
        */
       @Bean
       public Consumer<String> consumer2InCreateGrp() {
           return name -> log.info("Consumer two receive a order, name is : {}", name);
       }
   }
   ```

   这两个消费者默认将轮流消费队列中的消息，即它们共享同一个队列中消息。

### 消息分区

#### 生产者端（rabbit-producer）

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

   此处配置名称为 `partOut` 的绑定，它绑定名称为 `partitioned.msg` 的 **Exchange 交换机**。在生产者属性中设置 2 个分区，分区 SpEL 表达式：`headers['partitionKey'] % 2`，即根据消息头中的 `partitioniKey` 与 2 取模运算的结果当做分区索引。指定消费者需要属于 `partGrp` 分组才能消费分区队列中的消息。

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/exchanges-partition.png?raw=true)

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/queue-partition.png?raw=true)

   &emsp;&emsp; 生成两个分区队列，默认命名规则为：`exchangeName.groupName-index`，即分区交换机名.组名-分区索引。

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/binding-partition.png?raw=true)

   &emsp;&emsp;交换机下绑定两个分区队列，它们的路由键默认规则为：`exchangeName-index`，即分区交换机名.分区索引。

   

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

   

#### 消费者端（rabbit-consumer）

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
           # --- 分区形式的消费者 ---
           
           # 消费者1：消息长度为偶数的分区消费者
           evenPart1In:
             destination: partitioned.msg
             group: partGrp
             consumer:
               # 启用分区
               partitioned: true
               # 分区实例索引
               instance-index: 0
               
           # 消费者2：消息长度为奇数的分区消费者    
           oddPart1In:
             destination: partitioned.msg
             group: partGrp
             consumer:
               partitioned: true
               instance-index: 1
   ```

   &emsp;&emsp;定义两个分区消费者，它们分别消费消息长度为奇数、偶数的消息。注意消费属性配置 `partitioned: true` 将使得消费者成为分区消费者，而 `instance-index` 所设置的值将决定它所消费的目标分区（交换机的路由键会使用分区索引构建，而该索引与该实例索引对应）。

   

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

#### 生产者端（rabbit-producer）

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
   
         # === RabbitMQ 定制化配置 ===
         rabbit:
           bindings:
             groupOut:
               # Rabbit 生产者可定制属性，参考：RabbitProducerProperties
               producer:
                 # 绑定交换机类型，默认 topic
                 exchange-type: topic
             routeOut:
               producer:
                 # 自动绑定 DLQ，生产方与消费方需要配置一致
                 auto-bind-dlq: true
   ```

   &emsp;&emsp; 此处通过属性 `spring.cloud.function.routing-expression` 激活 Spring Cloud Stream 内置的 **RoutingFunction** 具备网关或路由特性的函数式 Bean，它的其中一项能力就是将这个路由表达式解析后的返回值当做路由键值，从而将消息路由到与路由键匹配的消费者。此处的路由表达式：`headers['routeNum'] % 2 == 0 ? 'even2RouteOut' : 'odd2RouteOut'`，即解析消息头中的 `routeNum` 字段值的奇偶性动态路由到 `even2RouteOut` 或 `odd2RouteOut`, 而这两个字符串是定义在容器中的函数式消费者的 Bean 名称，由它即可关联对应的函数处理消息。

   &emsp;&emsp;在本示例中，经过路由的消息最终被路由到 `even2RouteOut` 或 `odd2RouteOut` 本地函数式消费者，由它们将消息通过 `routeOut` 输出绑定发送到名为 `routed.msg` 的交换机，从而使得下游加入到 `downstreamGrp` 分组的消费者接收该消息。至于消息则是由下面的 **Controller** 请求产生后直接交给容器中的 **RoutingFunction** 进行路由。

   &emsp;&emsp;此外，消息在上游发送到下游所关联的交换机 `routed.msg` 增加了对 Dead Letter（死信，无法路由的消息） 的处理，而 RabbitMQ 对死信的一种方式是将它路由到与交换机绑定的 **DLQ（死信队列）**中。该特性属于特定中间件独有的配置，Spring Cloud Stream 目前官方支持的两个消息中间件 RabbitMQ、Kafka 分别在属性 `spring.cloud.stream.rabbit`、`spring.cloud.stream.kafka` 中进行配置。此处激活自动绑定死信队列的配置由 `auto-bind-dlq: true` 来实现。值得注意的是，下游与该交换机绑定的消费者也同样需要设置改属性，否则生产者消费者所关联的交换机属性配置不一致将会导致异常。

   

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/exchanges-route.png?raw=true)

   &emsp;&emsp;交换器 `routed.msg` 有关联的 处理死信的交换器 `DLX`，而名称为 `functionRouter-in-0` 的交换机是由上游激活的 **RoutingFunction** 的输入参数绑定的交换器，名称为 `gatewayfunctionRouter-out-0` 的交换器则是下游激活的 **RoutingFunction** 与 `gateway` 复合函数的输出绑定的交换器。

   

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/queue-route.png?raw=true)

   &emsp;&emsp;队列 `routed.msg.downstreamGrp` 是一个具备 DLX（死信交换机）、DLK（死信路由键）特性的队列，而 `routed.msg.downstreamGrp.dlq` 则是与 `routed.msg` 交换机关联的死信交换机 `DLX`  所绑定的队列，`routed.msg.downstreamGrp.parkingLot` 则是自定义处理死信队列的消费者消费完成后最终死信所保存的位置。

   

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/binding-route.png?raw=true)

   `routed.msg` 交换机的队列绑定关系。

   

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/binding-route-dlq.png?raw=true)

   `DLX` 交换机的队列绑定关系，可以看到该交换机被绑定的队列为正常消费队列后缀 **.dlq** 来命名，绑定的路由键即为交换机的全名。

   

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

   

#### 消费者端（rabbit-consumer）

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
   
         # === RabbitMQ 定制化配置 ===
         rabbit:
           bindings:
             routeIn:
               # Rabbit 消费者可定制属性，参考：RabbitConsumerProperties
               consumer:
                 # 自动绑定 DLQ，生产方与消费方需要配置一致
                 auto-bind-dlq: true
   ```

   &emsp;&emsp;首先，该下游消费者也通过 `routing-expression` 属性配置激活 **RoutingFunction** 具备网关路由功能的函数式 Bean，它解析传递给它的消息中的消息头的 `routeKey` 的值，动态将消息路由到值对应的函数式 Bean 名称的方法处理。值得注意的是，该网关路由函数式 Bean 接收的消息不再像上游的生产端那样，直接通过调用它的消息处理方法，而是通过复合函数的形式 `gateway|functionRouter` 进行定义，即：该路由函数接收 `gateway` 函数的输出消息。

   &emsp;&emsp;其次，该复合函数的输入绑定到上游具备 DLQ 特性的交换机 `routed.msg` 上，从而接收来自上游的消息，并最后通过路由表达式 `headers['routeKey']` 提取消息头中的 `routeKey` 的值，将消息路由到该值对应的下游 Bean 名称相匹配的函数式消费者处理，本例中为：``、``两个函数式 Bean，参考下面消费类的代码定义。

   &emsp;&emsp;此外，在本复合函数的 `gateway` 函数中，模拟消息处理异常，来验证常见的两种异常处理配置：

   - 自定义错误处理器

     该方法需在绑定配置中通过 `error-handler-definition` 设置自定义异常消息处理器 Bean 的名称，如本例中所配置的 `errorMessageConsumer`。

   - 利用 DLQ 机制

     该机制需在 RabbitMQ  定制化属性配置中，将消费者关联的交换机启用自动绑定 DLQ 的属性 `auto-bind-dlq: true`

   &emsp;&emsp;注意，这两种异常处理是互斥的，两个都配置时，只有一种会生效。

   &emsp;&emsp;最后，消费出现异常时默认会开启 3 次重试操作，如果想针对具体消费者单独定制重试配置，可以在其绑定设置的消费配置中设置

    `retry-template-name` 属性，它的值是被注解 **@StreamRetryTemplate** 修饰的自定义 **RetryTemplate** Bean 的名称，通过它可以定制重试次数、重试策略、注册自定义重试监听器等操作。

   

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
   
       private static final String ORIGINAL_QUEUE = "routed.msg.downstreamGrp";
       private static final String DLQ = ORIGINAL_QUEUE + ".dlq";
       private static final String PARKING_LOT = ORIGINAL_QUEUE + ".parkingLot";
   
       @Resource
       private RabbitTemplate rabbitTemplate;
   
       /**
        * 处理 RabbitMQ DLQ 中的失败消息
        */
       @RabbitListener(queues = DLQ)
       public void logDlq(Message failedMessage) {
           // 打印失败消息
           logMessage(failedMessage, false);
           // 将消息放入 parkingLot 队列
           this.rabbitTemplate.send(PARKING_LOT, failedMessage);
       }
   
       @Bean
       public Queue parkingLot() {
           return new Queue(PARKING_LOT);
       }
   
       private static void logMessage(Message message, boolean isPrintStacktrace) {
           try {
               Map<String, Object> headers = message.getMessageProperties().getHeaders();
               String payload = new String(message.getBody(), Charset.defaultCharset());
               SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               String timestamp = formatter.format(message.getMessageProperties().getTimestamp());
   
               StringBuffer strBuffer = new StringBuffer();
               strBuffer.append("\nMessage Properties:\n");
               strBuffer.append('\t').append("messageId").append(" : ").append(message.getMessageProperties().getMessageId()).append("\n");
               strBuffer.append('\t').append("timestamp").append(" : ").append(timestamp).append("\n");
               strBuffer.append('\t').append("priority").append(" : ").append(message.getMessageProperties().getPriority()).append("\n");
   
               strBuffer.append("Message Headers:\n");
               for (String key : headers.keySet()) {
                   if (key.equals("x-exception-stacktrace") && !isPrintStacktrace) {
                       continue;
                   }
                   strBuffer.append('\t').append(key).append(" : ").append(headers.get(key)).append("\n");
               }
               strBuffer.append("Message Payload:\n\t" + payload + "\n");
   
               log.info("\n\n=== DEAD LETTER INFO ===\n{}", strBuffer.toString());
           } catch (Exception e) {
               log.error("Print Message info error:{}", e.getMessage());
           }
       }
   }
   ```

   &emsp;&emsp;读取 DLQ 中的死信，打印死信内容后将其转移到 `routed.msg.downstreamGrp.parkingLog` 队列中。

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/dlq-msg.png?raw=true)

   

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

   设置自定义重试监听器后，每当发生重试时将会打印如下消息：

   ![](https://github.com/ReionChan/spring-ecosystem-samples/blob/main/spring-cloud-samples/stream-samples/stream-rabbit/img/retry-log.png?raw=true)
