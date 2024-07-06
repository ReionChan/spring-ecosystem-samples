package io.github.reionchan.consts;

/**
 * @author Reion
 * @date 2024-07-05
 **/
public interface RabbitMQConst {
    String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";
    String ORDER_QUEUE = "order.queue";
    String ORDER_ROUTING_KEY = "order.routing";
    String HEADER_MESSAGE_CORRELATION = "spring_returned_message_correlation";

    String DLX = "order.dlx";
    String DLX_ROUTING_KEY = "order.dlx.routing.key";
    String DLQ = ORDER_QUEUE + ".dlq";
}
