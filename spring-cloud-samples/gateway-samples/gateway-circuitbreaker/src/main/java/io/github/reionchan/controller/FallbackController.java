package io.github.reionchan.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 断路器熔断后的补偿控制器
 *
 * @author Reion
 * @date 2023-05-25
 **/
@RestController
public class FallbackController {

    /**
     * 设置熔断后的补偿操作的请求 endpoint
     */
    @RequestMapping("/circuitbreakerfallback")
    public String circuitbreakerfallback() {
        return "This is a fallback";
    }

}
