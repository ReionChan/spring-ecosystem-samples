package io.github.reionchan.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错误控制器
 *
 * @author Reion
 * @date 2023-04-22
 **/
@RestController
public class CustomizeErrorController implements ErrorController {

    /**
     * 错误页面
     */
    @GetMapping("/errorPage")
    public String errorPage(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        Integer statusCode = null;
        HttpStatus sta = HttpStatus.OK;
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            sta = HttpStatus.valueOf(statusCode);
        }

        return """
                 <h1>错误页面</h1>
                 <ul>
                     <li><a href="/">首页</a></li>
                     """ +
                    "<li><font color='red'>状态码：" + sta.value() + "</font></li>" +
                    "<li><font color='red'>异常消息：" + sta.getReasonPhrase() + "</font></li>" +
                """
                </ul>
                """;
    }
}
