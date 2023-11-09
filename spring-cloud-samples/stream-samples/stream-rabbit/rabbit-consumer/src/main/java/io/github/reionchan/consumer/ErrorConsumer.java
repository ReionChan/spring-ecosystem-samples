package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ErrorMessage;

import java.util.function.Consumer;

/**
 * 异常消费者
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class ErrorConsumer {
    @Bean
    public Consumer<ErrorMessage> errorMessageConsumer() {
        return message -> {
            log.info("=== Error: {} ===", message.getPayload().getCause().getMessage());
        };
    }
}
