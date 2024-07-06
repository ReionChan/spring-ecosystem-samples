package io.github.reionchan.config.routing;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("routing")
@Configuration
public class RoutingConfig {

    public static final String DIRECT_EXCHANGE_NAME = "direct-exchange";

    @Bean
    public DirectExchange direct() {
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    @Bean
    public Queue autoDeleteQueue1() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue autoDeleteQueue2() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding binding1(DirectExchange direct) {
        return BindingBuilder.bind(autoDeleteQueue1()).to(direct).with("red");
    }

    @Bean
    public Binding binding2(DirectExchange direct) {
        return BindingBuilder.bind(autoDeleteQueue2()).to(direct).with("green");
    }

    @Bean
    public Binding binding3(DirectExchange direct) {
        return BindingBuilder.bind(autoDeleteQueue2()).to(direct).with("blue");
    }

    @Bean
    public RoutingReceiver receiver() {
        return new RoutingReceiver();
    }

    @Bean
    public RoutingSender sender() {
        return new RoutingSender();
    }
}
