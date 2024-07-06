package io.github.reionchan.config.simple;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("simple")
@Configuration
public class SimpleConfig {
    @Bean
    public Queue hello() {
        return new Queue("hello");
    }

    @Bean
    public SimpleReceiver receiver() {
        return new SimpleReceiver();
    }

    @Bean
    public SimpleSender sender() {
        return new SimpleSender();
    }
}
