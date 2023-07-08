package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerMvcConfiguration;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.config.EnvironmentRepositoryConfiguration;
import org.springframework.cloud.config.server.encryption.EncryptionController;
import org.springframework.cloud.config.server.environment.*;
import org.springframework.cloud.config.server.resource.ResourceController;
import org.springframework.cloud.config.server.resource.ResourceRepository;
import org.springframework.cloud.config.server.support.EnvironmentRepositoryProperties;
import org.springframework.http.ResponseEntity;

/**
 * 基于文件系统的配置服务器启动器
 *
 * <pre>
 *
 * 配置服务核心抽象类：
 *
 * 1. {@link EnvironmentRepository}
 *  它定义根据 application 应用名, profile 条件配置, label 标签三个元素，
 *  获取配置 {@link Environment}，它可以认为是 {@link org.springframework.core.env.Environment}
 *  的可以被序列化的传输对象。
 *
 * 2. {@link EnvironmentRepositoryFactory}
 *  它是用来产生 {@link EnvironmentRepository} 的工厂，它定义根据
 *  {@link EnvironmentRepositoryProperties} 属性配置构建相应 {@link EnvironmentRepository}
 *  实例的构建方法。
 *
 * 3. {@link SearchPathLocator}
 *  它定义根据 application 应用名, profile 条件配置, label 标签三个元素，
 *  获取相应配置资源的路径 {@link SearchPathLocator.Locations}，
 *  路径可以是基于文件系统、或类路径下的。
 *
 * 4. {@link EnvironmentController}
 *  提供对配置的 Web 访问接口，返回的结果为 Json 序列化的 {@link Environment} 或 {@link ResponseEntity}
 *
 *  {@link EnvironmentController} Bean 的定义参考：
 *      {@link ConfigServerMvcConfiguration.RefreshableEnvironmentControllerConfiguration#environmentController(EnvironmentRepository, ConfigServerProperties)}
 *  注意：
 *      该 Bean 的定义上是被 @RefreshScope 注解修饰的，它对配置环境的变化
 *
 * 5. {@link ResourceController}
 *  提供对配置文件的 Web 访问接口，返回结果为配置资源文件的文本格式或二进制格式
 *
 *  {@link ResourceController} Bean 的定义参考：
 *      {@link ConfigServerMvcConfiguration.EnvironmentControllerConfiguration#resourceController(ResourceRepository, EnvironmentRepository, ConfigServerProperties)}
 *
 * 配置服务定义了许多 {@link EnvironmentRepository} 配置存储库实现，
 * 其中 {@link CompositeEnvironmentRepository} 提供对不同配置存储库封装调用的功能，
 * 它的子类 {@link SearchPathCompositeEnvironmentRepository} 同时实现了
 * {@link SearchPathLocator} 接口，具备获取所有搜索路径的能力。
 * 而在 {@link EnvironmentRepositoryConfiguration} 自动装配时，
 * 根据 spring.profiles.active 属性中指定的 profile 条件装配不同
 * 类型的 {@link EnvironmentRepository} Bean，然后汇总成为 {@link SearchPathCompositeEnvironmentRepository}
 * 本例中，由于演示基于本地文件系统的实现类，故设置
 *  spring.profiles.active=native
 * 来激活 {@link NativeEnvironmentRepository} Bean
 * 值得注意，当 spring.profiles.active 属性中的 profile 没有在配置类有匹配时，
 * 将会激活默认的 {@link MultipleJGitEnvironmentRepository} 基于 Git 的配置服务。
 *
 * 1. 本项目演示基于文件系统的配置库 {@link NativeEnvironmentRepository} 实现，
 *  它读取 classpath 下面的配置文件，本例中位置：classpath:/config/
 *  启动本项目，浏览器访问下面的地址，可以返回不同格式的配置信息：
 *      http://localhost:8888/config-client-bootstrap/dev
 *      http://localhost:8888/config-client-bootstrap-dev.yaml
 *      http://localhost:8888/config-client-bootstrap-dev.json
 *      http://localhost:8888/config-client-bootstrap-dev.properties
 *
 * 2. 在 application.yaml 配置中增加属性配置
 *  encrypt.key
 *  它将开启对称加密，可以用于解密配置文件中以 {cipher} 开头的密文
 *  具体对称与非对称密钥配置，参考：<a href="https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_key_management/">Key Management</a>
 *
 *  与此同时，自动配置暴露 Web 端口 {@link EncryptionController}，
 *  可以获取加解密及非对称加密公钥信息、对文本的加解密的 HTTP 接口
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-07-04
 **/
@EnableConfigServer
@SpringBootApplication
public class ConfigServerFileSystemBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerFileSystemBootstrap.class, args);
    }
}
