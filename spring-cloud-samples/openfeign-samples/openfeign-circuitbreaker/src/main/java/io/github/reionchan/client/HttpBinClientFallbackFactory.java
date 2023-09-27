package io.github.reionchan.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HttpBin 客户端回退工厂类
 *
 * @author Reion
 * @date 2023-09-27
 **/
@Slf4j
// 要使用 @Component 注解，使得容器中存在该类型的 bean 实例
// 否则 HttpBinClient 生成动态代理类时找不到回退工厂类的实力而异常
@Component
public class HttpBinClientFallbackFactory implements FallbackFactory<HttpBinClientFallbackFactory.TimeoutFallback> {
    @Override
    public TimeoutFallback create(Throwable cause) {
        log.warn("熔断器执行异常：{}", cause.getMessage());
        return new TimeoutFallback();
    }

    /**
     * 超时形式的回退
     */
    static class TimeoutFallback implements HttpBinClient {
        @Override
        public ResponseEntity<String> pathVar(Integer second) {
            return ResponseEntity.ok("TimeoutFallback: 超时执行回退方法！");
        }
    }
}
