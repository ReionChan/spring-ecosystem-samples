package io.github.reionchan.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello 控制器
 *
 * @author Reion
 * @date 2023-04-23
 **/
@RestController
public class HelloController {

    /**
     * 默认首页
     */
    @RequestMapping("/")
    public String index() {
        return "<h1>你好，Spring Security！</h1>";
    }
}
