package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Reion
 * @date 2024-07-02
 **/
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = RabbitMQBootstrap.class)
@ActiveProfiles("simple")
public class TestRabbitMQ extends RabbitContainer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testCollections() throws InterruptedException {
        assertThat(rabbitTemplate).isNotNull();
        Thread.sleep(10000);
    }
}
