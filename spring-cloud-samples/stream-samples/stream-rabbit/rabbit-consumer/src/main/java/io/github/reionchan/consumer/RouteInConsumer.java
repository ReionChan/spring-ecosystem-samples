package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 路由目标费者，接收上游消息，根据路由键进行本地路由
 *
 * 本地路由表达式：headers['routeKey']
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class RouteInConsumer {

    /**
     * 消费上游消息，通过复合函数将消息传递给本地 {@link RoutingFunction} 进行路由
     */
    @Bean
    public Function<Message<String>, Message<String>> gateway() {
        return msg -> {
            if(msg.getHeaders().get("routeNum").equals(0)) {
                throw new RuntimeException("routeNum is 0");
            }
            log.info("\n\nBINDING[ routeIn(gateway|functionRouter) ] \n\t===> \nROUTER[ routeKey:{} ]\n",
                    msg.getHeaders().get("routeKey"));
            return msg;
        };
    }

    @Bean
    public Consumer<Message<String>> evenRoutedConsumer() {
        return msg -> {
            log.info("\n\nROUTER[ routeKey:{} ] \n\t===> \nCONSUMER[ evenRoutedConsumer ] \n\tMessageBody:{}\n",
                    msg.getHeaders().get("routeKey"), msg.getPayload());
        };
    }

    @Bean
    public Consumer<Message<String>> oddRoutedConsumer() {
        return msg -> {
            log.info("\n\nROUTER[ routeKey:{} ] \n\t===> \nCONSUMER[ oddRoutedConsumer ] \n\tMessageBody:{}\n",
                    msg.getHeaders().get("routeKey"), msg.getPayload());
        };
    }

}
