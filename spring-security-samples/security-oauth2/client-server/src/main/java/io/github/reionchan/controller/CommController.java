package io.github.reionchan.controller;

import io.github.reionchan.util.HttpUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CommonsLog
public class CommController implements ErrorController {

    @Autowired
    private OAuth2AuthorizedClientService clientService;

    /**
     * 默认首页
     */
    @GetMapping("/")
    public String index(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;

        String loginOut = Strings.isNotBlank(user)
                ? "<li><a href=\"/logout\">退出</a></li><li><a href=\"/user/info\">用户信息</a></li>"
                : "<li><a href=\"/login\">登录</a></li>";
        String welcome = Strings.isNotBlank(user)
                ? "首页 &gt; " + user + "，欢迎您！<img src='"
                    + ((DefaultOidcUser)authentication.getPrincipal()).getPicture() +
                    "' alt='avatar' width='50' height='50'/>"
                : "首页 &gt; ";

        return "<h1>" + welcome + "</h1>" +
                "<ul>" + loginOut;
    }

    /**
     * 用户信息页
     */
    @GetMapping("/user/info")
    @PreAuthorize("hasAnyAuthority('SCOPE_profile', 'SCOPE_email', 'SCOPE_phone', 'SCOPE_MAGIC_SKILL', 'SCOPE_WEAPON')")
    public String userInfo(OAuth2AuthenticationToken authentication) {
        String user = authentication != null ? authentication.getName() : null;
        OidcUserInfo userInfo = ((DefaultOidcUser)authentication.getPrincipal()).getUserInfo();
        Set<String> authSet = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        StringBuffer stringBuffer = new StringBuffer(), avatarBuffer = new StringBuffer();

        if (authSet.contains("SCOPE_profile")) {
            avatarBuffer.append("<img src='" + userInfo.getPicture() + "' alt='avatar' width='100' height='100'/>");
        }
        if (authSet.contains("SCOPE_email")) {
            stringBuffer.append("<li>邮箱：" + userInfo.getEmail() + "</li>");
        }
        if (authSet.contains("SCOPE_phone")) {
            stringBuffer.append("<li>电话：" + userInfo.getPhoneNumber() + "</li>");
        }
        if (authSet.contains("SCOPE_MAGIC_SKILL")) {
            stringBuffer.append("<li><a href='/user/magicSkill'>关于他的魔法技能</a></li>");
        }
        if (authSet.contains("SCOPE_WEAPON")) {
            stringBuffer.append("<li><a href='/user/weapon'>关于他所用兵器</a></li>");
        }
        return
                "<h1>用户信息页 &gt; " + user + "，欢迎您！</h1>" +
                """
                <ul>
                     <li><a href="/">首页</a>&emsp;&emsp;&emsp;&emsp;<a href="/logout">退出</a></li>
                </ul>
                <br/><hr/>
                <p>
                <h2>用户详情：</h2><br/>
                """ + avatarBuffer.toString() +
                "<ul>"
                    + "<li>姓名：" + user + "</li>"
                    + stringBuffer.toString()
                + "</ul></p>";
    }

    /**
     * 西游人物法术技能
     */
    @GetMapping("/user/magicSkill")
    @PreAuthorize("hasAuthority('SCOPE_MAGIC_SKILL')")
    public String getMagicSkill(OAuth2AuthenticationToken authentication) {
        String user = authentication != null ? authentication.getName() : null;
        OidcUserInfo userInfo = ((DefaultOidcUser)authentication.getPrincipal()).getUserInfo();
        Set<String> authSet = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(authentication.getAuthorizedClientRegistrationId(), user);

        HttpHeaders headers = HttpUtil.getJwtTokenRequestHeaders(client.getAccessToken().getTokenValue());
        URI uri = UriComponentsBuilder
                .fromUriString("http://res-server:9091/user/magicSkill")
                .build().toUri();
        RequestEntity<?> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
        ResponseEntity<Map<String, Object>> responseEntity = null;
        StringBuffer stringBuffer = new StringBuffer(), avatarBuffer = new StringBuffer();
        if (authSet.contains("SCOPE_profile")) {
            avatarBuffer.append("<img src='" + userInfo.getPicture() + "' alt='avatar' width='100' height='100'/>");
        }
        try {
            responseEntity = new RestTemplate().exchange(request, new ParameterizedTypeReference<>() {
            });
            Map<String, Object> respMap =  responseEntity.getBody();
            log.info(respMap);
            for (String skill : (List<String>) respMap.get(user)) {
                stringBuffer.append("<li>" + skill + "</li>");
            }
        } catch (HttpClientErrorException e) {
            log.error("获取资源服务器信息失败：" + e.getStatusCode().toString());
            stringBuffer.append(e.getStatusCode().toString());
        }

        return
                "<h1>技能详情页 &gt; " + user + "，欢迎您！</h1>" +
                        """
                        <ul>
                             <li><a href="/user/info">用户信息页</a>&emsp;&emsp;&emsp;&emsp;<a href="/logout">退出</a></li>
                        </ul>
                        <br/><hr/>
                        <p>
                        <h2>法术技能详情：</h2><br/>
                        """ +
                        avatarBuffer.toString()
                        + "<ul><li>姓名：" + user + "</li>"
                        + stringBuffer.toString()
                        + "</ul></p>";
    }

    /**
     * 西游人物所用兵器
     */
    @GetMapping("/user/weapon")
    @PreAuthorize("hasAuthority('SCOPE_WEAPON')")
    public String getWeapon(OAuth2AuthenticationToken authentication) {
        String user = authentication != null ? authentication.getName() : null;
        OidcUserInfo userInfo = ((DefaultOidcUser)authentication.getPrincipal()).getUserInfo();
        Set<String> authSet = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(authentication.getAuthorizedClientRegistrationId(), user);

        HttpHeaders headers = HttpUtil.getJwtTokenRequestHeaders(client.getAccessToken().getTokenValue());
        URI uri = UriComponentsBuilder
                .fromUriString("http://res-server:9091/user/weapon")
                .build().toUri();
        RequestEntity<?> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
        ResponseEntity<Map<String, Object>> responseEntity = null;
        StringBuffer stringBuffer = new StringBuffer(), avatarBuffer = new StringBuffer();
        if (authSet.contains("SCOPE_profile")) {
            avatarBuffer.append("<img src='" + userInfo.getPicture() + "' alt='avatar' width='100' height='100'/>");
        }
        try {
            responseEntity = new RestTemplate().exchange(request, new ParameterizedTypeReference<>() {
            });
            Map<String, Object> respMap =  responseEntity.getBody();
            log.info(respMap);
            for (String weapon : (List<String>) respMap.get(user)) {
                stringBuffer.append("<li>" + weapon + "</li>");
            }
        } catch (HttpClientErrorException e) {
            log.error("获取资源服务器信息失败：" + e.getStatusCode().toString());
            stringBuffer.append(e.getStatusCode().toString());
        }
        return
                "<h1>兵器情页 &gt; " + user + "，欢迎您！</h1>" +
                        """
                        <ul>
                             <li><a href="/user/info">用户信息页</a>&emsp;&emsp;&emsp;&emsp;<a href="/logout">退出</a></li>
                        </ul>
                        <br/><hr/>
                        <p>
                        <h2>兵器详情：</h2><br/>
                        """ +
                        avatarBuffer.toString()
                        + "<ul><li>姓名：" + user + "</li>"
                        + stringBuffer.toString()
                        + "</ul></p>";
    }

    /**
     * 错误页面
     */
    @GetMapping("/errorPage")
    public String errorPage(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        Integer statusCode = null;
        HttpStatus sta = HttpStatus.OK;
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            sta = HttpStatus.valueOf(statusCode);
        }

        return """
                <h1>错误页 &gt; </h1>
                <ul>
                    <li><a href="/">首页</a></li>
                    """ +
                "<li><font color='red'>状态码：" + sta.value() + "</font></li>" +
                "<li><font color='red'>异常消息：" + sta.getReasonPhrase() + "</font></li>" +
                """
                </ul>
                """;
    }
}
