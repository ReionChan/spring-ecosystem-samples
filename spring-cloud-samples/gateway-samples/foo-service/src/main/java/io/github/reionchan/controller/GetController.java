package io.github.reionchan.controller;

import io.github.reionchan.response.WebResponse;
import io.github.reionchan.vo.RequestVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static io.github.reionchan.config.SpringDocConfig.RESPONSE_ERROR;
import static io.github.reionchan.config.SpringDocConfig.RESPONSE_FAIL;

/**
 * Get 请求控制器
 *
 * @author Reion
 * @date 2023-06-06
 **/
@RestController
@Validated
@RequestMapping("/get")
@Tag(name = "GetController", description = "Get 相关请求端点")
public class GetController {

    /**
     * 带有路径参数的 Get
     */
    // @formatter:off
    @GetMapping("/user/{id}")
    @Operation(summary = "Path 参数请求", description = "Path 参数的 Get 请求",
        responses = {@ApiResponse(
            responseCode = "200",
            description = "成功消息",
            content = {@Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WebResponse.class))}),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR),
    })
    public ResponseEntity<WebResponse<?>> queryVar(
        @PathVariable
        @Schema(description = "用户ID, 需要为正数")
        @Positive(message = "id 需为正数") Integer id) {
        RequestVo vo = RequestVo.builder().id(id).userName("zhangsan").age(20).build();
        return ResponseEntity.ok(WebResponse.success().data(vo).build());
    }
    // @formatter:on

    /**
     * 带有 Query 参数的 Get
     */
    // @formatter:off
    @GetMapping("/user")
    @Operation(summary = "Query 参数请求", description = "Query 参数的 Get 请求",
        responses = {@ApiResponse(
            responseCode = "200",
            description = "成功消息",
            content = {@Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WebResponse.class))}),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR)
    })
    public ResponseEntity<WebResponse<?>> queryVar(
        @RequestParam @Schema(description = "用户ID, 需要为正数")
        @Positive(message = "id 需为正数") Integer id,
        @RequestParam @Schema(description = "用户名，长度5~30，开头为字母，之后数字、字母、_")
        @Length(min = 5, max = 30, message = "用户名长度 5 ~ 30")
        @Pattern(regexp = "[a-zA-Z][0-9a-zA-Z_]+", message = "开头为字母，之后数字、字母、_") String userName) {
        RequestVo vo = RequestVo.builder().id(id).userName(userName).age(10).build();
        return ResponseEntity.ok(WebResponse.success().data(vo).build());
    }
    // @formatter:on
}
