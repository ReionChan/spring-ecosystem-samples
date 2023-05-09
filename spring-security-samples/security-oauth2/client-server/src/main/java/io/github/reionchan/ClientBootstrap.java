package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * OAuth2 客服端启动器
 *
 * @author Reion
 * @date 2023-05-03
 **/
@SpringBootApplication
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ClientBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ClientBootstrap.class, args);
    }
}
