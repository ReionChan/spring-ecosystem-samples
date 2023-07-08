package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.EnvironmentRepositoryConfiguration;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;

/**
 * 基于 Git 的配置服务器启动器
 *
 * <pre>
 * 基于 Git 的配置服务器是 Config Server 自动装配默认装配，
 * 因为未配置属性 spring.profiles.active、或者该属性配置的 profile
 * 没有在配置类 {@link EnvironmentRepositoryConfiguration} 有匹配时，
 * 会激活默认的 {@link MultipleJGitEnvironmentRepository} 基于 Git 的配置存储。
 *
 * 本示例主要演示 Git 配置库的配置，参考 application.yaml 文件注释。
 * 其中主要包含：
 * 1. Git 服务器 uri 的配置（SSH 连接，HTTP 设置参考 <a href="https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_git_backend">Git Backend</a>）
 * 2. 设置 Git 服务器的公钥
 * 3. 设置本配置服务器的私钥并将对应的公钥放入 Git 服务器的信任主机中
 * 4. 设置配置文件的搜索规则
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
