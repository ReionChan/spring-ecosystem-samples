package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * OAuth2 授权服务器启动器
 *
 * @author Reion
 * @date 2023-05-03
 **/
@SpringBootApplication
@EnableJpaRepositories
@EnableMethodSecurity
public class AuthorizationBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationBootstrap.class, args);
    }
}
