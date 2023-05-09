package io.github.reionchan.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController implements ErrorController {

    /**
     * 用户信息页
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('USER')")
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
}
