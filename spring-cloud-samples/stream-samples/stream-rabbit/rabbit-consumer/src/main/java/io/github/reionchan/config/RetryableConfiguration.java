package io.github.reionchan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamRetryTemplate;
import org.springframework.cloud.stream.binder.AbstractBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * 重试机制定制类
 *
 * @author Reion
 * @date 2023-10-27
 **/
@Slf4j
@Configuration
public class RetryableConfiguration {

    /**
     * 自定义重试策略
     *
     * <pre>
     * 自定义重试策略生效原理：{@link AbstractBinder#buildRetryTemplate(ConsumerProperties)}
     * </pre>
     */
    @StreamRetryTemplate
    public RetryTemplate myRetryTemplate() {
        RetryTemplate rt = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(2);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        rt.setRetryPolicy(retryPolicy);
        rt.setBackOffPolicy(backOffPolicy);
        rt.registerListener(myRetryListener());

        return rt;
    }
    private RetryListener myRetryListener() {
        RetryListenerSupport retryListenerSupport = new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                String queue = (String) ((MessageHandlingException) throwable).getFailedMessage().getHeaders().get("amqp_consumerQueue");
                log.error("\n\nSend message to {} error, retry {} times...\n", queue, context.getRetryCount());
            }
        };
        return retryListenerSupport;
    }
}
