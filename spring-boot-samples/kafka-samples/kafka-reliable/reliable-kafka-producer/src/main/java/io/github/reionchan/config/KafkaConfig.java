package io.github.reionchan.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static io.github.reionchan.consts.KafkaMQConst.ORDER_TOPIC;
import static io.github.reionchan.consts.KafkaMQConst.TOPIC_PARTITION_SIZE;

/**
 * @author Reion
 * @date 2024-07-10
 **/
@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topicExample() {
        return TopicBuilder.name(ORDER_TOPIC)
                .partitions(TOPIC_PARTITION_SIZE)
                .replicas(1)
                .build();
    }
}
