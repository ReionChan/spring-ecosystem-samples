package io.github.reionchan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/echoAppName")
    public String echoAppName() {
        return String.format("%s@%s", env.getProperty("spring.application.name"), env.getProperty("server.port"));
    }
}
