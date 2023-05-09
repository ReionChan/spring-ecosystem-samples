package io.github.reionchan.config;

import io.github.reionchan.filter.JwtAuthenticationTokenFilter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 基于 JWT 认证的无状态 Spring Security 配置
 *
 * @author Reion
 * @date 2023-04-27
 **/
@Configuration
@CommonsLog
@EnableConfigurationProperties(JwtProperties.class)
public class JwtSecurityConfiguration {

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
        log.info("--- 注册 WebSecurityCustomizer 类型的 Bean 定制 WebSecurity ---");
        return (web) -> web.debug(false);
    }


    /**
     * JWT 认证过滤器 Bean
     */
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(JwtProperties properties) {
        log.info("--- 生成 JwtAuthenticationTokenFilter 类型的过滤器 Bean ---");
        return new JwtAuthenticationTokenFilter(properties);
    }


    /**
     * 自定义配置 HttpSecurity，生成带有 JWT 校验规则的拦截器链
     *
     * <pre>
     * 思路：
     *      1. 失效原有的 Session 管理
     *      2. 在 {@link UsernamePasswordAuthenticationFilter} 前插入处理请求头中 JWT 的认证处理的 Filter
     *          2.1 该过滤器单次请求仅需处理一次
     *          2.2 校验成功生成 Authentication 并添加至 SecurityContextHolder 来跳过后面
     *              UsernamePasswordAuthenticationFilter 的认证拦截
     *      3. 自定义 {@link AuthenticationSuccessHandler} 类型的认证成功后的处理器，用来签发 JWT 返回给客户端
     *
     * 实现：
     *      1. 将 SessionManagement 的 Session 创建策略改为 SessionCreationPolicy.STATELESS 无状态
     *      2. 将 Servlet 服务器的 Session 跟踪置空，application.yaml 中设置 server.servlet.session.tracking-modes=
     *      3. 创建 JWT 认证过滤器 {@link JwtAuthenticationTokenFilter}
     *      4. 配置登录成功后签发 JWT 令牌的处理器 {@link JwtAuthenticationTokenFilter.JwtAuthenticationSuccessHandler}
     * </pre>
     *
     * @param http 容器中自动装配的默认 HttpSecurity {@literal  HttpSecurityConfiguration#httpSecurity()}
     */
    @Bean
    public SecurityFilterChain customizeSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService, JwtAuthenticationTokenFilter filter) throws Exception {
        log.info("--- 对容器中自动装配的默认 HttpSecurity 进行定制化配置 ---");
        http
                // 失效 CSRF
                .csrf().disable()
                // 配置 Session 管理，将 Session 生成策略该为无状态形式，不像服务器申请 JSESSIONID
                // 配合内嵌 Servlet 服务器的 Session 跟踪设置为空，将彻底失效 JSESSIONID
                // 配置属性：server.servlet.session.tracking-modes=
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 在 UsernamePasswordAuthenticationFilter 添加 JWT 认证过滤器
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests().requestMatchers(AntPathRequestMatcher.antMatcher("/h2/**")).permitAll()
                .and().headers().frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin).and()
                .authorizeHttpRequests().requestMatchers("/favicon.ico", "/", "/errorPage").permitAll()
                // 除以上之外，其他资源需要认证后才能访问
                .anyRequest().authenticated()
                // 定制登录成功后的处理器，设置 JwtAuthenticationTokenFilter 相关练得成功处理器
                .and().formLogin().successHandler(filter.getSuccessHandler())
                // 设置默认的 HTTP 认证协议
                .and().httpBasic();

        log.info("--- HttpSecurity 关联的 AuthenticationManagerBuilder 设置自定义的 userDetailsService ---");
        // 将自定义的 UserDetailsService 绑定到 httpSecurity 的 AuthenticationManagerBuilder
        http.userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--- 注册一个密码加密器 BCryptPasswordEncoder ---");
        return new BCryptPasswordEncoder();
    }
}
