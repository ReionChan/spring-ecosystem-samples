package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * OAuth2 资源服务器启动器
 *
 * @author Reion
 * @date 2023-05-03
 **/
@SpringBootApplication
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ResourceBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ResourceBootstrap.class, args);
    }
}
