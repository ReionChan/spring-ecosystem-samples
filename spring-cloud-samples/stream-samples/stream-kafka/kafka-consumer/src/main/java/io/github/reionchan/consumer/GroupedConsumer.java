package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * 分组型消费者
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class GroupedConsumer {

    /**
     * createGrp1 分组消费者，接收订单创建消息
     */
    @Bean
    public Consumer<String> consumer1InCreateGrp() {
        return msg -> log.info("Group one receive a order, name is : {}", msg);
    }

    /**
     * createGrp2 分组消费者，接收订单创建消息
     */
    @Bean
    public Consumer<String> consumer2InCreateGrp() {
        return msg -> log.info("Group two receive a order, name is : {}", msg);
    }
}
