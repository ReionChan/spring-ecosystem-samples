package io.github.reionchan;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reionchan.event.NotificationRemoteApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.*;
import org.springframework.cloud.bus.event.*;
import org.springframework.cloud.bus.jackson.BusJacksonAutoConfiguration;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventRegistrar;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.AntPathMatcher;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spring Cloud Bus 管理节点启动器
 *
 * <pre>
 * === 环境依赖 ===
 * 1. 引人 spring-boot-starter-actuator 激活 actuator 管理端点
 * 2. 引入 spring-boot-starter-web 开启 Web 支持
 * 3. 引入 spring-cloud-starter-bus-amqp 开启 Spring Cloud Bus 支持
 *  该 Starter 将会配置引入下面的依赖
 *  3.1 引入 spring-cloud-starter-stream-rabbit 开启 Spring Cloud Stream 支持
 *      由此看出，Spring Cloud Bus 是基于 Spring Cloud Stream 实现的
 *  3.2 引入 spring-cloud-bus Spring Cloud Bus 支持核心包
 *      它依赖 spring-integration-core Spring Integration 核心包
 * 4. RabbitMQ 服务器
 *    安装 RabbitMQ  服务器，并配置 application.yaml 文件中以
 *        spring.rabbitmq.*
 *    为前缀的属性
 * 5. 开启 Spring Boot Actuator 总线管理端点
 *    在 application.yaml 文件中添加
 *      management:endpoints.web.exposure.include=busrefresh,busenv
*     支持对总线相关的端点进行刷新和环境变量的修改
 *
 * === 默认组件 ===
 * 1. {@link BusProperties} 总线默认属性初始化（SCSt 消息总线绑定配置等）
 *  原理：{@link BusEnvironmentPostProcessor}
 *  在这些默认生成的总线属性中，总线 ID 即为当前应用在总线中的唯一标识，默认命名规则参考：
 *      {@link BusEnvironmentPostProcessor#postProcessEnvironment(ConfigurableEnvironment, SpringApplication) postProcessEnvironment()}
 *  本地环境命名规则：
 *      spring.application.name + ":" + server.port + ":" + RANDOM_UUID
 *  总线应用名称寻址匹配默认采取 {@link DefaultBusPathMatcher}，自动装配：{@link PathServiceMatcherAutoConfiguration#busPathMatcher() busPathMatcher()}
 *  其底层采取以 ":" 分隔的 Ant 风格的匹配器 {@link AntPathMatcher}，例如：
 *      表达式：bus-node:**       它表示匹配 bus-node 应用下的所有服务
 *      表达式：bus-node:8081:**  它表示匹配端口为 8081 的所有 bus-node 应用服务
 *
 * 2. {@link RemoteApplicationEventListener} 总线消息生产者（负责向总线发送消息）
 *  自动装配：{@link BusAutoConfiguration#busRemoteApplicationEventListener(ServiceMatcher, BusBridge) busRemoteApplicationEventListener()}
 *  向总线发送事件消息，都交给 {@link BusBridge}
 *
 * 3. {@link BusConsumer} 总线消息函数式消费者（负责从总线消费消息）
 *  自动装配：{@link BusAutoConfiguration#busConsumer(ApplicationEventPublisher, ServiceMatcher, ObjectProvider, BusProperties, Destination.Factory) busConsumer()}
 *
 * === 基本使用 ===
 * 1. 总线刷新端点
 *    该功能主要是通过向总线发出 {@link RefreshRemoteApplicationEvent} 事件，
 *    而后被目标应用总线消费者 {@link BusConsumer} 处理，再由它关联的应用内部事件广播器广播该事件，
 *    随后应用本地注册的监听器 {@link RefreshListener} 执行如下刷新操作：
 *    {@code
 *      this.contextRefresher.refresh()
 *    }
 *    从而对当前应用环境环境（Environment）、RefreshScope 刷新重新加载，
 *    至于环境刷新、RefreshScope 刷新原理，参考样例模块：3.1.1-Cloud Application Context
 *
 *    1.1 刷新总线上的所有节点：
 *      curl -X POST http://localhost:8080/actuator/busrefresh
 *    1.2 刷新匹配节点，如下刷新所有 bus-node 应用服务：
 *      curl -X POST http://localhost:8080/actuator/busrefresh/bus-node
 *
 * 2. 总线环境变量修改端点
 *    该功能主要是通过向总线发出 {@link EnvironmentChangeRemoteApplicationEvent}
 *    而后被目标应用总线消费者 {@link BusConsumer} 处理，再由它关联的应用内部事件广播器广播该事件，
 *    随后应用本地注册监听器 {@link EnvironmentChangeListener} 执行如下属性修改操作：
 *    {@code
 *      this.env.setProperty(entry.getKey(), entry.getValue());
 *    }
 *    触发 {@link EnvironmentChangeEvent} 事件，从而对当前的 {@link ConfigurationPropertiesBeans} 重新绑定，
 *    重新绑定原理参考样例模块：3.1.1-Cloud Application Context
 *
 *    2.1 启用所有 bus-node 应用服务的总线追踪功能，使监听 {@link SentApplicationEvent} 事件的自定义监听器生效：
 *      curl -X POST http://localhost:8080/actuator/busenv/bus-node \
 *          -H 'Content-Type: application/json' \
 *          -d '{"name":"spring.cloud.bus.trace.enabled", "value":true}'
 *    2.2 启用端口为 8081 的所有 bus-node 应用服务的订单创建功能，默认创建功能关闭：
 *      curl -X POST http://localhost:8080/actuator/busenv/bus-node:8081 \
 *          -H 'Content-Type: application/json' \
 *          -d '{"name":"order.createEnabled", "value":true}'
 *
 *      管理节点发出开启命令后，bus-node 开启订单创建功能后，下面请求将创建成功：
 *      curl -X POST http://localhost:8081/order/create -d 'name=car'
 *
 * 3. 自定义总线事件 {@link NotificationRemoteApplicationEvent}
 *    该功能主要通过函数式 Web 端点 {@link BusManagerBootstrap#pushNotification(ApplicationContext, BusProperties) pushNotification()} 向总线推送
 *    自定义 {@link NotificationRemoteApplicationEvent} 事件，从而广播通知 {@link NotificationRemoteApplicationEvent.Notification Notification}
 *    给所有节点。
 *
 *    由于总线消息收发需要进行序列化与反序列化，默认自动装配的 {@link BusJacksonAutoConfiguration#busJsonConverter(ObjectMapper) busJsonConverter()}
 *    它只搜索 classpath 中 "org.springframework.cloud.bus.event" 包下的事件类，
 *    并将其注册到 {@link ObjectMapper}，因此自定义的总线事件要么放在该包名路径下，
 *    要么在配置类上使用注解 {@link RemoteApplicationEventScan @RemoteApplicationEventScan} 生成包含自定义事件类包路径的
 *    {@link AbstractMessageConverter} 实现类 BusJacksonMessageConverter
 *    原理参考：{@link RemoteApplicationEventRegistrar#registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry) registerBeanDefinitions()}
 *
 *    本例中，将 {@link RemoteApplicationEventScan @RemoteApplicationEventScan} 注解到启动类 {@link BusManagerBootstrap}
 *    使用 basePackageClasses 指定自定义事件类包路径，从而顺利加载该类。
 *    注意：自定义总线事件类需被总线中所有节点以同样的方式注册加载，
 *         实际项目中，最好提取成为公共包，并被所有应用引用。
 *
 *    启动项目后，通过如下方式向总线推送自定义总线通知事件：
 *    curl -H "Content-Type: text/plain"  http://localhost:8080/pushNotification -d "Notification Message"
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@SpringBootApplication
@RemoteApplicationEventScan(basePackageClasses = NotificationRemoteApplicationEvent.class)
public class BusManagerBootstrap {

    /**
     * 函数式 Web 端点，使用如下方式访问：
     *  curl -H "Content-Type: text/plain"  http://localhost:8080/pushNotification -d "This is a message!"
     */
    @Bean
    public Consumer<String> pushNotification(ApplicationContext ctx, BusProperties busProperties) {
        return msg -> {
            // 发布自定义总线事件给所有节点
            NotificationRemoteApplicationEvent.Notification noti = new NotificationRemoteApplicationEvent.Notification();
            noti.setId(UUID.randomUUID().toString());
            noti.setTimestamp(System.currentTimeMillis());
            noti.setMessage(msg);
            Destination.Factory destinationFac = ctx.getBean(Destination.Factory.class);
            ctx.publishEvent(new NotificationRemoteApplicationEvent(ctx, busProperties.getId(), destinationFac.getDestination(null), noti));
        };
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(BusManagerBootstrap.class, args);
        BusProperties busProperties = ctx.getBean(BusProperties.class);
        // 打印 Spring Cloud Bus 相关信息
        log.info("\n\n=== Spring Cloud Bus Info ===\nID:\t{}\nStream Destination:\t{}\n",
                busProperties.getId(), busProperties.getDestination());

    }
}
