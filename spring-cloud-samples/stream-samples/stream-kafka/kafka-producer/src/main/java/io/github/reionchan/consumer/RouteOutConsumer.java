package io.github.reionchan.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;

/**
 * 路由目标消费者，用来向下游输出消息
 *
 * 与路由函数解析路由表达式的结果相对应的消息消费者函数，
 * 同时该消费者函数利用 <code>StreamBridge</code> 发送消息到输出绑定
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class RouteOutConsumer {

    @Resource
    private StreamBridge streamBridge;

    /**
     * 偶数消息路由目标消费者，且将消息发送到输出绑定
     */
    @Bean
    public Consumer<Message<String>> even2RouteOut() {
        return msg -> {
            log.info("\n\nROUTER[ message.headers.routeNum:{} ] \n\t===> \nCONSUMER[ even2RouteOut ] \n\t===> \nBINDING[ routeOut(routeKey:evenRoutedConsumer) ]\n",
                  msg.getHeaders().get("routeNum"));
            // 设置包含下游头部路由键值的消息
            Message<String> newMsg = MessageBuilder.withPayload(msg.getPayload())
                    .setHeader("routeNum", msg.getHeaders().get("routeNum"))
                    .setHeader("routeKey", "evenRoutedConsumer").build();
            // 发送新消息到输出绑定
            streamBridge.send("routeOut", newMsg);
        };
    }

    /**
     * 奇数消息路由目标消费者，且将消息发送到输出绑定
     */
    @Bean
    public Consumer<Message<String>> odd2RouteOut() {
        return msg -> {
            log.info("\n\nROUTER[ message.headers.routeNum:{} ] \n\t===> \nCONSUMER[ odd2RouteOut ] \n\t===> \nBINDING[ routeOut(routeKey:oddRoutedConsumer) ]\n",
                    msg.getHeaders().get("routeNum"));
            // 设置包含下游头部路由键值的消息
            Message<String> newMsg = MessageBuilder.withPayload(msg.getPayload())
                    .setHeader("routeNum", msg.getHeaders().get("routeNum"))
                    .setHeader("routeKey", "oddRoutedConsumer").build();
            // 发送新消息到输出绑定
            streamBridge.send("routeOut", newMsg);
        };
    }

}
