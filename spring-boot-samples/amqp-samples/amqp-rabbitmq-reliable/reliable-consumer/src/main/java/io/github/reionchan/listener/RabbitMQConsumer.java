package io.github.reionchan.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.github.reionchan.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.github.reionchan.consts.RabbitMQConst.*;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
@Component
@AllArgsConstructor
public class RabbitMQConsumer {

    private ObjectMapper objectMapper;

    @RabbitListener(queues = ORDER_QUEUE)
    public void consumer(Channel channel, Message message) throws IOException {
        String msgBodyStr = new String(message.getBody());
        log.info("接收消息：{}", msgBodyStr);
        String msgId = message.getMessageProperties().getHeader(HEADER_MESSAGE_CORRELATION);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        OrderDto dto = objectMapper.readValue(msgBodyStr, OrderDto.class);
        log.info("correlationId：{} - orderId: {} - deliveryTag: {}", msgId, dto.getId(), deliveryTag);

        try {
            if (deliveryTag % 5 == 0) {
                throw new RuntimeException("模拟消费异常");
            }
            if (deliveryTag % 6 == 0) {
                // 模拟拒绝不重入队列，直接进入 DLQ
                channel.basicReject(deliveryTag, false);
            }
            log.info("正确消费订单：{}", dto);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("异常消息：{}", deliveryTag);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    @RabbitListener(queues = DLQ)
    public void dlqConsumer(Channel channel, Message message) throws IOException {
        String msgBodyStr = new String(message.getBody());
        log.info("接收死信消息：{}", msgBodyStr);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        channel.basicAck(deliveryTag, false);
    }
}
