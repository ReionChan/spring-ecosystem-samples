package io.github.reionchan.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reionchan.callback.RabbitMQCallback;
import io.github.reionchan.dto.OrderDto;
import io.github.reionchan.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

import static io.github.reionchan.consts.RabbitMQConst.ORDER_DIRECT_EXCHANGE;
import static io.github.reionchan.consts.RabbitMQConst.ORDER_ROUTING_KEY;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static int count = 0;

    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private RabbitMQCallback callback;

    @Override
    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    public String createOrder() {

        if (callback.getReturnMessageCache().keySet().size() > 0) {
            String msgId = callback.getReturnMessageCache().keySet().stream().findFirst().get();
            Message msg = callback.getNeedConfirmMap().get(msgId);
            CorrelationData correlationData = new CorrelationData(msgId);
            rabbitTemplate.convertAndSend(ORDER_DIRECT_EXCHANGE, ORDER_ROUTING_KEY, msg, correlationData);
            callback.getReturnMessageCache().remove(msgId);
            return msgId;
        }

        count++;
        String exchange = ORDER_DIRECT_EXCHANGE;
        // 每三条消息模拟生成一条无法投递的消息，验证消息确认回调
        if (count % 3 == 0) {
            exchange += ".error";
            count = 0;
        }

        // 调度任务模拟生产订单
        OrderDto orderDto = new OrderDto();
        orderDto.setId(IdUtil.simpleUUID());
        BigDecimal price = NumberUtil.round(NumberUtil.generateRandomNumber(1000, 5000, 1)[0] / 3.0d, 2);
        orderDto.setPrice(price);
        orderDto.setCreateTime(new Date());

        CorrelationData correlationData = new CorrelationData(orderDto.getId());
        try {
            Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(orderDto))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            // 放入待确认缓存
            callback.getNeedConfirmMap().put(correlationData.getId(), message);
            rabbitTemplate.convertAndSend(exchange, ORDER_ROUTING_KEY, message, correlationData);
        } catch (JsonProcessingException e) {
            log.error("发生消息异常", e);
            return e.getMessage();
        }

        return orderDto.getId();
    }
}
