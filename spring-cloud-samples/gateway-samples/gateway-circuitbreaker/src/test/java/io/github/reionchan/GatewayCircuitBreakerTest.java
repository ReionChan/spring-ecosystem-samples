package io.github.reionchan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

/**
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayCircuitBreakerTest {

    @LocalServerPort
    int port;
    private WebTestClient client;

    @BeforeEach
    public void setup() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                // httpbin.org 服务器比较慢，这里将请求超时时间设置长一点
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 此处测试失败，有可能是 httpbin.org 服务器响应很慢导致请求超时（WebTestClient 设置的超时为 5 秒）
     * 另外熔断机制配置的 2 秒视作服务不可达，发生熔断
     */
    @Test
    @SuppressWarnings("unchecked")
    public void circuitBreakerRouteWorks() {
        client.get().uri("/delay/3")
                .header("Host", "www.circuitbreaker.org")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * 此处测试失败，有可能是 httpbin.org 服务器响应很慢导致请求超时（WebTestClient 设置的超时为 5 秒）
     * 另外熔断机制配置的 2 秒视作服务不可达，发生熔断
     */
    @Test
    @SuppressWarnings("unchecked")
    public void circuitBreakerFallbackRouteWorks() {
        client.get().uri("/delay/3")
                .header("Host", "www.circuitbreakerfallback.org")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("This is a fallback");
    }
}
