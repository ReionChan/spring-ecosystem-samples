package io.github.reionchan.controller;

import io.github.reionchan.response.WebResponse;
import io.github.reionchan.vo.RequestVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import static io.github.reionchan.config.SpringDocConfig.RESPONSE_ERROR;
import static io.github.reionchan.config.SpringDocConfig.RESPONSE_FAIL;

/**
 * Post 请求示例控制器
 *
 * @author Reion
 * @date 2023-06-06
 **/
@RestController
@RequestMapping("/post")
@Tag(name = "PostController", description = "Post 相关请求端点")
public class PostController {
    /**
     * Json 格式的 Post 请求
     */
    // @formatter:off
    @PostMapping("/jsonBody")
    @Operation(
        summary = "Json Post",
        description = "Json 格式的 Post 请求",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "RequestVo 值对象",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RequestVo.class))),
        responses = {@ApiResponse(
            responseCode = "200",
            description = "成功消息",
            content = {@Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebResponse.class))}
            ),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR),
        }
    )
    public ResponseEntity<WebResponse<?>> jsonBody(@RequestBody RequestVo vo) {
        return ResponseEntity.ok(WebResponse.success().data(vo).build());
    }
    // @formatter:on

    /**
     * form data 格式的 Post 请求
     */
    // @formatter:off
    @PostMapping("/formDataBody")
    @Operation(
        summary = "Form Data Post",
        description = "Form Data 格式的 Post 请求",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "参考 RequestVo 值对象说明",
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schemaProperties = {
                    @SchemaProperty(name = "userName", schema = @Schema(minLength = 5, maxLength = 30, type = "String")),
                    @SchemaProperty(name = "age", schema = @Schema(minimum = "0", maximum = "120", type = "int"))
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功消息",
                content = {@Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WebResponse.class))}),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR)
        }
    )
    public ResponseEntity<WebResponse<?>> formDataBody(@Valid RequestVo requestVo) {
        return ResponseEntity.ok(WebResponse.success().data(requestVo).build());
    }
    // @formatter:on

    /**
     * x-www-form-urlencoded 格式的 Post 请求
     */
    // @formatter:off
    @PostMapping("/formUrlEncodedDataBody")
    @Operation(
        summary = "Form Url Encoded Post",
        description = "x-www-form-urlencoded 格式的 Post 请求",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "参考 RequestVo 值对象说明",
            content = @Content(
                mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                schemaProperties = {
                    @SchemaProperty(name = "userName", schema = @Schema(minLength = 5, maxLength = 30, type = "String")),
                    @SchemaProperty(name = "age", schema = @Schema(minimum = "0", maximum = "120", type = "int"))
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "成功消息",
                content = {@Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = WebResponse.class))}),
            @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
            @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR)
        }
    )
    public ResponseEntity<WebResponse<?>> formUrlEncodedDataBody(@Valid RequestVo requestVo) {
        return ResponseEntity.ok(WebResponse.success().data(requestVo).build());
    }
    // @formatter:on

    /**
     * Multipart 文件上传 Post 请求
     */
    // @formatter:off
    @PostMapping(value = "/multipartFileBody", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "文件格式 Post",
            description = "文件格式的 Post 请求",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "成功消息",
                            content = {@Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = WebResponse.class))}),
                    @ApiResponse(responseCode = "400", ref = RESPONSE_FAIL),
                    @ApiResponse(responseCode = "500", ref = RESPONSE_ERROR)
            }
    )
    public ResponseEntity<WebResponse<?>> multipartFileBody(@RequestPart MultipartFile file, @RequestPart String fileName) {
        return ResponseEntity.ok(WebResponse.success(fileName + " 上传成功").build());
    }
    // @formatter:on
}
