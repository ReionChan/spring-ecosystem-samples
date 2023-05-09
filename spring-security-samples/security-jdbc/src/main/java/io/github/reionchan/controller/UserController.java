package io.github.reionchan.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements ErrorController {

    /**
     * 默认首页
     */
    @GetMapping("/")
    public String index(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;

        String loginOut = Strings.isNotBlank(user)
                ? "<li><a href=\"/logout\">退出</a></li>"
                : "<li><a href=\"/login\">登录</a></li>";
        String welcome = Strings.isNotBlank(user)
                ? "首页 &gt; " + user + "，欢迎您！"
                : "首页 &gt; ";

        return "<h1>" + welcome + "</h1>" +
                "<ul>" + loginOut +
                """     
                     <li><a href="/user/info">用户信息</a></li>
                     <li><a href="/admin/info">管理员信息</a></li>
                </ul>
                """;
    }

    /**
     * 用户信息页
     */
    @GetMapping("/user/info")
    public String userInfo(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        return
                "<h1>用户信息页 &gt; " + user + "，欢迎您！</h1>" +
                """
                 <ul>
                     <li><a href="/">首页</a></li>
                     <li><a href="/logout">退出</a></li>
                </ul>
                """;
    }

    /**
     * 管理员信息页
     */
    @GetMapping("/admin/info")
    public String adminInfo(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        return
                "<h1>管理员信息页 &gt; " + user + "，欢迎您！</h1>" +
                """
                <ul>
                     <li><a href="/">首页</a></li>
                     <li><a href="/logout">退出</a></li>
                     <li><a href="/h2">H2 数据库管理</a></li>
                </ul>
                """;
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
