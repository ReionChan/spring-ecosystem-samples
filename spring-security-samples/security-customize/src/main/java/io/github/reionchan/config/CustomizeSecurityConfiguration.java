package io.github.reionchan.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 自定义设置 Spring Security 配置
 *
 * @author Reion
 * @date 2023-04-23
 **/
@Configuration
public class CustomizeSecurityConfiguration {

    protected static final Log logger = LogFactory.getLog(CustomizeSecurityConfiguration.class);

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
            web     // 是否开启 debug 模式，输出请求调试信息，默认 false
                    .debug(true);
                    // 设置忽略配置，但是 Spring Security 已推荐将其放入 HttpSecurity 中进行 permitAll 操作
                    //.ignoring().requestMatchers(AntPathRequestMatcher.antMatcher("/static/**"));
        };
    }

    /**
     * <pre>
     * 通过向容器中注册 WebSecurityConfigurer 接口类型的 Bean，来配置 WebSecurity
     *
     * 原理：
     *      利用 {@link WebSecurityConfiguration#setFilterChainProxySecurityConfigurer(ObjectPostProcessor, ConfigurableListableBeanFactory)}
     *      方法中的的回调：
     *
     *          {@code
     *              // 从当前 beanFactory 中加载所有 WebSecurityConfigurer 类型的实现 Bean
     *              List<SecurityConfigurer<Filter, WebSecurity>> webSecurityConfigurers =
     *                  new AutowiredWebSecurityConfigurersIgnoreParents(beanFactory).getWebSecurityConfigurers();
     *
     *              for (SecurityConfigurer<Filter, WebSecurity> webSecurityConfigurer : webSecurityConfigurers) {
     *                  this.webSecurity.apply(webSecurityConfigurer);
     *              }
     *          }
     *
     *  注意：
     *      1. 由于这些注册此种类型的 Bean 是在 {@linkplain WebSecurityConfiguration#springSecurityFilterChain()}
     *         方法执行 {@code return this.webSecurity.build();} 构建时才会被执行，
     *         故此中配置会在 WebSecurityCustomizer 配置之后，它会覆盖 WebSecurityCustomizer 中的设置。
     *
     *      2. 当注册多个次中类型的 Bean 时，需要注意设置 @Order 或实现 Ordered 接口，设置好相应的顺序值，
     *         且每一个的顺序值不能有重复，否则会报错。
     *
     * </pre>
     */
    @Configuration
    @Order(Integer.MAX_VALUE - 100)
    public static class CustomWebSecurityConfigurer implements WebSecurityConfigurer<WebSecurity> {

        @Override
        public void init(WebSecurity web) throws Exception {

        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            logger.info("--- 注册 WebSecurityConfigurer 类型的 Bean 定制 WebSecurity ---");
            web.debug(true);
        }
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
    public SecurityFilterChain customizeSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.info("--- 对容器中自动装配的默认 HttpSecurity 进行定制化配置 ---");
        http
                // 设置无需认证授权的资源
                // AntPathRequestMatcher vs MvcMatcher，结论：匹配行为基本一致，但是 MVC 类型匹配器关联校验了 MVC 映射相关，安全性更高
                //.requestMatchers(AntPathRequestMatcher.antMatcher("/ant/**")).permitAll()
                //.requestMatchers("/mvc/**").permitAll()
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                    // 设置无需认证授权的资源
                    AntPathRequestMatcher.antMatcher("/"),
                    AntPathRequestMatcher.antMatcher("/favicon.ico"),
                    AntPathRequestMatcher.antMatcher("/errorPage")).permitAll()
                    // 设置具备管理员角色才能访问的资源路径
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/admin/**")).hasRole("ADMIN")
                    // 设置具备用户角色才能访问的资源路径
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/user/**")).hasAnyRole("ADMIN", "USER")
                    // 除以上之外，其他资源需要认证后才能访问
                    .anyRequest().authenticated()
                )
                // 定制登录成功后根据角色进行页面跳转逻辑
                .formLogin(loginConf ->
                    loginConf.successHandler((req, res, auth) -> {
                        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet()).contains("ROLE_ADMIN");
                        if (isAdmin) {
                            res.sendRedirect("/admin/info");
                        } else {
                            res.sendRedirect("/user/info");
                        }
                    }))
                // 设置默认的 HTTP 认证协议
                .httpBasic(withDefaults());

        // 获取 HttpSecurity 关联的 AuthenticationManagerBuilder，向其中设置 UserDetailsService 并添加用户信息
        http.getSharedObject(AuthenticationManagerBuilder.class)
                // 设置基于内存实现的 UserDetailsService
                .inMemoryAuthentication()
                // 向该内存实现设置用户 user 密码 user 同时设置角色 USER
                .withUser(User.withUsername("user").password("user").roles("USER").build())
                // 向该内存实现设置用户 admin 密码 admin 同时具备 USER ADMIN 两种权限
                .withUser(User.withUsername("admin").password("admin").roles("USER", "ADMIN").build())
                // 设置密码加密器
                .passwordEncoder(passwordEncoder());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("--- 注册一个密码加密器 NoOpPasswordEncoder ---");
        return NoOpPasswordEncoder.getInstance();
    }
}
