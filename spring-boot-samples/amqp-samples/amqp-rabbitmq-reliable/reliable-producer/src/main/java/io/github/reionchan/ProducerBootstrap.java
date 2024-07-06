package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@EnableScheduling
@SpringBootApplication
public class ProducerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ProducerBootstrap.class, args);
    }
}
