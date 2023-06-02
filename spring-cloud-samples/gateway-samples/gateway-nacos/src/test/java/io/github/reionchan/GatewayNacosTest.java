package io.github.reionchan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayNacosTest {

    @LocalServerPort
    int port;
    private WebTestClient client;

    @BeforeEach
    public void setup() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nacosDiscoveryLoadBalancerRouteWorks() {
        // 原始请求 http://localhost:port/gateway-nacos/echoAppName
        // 路由请求 lb://gateway-nacos/echoAppName
        // 最终请求 http://ip:port/echoAppName
        client.get().uri("/gateway-nacos/echoAppName")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("gateway-nacos");
    }
}
