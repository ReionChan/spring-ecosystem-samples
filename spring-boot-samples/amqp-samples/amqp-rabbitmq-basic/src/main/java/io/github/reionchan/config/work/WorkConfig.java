package io.github.reionchan.config.work;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@Profile("work")
@Configuration
public class WorkConfig {
    @Bean
    public Queue hello() {
        return new Queue("hello");
    }

    @Bean
    public WorkerReceiver receiver1() {
        return new WorkerReceiver("worker-1");
    }

    @Bean
    public WorkerReceiver receiver2() {
        return new WorkerReceiver("worker-2");
    }

    @Bean
    public WorkSender sender() {
        return new WorkSender();
    }
}
