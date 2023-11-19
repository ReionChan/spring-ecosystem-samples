package io.github.reionchan;

import io.github.reionchan.event.NotificationRemoteApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.bus.BusConsumer;
import org.springframework.cloud.bus.BusEnvironmentPostProcessor;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 消息总线管理节点使用测试
 *
 * <pre>
 * {@link ActiveProfiles @ActiveProfiles}
 *  设置本测试 profile 为 test
 *
 * {@link AutoConfigureMockMvc @AutoConfigureMockMvc}
 *  启用 MockMvc 自动配置
 *
 * {@link EnableAutoConfiguration @EnableAutoConfiguration}
 *  exclude 属性设置：
*       RabbitAutoConfiguration.class
 *          排除 RabbitMQ 自动配置，防止测试时启动 RabbitMQ
 *
 * {@link SpringBootTest @SpringBootTest}
 *  properties 属性设置：
 *      "trace=false","debug=false"
 *          设置 SpringBoot 常用包的初始日志级别，false 时将取消对应日志级别
 *          参考：{@link LoggingApplicationListener#initializeSpringBootLogging(LoggingSystem, LogLevel) initializeSpringBootLogging()}
 *      "spring.cloud.bus.id=bus-manager:${server.port}:${cachedrandom.${spring.application.name}.value}"
 *          总线 ID 移除 profile 关联，防止总线刷新重新加载的此 id 变化，导致后续测试失败
 *          默认设置参考：{@link BusEnvironmentPostProcessor#postProcessEnvironment(ConfigurableEnvironment, SpringApplication) postProcessEnvironment()}
 *
 * {@link TestMethodOrder @TestMethodOrder}
 *    MethodOrderer.OrderAnnotation
 *      按方法上 {@link Order @Order} 顺序执行测试
 * </pre>
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
@SpringBootTest(properties = {
    "trace=false",
    "debug=false",
    "spring.cloud.bus.id=bus-manager:${server.port}:${cachedrandom.${spring.application.name}.value}"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BusManagerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusConsumer busConsumer;

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    @DisplayName("Bus Refresh")
    @Order(1)
    public void testBusRefresh() throws Exception {
        // 调用刷新总线方法
        mockMvc.perform(MockMvcRequestBuilders.post("/actuator/busrefresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(204));

        // 验证总线消费者接收到 RefreshRemoteApplicationEvent 总线刷新事件，并被调用一次
        verify(busConsumer, times(1)).accept(any(RefreshRemoteApplicationEvent.class));
    }

    @Test
    @DisplayName("Bus Change Environment")
    @Order(2)
    public void testBusEnvChange() throws Exception {
        // 调用刷新总线方法
        mockMvc.perform(MockMvcRequestBuilders.post("/actuator/busenv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"spring.cloud.bus.trace.enabled\", \"value\":true}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(204));

        // 验证总线消费者接收到 EnvironmentChangeRemoteApplicationEvent 总线刷新事件，并被调用一次，且更改的属性与发送时一致
        verify(busConsumer, times(1)).accept(argThat(e -> {
            EnvironmentChangeRemoteApplicationEvent changeEvent = (EnvironmentChangeRemoteApplicationEvent) e;
            Map<String, String> values = changeEvent.getValues();
            return values.containsKey("spring.cloud.bus.trace.enabled") && values.get("spring.cloud.bus.trace.enabled").equals("true");
        }));
    }

    @Test
    @DisplayName("Bus Custom Event")
    @Order(3)
    public void testBusCustomEvent() throws Exception {
        // 调用刷新总线方法
        mockMvc.perform(MockMvcRequestBuilders.post("/pushNotification")
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content("Testing Message"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200));

        // 验证总线消费者接收到 NotificationRemoteApplicationEvent 总线刷新事件，并被调用一次，且自定义事件通知内容为 "Testing Message"
        verify(busConsumer, times(1)).accept(argThat(e ->
                ((NotificationRemoteApplicationEvent)e).getNotification().getMessage().equals("Testing Message"))
        );
    }
}


