package io.github.reionchan.controller;

import io.github.reionchan.response.WebResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.reionchan.config.SpringDocConfig.*;

/**
 * 网关控制器
 *
 * @author Reion
 * @date 2023-05-25
 **/
@RestController
@RequestMapping("/server")
@Tag(name = "ServerController", description = "服务器相关的端点")
public class ServerController {

    @Autowired
    Environment env;

    /**
     * 服务器地址 endpoint
     */
    // @formatter:off
    @GetMapping("/address")
    @Operation(
        summary = "服务地址信息",
        description = "获取服务地址信息",
        responses = {@ApiResponse(
            responseCode = "200",
            description = "成功消息",
            content = {@Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebResponse.class))
            }),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "405", ref = RESPONSE_NOT_ALLOW),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR)
        }
    )
    public ResponseEntity<WebResponse<?>> address() {
        return ResponseEntity.ok(WebResponse.success()
                    .data(String.format("%s@%s",
                        env.getProperty("spring.application.name"),
                        env.getProperty("server.port")))
                    .build());

    }
    // @formatter:on

    /**
     * 服务器条款 endpoint
     */
    // @formatter:off
    @GetMapping("/termsOfService")
    @Operation(summary = "服务条款", description = "获取服务条款信息")
    public String termsOfService() {
        return """
                <body><center><h1>《服务条款》</h1></center>
                      <hr>
                      <h3>本服务主要是为用户提供如下服务</h3>
                      <ol>
                          <li>收集集群下所有外部调用接口文档</li>
                          <li>利用 Gateway 进行统一文档入口</li>
                          <li>利用 Nacos 收集不同服务的接口文档</li>
                          <li>文档格式采用 OpenApi 协议的 Swagger 3.0</li>
                      </ol>
                </body>
                """;
    }
    // @formatter:on

}
