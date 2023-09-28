package io.github.reionchan.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HttpBin Feign 客户端
 *
 * <pre>
 * 关于本接口注解 {@link FeignClient @FeignClient} 说明：
 *
 * 1. name 属性设置目标服务名
 *  必填项，支持属性表达式，例如：${feign.name}
 *
 * 2. configuration 属性设置定制化配置
 *
 * 3. url 可附带协议的主机名
 *
 * 4. path 所有方法的公有路径
 *
 * 5. fallback 发生熔断时的回退类
 *  优先级高于下面的 fallbackFactory，
 *  当两者同时设置时，优先本回退类
 *
 * 6. fallbackFactory 发生熔断时的回退工厂
 *  可以根据熔断返回的异常动态创建不同的回退类
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-09-27
 **/
@FeignClient(name="httpBinClient",
            url = "http://httpbin.org",
            fallback = HttpBinClientFallback.class
)
public interface HttpBinClient {

    /**
     * 使用 @PathVariable 设置路径参数，value 属性值保持与路径表达式的 id 变量一致
     * 路径表达式支持正则校验
     */
    @GetMapping("/gzip")
    ResponseEntity<String> gzip();
}
