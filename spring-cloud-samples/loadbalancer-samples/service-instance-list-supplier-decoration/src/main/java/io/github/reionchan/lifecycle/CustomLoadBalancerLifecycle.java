package io.github.reionchan.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.stereotype.Component;

/**
 * 自定义负载均衡生命周期 Bean
 *
 * @author Reion
 * @date 2023-06-22
 **/
@Slf4j
@Component
public class CustomLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {
    @Override
    public void onStart(Request request) {
        log.info("=== onStart ===");
    }

    @Override
    public void onStartRequest(Request request, Response lbResponse) {
        log.info("=== onStartRequest ===");
    }

    @Override
    public void onComplete(CompletionContext completionContext) {
        log.info("=== onComplete ===\n{}\n\n", completionContext);
    }

    @Override
    public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
        return ServiceInstance.class.isAssignableFrom(serverTypeClass);
    }
}
