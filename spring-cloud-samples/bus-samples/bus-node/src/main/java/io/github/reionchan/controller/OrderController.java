package io.github.reionchan.controller;

import io.github.reionchan.config.OrderProperties;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Validated
@RestController
@ControllerAdvice
@RequestMapping("/order")
@EnableConfigurationProperties(OrderProperties.class)
public class OrderController {

    @Autowired
    private OrderProperties orderProperties;

    /**
     * 创建并发送订单创建消息
     */
    @PostMapping(path = "/create")
    public String createOrder(@RequestParam("name") @NotBlank(message = "name is blank!") String name) {
        // 订单创建功能未开启时，直接返回 false
        if (!orderProperties.isCreateEnabled()) {
            log.warn("order.createEnabled = {}", orderProperties.isCreateEnabled());
            return "Creating order is disabled!";
        }
        log.info("Create {} ...", name);
        return "Success to create " + name + " order!";
    }

    /**
     * 统一异常处理
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        return e.getMessage();
    }
}
