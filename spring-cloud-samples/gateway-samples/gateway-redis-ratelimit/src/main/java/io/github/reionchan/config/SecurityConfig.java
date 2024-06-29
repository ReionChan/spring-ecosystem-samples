package io.github.reionchan.config;

import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security 配置
 *
 * @author Reion
 * @date 2023-05-25
 **/
@Configuration
public class SecurityConfig {
    /**
     * 由于 RequestRateLimiterGatewayFilterFactory 中关于辨别请求来自哪个用户的 KeyResolver
     * 默认配置的是 {@link GatewayAutoConfiguration#principalNameKeyResolver()}，
     * 它获取请求中已认证用户的名称 getPrincipal() 来辨别同一用户的多次请求。
     * 所以此处配置基于 Spring Security 的 Http Basic 认证
     */
    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
        return http.httpBasic(Customizer.withDefaults())
            .csrf(csrfSpec -> csrfSpec.disable())
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec.pathMatchers("/anything/**").authenticated()
                .anyExchange().permitAll()
            ).build();
    }

    /**
     * 此处配置 Spring Security 用户名 user 密码 password
     * 测试时，请设置 Authorization 请求头值为 Basic dXNlcjpwYXNzd29yZA==
     */
    @Bean
    public MapReactiveUserDetailsService reactiveUserDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
        return new MapReactiveUserDetailsService(user);
    }
}
