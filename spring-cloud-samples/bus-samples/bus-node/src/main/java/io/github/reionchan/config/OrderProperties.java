package io.github.reionchan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 订单可配置配置属性
 *
 * @author Reion
 * @date 2023-11-11
 **/
// 配置属性的 Getter Setter 不推荐使用 Lombok
@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    /**
     * Whether to enable creating order.
     */
    private boolean createEnabled = false;

    public boolean isCreateEnabled() {
        return createEnabled;
    }

    public void setCreateEnabled(boolean createEnabled) {
        this.createEnabled = createEnabled;
    }
}
