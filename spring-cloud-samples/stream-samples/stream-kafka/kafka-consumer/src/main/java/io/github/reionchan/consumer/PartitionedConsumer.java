package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 分区型消费者
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class PartitionedConsumer {

    /**
     * 头部字段 partitionKey 进行分区
     * 消费 partitionKey=1 的奇数消息
     */
    @Bean
    public Consumer<Message<String>> oddPartitionConsumer() {
        return msg -> log.info("partitionKey: {} is odd，oddPartitionConsumer get msg body: {}",
                msg.getHeaders().get("partitionKey"), msg.getPayload());
    }

    /**
     * 头部字段 partitionKey 进行分区
     * 消费 partitionKey=0 的偶数消息
     */
    @Bean
    public Consumer<Message<String>> evenPartitionConsumer() {
        return msg -> log.info("partitionKey: {} is even，evenPartitionConsumer get msg body: {}",
                msg.getHeaders().get("partitionKey"), msg.getPayload());
    }
}
