package outside.scan.config;

import feign.Logger;
import feign.Request;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OpenFeign 客户端定制化配置类
 *
 * <pre>
 * 此处由于使用 @Configuration 注解，因此将其放在本应用扫描之外的包名，
 * 从而避免原本只为 FooClient 定制配置的 Feign 组件 Bean 被主应用上下文扫描，
 * 进而覆盖全局默认的组件。
 *
 * 详细参考：
 *  <a href="https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults">覆盖默认配置</a>
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@Configuration
public class HttpBinClientConfiguration {

    /**
     * <pre>
     * 设置 feign 日志打印等级
     *
     * 请同时在 application.yaml 文件指定 feign 客户端的日志级别为 DEBUG
     *  logging.level.your.feign.client.ClientName: DEBUG
     *  </pre>
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        // FULL 基本将打印请求及响应的详细信息
        return Logger.Level.BASIC;
    }
}
