package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 基于 Git 的配置服务器启动器
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
public class ConfigServerGitBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerGitBootstrap.class, args);
    }
}
