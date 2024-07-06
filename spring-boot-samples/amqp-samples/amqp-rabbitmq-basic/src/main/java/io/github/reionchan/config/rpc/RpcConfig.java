package io.github.reionchan.config.rpc;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("rpc")
@Configuration
public class RpcConfig {

    public static final String DIRECT_EXCHANGE_NAME = "direct-exchange";
    public static final String QUEUE_NAME = "rpc-queue";
    public static final String ROUTING_KEY = "rpc";

    @Bean
    public DirectExchange topic() {
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(DirectExchange direct) {
        return BindingBuilder.bind(queue()).to(direct).with(ROUTING_KEY);
    }

    @Bean
    public RpcServer receiver() {
        return new RpcServer();
    }

    @Bean
    public RpcClient sender() {
        return new RpcClient();
    }
}
