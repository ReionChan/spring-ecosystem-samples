package bootstrap.config;

import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.cloud.bootstrap.BootstrapImportSelector;
import org.springframework.cloud.bootstrap.BootstrapImportSelectorConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义属性配置源定位器
 *
 * <pre>
 * 通过 Spring Factories 机制，被 bootstrap 上下文加载到其容器中，实现定制外部化属性源配置
 *
 * 原理：
 *    由 {@link BootstrapImportSelectorConfiguration} 通过 @Import 导入 {@link BootstrapImportSelector} 配置文件导入器。
 *    该导入器读取 spring.factories 文件中配置属性为 {@link BootstrapConfiguration} 类型的值，
 *    其中就包含类 {@link PropertySourceBootstrapConfiguration}，而该配置文件中依赖注入 bootstrap 上下文容器中所有
 *    类型为 {@link PropertySourceLocator} 的属性源定位器 Bean。
 *    而要将 Bean 注册到 bootstrap 上下文容器，还得依赖 Spring Factories 机制。
 *    将本类 {@link CustomizePropertySourceLocator} 的全限定名追加到 META-INF/spring.factories 中的
 *    'org.springframework.cloud.bootstrap.BootstrapConfiguration' 属性上。
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-10
 **/
public class CustomizePropertySourceLocator implements PropertySourceLocator {
    @Override
    public PropertySource<?> locate(Environment environment) {
        Map<String, Object> mapSource = new HashMap<>();
        // 外部配置变量 env.p1
        mapSource.put("env.p1", "p1-value-in-customize-external-property-source");
        // 外部配置变量 env.p2
        mapSource.put("env.p2", "p2-value-in-customize-external-property-source");
        // 外部配置变量 env.p3，采用对称加密方式 key=please-change-me salt=cafebabe，参考 bootstrap.yaml 文件
        mapSource.put("env.p3", "{cipher}c45bd3c09c840f298ca2776d454b923ba2561e89dd30b346954cbb27c43959ca");


        // 步骤一：默认情况，外部配置的变量是会覆盖应用本地的同名属性。通过如下设置，允许应用本地变量覆盖外部同名变量
        mapSource.put("spring.cloud.config.allowOverride", true);// 设置本地变量允许覆盖外部变量权限

        // 步骤二：【控制是否远程属性覆盖本地系统属性源、系统环境变量、命令行属性源】或者【控制是否远程属性不覆盖本地变量】
        // 如果同时设置 true，则远程属性不覆盖本地变量

        // 2.1【控制是否远程属性覆盖本地系统属性源、系统环境变量、命令行属性源】，默认不覆盖这三个本地属性源，即 false
        // 即覆盖本地以下源：
        //    系统属性源 (system properties)
        //    系统环境变量 (environment variables)
        //    命令行属性源 (command line arguments)
        mapSource.put("spring.cloud.config.overrideSystemProperties", false);

        // 2.2【控制是否远程属性不覆盖本地变量】，默认远程覆盖本地变量（定义在文件中的），即 false
        mapSource.put("spring.cloud.config.overrideNone", false);

        // 自定义一个名称为 customizePropertySourceLocator 的外部属性源
        return new MapPropertySource("customizePropertySourceLocator", mapSource);
    }
}
