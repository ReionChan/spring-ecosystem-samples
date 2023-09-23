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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@Validated
public class PostController {

    /**
     * Json 格式的 Post 请求
     * ============================================================================
     * 方法参数 RequestVo 采用 @RequestBody 注释，表示请求的 Body 将被映射为 RequestVo Bean
     * 此外，@RequestBody 不支持 multipart/form-data、application/x-www-form-urlencoded 的请求类型
     * 所以下面两种请求方式，请不要把 RequestVo 注解 @RequestBody
     */
    // @formatter:off
    @PostMapping(value = "/jsonBody", consumes = MediaType.APPLICATION_JSON_VALUE)
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
    public ResponseEntity<WebResponse<?>> jsonBody(@Valid @RequestBody RequestVo vo) {
        return ResponseEntity.ok(WebResponse.success().data(vo).build());
    }
    // @formatter:on

    /**
     * form data 格式的 Post 请求
     * =========================
     * 将请求参数放到 Body 中，每一个参数占用一个 boundary，例如：
     * 请求：
     * ----------------------------------------------------------
     * curl -X 'POST' \
     *   'http://localhost:8080/foo-service/post/formDataBody' \
     *   -H 'accept: application/json' \
     *   -H 'Content-Type: multipart/form-data' \
     *   -F 'userName=zhangsan' \
     *   -F 'age=10'
     *
     * Body 请求体：
     * ------------------------------------------------------------
     * ------WebKitFormBoundaryx9KCjEiDGEjD7ANu
     * Content-Disposition: form-data; name="userName"
     *
     * zhangsan
     * ------WebKitFormBoundaryx9KCjEiDGEjD7ANu
     * Content-Disposition: form-data; name="age"
     *
     * 10
     * ------WebKitFormBoundaryx9KCjEiDGEjD7ANu--
     *
     */
    // @formatter:off
    @PostMapping(value = "/formDataBody")
    @Operation(
        summary = "Form Data Post",
        description = "Form Data 格式的 Post 请求",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "参考RequestVo 值对象说明",
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schemaProperties = {
                    @SchemaProperty(name = "userName", schema = @Schema(type = "string",
                            description = "用户名，长度5~30，开头为字母，之后数字、字母、_")),
                    @SchemaProperty(name = "age", schema = @Schema(type = "integer",
                            description = "年龄区间 [0, 120]"))
        })),
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
    public ResponseEntity<WebResponse<?>> formDataBody(@Valid RequestVo vo) {
        return ResponseEntity.ok(WebResponse.success().data(vo).build());
    }
    // @formatter:on

    /**
     * x-www-form-urlencoded 格式的 Post 请求
     * =====================================
     * 将 Query 参数放到 Body 中进行 Post 请求，例如：
     * 请求：
     * ----------------------------------------------------------
     * curl -X 'POST' \
     *   'http://localhost:8080/foo-service/post/formDataBody' \
     *   -H 'accept: application/json' \
     *   -H 'Content-Type: application/x-www-form-urlencoded' \
     *   -d 'userName=zhangsan&age=10'
     *
     * Body 请求体：
     * -----------------------------------------------------------
     * userName=zhangsan&age=10
     *
     */
    // @formatter:off
    @PostMapping(value = "/formUrlEncodedDataBody", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
        summary = "Form Url Encoded Post",
        description = "x-www-form-urlencoded 格式的 Post 请求",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "参考 RequestVo 值对象说明",
            content = @Content(
                mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                schemaProperties = {
                    @SchemaProperty(name = "userName", schema = @Schema(type = "string", description = "用户名，长度5~30，开头为字母，之后数字、字母、_")),
                    @SchemaProperty(name = "age", schema = @Schema(type = "integer", description = "年龄区间 [0, 120]"))
        })),
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
            summary = "文件上传格式 Post",
            description = "文件上传格式的 Post 请求",
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
    @Valid
    public ResponseEntity<WebResponse<?>> multipartFileBody(
            @RequestPart("file") @Schema(title = "文件", description = "包括但不限于图片、文档等", type = "string", format = "binary") MultipartFile file,
            @RequestPart("fileName") @Schema(title = "文件名", description = "5~20字符文件名，且需带扩展名", minLength = 5)
            @Pattern(regexp = "^[a-zA-Z][a-zA-Z_-]{4,19}\\.[a-zA-Z0-9]+$", message = "文件名5~20字符且需带扩展名") String fileName) {
        return ResponseEntity.ok(WebResponse.success(fileName + " 上传成功").build());
    }
    // @formatter:on
}
