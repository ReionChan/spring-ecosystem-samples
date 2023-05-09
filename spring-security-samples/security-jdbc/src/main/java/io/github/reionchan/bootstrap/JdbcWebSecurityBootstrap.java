package io.github.reionchan.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 使用数据库存储的用户参与 Spring Security 认证
 *
 * <pre>
 * 1. 引入 spring-boot-starter-jdbc、 h2 内存数据库依赖
 * 2. 建立数据库连接配置文件 classpath:db.properties
 * 3. 建立内嵌数据库初始化脚本
 *      classpath:META-INF/sql/schema.sql
 *      初始数据使用 Java 配置插入，未使用 data.sql
 * 4. 配置 classpath:application.yaml 文件，错误页面设置、H2 网页控制台开启
 * 5. 新增数据源配置 {@link io.github.reionchan.config.DataSourceConfig}
 * 6. Security 配置使用 JDBC 数据库用户 {@link io.github.reionchan.config.JdbcSecurityConfiguration}
 * 7. 新建用于权限控制测试的控制器 {@link io.github.reionchan.controller.UserController}
 * </pre>
 *
 * @author Reion
 * @date 2023-04-23
 **/
@SpringBootApplication(scanBasePackages = "io.github.reionchan")
public class JdbcWebSecurityBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(JdbcWebSecurityBootstrap.class);
    }
}
