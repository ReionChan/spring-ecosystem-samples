package io.github.reionchan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 *
 * @author Reion
 * @date 2023-04-22
 **/
@RestController
public class UserController {

    /**
     * 默认首页
     */
    @GetMapping("/")
    public String index() {
        return """
                <h1>自定义配置 WebSecurity，这里是首页！</h1>
                <ul>
                     <li><a href="/login">登录</a></li>
                     <li><a href="/user/info">用户信息</a></li>
                     <li><a href="/admin/info">管理员信息</a></li>
                </ul>
                """;
    }

    /**
     * 用户登录成功首页
     */
    @GetMapping("/user/info")
    public String userInfo() {
        return """
                 <h1>用户信息页</h1>
                 <ul>
                     <li><a href="/">首页</a></li>
                     <li><a href="/logout">退出</a></li>
                </ul>
                """;
    }

    /**
     * 管理员登录成功首页
     */
    @GetMapping("/admin/info")
    public String adminInfo() {
        return """
                 <h1>管理员信息页</h1>
                 <ul>
                     <li><a href="/">首页</a></li>
                     <li><a href="/logout">退出</a></li>
                </ul>
                """;
    }
}
