package io.github.reionchan.config.simple;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@RabbitListener(queues = "hello")
public class SimpleReceiver {
    @RabbitHandler
    public void receive(String in) {
        System.out.println(" [x] Received '" + in + "'");
    }
}
