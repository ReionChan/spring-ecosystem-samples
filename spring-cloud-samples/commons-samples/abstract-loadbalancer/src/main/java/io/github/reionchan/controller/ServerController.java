package io.github.reionchan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器控制器
 *
 * @author Reion
 * @date 2023-05-25
 **/
@RestController
@RequestMapping("/server")
public class ServerController {

    @Autowired
    Environment env;

    /**
     * 服务器地址 endpoint
     */
    // @formatter:off
    @GetMapping("/address")
    public String address() {
        return String.format("%s@%s",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"));

    }
    // @formatter:on

}
