package io.github.reionchan.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

import static io.github.reionchan.consts.RabbitMQConst.HEADER_MESSAGE_CORRELATION;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
public class RabbitMQCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private Map<String, ReturnedMessage> returnMessageCache = new HashMap<>();
    private Map<String, Message> needConfirmMap = new HashMap<>();

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        log.info("correlationData: {} - ack: {}", correlationData.getId(), ack);
        if (!ack) {
            log.info("投递交换机失败：{} 原因：{}", correlationData.getId(), cause);
            returnMessageCache.put(correlationData.getId(), correlationData.getReturned());
        } else {
            needConfirmMap.remove(correlationData.getId());
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.info("returnedMessage: {}", returned);
        String msgId = returned.getMessage().getMessageProperties().getHeader(HEADER_MESSAGE_CORRELATION);
        needConfirmMap.computeIfAbsent(msgId, id -> returned.getMessage());
        returnMessageCache.put(msgId, returned);
    }

    public Map<String, ReturnedMessage> getReturnMessageCache() {
        return returnMessageCache;
    }

    public Map<String, Message> getNeedConfirmMap() {
        return needConfirmMap;
    }
}
