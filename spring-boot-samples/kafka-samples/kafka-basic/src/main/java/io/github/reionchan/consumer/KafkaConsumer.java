package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.StopWatch;

import static io.github.reionchan.config.KafkaConst.GROUP;
import static io.github.reionchan.config.KafkaConst.TOPIC;

/**
 * @author Reion
 * @date 2024-07-10
 **/
@Slf4j
public class KafkaConsumer {
    // 手动指定消费分区
    //@KafkaListener(topicPartitions = {@TopicPartition(topic = TOPIC, partitions = "0")})
    // 由组管理自动分配
    @KafkaListener(topics = TOPIC, groupId = GROUP)
    public void receive1(ConsumerRecord<String, String> consumerRecord) {
        receive(consumerRecord.value(), "consumer-1");
    }

    //@KafkaListener(topicPartitions = {@TopicPartition(topic = TOPIC, partitions = "1")})
    @KafkaListener(topics = TOPIC, groupId = GROUP)
    public void receive2(ConsumerRecord<String, String> consumerRecord) {
        receive(consumerRecord.value(), "consumer-2");
    }

    public void receive(String in, String name) {
        StopWatch watch = new StopWatch();
        watch.start();
        StringBuilder builder = new StringBuilder("\n [x] " + name + " Received '" + in + "'");
        doWork(in);
        watch.stop();
        builder.append('\n');
        builder.append("\t [x] " + name + " Done in " +  String.format("%.2f", watch.getTotalTimeSeconds()) + "s");
        System.out.println(builder);
    }

    private void doWork(String in) {
        try {
            for (char ch : in.toCharArray()) {
                if (ch == '.') {
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
