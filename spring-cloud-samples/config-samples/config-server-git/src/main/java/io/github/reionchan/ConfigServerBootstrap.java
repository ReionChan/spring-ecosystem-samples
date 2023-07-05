package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 配置服务器启动器
 *
 * <pre>
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-07-04
 **/
@EnableConfigServer
@SpringBootApplication
public class ConfigServerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerBootstrap.class, args);
    }
}
