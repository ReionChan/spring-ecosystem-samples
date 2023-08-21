package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 利用 Import 加载外部配置文件的配置客户端启动器
 *
 * <pre>
 * 加载外部化配置文件，Spring Boot 2.4 之后，默认采用 Spring Boot Config Data Import
 * 的方式来指定所需的配置存储的位置，从而在启动时去该位置获取配置，详细参考：
 *  <a href="https://spring.io/blog/2020/08/14/config-file-processing-in-spring-boot-2-4">Config File Processing</a>
 *  <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-Config-Data-Migration-Guide">Spring Boot Config Data Migration Guide</a>
 *  <a href="https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#config-data-import">Config Data Import</a>
 *
 * 由于不再使用 bootstrap 方式，故无需 bootstrap.yaml 文件，
 * 仅使用 application.yaml 文件配置即可。
 *
 * 支持 Config Data Import 特性的核心抽象包括如下接口：
 * 1. {@link ConfigDataLocationResolver}
 * 2. {@link ConfigDataLoader}
 *
 *
 * //启动时，会去配置服务器请求三次配置
 * 原因：
 *      当 spring.cloud.config.profile 未设置时，使用 SpringApplicationBuilder#profiles 指定 additionalProfiles 为 dev
 *          第一次：请求 applicationName+default_profile 的配置 {@link ConfigDataEnvironment#processInitial} 由于未激活 profile，故使用默认 default 请求
 *          第二次：请求 applicationName+dev_profile 的配置 {@link ConfigDataEnvironment#processWithProfiles} isProfileSpecific=true 使用 dev 请求
 *          第三次：请求 applicationName+default_profile 的配置 {@link ConfigDataEnvironment#processWithProfiles} isProfileSpecific=false 使用 default 请求
 *
 *          此时，获取的 demo.encrypt-key 属性值为 第一次请求的 default profile 配置的值：default: got it!
 *
 *      当 spring.cloud.config.profile 设置为 dev 时，使用 SpringApplicationBuilder#profiles 指定 additionalProfiles 为 dev
 *          由于 ConfigServerConfigDataResource#getProfiles() 优先读取 spring.cloud.config.profile=dev 的值，故下面所有三次请求都使用 dev 请求
 *          第一次：请求 applicationName+default_profile 的配置 {@link ConfigDataEnvironment#processInitial}
 *          第二次：请求 applicationName+dev_profile 的配置 {@link ConfigDataEnvironment#processWithProfiles}
 *          第三次：请求 applicationName+default_profile 的配置 {@link ConfigDataEnvironment#processWithProfiles}
 *
 *          此时，获取的 demo.encrypt-key 属性值为 第一次请求的 dev profile 配置的值：Got it!
 *
 *      而 spring.config.activate.on-profile 如果设置为 dev 时，
 *          1. {@link ConfigDataEnvironment#processInitial} 时，由于 {@link ConfigDataProperties#isActive} 条件不满足
 *              此次请求将不会被执行（故上面三次请求中的第一次请求不执行，而值向服务器请求两次）
 *          2. 当 SpringApplicationBuilder#profiles 指定 additionalProfiles 包含 dev 时，
 *              spring.config.import 所设置的导入资源将被加载，否则会因为 spring.config.import 不满足条件缺失而报错
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-07-08
 **/
@Slf4j
@SpringBootApplication
public class ConfigClientImportBootstrap {
    public static void main(String[] args) {
        // 默认 active profile 为 default
        // ConfigurableApplicationContext context = SpringApplication.run(ConfigClientImportBootstrap.class, args);
        // 指定 active profile 为 dev
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ConfigClientImportBootstrap.class).profiles("dev").run(args);
        Environment env = context.getEnvironment();
        // 如果获得的属性值为空，请确保 application.yaml 的属性 spring.cloud.config.profile 的值
        // 包含在配置服务的 spring.profiles.active 属性配置值之中
        // profile 可选值：
        //      dev 采用对称加密
        //      pro 采用 RAS 加密
        log.info("来自配置服务器的属性 demo.encrypt-key = {}", env.getProperty("demo.encrypt-key"));
    }
}
