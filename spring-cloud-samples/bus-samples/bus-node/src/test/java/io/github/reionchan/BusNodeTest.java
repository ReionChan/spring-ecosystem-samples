package io.github.reionchan;

import io.github.reionchan.event.NotificationRemoteApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.bus.BusConsumer;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bus 节点测试
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
@SpringBootTest(properties = {
        "trace=false",
        "debug=false",
        "spring.cloud.bus.id=bus-node:${server.port}:${cachedrandom.${spring.application.name}.value}"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
public class BusNodeTest {

    @MockBean
    private BusConsumer busConsumer;

    /**
     * SCSt test binder 的输入源
     * 它与应用的输入源绑定在一起，向它发送消息即可让应用消费者接收，
     * 用来模拟应用接收到总线的消息。
     */
    @Autowired
    private InputDestination input;

    @Autowired
    private BusProperties busProperties;

    @Autowired
    private Destination.Factory destFactory;

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Bus Node receive environment change event")
    class EnvironmentChangeEventTest {
        @Test
        @DisplayName("Bus Node receive spring.cloud.bus.trace.enabled change event")
        @Order(3)
        public void testBusTraceChange() {
            // 构造环境变更事件
            Map<String, String> envChange = Map.of("spring.cloud.bus.trace.enabled", "true");
            RemoteApplicationEvent event = new EnvironmentChangeRemoteApplicationEvent(this, busProperties.getId(),
                    destFactory.getDestination("bus-node"), envChange);
            Message<RemoteApplicationEvent> message = MessageBuilder.withPayload(event).build();

            // 向目标输入源发送总线刷新事件
            input.send(message);

            // 验证总线消费者接收到 EnvironmentChangeRemoteApplicationEvent 总线刷新事件，并被调用一次，且更改的属性与发送时一致
            verify(busConsumer, times(1)).accept(argThat(e -> {
                EnvironmentChangeRemoteApplicationEvent changeEvent = (EnvironmentChangeRemoteApplicationEvent) e;
                Map<String, String> values = changeEvent.getValues();
                return values.containsKey("spring.cloud.bus.trace.enabled") && values.get("spring.cloud.bus.trace.enabled").equals("true");
            }));

            // 验证完后，代理 mockBean 即 busConsumer 发送事件，激发真正的环境变量改变
            ctx.publishEvent(event);
            assertEquals("true", ctx.getEnvironment().getProperty("spring.cloud.bus.trace.enabled"));
        }

        @Test
        @DisplayName("Bus Node receive order.createEnabled change event")
        @Order(4)
        public void testOrderCreateEnableChange() throws Exception {
            // 构造环境变更事件
            Map<String, String> envChange = Map.of("order.createEnabled", "true");
            RemoteApplicationEvent event = new EnvironmentChangeRemoteApplicationEvent(this, busProperties.getId(),
                    destFactory.getDestination("bus-node"), envChange);
            Message<RemoteApplicationEvent> message = MessageBuilder.withPayload(event).build();

            // 向目标输入源发送总线刷新事件
            input.send(message);

            // 验证总线消费者接收到 EnvironmentChangeRemoteApplicationEvent 总线刷新事件，并被调用一次，且更改的属性与发送时一致
            verify(busConsumer, times(1)).accept(argThat(e -> {
                EnvironmentChangeRemoteApplicationEvent changeEvent = (EnvironmentChangeRemoteApplicationEvent) e;
                Map<String, String> values = changeEvent.getValues();
                return values.containsKey("order.createEnabled") && values.get("order.createEnabled").equals("true");
            }));


            // 环境变量未改变前，验证 /order/create 接口返回创建订单被禁止
            mockMvc.perform(post("/order/create")
                .param("name", "testing"))
                    .andExpectAll(
                        status().isOk(),
                        content().string("Creating order is disabled!")
                    );

            // 验证完后，代理 mockBean 即 busConsumer 发送事件，激发真正的环境变量改变
            ctx.publishEvent(event);
            assertEquals("true", ctx.getEnvironment().getProperty("order.createEnabled"));

            // 环境变量未改变后，验证 /order/create 接口返回创建订单成功
            mockMvc.perform(post("/order/create")
                .param("name", "testing"))
                    .andExpectAll(
                        status().isOk(),
                        content().string("Success to create testing order!")
            );
        }

    }

    @Test
    @DisplayName("Bus Node receive refresh event")
    @Order(1)
    public void testBusRefresh() {
        // 构造总线刷新事件
        RemoteApplicationEvent event = new RefreshRemoteApplicationEvent(this, busProperties.getId(),
                destFactory.getDestination("bus-node"));
        Message<RemoteApplicationEvent> message = MessageBuilder.withPayload(event).build();

        // 向目标输入源发送总线刷新事件
        input.send(message);

        // 验证总线消费者接收到 RefreshRemoteApplicationEvent 总线刷新事件，并被调用一次
        verify(busConsumer, times(1)).accept(any(RefreshRemoteApplicationEvent.class));
    }

    @Test
    @DisplayName("Bus Node receive custom event")
    @Order(2)
    public void testBusCustomEvent() throws Exception {
        // 构造自定义事件
        NotificationRemoteApplicationEvent.Notification notification = NotificationRemoteApplicationEvent.Notification.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .message("Testing Message").build();
        RemoteApplicationEvent event = new NotificationRemoteApplicationEvent(this, busProperties.getId(),
                destFactory.getDestination("bus-node"), notification);
        Message<RemoteApplicationEvent> message = MessageBuilder.withPayload(event).build();

        // 向目标输入源发送总线刷新事件
        input.send(message);

        // 验证总线消费者接收到 NotificationRemoteApplicationEvent 总线刷新事件，并被调用一次，且自定义事件通知内容为 "Testing Message"
        verify(busConsumer, times(1)).accept(argThat(e ->
                ((NotificationRemoteApplicationEvent) e).getNotification().getMessage().equals("Testing Message"))
        );
    }
}
