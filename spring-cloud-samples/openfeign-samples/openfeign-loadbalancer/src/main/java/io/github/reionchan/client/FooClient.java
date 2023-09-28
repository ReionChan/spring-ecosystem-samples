package io.github.reionchan.client;

import io.github.reionchan.service.FooService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;

/**
 * Foo Service OpenFeign Client
 *
 * <pre>
 * 采用 Feign 继承样板代码的特性，本客户端接口继承 {@link FooService} 接口
 * 后者包含 SpringMVC 注释，可被继承的本接口直接解析。
 * 注意：
 *  1. @FeignClient 注解的客户端接口不能被服务端及客户端共享
 *  2. @FeignClient 注解的类上面不再支持类上的 @RequestMapping 注解
 *
 * 关于本接口注解 {@link FeignClient @FeignClient} 说明：
 *
 * 1. name 属性设置目标服务名
 *  必填项，支持属性表达式，例如：${feign.name}
 *
 * 2. configuration 属性设置定制化配置
 *  指定的配置类可以不使用 {@link Configuration @Configuration} 注解修饰，
 *  如使用该注解，为了避免其只被该客户端接口对应的子上下文加载，
 *  务必将其放在主程序 Bean 扫描路径之外。
 *  原因：
 *      {@link FeignClient @FeignClient} 与之前 3.1.3 abstract-loadbalancer 示例
 *      LoadBalancerBootstrap 类注释关于注解 {@link org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient @LoadBalancerClient}
 *      语义中描述的一致，它们都是通过子上下文的形式隔离针对单个客户端的定制化配置。
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@FeignClient(name = "foo-service")
public interface FooClient extends FooService {

}
