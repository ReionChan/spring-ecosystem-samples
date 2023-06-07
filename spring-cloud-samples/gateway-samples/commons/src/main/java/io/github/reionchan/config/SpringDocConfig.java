package io.github.reionchan.config;

import io.github.reionchan.openapi.ReadResponseJson;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文档配置类
 *
 * @author Reion
 * @date 2023-06-06
 **/
@Configuration
@OpenAPIDefinition
public class SpringDocConfig {
    /**
     * 客户端异常响应引用名称
     */
    public static final String RESPONSE_FAIL = "failResponse";
    /**
     * 服务端异常响应引用名称
     */
    public static final String RESPONSE_ERROR = "errorResponse";

    public static final String RESPONSE_NOT_ALLOW = "notAllowResponse";

    @Value("${maven.version}")
    private String version;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Autowired
    private ReadResponseJson readResponseJson;

    @Bean
    // @formatter:off
    public OpenAPI baseOpenAPI()throws IOException {
        Map<String, Object> exampleMap = readResponseJson.read();
        // 定义通用客户端异常返回格式
        ApiResponse failResponse = new ApiResponse().content(
                new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType().addExamples("default",
                                new Example().value(exampleMap.get(RESPONSE_FAIL))))
        ).description("客户端异常消息");

        // 定义通用客户端异常返回格式
        ApiResponse notAllowResponse = new ApiResponse().content(
                new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType().addExamples("default",
                                new Example().value(exampleMap.get(RESPONSE_NOT_ALLOW))))
        ).description("客户端异常消息");

        // 定义通用服务端异常返回格式
        ApiResponse errorResponse = new ApiResponse().content(
                new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType().addExamples("default",
                                new Example().value(exampleMap.get(RESPONSE_ERROR))))
        ).description("服务端异常消息");

        // 通用组件
        Components components = new Components();
        components.addResponses(RESPONSE_FAIL, failResponse);
        components.addResponses(RESPONSE_NOT_ALLOW, notAllowResponse);
        components.addResponses(RESPONSE_ERROR, errorResponse);

        return new OpenAPI()
            .servers(List.of(new Server().url(gatewayUrl + "/" + appName)))
            .components(components)
            .info(new Info()
                .title("Gateway 集成 SpringDoc")
                .description("利用网关、服务发现 Nacos 来统一 API 文档入口")
                .version(version)
                // 联系信息
                .contact(new Contact()
                    .name("ReionChan")
                    .email("reion78@gmail.com")
                    .url("https://reionchan.github.io/"))
                    // 许可协议
                    .license(new License()
                        .name("GPL-3.0 license")
                        .url("https://www.gnu.org/licenses/gpl-3.0.txt")
                        .identifier("GPL-3.0"))
                    // 服务条款的 URL 地址
                    .termsOfService("/server/termsOfService")
        );
    }
    // @formatter:on
}
