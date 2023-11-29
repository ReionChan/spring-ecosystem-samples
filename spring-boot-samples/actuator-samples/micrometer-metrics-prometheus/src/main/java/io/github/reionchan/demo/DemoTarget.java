package io.github.reionchan.demo;

import io.github.reionchan.demo.observation.DemoObservationDocument;
import io.github.reionchan.demo.observation.context.DemoContext;
import io.github.reionchan.demo.observation.convention.DefaultDemoObservationConvention;
import io.github.reionchan.demo.observation.convention.DemoObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

import static org.springframework.util.ReflectionUtils.findMethod;

/**
 * 被观测的演示对象
 *
 * <pre>
 *  在插装支持观察时，需要保证未激活可观察性时，代码执行不受插装代码影响
 * </pre>
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Slf4j
@Component
public class DemoTarget {

    private static final DemoObservationConvention DEFAULT_CONVENTION = new DefaultDemoObservationConvention();

    // 需要考虑未开启 observation 的情况
    @Autowired(required = false)
    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

    // 此处保留自定义的 convention，如果不配置，则使用默认的 convention
    @Autowired(required = false)
    private DemoObservationConvention convention;

    private Random random = new Random();

    public void methodA() {
        // 上下文 DemoContext 放入 lambda 表达式，仅当启用 observation 时才会创建
        Observation observation = DemoObservationDocument.DEFAULT.observation(
            convention, DEFAULT_CONVENTION,
            () -> {
                DemoContext ctx = new DemoContext(findMethod(DemoTarget.class, "methodA"));
                ctx.setNote("caller: methodA");
                return ctx;
            }, observationRegistry);
        // 观察开始
        observation.start();
        log.info("do something in methodA");
        try {
            Thread.sleep(random.nextInt(300));
            // 嵌套观察
            observation.scoped(() -> methodB());
        } catch (Exception e) {
            log.error("error", e);
            // 观察异常
            observation.error(e);
        } finally {
            // 观察结束
            observation.stop();
        }
    }

    public void methodB() {
        log.info("do something in methodB");
        try {
            Thread.sleep(random.nextInt(500));
        } catch (Exception e) {
            log.error("error", e);
        }
    }
}
