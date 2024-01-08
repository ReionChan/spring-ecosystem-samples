package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 资源服务安全配置
 *
 * @author Reion
 * @date 2023-05-03
 **/
@Configuration
@CommonsLog
public class ResourceSecurityConfiguration {

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
        log.info("--- OAuth2 资源服务器 WebSecurity 自定义配置 ---");
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
        log.info("--- OAuth2 资源服务器 HttpSecurity 进行定制化配置 ---");
        http
                // 失效 CSRF
                .csrf(csrf -> csrf.disable())
                // 禁用登录入口
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                // 启用资源服务器配置，并定制 JWT 令牌解析器配置，提供基于 Bearer JWT 的认证器
                .oauth2ResourceServer(resServerConf -> resServerConf.jwt(withDefaults()))
                // 使资源服务器变成无状态服务器
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(httpReq -> { httpReq
                        .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                        .requestMatchers("/favicon.ico", "/errorPage").permitAll()
                        .anyRequest().authenticated();
                });

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--- OAuth2 资源服务器密码加密器 ---");
        return new BCryptPasswordEncoder();
    }
}
