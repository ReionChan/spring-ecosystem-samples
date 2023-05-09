package io.github.reionchan.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.reionchan.config.JwtProperties;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

/**
 * JWT 工具类
 *
 * @author Reion
 * @date 2023-04-28
 **/
@CommonsLog
@Data
public class JwtUtil {

    public static final String CLAIM_USER_NAME = "username";
    public static final String CLAIM_USER_ROLES = "roles";

    private Algorithm algorithm;
    private Integer validMinutes;
    private JWTCreator.Builder builder;
    private JWTVerifier verifier;


    public JwtUtil(JwtProperties config) {
        Assert.isTrue(Strings.isNotBlank(config.getSecret()), "reion.jwt.secret must not empty");
        Assert.isTrue(Strings.isNotBlank(config.getIssuer()), "reion.jwt.issuer must not empty");
        Assert.isTrue(Strings.isNotBlank(config.getSubject()), "reion.jwt.subject must not empty");
        Assert.isTrue(Objects.nonNull(config.getValidMinutes()) && config.getValidMinutes() > 0, "reion.jwt.valid-minutes must be a positive number");
        this.algorithm = Algorithm.HMAC512(config.getSecret());
        this.validMinutes = config.getValidMinutes();
        this.builder = JWT.create().withIssuer(config.getIssuer()).withSubject(config.getSubject());
        this.verifier = JWT.require(algorithm).withIssuer(config.getIssuer()).withSubject(config.getSubject())
                .withClaimPresence(CLAIM_USER_NAME).withClaimPresence(CLAIM_USER_ROLES).build();
    }

    /**
     * 签发用户认证 JWT
     *
     * @param details 用户详情
     * @return JWT 令牌
     */
    public String createToken(Map<String, Object> details) {
        Assert.notNull(details, "details must not null");
        String userName = (String) details.get(CLAIM_USER_NAME);
        Assert.isTrue(Strings.isNotBlank(userName), "username must not empty");
        String[] auths =  (String[]) details.get(CLAIM_USER_ROLES);
        Assert.isTrue(auths != null && auths.length> 0, "authorities must not empty");

        return builder.withClaim(CLAIM_USER_NAME, userName)
                .withArrayClaim(CLAIM_USER_ROLES, auths)
                .withExpiresAt(Instant.now().plus(validMinutes, ChronoUnit.MINUTES)).sign(algorithm);
    }

    /**
     * 校验 JWT
     *
     * @param token JWT 令牌
     * @return 用户详情
     */
    public UserDetails verifierToken(String token) {
        Assert.isTrue(Strings.isNotBlank(token), "JWT token string must not null");
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            String username = decodedJWT.getClaim(CLAIM_USER_NAME).asString();
            String[] roles = decodedJWT.getClaim(CLAIM_USER_ROLES).asArray(String.class);
            UserDetails userDetails = User.withUsername(username).roles(roles)
                    .password("").passwordEncoder(String::toString).build();
            return userDetails;
        } catch (TokenExpiredException expiredException) {
            log.error("JWT 已过期");
        } catch (MissingClaimException e) {
            log.error("JWT 未包含有效信息");
        } catch (Exception e) {
            log.error("JWT 无效", e);
        }
        return null;
    }
}
