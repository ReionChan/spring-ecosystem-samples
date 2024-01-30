package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka 消息生产者启动器
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@SpringBootApplication
public class StreamKafkaProducerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(StreamKafkaProducerBootstrap.class, args);
    }
}
