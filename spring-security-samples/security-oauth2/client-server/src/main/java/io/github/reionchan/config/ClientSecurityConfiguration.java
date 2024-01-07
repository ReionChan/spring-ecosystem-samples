package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 客户端安全配置
 *
 * @author Reion
 * @date 2023-05-03
 **/
@Configuration
@CommonsLog
public class ClientSecurityConfiguration {

    /**
     * <pre>
     * 通过向容器中注册 WebSecurityCustomizer 类型的 Bean，来配置 WebSecurity
     *
     * 原理：
     *      利用 {@link  WebSecurityConfiguration#springSecurityFilterChain()}
     *      方法中的的回调：
     *
     *          {@code
     *              for (WebSecurityCustomizer customizer : this.webSecurityCustomizers) {
     *                  customizer.customize(this.webSecurity);
     *              }
     *          }
     * </pre>
     */
    @Bean
    public WebSecurityCustomizer customize() {
        log.info("--- OAuth2 客户端的 WebSecurity 定制化配置 ---");
        return (web) -> web.debug(false);
    }

    /**
     * 自定义配置 HttpSecurity，生成带有 JWT 校验规则的拦截器链
     *
     * <pre>
     * </pre>
     *
     * @param http 容器中自动装配的默认 HttpSecurity {@literal  HttpSecurityConfiguration#httpSecurity()}
     */
    @Bean
    public SecurityFilterChain customizeSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("--- OAuth2 客户端的 HttpSecurity 定制化配置 ---");
        http
                // 失效 CSRF
                .csrf(csrf -> csrf.disable())
                // 开启 OAuth2 授权登录端点
                .oauth2Login(login->login.successHandler((req, res, auth) -> {
                    res.sendRedirect("/user/info");
                }))
                // 激活 OAuth2 客户端支持
                .oauth2Client(client -> {})
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // 忽略 OAuth2 客户端端点及一些静态资源的认证
                .authorizeHttpRequests(httpReq -> httpReq
                    .requestMatchers("/favicon.ico", "/", "/errorPage", "/oauth2/**", "/login/**").permitAll()
                    .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--- 注册一个密码加密器 BCryptPasswordEncoder ---");
        return new BCryptPasswordEncoder();
    }
}
