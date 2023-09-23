package outside.scan.config;

import feign.Logger;
import feign.Request;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
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
public class FooClientConfiguration {

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
        return Logger.Level.FULL;
    }

    /**
     * 设置该客户端请求相关配置
     *  connectTimeout  建立连接超时时间
     *  readTimeout     读取请求响应超时时间
     *  followRedirects 是否允许重定向
     *
     *  注意：虽然时间单位可以自定义，但由于内部使用时会转换为毫秒
     *       如果小于 1 毫秒，将都被截断为 0 毫秒，即永不超时
     */
    @Bean
    Request.Options options() {
        return new Request.Options(
                1, TimeUnit.SECONDS,
                2, TimeUnit.SECONDS,
                true
                );
    }

}
