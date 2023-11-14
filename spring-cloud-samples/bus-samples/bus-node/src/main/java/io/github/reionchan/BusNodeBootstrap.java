package io.github.reionchan;

import io.github.reionchan.config.OrderProperties;
import io.github.reionchan.controller.OrderController;
import io.github.reionchan.event.NotificationRemoteApplicationEvent;
import io.github.reionchan.listener.SentApplicationEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Cloud Bus 普通节点启动器
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
 *
 * === 基本使用 ===
 *
 * 此为总线普通节点，它接收来自 bus-manager 推送的总线消息，进行必要的应答。
 * 本例中，订单创建功能 {@link OrderController}，接受管理节点的启用禁用消息命令。
 * 本例中，总线消息监听器 {@link SentApplicationEventListener} 是否对总线消息进行 log 记录，
 * 同样接受管理节点的启用禁用消息命令，详细使用参考 bus-manager 启动器 {@code BusManagerBootstrap} 注解描述
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@SpringBootApplication
@RemoteApplicationEventScan(basePackageClasses = NotificationRemoteApplicationEvent.class)
public class BusNodeBootstrap {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(BusNodeBootstrap.class, args);
        ConfigurableEnvironment env = ctx.getEnvironment();

        // 打印 Spring Cloud Bus 相关信息
        BusProperties busProperties = ctx.getBean(BusProperties.class);
        log.info("\n\n=== Spring Cloud Bus Info ===\nID:\t{}\nStream Destination:\t{}\n",
                busProperties.getId(), busProperties.getDestination());

        // 创建订单启用属性默认值
        OrderProperties orderProperties = ctx.getBean(OrderProperties.class);
        log.info("\n\norder.createEnabled={}\n", orderProperties.isCreateEnabled());
    }
}
