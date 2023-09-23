package io.github.reionchan.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;

/**
 * Web 响应包装类
 *
 * @author Reion
 * @date 2023-06-06
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"timeStamp", "code", "status", "success", "message", "data"})
@ToString
@Schema(name = "WebResponse", description = "接口响应对象")
public class WebResponse<T> {

    @JsonProperty("timeStamp")
    @Schema(title = "响应时间戳")
    private final long timeStamp = System.currentTimeMillis()/1000;

    @JsonProperty("code")
    @Schema(title = "状态码")
    private int code;

    @JsonProperty("status")
    @Schema(title = "状态信息")
    private String status;

    @JsonProperty("success")
    @Schema(title = "成功标志")
    private boolean success;

    @JsonProperty("message")
    @Schema(title = "失败异常消息")
    private String message;

    @JsonProperty("data")
    @Schema(title = "响应数据")
    private T data;

    /**
     * 客户端异常构造器
     */
    public static final <T> WebResponseBuilder<T> fail(@Nullable String message) {
        return new WebResponseBuilder<T>()
                .code(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .success(false)
                .message(Strings.isBlank(message) ? HttpStatus.BAD_REQUEST.getReasonPhrase() : message);
    }

    /**
     * 客户端异常构造器
     */
    public static final <T> WebResponseBuilder<T> fail() {
        return fail(null);
    }

    /**
     * 服务端异常构造器
     */
    public static final <T> WebResponseBuilder<T> error(@Nullable String message, Throwable t) {
        return new WebResponseBuilder<T>()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .success(false)
                .message(Strings.isBlank(message) ? t.getMessage() : message);
    }

    /**
     * 服务端异常构造器
     */
    public static final <T> WebResponseBuilder<T> error(Throwable t) {
        return error(null, t);
    }

    /**
     * 服务端正常响应构造器
     */
    public static final <T> WebResponseBuilder<T> success(@Nullable String message) {
        return new WebResponseBuilder<T>()
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK.getReasonPhrase())
                .success(true)
                .message(Strings.isBlank(message) ? HttpStatus.OK.getReasonPhrase() : message);
    }

    /**
     * 服务端正常响应构造器
     */
    public static final <T> WebResponseBuilder<T> success() {
        return success(null);
    }
}
