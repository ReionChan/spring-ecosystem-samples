package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 基于 DAO 操作的 Spring Security 配置
 *
 * @author Reion
 * @date 2023-04-23
 **/
@Configuration
@CommonsLog
public class DaoSecurityConfiguration {

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
        return (web) -> {
            web.debug(false);
        };
    }

    /**
     * <pre>
     * 把自定义的 UserDetailsService Bean 配置到 HttpSecurity 关联的 AuthenticationManagerBuilder
     *
     * 原理：
     *      使用 HttpSecurity 暴露的配置方法
     *
     *      {@code
     *          @Override
     * 	        public HttpSecurity userDetailsService(UserDetailsService userDetailsService) {
     * 	            // getAuthenticationRegistry() 即关联的 AuthenticationManagerBuilder
     * 	            // 调用此 Builder 的 userDetailsService 配置方法，将自定义的服务注入
     * 		        getAuthenticationRegistry().userDetailsService(userDetailsService);
     * 		        return this;
     * 	        }
     *      }
     *
     * 注意：
     *      1. 此中设置法，将把自定义的 userDetailsService 服务设置到 httpSecurity 级别。
     *          其实，HttpSecurity 关联的 AuthenticationManagerBuilder 还有父 AuthenticationManager
     *          这个父管理器用来兜底，如果用户不对 HttpSecurity 关联的 AuthenticationManagerBuilder
     *          设置任何的认证服务，那么最后的认证将由父管理器的默认基于内存存储的默认用户名来认证。
     *
     *      {@code
     *          HttpSecurity httpSecurity() throws Exception {
     *         		// 实例化 httpSecurity 关联的 AuthenticationManagerBuilder
     *         		AuthenticationManagerBuilder authenticationBuilder = new DefaultPasswordEncoderAuthenticationManagerBuilder(
     *         				this.objectPostProcessor, passwordEncoder);
     *         		// 将 authenticationManager() 获得的全局认证管理器设为此 builder 的父认证管理器
     *         		authenticationBuilder.parentAuthenticationManager(authenticationManager());
     *         		// 将 builder 参与 HttpSecurity 的构造，进行关联
     *         		HttpSecurity http = new HttpSecurity(this.objectPostProcessor, authenticationBuilder, createSharedObjects());
     *         		// ...
     *         	}
     *      }
     *
     *      2. 其父认证管理器的构建逻辑也颇有意思，大致如下：
     *
     *          2.1 AuthenticationConfiguration 自动装配配置类使用 @Bean 配置类
     *              EnableGlobalAuthenticationAutowiredConfigurer，将 @EnableGlobalAuthentication
     *              作为注解或元注解标记的配置类在 HttpSecurity 获取父认证管理器的构建器 build 之前先实例化，
     *              那么，该被此注解配置类下的 UserDetailsService、AuthenticationProvider、
     *              AuthenticationManager、AuthenticationResolver 类型的 Bean 就可以提前初始化（抢在 HttpSecurity 关联的 builder 之前）
     *
     *              {@code
     *                  // 片段一 AuthenticationConfiguration 类中的 @Bean EnableGlobalAuthenticationAutowiredConfigurer
     *                  @Bean
     *                  public static GlobalAuthenticationConfigurerAdapter enableGlobalAuthenticationAutowiredConfigurer(
     * 			            ApplicationContext context) {
     * 		                return new EnableGlobalAuthenticationAutowiredConfigurer(context);
     *                  }
     *
     *                  // 片段二 EnableGlobalAuthenticationAutowiredConfigurer 类中的方法
     *                      @Override
     *                      public void init(AuthenticationManagerBuilder auth) {
     *                          // 强制调用 getBeansWithAnnotation，将 EnableGlobalAuthentication 标记的在此初始化
     * 			                Map<String, Object> beansWithAnnotation = this.context
     * 					            .getBeansWithAnnotation(EnableGlobalAuthentication.class);
     * 			                if (logger.isTraceEnabled()) {
     * 				                logger.trace(LogMessage.format("Eagerly initializing %s", beansWithAnnotation));
     *                          }
     *                      }
     *              }
     *
     *          2.2 上面讲的四种类型的 Bean 如果有，将会被父认证管理器关联，从而替代 UserDetailsServiceAutoConfiguration
     *              所配置的默认基于内存实现的用户认证服务。而 HttpSecurity 关联的 builder 会讲此作为父级认证服务。
     *              当 HttpSecurity 关联的 builder 构建器没有设置任何认证来源时，它会转交父级认证服务。
     *              换言之，如果将自定义的 UserDetailsService 标记上 @EnableGlobalAuthentication 注解，
     *              就无需在 HttpSecurity 中配置 UserDetailsService，自定义的 UserDetailsService 自动晋升成全局的父认证服务
     *
     *              {@code
     *                  @AutoConfiguration
     *                  // 此条件装配注解清除指明下面几种类型的 Bean 出现，此配置类将不进行装配，其下的内存式的认证器失效
     *                  @ConditionalOnMissingBean(
     *                      value = { AuthenticationManager.class, AuthenticationProvider.class, UserDetailsService.class,
     * 				                AuthenticationManagerResolver.class },
     * 		                type = { "org.springframework.security.oauth2.jwt.JwtDecoder",
     * 				                "org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector",
     * 				                "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository",
     * 				                "org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository" })
     *                  public class UserDetailsServiceAutoConfiguration {
     *                      // 此 Bean InMemoryUserDetailsManager 失效
     *                      @Bean
     *                      public InMemoryUserDetailsManager inMemoryUserDetailsManager(SecurityProperties properties,
     * 		                    return new InMemoryUserDetailsManager(User.withUsername(user.getName())
     * 			                    .password(getOrDeducePassword(user, passwordEncoder.getIfAvailable()))
     * 			                    .roles(StringUtils.toStringArray(roles))
     * 			                    .build());
     *                  }
     *               }
     * </pre>
     *
     * @param http 容器中自动装配的默认 HttpSecurity {@literal  HttpSecurityConfiguration#httpSecurity()}
     */
    @Bean
    public SecurityFilterChain customizeSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        log.info("--- 对容器中自动装配的默认 HttpSecurity 进行定制化配置 ---");
        http
                .csrf().disable()
                // 将 H2 Console 网页地址排除权限验证 (目前使用 MvcMatcher 不行，只能使用 AntMatcher 原因：h2 console 非 servlet 项目)
                .authorizeHttpRequests().requestMatchers(AntPathRequestMatcher.antMatcher("/h2/**")).permitAll()
                // H2 Console 网页使用 Frame 排版，设置允许同源，默认 DENY (拒绝所有)
                .and().headers().frameOptions(cfg -> cfg.sameOrigin()).and()
                // 设置无需认证授权的资源
                .authorizeHttpRequests().requestMatchers("/favicon.ico", "/", "/errorPage").permitAll()
                // 设置具备管理员角色才能访问的资源路径
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 设置具备用户角色才能访问的资源路径
                .requestMatchers("/user/**").hasAnyRole("ADMIN", "USER")
                // 除以上之外，其他资源需要认证后才能访问
                .anyRequest().authenticated()
                // 定制登录及登录页面
                .and().formLogin()
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
