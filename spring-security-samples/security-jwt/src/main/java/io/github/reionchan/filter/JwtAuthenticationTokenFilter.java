package io.github.reionchan.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reionchan.config.JwtProperties;
import io.github.reionchan.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT 校验过滤器
 *
 * @author Reion
 * @date 2023-04-27
 **/
@CommonsLog
@Data
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private AuthenticationManager authenticationManager;
    private String credentialsCharset = "UTF-8";
    private JwtAuthenticationConverter authenticationConverter;
    private JwtAuthenticationSuccessHandler successHandler;


    public JwtAuthenticationTokenFilter(JwtProperties properties) {
        JwtUtil jwtUtil = new JwtUtil(properties);
        this.authenticationConverter = new JwtAuthenticationConverter(jwtUtil);
        this.successHandler = new JwtAuthenticationSuccessHandler(jwtUtil);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UsernamePasswordAuthenticationToken authRequest = this.authenticationConverter.convert(request);
            if (authRequest == null) {
                log.trace("Did not process authentication request since failed to find "
                        + "jwt in Bearer Authorization header");
                filterChain.doFilter(request, response);
                return;
            }
            String username = authRequest.getName();
            log.info(String.format("用户 '%s' JWT 认证成功！", username));

            // 该步骤至关重要，由 JWT 解析形成的 Authentication 放入安全上下文，使得它后面的登录过滤器跳过登录拦截
            SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authRequest);
            this.securityContextHolderStrategy.setContext(context);
        } catch (AuthenticationException ex) {
            this.securityContextHolderStrategy.clearContext();
            log.debug("JWT 认证失败", ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @CommonsLog
    @Data
    public static class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        private JwtUtil jwtUtil;

        public JwtAuthenticationSuccessHandler(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

            try {
                String username = authentication.getName();
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                Assert.isTrue(Strings.isNotBlank(username), "username must not empty");
                Assert.isTrue(authorities!=null && authorities.size()>0, "authorities must not empty");
                String[] authArray = authorities.stream().map(GrantedAuthority::getAuthority)
                        .map(s -> s.substring(5))
                        .collect(Collectors.toList()).toArray(new String[]{});

                Map<String, Object> claimMap = new HashMap<>();
                claimMap.put(JwtUtil.CLAIM_USER_NAME, username);
                claimMap.put(JwtUtil.CLAIM_USER_ROLES, authArray);

                String jwt = jwtUtil.createToken(claimMap);

                log.info(String.format("用户 %s 认证成功，签发有效期 %d 分钟的 JWT 令牌：\n%s", username, jwtUtil.getValidMinutes(), jwt));

                response.setStatus(HttpStatus.OK.value());
                response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
                Map<String, Object> retMap = new HashMap<>();
                retMap.put("status", 200);
                retMap.put("msg", "请将此令牌加到每一次请求的 Authorization 头部，格式：[Bearer jwt]");
                retMap.put("jwt", jwt);
                ObjectMapper mapper = new ObjectMapper();
                String retStr = mapper.writeValueAsString(retMap);
                response.getWriter().write(retStr);

            } catch (Exception e) {
                log.error("签发 JWT 异常", e);
                throw new AuthenticationServiceException("签发 JWT 异常");
            }
        }
    }

    @Data
    @CommonsLog
    private static class JwtAuthenticationConverter implements AuthenticationConverter {

        public static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";

        private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

        private Charset credentialsCharset = StandardCharsets.UTF_8;

        private JwtUtil jwtUtil;

        public JwtAuthenticationConverter(JwtUtil jwtUtil) {
            this(new WebAuthenticationDetailsSource(), jwtUtil);
        }

        public JwtAuthenticationConverter(
                AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource, JwtUtil jwtUtil) {
            this.authenticationDetailsSource = authenticationDetailsSource;
            this.jwtUtil = jwtUtil;
        }

        @Override
        public UsernamePasswordAuthenticationToken convert(HttpServletRequest request) {
            try {
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (header == null) {
                    return null;
                }
                header = header.trim();
                if (!StringUtils.startsWithIgnoreCase(header, AUTHENTICATION_SCHEME_BEARER)) {
                    return null;
                }
                if (header.equalsIgnoreCase(AUTHENTICATION_SCHEME_BEARER)) {
                    throw new BadCredentialsException("Empty bearer authentication token");
                }
                String jwtToken = header.substring(7);
                log.info("接收到令牌：" + jwtToken);
                UserDetails userDetails = jwtUtil.verifierToken(jwtToken);
                if (userDetails == null) {
                    return null;
                }
                UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken
                        .authenticated(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
                return result;
            } catch (Exception e) {
                log.info("转换异常", e);
            }
            return null;
        }
    }
}
