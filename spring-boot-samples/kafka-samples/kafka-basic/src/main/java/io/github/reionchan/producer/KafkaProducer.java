package io.github.reionchan.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.reionchan.config.KafkaConst.TOPIC;

/**
 * @author Reion
 * @date 2024-07-10
 **/
@Slf4j
public class KafkaProducer {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    private static AtomicInteger counter = new AtomicInteger(0);

    @Scheduled(fixedDelay = 2000, initialDelay = 500)
    public void send() throws ExecutionException, InterruptedException {
        int cnt = counter.incrementAndGet();
        String message = "Hello World! " + cnt;
        // 提供 key 采取 hash 分区
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, String.valueOf(cnt), message);
        SendResult<String, String> result = future.get();
        System.out.println("\n [x] Sent '" + result.getProducerRecord().value() + "' to partition " + result.getProducerRecord().partition());
    }
}
