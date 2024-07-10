package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Reion
 * @date 2024-07-09
 **/
@SpringBootApplication
@EnableScheduling
public class KafkaBasicBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(KafkaBasicBootstrap.class, args);
    }
}
