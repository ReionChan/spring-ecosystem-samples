package io.github.reionchan.exception;

import io.github.reionchan.response.WebResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MethodNotAllowedException;

/**
 * 全局异常处理
 *
 * @author Reion
 * @date 2023-06-06
 **/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 客户端参数校验异常
     */
    @ExceptionHandler({WebExchangeBindException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<WebResponse<?>> handleValidateException(Exception e) {
        String errorMessage = Errors.class.isAssignableFrom(e.getClass())
                ? ((Errors) e).getAllErrors().get(0).getDefaultMessage() : e.getMessage();
        log.error("客户端参数异常: ", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponse.fail(errorMessage).build());
    }


    /**
     * 客户端请求方法异常
     */
    @ExceptionHandler(MethodNotAllowedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<WebResponse<?>> handleMethodNotSupportedException(MethodNotAllowedException e) {
        log.error("客户端请求方法异常: ", e);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(WebResponse.fail(String.format("不支持的请求方法：{}", e.getHttpMethod())).build());
    }

    /**
     * 服务端兜底异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WebResponse<?>> handleInternalServerException(Exception e) {
        log.error("服务端内部异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e).build());
    }
}
