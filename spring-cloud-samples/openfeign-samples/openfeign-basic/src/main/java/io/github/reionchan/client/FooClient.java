package io.github.reionchan.client;

import org.springframework.context.annotation.Configuration;
import outside.scan.config.FooClientConfiguration;
import io.github.reionchan.response.WebResponse;
import io.github.reionchan.vo.RequestVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Foo Service OpenFeign Client
 *
 * <pre>
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
@FeignClient(name = "foo-service", configuration = FooClientConfiguration.class)
public interface FooClient {
    /**
     * 使用 @PathVariable 设置路径参数，value 属性值保持与路径表达式的 id 变量一致
     * 路径表达式支持正则校验
     */
    @GetMapping("/get/user/{id:\\d+}")
    ResponseEntity<WebResponse<?>> pathVar(@PathVariable("id") Integer id);

    /**
     * 使用 @RequestParam 设置 Query 参数，value 属性值确定最终的 Query 参数名
     */
    @GetMapping("/get/user")
    ResponseEntity<WebResponse<?>> queryVar(@RequestParam("id") Integer id, @RequestParam("userName") String userName);

    /**
     * 采用 application/x-www-form-urlencoded 形式的 Post 请求
     * 注意：
     *  1. 不指定 consumes 时，方法参数 vo 必须指定 @SpringQueryMap 注解
     *  2. 指定 consumes 时，方法参数 vo 可不用注解
     */
    @PostMapping(value = "/post/formUrlEncodedDataBody", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<WebResponse<?>> formUrlEncodedDataBody(@SpringQueryMap RequestVo requestVo);

    /**
     * 采用 multipart/form-data 形式的 Post 请求
     * 注意：
     *  1. 指定了 consumes 参数格式后，方法参数 vo 可不用注解 @RequestBody
     *  2. 不指定 consumes 时，方法参数 vo 不标记注解时，将会丢失 vo 信息
     */
    @PostMapping(value = "/post/formDataBody", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<WebResponse<?>> formDataBody(RequestVo vo);

    /**
     * 采用 application/json 形式的 Post 请求（也是 Feign Post 请求的默认格式）
     * 注意：
     *  1. 不指定 consumes 时，方法参数 vo 对象将默认采用 Json 格式的 Body 请求，可不用注解 @RequestBody
     *  2. 指定 consumes 时，方法参数 vo 可不用注解 @RequestBody
     */
    @PostMapping(value = "/post/jsonBody", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<WebResponse<?>> jsonBody(RequestVo vo);

    /**
     * 采用 multipart/form-data 形式的 Post 请求上传文件
     */
    @PostMapping(value = "/post/multipartFileBody", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<WebResponse<?>> multipartFileBody(
            @RequestPart("file") MultipartFile file,
            @RequestPart("fileName") String fileName);
}
