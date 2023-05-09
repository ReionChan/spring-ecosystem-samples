package io.github.reionchan.bootstrap;

import io.github.reionchan.config.JwtProperties;
import io.github.reionchan.filter.JwtAuthenticationTokenFilter;
import io.github.reionchan.util.JwtUtil;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 基于 JWT 实现无状态认证
 *
 * <pre>
 *     1. 运行环境基于上一个模块 spring-security-dao
 *        故基本 SpringBoot + JPA + H2 基本设置参考之前模块介绍
 *     2. 使 Servlet 服务器 Session 管理失效，变为无状态服务器
 *          2.1 application.yam 设置 session 跟踪模式为空：
 *              server.servlet.session.tracking-modes=
 *          2.2 Security 设置 session 生成策略为无状态：
 *              SessionCreationPolicy.STATELESS
 *     3. 创建 JWT 签发与校验工具类 {@link JwtUtil}
 *     4. 创建 JWT 外置可配置属性类 {@link JwtProperties}
 *     5. 创建用户登录成功后生成 JWT 的处理器 {@link JwtAuthenticationTokenFilter.JwtAuthenticationSuccessHandler}
 *     6. 创建 JWT 认证过滤器 {@link JwtAuthenticationTokenFilter}
 *          6.1 将其放在用户名密码认证的过滤器之前
 *          6.2 请求头包含 JWT 时，校验后直接生成 Authentication 并放入安全上下文，避免后面的用户名密码校验
 *     7. 在 HttpSecurity 自定义配置中将创建的过滤器、成功后处理器、无状态生成配置到其中
 *     8. 当用户登录成功后，将会返回 Json 格式的响应体，包含 JWT 令牌，
 *        之后的请求中将其放入请求头 Authorization 中即可跳过用户名密码验证。
 *          8.1 请求头样式 Bearer+单空格+JWT
 *          8.2 可以分别以 user、admin 用户获得 JWT
 *              然后追加认证头信息访问他们权限范围内或外的页面，验证权限控制
 *              例如：/user/info  /admin/info
 *
 *  本示例还是用了注解 {@link EnableMethodSecurity}，激活了基于方法基本的注解形式的安全控制。
 *
 *  1. prePostEnabled=true (默认)
 *      开启 Spring 的安全注解 {@link PreAuthorize} {@link PreAuthorize} {@link PreFilter} {@link PostFilter}
 *  2. securedEnabled=true
 *      开启 Spring 的另外一个安全注解 {@link Secured}
 *
 *  3. jsr250Enabled=true
 *      开启 jsr250 规范安全注解 {@link DenyAll} {@link PermitAll} {@link RolesAllowed}
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-04-27
 **/
@EntityScan("io.github.reionchan")
@EnableJpaRepositories("io.github.reionchan")
@SpringBootApplication(scanBasePackages = "io.github.reionchan")
// 激活方法
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class JwtWebSecurityBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(JwtWebSecurityBootstrap.class, args);
    }
}
