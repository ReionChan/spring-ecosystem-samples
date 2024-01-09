package io.github.reionchan.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 基于 Jdbc 操作的 Spring Security 配置
 *
 * @author Reion
 * @date 2023-04-23
 **/
@Configuration
public class JdbcSecurityConfiguration {

    protected static final Log logger = LogFactory.getLog(JdbcSecurityConfiguration.class);

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
        logger.info("--- 注册 WebSecurityCustomizer 类型的 Bean 定制 WebSecurity ---");
        return (web) -> {
            web.debug(false);
        };
    }

    /**
     * <pre>
     * 配置 HttpSecurity
     *
     * 添加 SecurityFilterChain 类型的 Bean，替换自动装配生成的默认的 Bean
     *
     * 原理：
     *      使 {@literal SecurityFilterChainConfiguration }
     *      标注的条件注解 {@literal @ConditionalOnDefaultWebSecurity} 条件不成立
     *      进而不装载其中的默认 SecurityFilterChain 类型的 Bean，而使用当前的 Bean
     *
     *      {@code
     *          // 失效改条件
     *          @ConditionalOnDefaultWebSecurity
     *          static class SecurityFilterChainConfiguration {
     *              // 造成下面的这个 Bean 不装载
     *              @Bean
     *              SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
     *                  //...
     *              }
     *          }
     *      }
     * </pre>
     *
     * @param http 容器中自动装配的默认 HttpSecurity {@literal  HttpSecurityConfiguration#httpSecurity()}
     */
    @Bean
    // 依赖 DDL 脚本建立表结构，实例化此 Bean 时先实例化 dataSourceScriptDatabaseInitializer
    @DependsOn("dataSourceScriptDatabaseInitializer")
    public SecurityFilterChain customizeSecurityFilterChain(HttpSecurity http, DataSource dataSource) throws Exception {
        logger.info("--- 对容器中自动装配的默认 HttpSecurity 进行定制化配置 ---");
        http
            .csrf(csrf -> csrf.disable())
            // H2 Console 网页使用 Frame 排版，设置允许同源，默认 DENY (拒绝所有)
            .headers(headers -> headers.frameOptions(cfg -> cfg.sameOrigin()))
            // 将 H2 Console 网页地址排除权限验证 (目前使用 MvcMatcher 不行，只能使用 AntMatcher 原因：h2 console 非 servlet 项目)
            .authorizeHttpRequests(auth -> auth.requestMatchers(
                    // 设置无需认证授权的资源
                    AntPathRequestMatcher.antMatcher("/"),
                    // org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console 自动装配的匹配器也同样使用 AntMatcher
                    AntPathRequestMatcher.antMatcher("/h2/**"),
                    AntPathRequestMatcher.antMatcher("/favicon.ico"),
                    AntPathRequestMatcher.antMatcher("/errorPage")).permitAll()
                    // 设置具备管理员角色才能访问的资源路径
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/admin/**")).hasRole("ADMIN")
                    // 设置具备用户角色才能访问的资源路径
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/user/**")).hasAnyRole("ADMIN", "USER")
                    // 除以上之外，其他资源需要认证后才能访问
                    .anyRequest().authenticated())
            // 定制登录及登录页面
            .formLogin(withDefaults())
            // 设置默认的 HTTP 认证协议
            .httpBasic(withDefaults());

        // 获取 HttpSecurity 关联的 AuthenticationManagerBuilder，向其中设置 UserDetailsService 并添加用户信息
        http.getSharedObject(AuthenticationManagerBuilder.class)
                // 设置基于 JDBC 操作的 UserDetailsService，并指定数据源
                .jdbcAuthentication().dataSource(dataSource)
                // 向该内存实现设置用户 user 密码 user 同时设置角色 USER
                .withUser(User.withUsername("user").passwordEncoder(passwordEncoder()::encode)
                        .password("user").roles("USER").build())
                // 向该内存实现设置用户 admin 密码 admin 同时具备 USER ADMIN 两种权限
                .withUser(User.withUsername("admin").passwordEncoder(passwordEncoder()::encode)
                        .password("admin").roles("USER", "ADMIN").build())
                // 设置密码加密器
                .passwordEncoder(passwordEncoder());
                logger.info("--- 初始化数据库用户完毕 ---");

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("--- 注册一个密码加密器 BCryptPasswordEncoder ---");
        return new BCryptPasswordEncoder();
    }
}
