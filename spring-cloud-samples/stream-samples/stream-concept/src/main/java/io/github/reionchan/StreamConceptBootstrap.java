package io.github.reionchan;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationProperties;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.binding.*;
import org.springframework.cloud.stream.config.*;
import org.springframework.cloud.stream.function.BindableFunctionProxyFactory;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <pre>
 * Spring Cloud Stream 概念及抽象启动类
 *
 * ==================================================
 * SPRING CLOUD STREAM 概念
 * ==================================================
 *
 * Spring Cloud Stream 是一个用于构建与共享消息系统连接的高度可扩展的基于事件驱动的微服务框架。
 * 该框架提供灵活的可编程模型，该模型建立在已经建立的、熟悉的 Spring 习惯用法和最佳实践的基础上，
 * 包括对持久发布/订阅语义、消费者组和有状态分区的支持。
 *
 * Spring Cloud Stream 是一个更高层次的抽象，它核心思想是绑定和激活框架。它将一段用户提供的代码
 * 绑定到通过绑定器而暴露的源或目标数据，并且通过绑定器的不同实现来激活这段代码的执行。
 * 它复用 Spring Integration 模块来达到这个目的，但前者绝对不是后者的轻量级路由实现。
 * Spring Integration 主要目标是支持 EIP 模式来构建简单企业集成解决方案，同时保证关注点分离、
 * 从而达到可维护性、可测试性。
 *
 * 推荐阅读：
 *  <a href="https://spring.io/blog/2019/10/14/spring-cloud-stream-demystified-and-simplified">Spring Cloud Stream - demystified and simplified</a>
 *  <a href="https://spring.io/blog/2019/11/04/spring-cloud-stream-composed-functions-or-eip">Spring Cloud Stream - Composed Functions or EIP</a>
 *
 * ==================================================
 * SPRING CLOUD STREAM 核心组件抽象
 * ==================================================
 *
 * 1. 目标绑定器（Destination Binders）
 *      负责与外部消息系统的集成组件，接口 {@link Binder}
 *      该策略接口将应用接口绑定到一个逻辑名称，该名称用来标识消息的逻辑生产者或消费者，
 *      应用接口可以是队列、通道适配器、消息通道或者 Spring Bean 等等
 *
 *      抽象模板实现类 {@link AbstractMessageChannelBinder} 给消息中间件定义的抽象方法
 *
 *      - {@link AbstractMessageChannelBinder#createProducerMessageHandler(ProducerDestination, ProducerProperties, MessageChannel) MessageHandler createProducerMessageHandler()}
 *          调用此方法返回可以将消息发送给目标消息中间的 {@link MessageHandler} 实例
 *          由它订阅来自生产者 outputChannel 通道的消息，使消息发送到目标消息中间件
 *
 *      - {@link AbstractMessageChannelBinder#createConsumerEndpoint(ConsumerDestination, String, ConsumerProperties) MessageProducer createConsumerEndpoint()}
 *          调用此方法返回可以从目标消息中间件获取消息的 {@link MessageProducer} 实例
 *          将它的 outputChannel 输出通道设置成消费者 inputChannel 通道，
 *          使得来源于消息中间件的消息输送到消费者的 inputChannel
 *
 * 2. 生产者目标（Producer Destination）
 *      表示生产者所生产的消息要投递的目的地信息，接口 {@link ProducerDestination}
 *      由不同的消息中间件提供实现
 *
 * 3. 消费者目标（Consumer Destination）
 *      表示消费者所接收的消息的产生地点，接口 {@link ConsumerDestination}
 *      由不同的消息中间件提供实现
 *
 * 4. 目标绑定（Destination Bindings）
 *      承担外部消息系统与用户的应用代码（生产者、消费者）之间桥梁的角色，接口 {@link Binding}
 *      它表示输入输出（input/output）与连接目标绑定器的适配端点（Adapter Endpoint）的绑定，
 *      该目标绑定发生在生产者或消费者之上，消费者之上的绑定表示从适配端点到 input 输入的连接，
 *      反之，生产者之上的绑定表示从 output 输出到适配端点的连接。
 *
 * 5. 消息（Message）
 *      生产者、消费者通过目标绑定器进行沟通的标准数据结构，接口 {@link Message}
 *
 * ==================================================
 * 本样例项目说明
 * ==================================================
 *
 * 【环境依赖】
 *
 * 1. 引人 spring-boot-starter-actuator 激活 actuator 管理端点
 * 2. 引入 spring-boot-starter-web 开启 Web 支持
 * 3. 引入 spring-cloud-starter-stream-rabbit 激活 SCS
 *  该依赖会引入 spring-cloud-stream-binder-rabbit，它将引入：
 *  3.1 spring-integration-core
 *      Spring 系统集成核心模块，其中包含支持消息传递 API 及协议的 spring-messaging
 *  3.2 spring-integration-amqp
 *      Spring 系统集成对 AMQP 协议的支持
 *  3.3 spring-boot-starter-amqp
 *      Spring Boot 对 AMQP 协议的支持，且引入协议实现 spring-rabbit
 *  3.4 spring-cloud-stream
 *      基于事件驱动的高可用微服务框架，提供对数据处理的绑定与激活的高级别抽象
 *      借助 spring-integration-core 实现基于事件驱动的消息编程模型
 *      借助 spring-cloud-function-context 实现将流式数据（MQ-like）映射输入输出至处理函数
 *  3.5 spring-cloud-stream-binder-rabbit-core
 *      定义针对接口 Binder 的提供商实现 RabbitMQ 独有属性配置
 * 4. RabbitMQ 服务器
 *  按照 RabbitMQ  服务器，并配置 application.yaml 文件中以
 *      spring.rabbitmq.*
 *  为前缀的属性
 *
 * 【基本使用与配置】
 *
 * 1. 编写启动器 {@link StreamConceptBootstrap}
 *  1.1 定义消息生产者（Messaging Endpoint, output）
 *      {@link Consumer} 类型的函数式生产者 Bean
 *      {@link StreamConceptBootstrap#product() product()}
 *  1.2 定义消息消费者（Messaging Endpoint, input）
 *      {@link Supplier} 类型的函数式消费者 Bean
 *      {@link StreamConceptBootstrap#print() print()}
 *  1.3 定义消息转换器（Messaging Transformer, input&out）
 *      {@link Function} 类型的函数式转换器 Bean
 *      {@link StreamConceptBootstrap#timestamp() timestamp()}
 *
 *  消息流转如下图：
 *
 *                    output: demo-0
 *     product-out-0 ===============> timestamp-in-0
 *                                             ||
 *                                             ||
 *     print-in-0 <================= timestamp-out-0
 *                   input: demo-1
 *
 *  1.4 编写 main 测试方法
 *      打印出自动配置下 Spring Cloud Stream 抽象组件的默认实现及相关信息
 *      下面介绍其中重要的两个组件的实例化原理：
 *
 *    1. Binder 实例化原理
 *      1.1 {@link BinderTypeRegistry} 收集类路径中的配置将其转化为 {@link BinderType} Map 集合
 *       - 该注册器由 {@link BinderFactoryAutoConfiguration#binderTypeRegistry(ConfigurableApplicationContext) binderTypeRegistry(ctx)} 配置装载
 *       - 在类路径搜索 META-INF/spring.binders，它包含绑定器名称与相关配置类，例如：
 *          {@code
 *          rabbit:org.springframework.cloud.stream.binder.rabbit.config.RabbitBinderConfiguration
 *          }
 *         并将其封装成 {@link BinderType} 实例，并以 rabbit 为键值保存在其 Map 集合中
 *      1.2 {@link DefaultBinderFactory} 默认 {@link Binder} 绑定器的工厂类
 *       - 它在 {@link BindingServiceConfiguration#binderFactory(BinderTypeRegistry, BindingServiceProperties, ObjectProvider, BinderChildContextInitializer) binderFactory()} 配置装载
 *       - 构造时将 spring.cloud.stream.binders 配置信息收集构造成按绑定器名为键名
 *         键值为 {@link BinderConfiguration} 的 Map
 *       - 它定义同步方法 {@link DefaultBinderFactory#getBinder(String, Class) getBinder()} 来生成 {@link Binder} 实例
 *
 *    2. Binding 实例化原理
 *      绑定操作发生在外部消息系统与用户的应用代码（生产者、消费者）建立联系时，
 *      该操作是由 {@link BindingService} 调用 {@link Binder} 来具体实现。
 *      一旦将应用代码与 {@link Binder} 关联的输入、输出目标完成关联绑定操作，
 *      其绑定的细节信息将由 {@link Binding} 的实例来保存。
 *      而绑定操作发生的时机交由抽象绑定生命周期类 {@link InputBindingLifecycle}、{@link OutputBindingLifecycle}
 *
 *      - {@link BindingService} 在 {@link BindingServiceConfiguration#bindingService(BindingServiceProperties, BinderFactory, TaskScheduler, ObjectMapper) bindingService()} 装载
 *
 *      - {@link InputBindingLifecycle}、{@link OutputBindingLifecycle} 分别在 {@link BindingServiceConfiguration#outputBindingLifecycle(BindingService, Map) outputBindingLifecycle()}、
 *        {@link BindingServiceConfiguration#inputBindingLifecycle(BindingService, Map) inputBindingLifecycle()} 中装配
 *
 *      - 绑定操作发生在生命周期方法 {@link AbstractBindingLifecycle#start()} 中，
 *        它收集容器中类型为 {@link Bindable} 的所有 Bean，依次执行 {@link AbstractBindingLifecycle#doStartWithBindable(Bindable) doStartWithBindable()} 方法，
 *        该抽象方法在 {@link InputBindingLifecycle}、{@link OutputBindingLifecycle} 有对应的实现，
 *        它们最终将绑定操作委托给：
 *          {@link AbstractBindableProxyFactory#createAndBindInputs(BindingService) createAndBindInputs(BindingService)}
 *          {@link AbstractBindableProxyFactory#createAndBindOutputs(BindingService) createAndBindOutputs(BindingService)}
 *        进行绑定，生成 {@link Binding} 实例，可被绑定组件有：
 *        - {@link BindableProxyFactory} 创建要绑定到类的 FactoryBean
 *        - {@link BindableFunctionProxyFactory} 创建要绑定到函数的 FactoryBean
 *        - {@link DynamicDestinationsBindable} 动态目标名称的可绑定组件
 *
 *      - 函数式绑定是由 {@link FunctionConfiguration#functionBindingRegistrar(Environment, FunctionCatalog, StreamFunctionProperties) functionBindingRegistrar()} 定义的初始化 Bean
 *        的生命周期方法 {@link FunctionConfiguration.FunctionBindingRegistrar#afterPropertiesSet() FunctionBindingRegistrar#afterPropertiesSet()} 中将配置文件中
 *        定义的函数方法名所对应的函数注册成为 {@link BindableFunctionProxyFactory} 类型的工厂 Bean
 *
 *      - 函数到目标的绑定是在 {@link FunctionConfiguration#functionInitializer(FunctionCatalog, StreamFunctionProperties, BindingServiceProperties, ConfigurableApplicationContext, StreamBridge) functionInitializer()} 装配的 Bean
 *        的生命周期方法 {@link FunctionConfiguration.FunctionToDestinationBinder#afterPropertiesSet() FunctionToDestinationBinder#afterPropertiesSet()} 执行，
 *        它将 {@link FunctionConfiguration.FunctionBindingRegistrar FunctionBindingRegistrar} 注册的所有 BindableProxyFactory
 *        类型的 Bean 经过方法 {@link FunctionConfiguration.FunctionToDestinationBinder#bindFunctionToDestinations(BindableProxyFactory, String, ConfigurableEnvironment) bindFunctionToDestinations()}
 *        绑定到配置文件中配置的输入输出目标（input/output destination）名称对应的消息通道。
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-10-14
 **/
@Slf4j
@SpringBootApplication
public class StreamConceptBootstrap {

    /**
     * 声明函数式的消费者
     */
    @Bean
    public Consumer<String> print() {
        return p -> log.info("=== Consumer received：\n{}", p);
    }

    /**
     * 声明函数式的转换器
     */
    @Bean
    public Function<String, String> timestamp() {
        return msg -> {
            long t = System.currentTimeMillis();
            log.info("=== Transformer ===\n{}", t);
            return t + ": " + msg;
        };
    }

    /**
     * 声明函数式的生产者
     *
     * <pre>
     * 该生产者需要借助 Spring Cloud Stream 内部的 Poller 定时，
     * 轮询拉取消息并发送到输出通道 Output Channel
     *
     * 当没有配置下面属性时，
     *  spring.cloud.stream.bindings.YOUR-BINDING-NAME.producer.poller.*
     *
     * 将获取容器中自动装载的默认配置，即：
     *  {@link IntegrationAutoConfiguration.IntegrationConfiguration#defaultPollerMetadata(IntegrationProperties)}
     *
     * 具体方法参考：
     *  {@link FunctionConfiguration#integrationFlowFromProvidedSupplier(Supplier, Publisher, PollableBean, GenericApplicationContext, TaskScheduler, ProducerProperties, String) integrationFlowFromProvidedSupplier()}
     *  {@code
     *      // 集成流的构建方法
     *      IntegrationFlow.fromSupplier(supplier, spca -> spca.id(bindingName + "_spca").poller(pollerMetadata.get()).autoStartup(autoStartup));
     *  }
     *  最终调用方法：
     *  {@link IntegrationFlow#from(MessageSource, Consumer, IntegrationFlowBuilder) from()}
     *  {@link SourcePollingChannelAdapterSpec}
     *  {@link SourcePollingChannelAdapterFactoryBean#initializeAdapter()}
     *  {@code
     *      // 将消息源封装到 SourcePollingChannelAdapterSpec，它来适配对源通道的轮询
     * 		SourcePollingChannelAdapterSpec spec = new SourcePollingChannelAdapterSpec(messageSource);
     * 		if (endpointConfigurer != null) {
     * 			endpointConfigurer.accept(spec);
     *      }
     * 		return integrationFlowBuilder.addComponent(spec)
     * 				.currentComponent(spec);
     *  }
     * </pre>
     */
    @Bean
    public Supplier<String> product() {
        String msg = "Hello Spring Cloud Stream!";
        return () -> {
            log.info("=== Producer send: ===\n{}", msg);
            return msg;
        };
    }

    /**
     * 声明函数式的生产者
     *
     * 该生产者由于基于流式编程，将切换成基于 {@link Publisher} 流机制，故无需轮询
     *
     * 参考方法：
     *  {@link FunctionConfiguration#integrationFlowFromProvidedSupplier(Supplier, Publisher, PollableBean, GenericApplicationContext, TaskScheduler, ProducerProperties, String) integrationFlowFromProvidedSupplier()}
     *  {@code
     *      integrationFlowBuilder = IntegrationFlow.from(publisher);
     *  }
     */
    @Bean
    public Supplier<Flux<String>> streamProduct() {
        return () -> Flux.interval(Duration.ofSeconds(5)).map(lo -> lo+"").log();
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        ConfigurableApplicationContext ctx = SpringApplication.run(StreamConceptBootstrap.class, args);

        /*=====================================================================
                       BINDER AUTOCONFIGURATION COMPONENTS
         =====================================================================*/

        // 1. BinderTypeRegistry 注册类路径中所有可用的 BinderType，用来生成外部消息系统的 Binder 实例
        //    扫描类路径下 META-INF/spring.binders 文件定义
        BinderTypeRegistry binderTypeRegistry = ctx.getBean(BinderTypeRegistry.class);
        StringBuffer strB = new StringBuffer();
        for (BinderType type : binderTypeRegistry.getAll().values()) {
            strB.append(type.getDefaultName()).append("\n");
        }
        log.info("\n\n=== Current Binder Names ===\n{}", strB.toString());

        // 2. BinderFactory 外部消息系统绑定器工厂，根据上面收集的绑定器类型注册器生成具体的 Binder 实例
        //    同时将缓存已生成的 Binder 实例及它对应的子上下文 Map 对象：binderInstanceCache
        //    还包含 Binder 名称及其对应的 BinderConfiguration 的 Map 对象：binderConfigurations
        BinderFactory binderFactory = ctx.getBean(BinderFactory.class);
        log.info("\n\n=== BinderFactory type ===\n{}", binderFactory.getClass().getName());
        // 反射获取绑定器名称及类型
        if (binderFactory instanceof DefaultBinderFactory defaultBinderFactory) {
            Field binderCacheMap = defaultBinderFactory.getClass().getDeclaredField("binderInstanceCache");
            binderCacheMap.setAccessible(true);
            Map<String, Map.Entry<Binder<?, ?, ?>, ConfigurableApplicationContext>> map = (Map<String,
                    Map.Entry<Binder<?, ?, ?>, ConfigurableApplicationContext>>) binderCacheMap.get(defaultBinderFactory);
            for(Map.Entry<String, Map.Entry<Binder<?, ?, ?>, ConfigurableApplicationContext>> ent : map.entrySet()) {
                log.info("\n=== Binders ===\nBinder Name:{}\nBinder Class:{}", ent.getKey(), ent.getValue().getKey().getClass().getName());
            }
        }

       /*=====================================================================
                                BINDING COMPONENTS
        =====================================================================*/

        // 3. BindingService 绑定代理，用来将可绑定的组件（接口、函数式方法）绑定到其代理的 Binder 实例上
        //    而输入、输出生命周期类（InputBindingLifecycle、OutputBindingLifecycle）引用该绑定代理
        //    在它们生命周期方法中，会将容器中所有声明的 Bindable 可绑定的组件（接口、函数式方法交由 BindingService
        //    进行与 Binder 的绑定，并将绑定后的 Binding 信息保存在其属性集合（inputBindings、outputBindings）
        //    而输入、输出生命周期类所收集的 Bindings 的生命周期将交由 BindingsLifecycleController 来统一控制
        //    而该控制器将被交由 BindingsEndpoint （Spring Boot Actuator 控制端点：/bindings）来进行调用
        BindingService bindingService = ctx.getBean(BindingService.class);
        log.info("\n\n=== BindingService ===\nConsumer Binding Names: {}\nProducer Binding Names: {}",
                bindingService.getConsumerBindingNames(),
                bindingService.getProducerBindingNames());
        // 以 spring.cloud.stream 为前缀的属性类
        BindingServiceProperties bindingServiceProperties = bindingService.getBindingServiceProperties();
        // 将 spring.cloud.stream 前缀配置汇总成一个 Map
        strB.delete(0, strB.length());
        Map<String, Object> mapProperties = bindingServiceProperties.asMapProperties();
        for (Map.Entry<String, Object> ent : mapProperties.entrySet()) {
            strB.append(ent.getKey()).append(" --> ").append(ent.getValue()).append('\n');
        }
        log.info("\n\n=== spring.cloud.stream ===\n{}", strB.toString());

        // 以 spring.cloud.stream.binders 为前缀的绑定器 binder 实例配置，键值为绑定器名称
        Map<String, BinderProperties> binders = bindingServiceProperties.getBinders();
        strB.delete(0, strB.length());
        for (Map.Entry<String, BinderProperties> ent : binders.entrySet()) {
            strB.append(ent.getKey()).append(" --> ").append(ent.getValue()).append('\n');
        }
        log.info("\n\n=== spring.cloud.stream.binders ===\n{}", strB.toString());

        // 以 spring.cloud.stream.bindings 为前缀的绑定 binding 实例配置，键值为绑定名称（bean 名称、函数名-[input|output]-index）
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();
        strB.delete(0, strB.length());
        for (Map.Entry<String, BindingProperties> ent : bindings.entrySet()) {
            strB.append(ent.getKey()).append(" --> ").append(ent.getValue()).append('\n');
        }
        log.info("\n\n=== spring.cloud.stream.bindings ===\n{}", strB.toString());

        // 4. 生产者绑定
        strB.delete(0, strB.length());
        strB.append("\n\n=== Producer Bindings ===\n");
        for(String name : bindingService.getProducerBindingNames()) {
            Binding<?> tmpBinding = bindingService.getProducerBinding(name);
            getBindingInfo(strB, tmpBinding);
        }
        // 5. 消费者绑定
        strB.append("\n=== Consumer Bindings ===\n");
        for(String name : bindingService.getConsumerBindingNames()) {
            List<Binding<?>> bindingList = bindingService.getConsumerBindings(name);
            for(Binding<?> tmpBinding: bindingList) {
                getBindingInfo(strB, tmpBinding);
            }
        }
        log.info(strB.toString());
    }
    private static void getBindingInfo(StringBuffer strB, Binding<?> tmpBinding) throws NoSuchFieldException, IllegalAccessException {
        strB.append("BindingTargetName: " + tmpBinding.getBindingName() + "\nBindingType: " + tmpBinding.getClass().getName() + "\n");
        if(tmpBinding instanceof DefaultBinding<?> defaultBinding) {
            strB.append("BindingInfo: " + "\n");
            strB.append("\tBindingDestinationName : " + defaultBinding.getName() + "\n");
            strB.append("\tBindingGroupName: " + defaultBinding.getGroup() + "\n");
            Field target = DefaultBinding.class.getDeclaredField("target");
            target.setAccessible(true);
            strB.append("\tBindingTargetClass: " + target.get(defaultBinding).getClass().getName() + "\n");
        } else {
            strB.append("BindingInfo: " + tmpBinding + "\n");
        }
    }
}
