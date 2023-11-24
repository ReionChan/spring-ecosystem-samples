package io.github.reionchan.demo;

import io.github.reionchan.demo.observation.DefaultDemoObservationConvention;
import io.github.reionchan.demo.observation.DemoContext;
import io.github.reionchan.demo.observation.DemoObservationConvention;
import io.github.reionchan.demo.observation.DemoObservationDocument;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.springframework.util.ReflectionUtils.findMethod;

/**
 * 被观测的演示对象
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Slf4j
@Component
public class DemoTarget {

    private static final DemoObservationConvention DEFAULT_CONVENTION = new DefaultDemoObservationConvention();

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private DemoObservationConvention convention;

    public void methodA() {
        final DemoContext ctx = new DemoContext(findMethod(this.getClass(), "methodA"));
        ctx.setNote("caller: methodA");
        Observation observation = DemoObservationDocument.DEFAULT.observation(convention, DEFAULT_CONVENTION, ()-> ctx, observationRegistry);
        observation.start();
        log.info("do something in methodA");
        try {
            Thread.sleep(200);
            ctx.put("scope", findMethod(this.getClass(), "methodB"));
            observation.scoped(() -> methodB());
        } catch (Exception e) {
            log.error("error", e);
            observation.error(e);
        } finally {
            observation.stop();
        }
    }

    public void methodB() {
        log.info("do something in methodB");
        try {
            Thread.sleep(300);
        } catch (Exception e) {
            log.error("error", e);
        }
    }
}
