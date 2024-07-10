package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Reion
 * @date 2024-07-11
 **/
@SpringBootApplication
@EnableScheduling
public class OrderProducerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(OrderProducerBootstrap.class, args);
    }
}
