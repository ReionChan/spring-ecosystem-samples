package io.github.reionchan;

import bootstrap.config.CustomizePropertySourceLocator;
import io.github.reionchan.bean.EnvProperties;
import io.github.reionchan.bean.RefreshScopeAnnotationBean;
import io.github.reionchan.listener.EnvironmentChangeListener;
import io.github.reionchan.listener.RefreshScopeRefreshedListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.refresh.LegacyContextRefresher;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.logging.LoggingRebinder;
import org.springframework.cloud.util.PropertyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 启动上下文演示启动器
 *
 * <pre>
 * Bootstrap 上下文作用：
 *
 *  1. 为主应用 {@link SpringApplication} 启动前，提供注册 {@link PropertySources} 外部化属性配置源
 *      具体代码：
 *         {@link BootstrapApplicationListener#mergeDefaultProperties(MutablePropertySources, MutablePropertySources)}
 *
 *     规定默认外部化属性源与本地属性源的同名变量覆盖优先级
 *     默认：外部外部属性源覆盖本地文件属性源同名变量，
 *          但本地系统属性源、系统环境属性源、命令行参数属性源覆盖外部属性源
 *
 *     1.1 spring.cloud.config.allowOverride
 *          true (默认) 表明运行本地系统属性源、系统环境属性源、命令行参数属性源覆盖外部属性源
 *          false 表明不允许任何本地属性源覆盖，即外部属性源优先级最高
 *
 *     1.2 当 1.1 的属性设置为 true 时，可以设置外部外部属性源被允许覆盖的范围
 *          1.2.1 spring.cloud.config.overrideNone
 *              false (默认) 表明外部属性源并不能覆盖所有本地属性源，单单只覆盖文件属性源
 *              true 表明外部属性源不覆盖任何本地属性源，即，外部属性源优先级最低
 *          1.2.2 spring.cloud.config.overrideSystemProperties
 *              false (默认) 表明外部属性源不覆盖本地的系统属性源、系统环境属性源、命令行属性源
 *              true 表明外部属性源覆盖本地的系统属性源、系统环境属性源、命令行属性源
 *
 *     可以通过 {@link PropertySourceLocator} 自定义外部属性源
 *     实现方法：
 *          1. 自定义 {@link PropertySourceLocator} 实现类 {@link CustomizePropertySourceLocator}
 *          2. 通过 Spring Factories 机制像 bootstrap 上下文注册该 Bean
 *              在 META-INF/spring.factories 文件将该类全限定名当做值设置到以下属性名：
 *              org.springframework.cloud.bootstrap.BootstrapConfiguration
 *     原理：
 *          参考 {@link CustomizePropertySourceLocator} 类注释
 *
 *
 *  2. 为主应用 {@link SpringApplication} 启动前，提供注册 {@link ApplicationContextInitializer} 上下文初始化器
 *      具体代码：
 *          {@link BootstrapApplicationListener#apply(ConfigurableApplicationContext, SpringApplication, ConfigurableEnvironment)}
 *
 *
 * 代表性应用场景：
 *  项目：<a href="https://spring.io/projects/spring-cloud-config">Spring Cloud Config</a>
 *  介绍：分布式系统外部化配置服务器及客户端
 *
 *  Bootstrap Context 其他相关：
 *
 *  1. 如需打印 bootstrap 上下文的日志，请在 bootstrap.yaml 文件定义日志级别
 *      本例中，将 bootstrap 上下文有关 spring 的日志级别设置为 debug
 *      bootstrap.yaml 中 logging.level.org.springframework=debug
 *      而应用上下文有关 spring 的日志级别设置为 info
 *      application.yaml 中 logging.level.org.springframework=info
 *
 *  2. {@link Environment} 变更时会触发 {@link EnvironmentChangeEvent} 事件，
 *     Spring Cloud 通过 {@link ConfigurationPropertiesRebinder}、{@link LoggingRebinder}
 *     两个针对该事件的监听器，达到重新绑定 @ConfigurationProperties 注解的 Bean、设置日志级别配置属性 logging.level.*
 *
 *     2.1 {@link ConfigurationPropertiesRebinder} 将触发所有 @ConfigurationProperties 注解的 Bean 销毁并重新初始化，
 *         即使当前发生变化的属性与该 Bean 无关，具体示例参考 {@link EnvProperties}
 *     2.2 {@link LoggingRebinder} 将重新载入环境中的 logging.level.* 的日志级别信息，并根据此信息重设日志级别
 *
 *  3. Spring Boot Actuator 提供了对 {@link Environment} 管理的端点
 *      1. 引入 Actuator 依赖
 *      2. application.yaml 中增加 env、configprops、refresh 等端点设置
 *          其中，management.endpoint.env.post.enabled=true 使 env 端点支持 POST 请求修改，例如：
 *
 *          curl -X POST -H "Content-Type: application/json"\
 *               -d '{"name": "spring.cloud.config.allowOverride","value": false}'\
 *               http://localhost:8080/actuator/env
 *
 *       3. 当使用 POST 对环境变量变更时，自定义监听器 {@link EnvironmentChangeListener}
 *          将能收到 {@link EnvironmentChangeEvent} 事件
 *
 *   4. 被 @RefreshScope 注解的 Bean 类会被 AOP 增强，它专门用来处理所依赖环境变量配置只通过初始化时进行注入的 Bean。
 *      当发生 {@link EnvironmentChangeEvent} 事件时，@RefreshScope 机制会使得被标注的 Bean 重新执行初始化，
 *      并将新生成的原始类实例覆盖 AOP 代理类的目标对象，从而达到动态刷新的目的。
 *
 *      {@link RefreshScopeAnnotationBean} 示例就是被 @RefreshScope 注解的 Bean, 它依赖的环境属性配置 {@link EnvProperties}
 *      是通过构造方法进行注入的，如果要实现动态刷新，只能通过构造方法重新绑定新的 {@link EnvProperties}，但是重新执行构造方法生成的
 *      {@link RefreshScopeAnnotationBean} 实例，不能重新赋值给之前就依赖它的 {@link RefreshScopeRefreshedListener}。
 *      为了解决此问题，给该类加上 @RefreshScope 注解后，{@link RefreshScopeRefreshedListener} 将依赖的是一直不变的 AOP 代理对象，
 *      新构造生成的对象只用来覆盖此代理对象的 target 对象。
 *
 *      值得注意：
 *          1. @RefreshScope 的 AOP 代理是懒加载 (非 singleton or prototype，而是 refresh) 形式，只有在该代理被使用时才会重新执行初始化
 *             并且，当执行 /refresh 端点刷新时，即使所绑定的 {@link EnvProperties} 不是通过构造方法进行注入，也会调用构造方法生成新对象。
 *
 *              原理：
 *                  Bean 创建方法 {@link AbstractBeanFactory#doGetBean(String, Class, Object[], boolean)} 中，
 *
 *                  {@code
 *                      protected <T> T doGetBean(String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly) {
 * 	                        // ...
 * 	                        // Create bean instance.
 * 	                        if (mbd.isSingleton()) {
 * 		                        // ...
 *                          }else if (mbd.isPrototype()) {
 * 		                        // ...
 *                          } else {
 * 		                        String scopeName = mbd.getScope();
 * 		                        Scope scope = this.scopes.get(scopeName);
 * 		                        // 调用 RefreshScope 的 get() 方法，其中 cache 会缓存已经创建过的 Bean
 * 		                        // 当调用 /refresh 端点触发上下文刷新时，cache 会被清除，从而在需要该 Bean 时，重新构建初始化获得新 Bean
 * 		                        Object scopedInstance = scope.get(beanName, () -> {
 * 			                        beforePrototypeCreation(beanName);
 * 			                        try {
 * 				                        return createBean(beanName, mbd, args);
 *                                  } finally {
 * 				                        afterPrototypeCreation(beanName);
 *                                  }});
 * 		                        beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
 *                         }
 * 	                       // ...
 *                    }
 *                  }
 *
 *          2. 执行 /refresh 端点进行上下文刷新时，会触发 {@link EnvironmentChangeEvent} 事件
 *
 *              原理： {@link RefreshEndpoint} 中调用 {@link ContextRefresher#refresh()} 方法，该类型在启用了 bootstrap 上下文时，
 *                    自动装载的实例为 {@link LegacyContextRefresher} 具体由自动装配类 {@link RefreshAutoConfiguration} 控制，
 *                    该 refresh() 方法包含 {@link ContextRefresher#refreshEnvironment()} 方法，它会产生 {@link EnvironmentChangeEvent} 事件
 *
 *          3. 而 {@link EnvironmentChangeListener} 中有对 {@link RefreshScopeAnnotationBean} 的调用，故打破懒加载，会进行
 *             {@link RefreshScopeAnnotationBean} 的重新构造。仔细观察下面示例的日志中 RefreshScopeAnnotationBean '@' 后的
 *             hashCode 值，它显示 AOP 代理对象中的原始目标对象已经被替换
 *
 *   5. {@link RefreshScope} 是 Spring 中除 singleton、prototype 之外定义的另一种 bean 范围，该类型的 bean 在 {@link ContextRefresher}
 *      的 refresh() 方法执行时，触发其 {@link RefreshScope#refreshAll()} 方法，将当前的已装载实例化的 bean 缓存 {@link GenericScope.BeanLifecycleWrapperCache}
 *      全部清空，并在随后针对该 refresh scope 类型的 Bean 被调用时，再次进行构造初始化，并重新放入该缓存。
 *
 *      {@code
 *        public void refreshAll() {
 * 		      // 在此处对现有 cache 清空处理
 * 		      super.destroy();
 * 		      this.context.publishEvent(new RefreshScopeRefreshedEvent());
 *        }
 *      }
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Slf4j
@SpringBootApplication
public class EnabledBootstrapContextBootstrap {

    // @formatter: off
    /**
     * <pre>
         激活 Spring Cloud Bootstrap 两种方式：
         1. 引入 spring-cloud-starter-bootstrap 依赖
         2. 环境变量 spring.cloud.bootstrap.enabled 设置为 true

         原理：
         由方法 {@link BootstrapApplicationListener#onApplicationEvent(ApplicationEnvironmentPreparedEvent)}
         条件判断子方法 {@link PropertyUtils#bootstrapEnabled(Environment)}
     * </pre>
     */
    public static void main(String[] args) throws IOException {

        //1.  已引入 spring-cloud-starter-bootstrap 故注释环境变量方式激活
        //System.getProperties().setProperty("spring.cloud.bootstrap.enabled", "true");
        ConfigurableApplicationContext context = SpringApplication.run(EnabledBootstrapContextBootstrap.class, args);

        //2. 环境变量关系（本地 application.yml > bootstrap.yml）(远程外部变量>本地变量，将在下面自定义属性配置源验证)
        Environment appEnv = context.getEnvironment();
        // 如果 application.yaml 中未定义此变量，将打印父类上下文 bootstrap 上下文定义的此属性的值 boot
        log.info("app 上下文-应用名称：{}", appEnv.getProperty("spring.application.name"));
        Environment bootstrapEnv = context.getParent().getEnvironment();
        log.info("bootstrap 上下文-应用名称：{}", bootstrapEnv.getProperty("spring.application.name"));

        //3. Spring Factories 机制定制 bootstrap 上下文的配置类 (参考本类注释的方法向 bootstrap 上下文注册配置 Bean)
        ApplicationContext bootstrapContext = context.getParent();
        // 子上下文能搜索到，是其搜索 bean 时，如果本身没找到，会顺带搜索其父上下文，故能搜索到
        Object beanByAppContext = context.getBean("beanLoadBySpringFactoryInBootstrap");
        log.info("名称为 {} 的 bean 实例，通过子上下文搜索：{}", "beanLoadBySpringFactoryInBootstrap", beanByAppContext);
        // 直接通过 bootstrap 上下文自身搜索
        Object beanByBootstrapContext = bootstrapContext.getBean("beanLoadBySpringFactoryInBootstrap");
        log.info("名称为 {} 的 bean 实例，通过 bootstrap 上下文本身搜索：{}", "beanLoadBySpringFactoryInBootstrap", beanByBootstrapContext);
        log.info("beanByAppContext == beanByBootstrapContext ? {}", beanByAppContext == beanByBootstrapContext);

        //4. Spring Factories 机制定制 Bootstrap 属性配置源 PropertySources
        // 环境变量关系 (远程外部变量 > 本地变量)
        // 外部变量 CustomizePropertySourceLocator 中定义了 env.p1 的变量，值为：p1-value-in-customize-external-property-source
        // 本地 application.yaml 文件中也定义了 env.p1 的同名变量，值为：p1-value-in-local-application-yaml
        log.info("属性：{} 的值为：{}", "env.p1", appEnv.getProperty("env.p1"));
        // 更改环境变量关系，使得 (本地变量 覆盖 远程外部变量)，需要在外部配置中进行两步骤操作：
        /**
         * 1. 外部属性源设置允许本地属性覆盖外部属性的权限
         * 请将 {@link bootstrap.config.CustomizePropertySourceLocator} 类中
         * 【步骤一】spring.cloud.config.allowOverride 的属性值修改为 true
         *
         * 2. 外部属性源设置外部属性源覆盖本地属性源范围，有两种范围：远程属性不覆盖本地变量、远程属性只覆盖来源于本地文件的属性
         *
         *  2.1 默认情况，以下三种本地变量是会覆盖外部属性源配置，即 overrideSystemProperties = false
         *       系统属性源 (system properties)
         *       系统环境变量 (environment variables)
         *       命令行属性源 (command line arguments)
         *
         *      请将 {@link bootstrap.config.CustomizePropertySourceLocator} 类中
         *      【步骤二】2.1 spring.cloud.config.overrideSystemProperties 的属性修改为 true
         *      则下面 env.p2 的值将会打印外部属性的设置值，即：p2-value-in-customize-external-property-source
         *
         *  2.2 默认情况，允许外部属性覆盖本地文件定义的变量，即 spring.cloud.config.overrideNone = false
         *
         *      请将 {@link bootstrap.config.CustomizePropertySourceLocator} 类中
         *      【步骤二】2.2 spring.cloud.config.overrideNone 的属性修改为 true
         *      则下面 env.p2 的值将会打印系统本地变量的值（优先三种本地变量，其次本地文件变量），即：p2-value-from-system-properties
         *      如果将下面设置系统变量的代码注释，将打印本地文件 application.yaml 中的变量，即：p2-value-in-local-application-yaml
         */
         System.getProperties().setProperty("env.p2", "p2-value-from-system-properties");
        log.info("属性：{} 的值为：{}", "env.p2", appEnv.getProperty("env.p2"));

        // 此处假定已经对 env 端点进行 POST 属性修改，产生了 EnvironmentChangeEvent 事件
        log.info("生成 EnvironmentChangeEvent 事件...");
        EnvironmentChangeEvent changeEvent = new EnvironmentChangeEvent(Set.copyOf(List.of("env.p1")));
        context.publishEvent(changeEvent);

        //5. 自定义 EnvironmentChangeListener，观察日志显示能够监听到该事件

        //6. 同时，EnvironmentChangeEvent 事件使得 ConfigurationPropertiesRebinder 对所有
        //   被 @ConfigurationProperties 注解的 Bean 销毁并重新初始化，观察日志中有关 EnvProperties 生命周期方法打印信息
        //   类似地，LoggingRebinder 也会重新读取环境变量中的 logging.level.* 属性，重新设置日志级别

        //7. @RefreshScope 注解的类，也会在事件发生时，重现

    }
    // @formatter: off
}
