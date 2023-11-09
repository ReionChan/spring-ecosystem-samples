package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 消息生产者启动器
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@SpringBootApplication
public class StreamRabbitProducerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(StreamRabbitProducerBootstrap.class, args);
    }
}
