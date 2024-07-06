package io.github.reionchan.config.topic;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("topic")
@Configuration
public class TopicConfig {

    public static final String TOPIC_EXCHANGE_NAME = "topic-exchange";

    @Bean
    public TopicExchange topic() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
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
    public Binding binding1(TopicExchange topic) {
        return BindingBuilder.bind(autoDeleteQueue1()).to(topic).with("*.orange.*");
    }

    @Bean
    public Binding binding2(TopicExchange topic) {
        return BindingBuilder.bind(autoDeleteQueue2()).to(topic).with("*.*.rabbit");
    }

    @Bean
    public Binding binding3(TopicExchange topic) {
        return BindingBuilder.bind(autoDeleteQueue2()).to(topic).with("lazy.#");
    }

    @Bean
    public TopicReceiver receiver() {
        return new TopicReceiver();
    }

    @Bean
    public TopicSender sender() {
        return new TopicSender();
    }
}
