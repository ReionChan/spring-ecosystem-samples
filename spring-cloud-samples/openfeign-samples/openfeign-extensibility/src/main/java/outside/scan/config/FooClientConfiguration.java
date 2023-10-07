package outside.scan.config;

import feign.Request;
import feign.micrometer.FeignContext;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenFeign 客户端定制化配置类
 *
 * <pre>
 * 此处由于使用 @Configuration 注解，因此将其放在本应用扫描之外的包名，
 * 从而避免原本只为 FooClient 定制配置的 Feign 组件 Bean 被主应用上下文扫描，
 * 进而覆盖全局默认的组件。
 *
 * 详细参考：
 *  <a href="https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-overriding-defaults">覆盖默认配置</a>
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@Configuration
public class FooClientConfiguration {

    /**
     * 定义 FooClient 独占的 ObservationRegistry
     * 与容器自动装载的相同类型的 Bean 做隔离
     * {@link ObservationAutoConfiguration#observationRegistry() observationRegistry()}
     */
    @Bean
    ObservationRegistry fooClientObservationRegistry() {
        return ObservationRegistry.create();
    }

    /**
     * 自定义 MicrometerObservationCapability 覆盖自动装配的 MicrometerObservationCapability
     *
     * @param registry 使用本子上下文中的 ObservationRegistry
     * @param meterRegistry 使用全局的 simple 仪表注册器
     *      {@link SimpleMetricsExportAutoConfiguration#simpleMeterRegistry(SimpleConfig, Clock) simpleMeterRegistry()}
     * @return
     */
    @Bean
    public MicrometerObservationCapability micrometerObservationCapability(
            @Qualifier("fooClientObservationRegistry") ObservationRegistry registry,
            MeterRegistry meterRegistry) {
        registry.observationConfig().observationHandler(new FooClientObservationHandler(meterRegistry));
        return new MicrometerObservationCapability(registry);
    }

    /**
     * 自定义 FooClient OpenFeign 客户端指标观察处理器
     *
     * 用来在端点 /actuator/metrics 中显示 FooClient 各个方法执行次数及耗时指标
     * 以 fooClient 为前缀的指标
     */
    @Slf4j
    private static class FooClientObservationHandler implements MeterObservationHandler<FeignContext> {

        /**
         * FooClient 在端点 /actuator/metrics 中的指标名称前缀
         */
        private static final String FOO_CLIENT_PREFIX = "fooClient.";

        private MeterRegistry registry;

        public FooClientObservationHandler(MeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void onStart(FeignContext context) {
            log.info("onStart ==> {}", context.getClass().getSimpleName());
            String callMethod = context.getCarrier().requestTemplate().methodMetadata().method().getName();
            // 统计次数
            Counter methodCounter = Counter.builder(FOO_CLIENT_PREFIX + callMethod + ".count")
                    .tags(getTags(context.getCarrier()))
                    .description("调用 FooClient 的 " + callMethod + " 方法次数")
                    .baseUnit("次")
                    .register(registry);

            // 统计时间
            Timer.Sample sample = Timer.start(registry);
            context.put("sample", sample);
            methodCounter.increment();
        }

        @Override
        public void onStop(FeignContext context) {
            log.info("onStop ==> {}", context.getClass().getSimpleName());
            String callMethod = context.getCarrier().requestTemplate().methodMetadata().method().getName();
            Timer.Sample sample = context.get("sample");
            Timer timer = Timer.builder(FOO_CLIENT_PREFIX + callMethod + ".time")
                    .description("调用 FooClient 的 " + callMethod + " 方法耗时")
                    .tags(getTags(context.getCarrier()))
                    .register(registry);
            sample.stop(timer);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            log.info("supportsContext ==> {}", context.getClass().getSimpleName());
            return context instanceof FeignContext;
        }

        private Iterable<Tag> getTags(Request request) {
            List<Tag> tags = new ArrayList<>();
            tags.add(Tag.of("method", request.requestTemplate().method()));
            tags.add(Tag.of("uri", request.requestTemplate().methodMetadata().template().path()));
            tags.add(Tag.of("name", request.requestTemplate().methodMetadata().method().getName()));
            return tags;
        }
    }
}
