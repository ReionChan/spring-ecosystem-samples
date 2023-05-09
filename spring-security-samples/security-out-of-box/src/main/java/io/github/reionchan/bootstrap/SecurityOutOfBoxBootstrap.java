package io.github.reionchan.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * <pre>
 * POM 中引入 Spring Security 依赖后不做任何配置，开箱即用的自动配置版本。
 *
 * 运行后，访问 <http://localhost:8080> 被 Security 拦截到登录页
 * ---------------------------------------------------------
 * 用户名：user
 * 密  码：控制台日志中有打印默认密码（每次启动都会动态生成）
 * 此外，SpringBoot 提供外部化配置 SpringSecurity 的属性源 SecurityProperties
 * 可以在 application.properties 中设置用户，替代默认控制台的用户
 * 注意：使用外部配置后，控制台用户将不再输出
 *
 * 例如：
 *      spring.security.user.name=user
 *      spring.security.user.password=user
 *      spring.security.user.roles=USER
 *
 * 登录成功后，将会显示 HelloController 的默认首页
 *
 * 访问 <http://localhost:8080/logout> 进入确认退出页面
 * 点击 Log Out 退出后跳转到登录页面
 * ---------------------------------------------------------
 *
 * Spring Security 默认自动装配配置类
 * ---------------------------------------------------------
 * {@link SecurityAutoConfiguration} 自动装配配置类
 * {@link UserDetailsServiceAutoConfiguration} 自动加载外部化配置所设置的用户
 * {@link SecurityFilterAutoConfiguration} 自动化将安全过滤器链注册到 Servlet 容器配置
 * </pre>
 *
 * @author Reion
 * @date 2023-04-23
 */
@SpringBootApplication(scanBasePackages = "io.github.reionchan")
public class SecurityOutOfBoxBootstrap {

    public static void main(String[] args) {
        SpringApplication.run(SecurityOutOfBoxBootstrap.class, args);
    }
}
