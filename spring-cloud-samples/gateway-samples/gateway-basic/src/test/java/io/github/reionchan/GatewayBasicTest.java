package io.github.reionchan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayBasicTest {

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

    // --- 自定义路由方式一：使用 InMemoryRouteDefinitionRepository ---

    @Test
    @SuppressWarnings("unchecked")
    public void pathRouteWorksFromInMemory() {
        client.get().uri("/user-agent")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody()).isNotEmpty();
                });
    }

    // --- 自定义路由方式二：使用 RouteLocatorBuilder ---

    @Test
    @SuppressWarnings("unchecked")
    public void pathRouteWorks() {
        client.get().uri("/get")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody()).isNotEmpty();
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void hostRouteWorks() {
        client.get().uri("/headers")
                .header("Host", "www.myhost.org")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody()).isNotEmpty();
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rewriteRouteWorks() {
        client.get().uri("/foo/get")
                .header("Host", "www.rewrite.org")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody()).isNotEmpty();
                });
    }

    // --- 自定义路由方式三：使用 PropertiesRouteDefinitionLocator ---

    @Test
    @SuppressWarnings("unchecked")
    public void pathRouteWorksFromInProperties() {
        client.get().uri("/status/404")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}
