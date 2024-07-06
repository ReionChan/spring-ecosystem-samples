package io.github.reionchan.config.routing;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicInteger;

public class RoutingSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DirectExchange directExchange;

    AtomicInteger index = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);

    private final String[] keys = {"red", "green", "blue"};

    @Scheduled(fixedDelay = 1000, initialDelay = 500)
    public void send() {
        StringBuilder builder = new StringBuilder("Hello to ");
        if (index.incrementAndGet() == 3) {
            index.set(0);
        }
        String key = keys[index.get()];
        builder.append(key).append(' ').append(count.incrementAndGet());
        String message = builder.toString();
        rabbitTemplate.convertAndSend(directExchange.getName(), key,  message);
        System.out.println("\n [x] Sent '" + message + "'");
    }
}
