package io.github.reionchan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

/**
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayRateLimitTest {

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

    // 这里测试失败有可能是由于 http://httpbin.org 网站本身响应慢造成，请多重复测试几次
    @Test
    public void rateLimiterWorks() {
        WebTestClient authClient = client.mutate()
                .filter(basicAuthentication("user", "password"))
                .build();

        boolean wasLimited = false;

        for (int i = 0; i < 20; i++) {
            FluxExchangeResult<Map> result = authClient.get()
                    .uri("/anything/1")
                    .header("Host", "www.limited.org")
                    .exchange()
                    .returnResult(Map.class);
            if (result.getStatus().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                System.out.println("Received result: "+result);
                wasLimited = true;
                break;
            }
        }

        assertThat(wasLimited)
                .as("A HTTP 429 TOO_MANY_REQUESTS was not received")
                .isTrue();

    }
}
