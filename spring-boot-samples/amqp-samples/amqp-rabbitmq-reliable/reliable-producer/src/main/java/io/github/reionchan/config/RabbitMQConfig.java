package io.github.reionchan.config;

import io.github.reionchan.callback.RabbitMQCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.reionchan.consts.RabbitMQConst.*;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange orderDirectExchange() {
        return new DirectExchange(ORDER_DIRECT_EXCHANGE);
    }

    @Bean
    public DirectExchange directDLX() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DLX_ROUTING_KEY).build();
    }

    @Bean
    public Queue dlq() {
        return new Queue(DLQ);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderDirectExchange()).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(directDLX()).with(DLX_ROUTING_KEY);
    }

    @Bean
    public RabbitMQCallback callback() {
        return new RabbitMQCallback();
    }

    @Bean
    public RabbitTemplateCustomizer templateCustomizer() {
        return rabbitTemplate -> {
            log.info("--- 生产方配置 RabbitTemplate 可靠消息发送 ---");
            rabbitTemplate.setMandatory(true);
            rabbitTemplate.setConfirmCallback(callback());
            rabbitTemplate.setReturnsCallback(callback());
        };
    }
}
