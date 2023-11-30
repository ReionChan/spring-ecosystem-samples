package io.github.reionchan.config.logging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Otlp log 属性配置
 *
 * @author Reion
 * @date 2023-11-28
 **/
@Data
@ConfigurationProperties("management.otlp.logging")
public class OtlpProperties {
    /**
     * URL to the OTel collector's HTTP API.
     */
    private String endpoint = "http://localhost:4318/v1/logs";

    private Duration timeout = Duration.ofSeconds(10);

    private Duration exporterTimeout = Duration.ofSeconds(10);

}
