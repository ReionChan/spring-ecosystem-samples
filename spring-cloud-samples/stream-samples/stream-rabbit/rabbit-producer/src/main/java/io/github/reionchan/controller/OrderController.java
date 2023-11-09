package io.github.reionchan.controller;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
public class OrderController {

    @Resource
    private StreamBridge bridge;

    @Resource
    private RoutingFunction routingFunction;

    /**
     * 创建并发送订单创建消息
     */
    @PostMapping(path = "/create")
    public boolean createOrder(@RequestParam("name") @NotBlank(message = "name is blank!") String name) {
        log.info("Create {} ...", name);
        // 将 WebEndpoint 消息桥接到消息绑定 groupOut
        bridge.send("groupOut", name);
        return true;
    }

    /**
     * 发送分区类型的消息
     *
     * 根据消息长度的奇偶来决定所属分区
     */
    @PostMapping(path = "/partitionedMsg")
    public boolean partitionedMsg(@RequestParam("msg") @NotBlank(message = "msg is blank!") String msg) {
        log.info("Send message: {}, message length: {}", msg, msg.length());
        Message<String> message = MessageBuilder.withPayload(msg).setHeader("partitionKey", msg.length()).build();
        // 将 WebEndpoint 消息桥接到消息绑定 partOut
        bridge.send("partOut", message);
        return true;
    }

    /**
     * 设置包含头属性的消息，将其交给路由函数进行消息路由
     *
     * 发送 routeNum=0 的消息，将引起下游消费者异常，
     * 将激发消息重试，然后将重试次数内还未成功的消息发送到 DLQ。
     */
    @PostMapping(path = "/routedMsg")
    public boolean routedMsg(@RequestParam("routeNum") @PositiveOrZero(message = "routeNum is negative!") Integer routeNum) {
        Message<String> message = MessageBuilder.withPayload("WebEndpoint send a Number: " + routeNum)
                .setHeader("routeNum", routeNum).build();
        log.info("\n\nWEB-ENDPOINT[ routeNum:{} ] \n\t===> \nROUTER[ message.headers.routeNum:{} ]\n", routeNum, message.getHeaders().get("routeNum"));
        // 路由消息
        routingFunction.apply(message);
        return true;
    }

    /**
     * 统一异常处理
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        return e.getMessage();
    }
}
