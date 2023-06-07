package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Foo 服务启动器
 *
 * @author Reion
 * @date 2023-06-06
 **/
@SpringBootApplication
public class FooBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(FooBootstrap.class, args);
    }
}
