package io.github.reionchan;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Reion
 * @date 2024-07-09
 **/
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = KafkaBasicBootstrap.class)
@ActiveProfiles("test")
@EmbeddedKafka(topics = "someTopic", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class KafkaTest {

    static {
        System.setProperty(EmbeddedKafkaBroker.BROKER_LIST_PROPERTY, "spring.kafka.bootstrap-servers");
    }

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void test() throws ExecutionException, InterruptedException {
        assertThat(kafkaTemplate).isNotNull();
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("someTopic", "test");
        SendResult<String, String> result = future.get();
        log.info("message:{} - partition:{} - offset:{}", result.getProducerRecord().value(), result.getRecordMetadata().partition(), result.getRecordMetadata().hasOffset());
    }
}
