package io.github.reionchan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关控制器
 *
 * @author Reion
 * @date 2023-05-25
 **/
@RestController
public class EchoController {

    @Autowired
    Environment env;

    /**
     * 服务名称应答 endpoint
     */
    @RequestMapping("/echoAppName")
    public String echoAppName() {
        return env.getProperty("spring.application.name");
    }
}
