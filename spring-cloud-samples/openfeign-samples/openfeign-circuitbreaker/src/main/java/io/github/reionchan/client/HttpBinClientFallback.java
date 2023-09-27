package io.github.reionchan.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * HttpBin 客户端回退实现类
 *
 * @author Reion
 * @date 2023-09-27
 **/
// 要使用 @Component 注解，使得容器中存在该类型的 bean 实例
// 否则 HttpBinClient 生成动态代理类时找不到回退类的实力而异常
@Component
public class HttpBinClientFallback implements HttpBinClient {
    @Override
    public ResponseEntity<String> pathVar(Integer second) {
        return ResponseEntity.ok("超时导致回退！");
    }
}
