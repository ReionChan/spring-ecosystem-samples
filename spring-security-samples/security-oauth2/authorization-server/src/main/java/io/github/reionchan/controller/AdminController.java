package io.github.reionchan.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminController implements ErrorController {


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
}
