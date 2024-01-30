package io.github.reionchan.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * Kafka RouteIn DLQ 消息队列消费者
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class RouteInDlqConsumer {

    private static final String ORIGINAL_TOPIC = "routed.msg";
    private static final String DLQ = ORIGINAL_TOPIC + ".dlq";

    /**
     * 处理 Kafka DLQ 中的失败消息
     */
    @KafkaListener(topics = DLQ, groupId = "downstreamGrp")
    public void logDlq(Object failedMessage) {
        // 打印失败消息
        logMessage((ConsumerRecord) failedMessage, false);
    }

    private static void logMessage(ConsumerRecord<byte[], byte[]> message, boolean isPrintStacktrace) {
        try {
            RecordHeaders headers = (RecordHeaders) message.headers();
            String payload = new String(message.value(), Charset.defaultCharset());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = formatter.format(new Date(message.timestamp()));

            StringBuffer strBuffer = new StringBuffer();
            strBuffer.append("\nMessage Properties:\n");
            strBuffer.append('\t').append("messageTopic").append(" : ").append(message.topic()).append("\n");
            strBuffer.append('\t').append("timestamp").append(" : ").append(timestamp).append("\n");

            strBuffer.append("Message Headers:\n");
            Iterator<Header> iterable = headers.iterator();
            while(iterable.hasNext()) {
                Header header = iterable.next();
                if (header.key().equals("x-exception-stacktrace") && !isPrintStacktrace) {
                    continue;
                }
                strBuffer.append('\t').append(header.key()).append(" : ").append(header.value()).append("\n");
            }

            strBuffer.append("Message Payload:\n\t" + payload + "\n");

            log.info("\n\n=== DEAD LETTER INFO ===\n{}", strBuffer.toString());
        } catch (Exception e) {
            log.error("Print Message info error:{}", e.getMessage());
        }
    }
}
