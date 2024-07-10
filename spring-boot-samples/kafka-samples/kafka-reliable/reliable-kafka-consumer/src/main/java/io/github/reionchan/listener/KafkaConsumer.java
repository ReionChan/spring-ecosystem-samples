package io.github.reionchan.listener;

import io.github.reionchan.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.reionchan.consts.KafkaMQConst.ORDER_GROUP;
import static io.github.reionchan.consts.KafkaMQConst.ORDER_TOPIC;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
public class KafkaConsumer {

    private static AtomicInteger[] counter = {new AtomicInteger(0), new AtomicInteger(0)};

    @KafkaListener(topics = ORDER_TOPIC, groupId = ORDER_GROUP)
    public void receive1(ConsumerRecord<String, OrderDto> consumerRecord, Acknowledgment ack) {
        try {
            if (counter[0].incrementAndGet() % 3 == 0) {
                throw new RuntimeException("error");
            }
            receive(consumerRecord.value(), "consumer-1");
            ack.acknowledge();
            log.info("{} {} ack {}", counter[0].get(), "consumer-1", consumerRecord.value().getId());
        } catch (Exception e) {
            log.error("{} {} nack {}", counter[0].get(), "consumer-1", consumerRecord.value().getId());
            ack.nack(Duration.ofSeconds(1));
        }
    }

    @KafkaListener(topics = ORDER_TOPIC, groupId = ORDER_GROUP)
    public void receive2(ConsumerRecord<String, OrderDto> consumerRecord, Acknowledgment ack) {
        try {
            if (counter[1].incrementAndGet() % 5 == 0) {
                throw new RuntimeException("error");
            }
            receive(consumerRecord.value(), "consumer-2");
            ack.acknowledge();
            log.info("{} {} ack {}", counter[1].get(), "consumer-2", consumerRecord.value().getId());
        } catch (Exception e) {
            log.error("{} {} nack {}", counter[1].get(), "consumer-2", consumerRecord.value().getId());
            ack.nack(Duration.ofSeconds(1));
        }
    }

    public void receive(OrderDto in, String name) {
        StopWatch watch = new StopWatch();
        watch.start();
        StringBuilder builder = new StringBuilder("\n [x] " + name + " Received '" + in.getId() + "'");
        watch.stop();
        builder.append('\n');
        builder.append("\t [x] " + name + " Done in " +  String.format("%.2f", watch.getTotalTimeSeconds()) + "s");
        System.out.println(builder);
    }
}
