package io.github.reionchan.config.pubsub;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("pubsub")
@Configuration
public class PubSubConfig {

    public static final String FANOUT_EXCHANGE_NAME = "fanout-exchange";

    @Bean
    public FanoutExchange fanout() {
        return new FanoutExchange(FANOUT_EXCHANGE_NAME);
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
    public Binding binding1(FanoutExchange fanout) {
        return BindingBuilder.bind(autoDeleteQueue1()).to(fanout);
    }

    @Bean
    public Binding binding2(FanoutExchange fanout) {
        return BindingBuilder.bind(autoDeleteQueue2()).to(fanout);
    }

    @Bean
    public SubReceiver receiver() {
        return new SubReceiver();
    }

    @Bean
    public PubSender sender() {
        return new PubSender();
    }
}
