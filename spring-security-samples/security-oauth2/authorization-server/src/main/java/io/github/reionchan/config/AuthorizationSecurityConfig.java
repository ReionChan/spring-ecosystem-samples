package io.github.reionchan.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.github.reionchan.entity.User;
import io.github.reionchan.service.IUserService;
import io.github.reionchan.util.RSAKeyUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 授权服务器安全配置
 *
 * @author Reion
 * @date 2023-05-03
 **/
@Configuration
@CommonsLog
public class AuthorizationSecurityConfig {

    @Autowired
    private IUserService userService;

    @Value("${user.home}")
    private String keyRoot;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        log.info("--- OAuth2 授权服务器的 HttpSecurity 定制化配置 ---");
        // 默认配置：已开启 OAuth2 协议所需的端点
        // ==================================================================
        // 端点                   | 说明
        // ------------------------------------------------------------------
        // /oauth2/token         | 获取访问令牌或刷新访问令牌
        // /oauth2/introspect    | 令牌自省
        // /oauth2/revoke        | 令牌撤回
        // /oauth2/authorize     | 客户端授权认证
        // /.well-known/oauth-authorization-server | OAuth 授权服务密钥等设置信息
        // ===================================================================
        //OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        //log.info("应用授权服务器默认安全设置");
        //http
        //        .exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(
        //                new LoginUrlAuthenticationEntryPoint("/login"))
        //        );


        // 自定义配置：开启OpenID Connect 1.0 协议支持
        // ==================================================================
        // 端点                                  | 说明
        // ------------------------------------------------------------------
        // /.well-known/openid-configuration    | OIDC 协议设置信息端点
        // /userinfo                            | 用户信息
        // ===================================================================
        OAuth2AuthorizationServerConfigurer serverConfigurer = new OAuth2AuthorizationServerConfigurer();
        // 开启 OIDC (默认关闭，所以需要自定义配置)
        log.info("开启 OpenID Connect 1.0 协议支持，并配置用户信息端点");
        serverConfigurer
                .oidc(oidc -> {
                    // 自定义 /userinfo 端点用户相关的信息
                    oidc.userInfoEndpoint(userInfo -> userInfo.userInfoMapper(ctx -> {
                        String userName = ctx.getAuthorization().getPrincipalName();
                        Set<String> scopes = ctx.getAuthorization().getAuthorizedScopes();
                        Map<String, Object> userInfoMap = new HashMap<>();
                        User user = userService.findUserByUsername(userName);
                        if (scopes.contains(OidcScopes.PROFILE)) {
                            userInfoMap.put("name", user.getUserName());
                            userInfoMap.put("picture", user.getAvatar());
                            userInfoMap.put("updated_at", user.getUpdateTime());
                        }
                        if (scopes.contains(OidcScopes.EMAIL)) {
                            userInfoMap.put("email", user.getEmail());
                        }
                        if (scopes.contains(OidcScopes.PHONE)) {
                            userInfoMap.put("phone_number", user.getPhone());
                        }
                        // 将 JWT 中的 claims 一并放入，客户端配置的 user-name-attribute 属性 sub 由此获得
                        userInfoMap.putAll(ctx.getAuthorization().getAccessToken().getClaims());
                        return new OidcUserInfo(userInfoMap);
                    }));
                });
        // 汇聚所有配置的端点匹配器
        RequestMatcher endpointsMatcher = serverConfigurer.getEndpointsMatcher();

        http
            // 将所有暴露的端点匹配器设置到 httpSecurity 的安全过滤器的匹配器
            .securityMatcher(endpointsMatcher)
            // 开启 BearerTokenAuthenticationFilter 过滤器，校验请求里的 Bearer 令牌
            // OIDC 的 /userinfo 端点需要基于 Bearer 令牌的身份认证
            .oauth2ResourceServer(cfg -> cfg.jwt(withDefaults()))
            .authorizeHttpRequests(authorize -> {
                authorize.anyRequest().authenticated();
            })
            .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
            // 配置未认证异常 401 时的认证端点 /login
            .exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/login"))
            )
            // 应用本自定义配置 OAuth2AuthorizationServerConfigurer
            .apply(serverConfigurer);

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        log.info("--- OAuth2 授权服务器构建内存式的客户端仓库 ---");
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("JourneyToTheWestStory")
                .clientName("西游记故事")
                // secret
                .clientSecret("$2a$10$XWY/JeWE1SpNQus1mCCJqeh93jrj.hjGDHqJwKLuEEBwv/s5Xylm2")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://cli-server:8080/login/oauth2/code/auth-server")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.PHONE)
                .scope(OidcScopes.EMAIL)
                .scope("MAGIC_SKILL")
                .scope("WEAPON")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = RSAKeyUtil.loadOrCreateThenSaveByPemFile(Paths.get(keyRoot,"keys"), 2048);
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }


    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        Set<JWSAlgorithm> jwsAlgs = new HashSet<>();
        jwsAlgs.addAll(JWSAlgorithm.Family.RSA);
        jwsAlgs.addAll(JWSAlgorithm.Family.EC);
        jwsAlgs.addAll(JWSAlgorithm.Family.HMAC_SHA);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector =
                new JWSVerificationKeySelector<>(jwsAlgs, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        // Override the default Nimbus claims set verifier as NimbusJwtDecoder handles it instead
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });
        return new NimbusJwtDecoder(jwtProcessor);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // 这里的 issuer 后缀不要有 '/'，否则安全端点会被 security 拦截
        return AuthorizationServerSettings.builder().issuer("http://auth-server:9090").build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        log.info("--- OAuth2 授权服务器自身的 HttpSecurity 自定义配置 ---");
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests((authorize) -> authorize
                    .requestMatchers(
                        new AntPathRequestMatcher("/"),
                        new AntPathRequestMatcher("/favicon.ico"),
                        new AntPathRequestMatcher("/errorPage"),
                        new AntPathRequestMatcher("/avatar/**")
                    ).permitAll()
                    .requestMatchers(PathRequest.toH2Console()).hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .formLogin(withDefaults());

        log.info("--- OAuth2 授权服务器设置用户认证查询服务 ---");
        http.userDetailsService(userService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("--- 设置授权服务器密码加密器 ---");
        return new BCryptPasswordEncoder();
    }
}
