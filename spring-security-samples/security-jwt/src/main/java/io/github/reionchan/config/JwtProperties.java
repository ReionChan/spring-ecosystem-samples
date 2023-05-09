package io.github.reionchan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性类
 *
 * @author Reion
 * @date 2023-04-28
 **/
@Data
@ConfigurationProperties(prefix = "reion.jwt")
public class JwtProperties {
    private String issuer = "Reion";
    private String subject = "Auth";
    private String secret = "DefaultSecretKey";
    private Integer validMinutes = 10;
}
