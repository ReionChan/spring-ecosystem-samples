package io.github.reionchan.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 自定义设置 Spring Security 启动器
 *
 * @author Reion
 * @date 2023-04-23
 **/
@SpringBootApplication(scanBasePackages = "io.github.reionchan")
public class CustomizeWebSecurityBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(CustomizeWebSecurityBootstrap.class, args);
    }
}
