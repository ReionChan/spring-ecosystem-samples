package io.github.reionchan.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * RabbitMQ RouteIn DLQ 消息队列消费者
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class RouteInDlqConsumer {

    private static final String ORIGINAL_QUEUE = "routed.msg.downstreamGrp";
    private static final String DLQ = ORIGINAL_QUEUE + ".dlq";
    private static final String PARKING_LOT = ORIGINAL_QUEUE + ".parkingLot";

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 处理 RabbitMQ DLQ 中的失败消息
     */
    @RabbitListener(queues = DLQ)
    public void logDlq(Message failedMessage) {
        // 打印失败消息
        logMessage(failedMessage, false);
        // 将消息放入 parkingLot 队列
        this.rabbitTemplate.send(PARKING_LOT, failedMessage);
    }

    @Bean
    public Queue parkingLot() {
        return new Queue(PARKING_LOT);
    }

    private static void logMessage(Message message, boolean isPrintStacktrace) {
        try {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            String payload = new String(message.getBody(), Charset.defaultCharset());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = formatter.format(message.getMessageProperties().getTimestamp());

            StringBuffer strBuffer = new StringBuffer();
            strBuffer.append("\nMessage Properties:\n");
            strBuffer.append('\t').append("messageId").append(" : ").append(message.getMessageProperties().getMessageId()).append("\n");
            strBuffer.append('\t').append("timestamp").append(" : ").append(timestamp).append("\n");
            strBuffer.append('\t').append("priority").append(" : ").append(message.getMessageProperties().getPriority()).append("\n");

            strBuffer.append("Message Headers:\n");
            for (String key : headers.keySet()) {
                if (key.equals("x-exception-stacktrace") && !isPrintStacktrace) {
                    continue;
                }
                strBuffer.append('\t').append(key).append(" : ").append(headers.get(key)).append("\n");
            }
            strBuffer.append("Message Payload:\n\t" + payload + "\n");

            log.info("\n\n=== DEAD LETTER INFO ===\n{}", strBuffer.toString());
        } catch (Exception e) {
            log.error("Print Message info error:{}", e.getMessage());
        }
    }
}
