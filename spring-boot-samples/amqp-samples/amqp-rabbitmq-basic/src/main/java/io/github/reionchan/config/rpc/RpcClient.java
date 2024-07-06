package io.github.reionchan.config.rpc;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicInteger;

import static io.github.reionchan.config.rpc.RpcConfig.ROUTING_KEY;

public class RpcClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DirectExchange directExchange;

    int start = 0;

    @Scheduled(fixedDelay = 1000, initialDelay = 500)
    public void send() {
        System.out.println(" [x] Requesting fib(" + start + ")");
        Integer response = (Integer) rabbitTemplate.convertSendAndReceive
                (directExchange.getName(), ROUTING_KEY, start++);
        System.out.println(" [.] Got '" + response + "'");
    }
}
