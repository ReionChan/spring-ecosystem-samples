package io.github.reionchan.demo.observation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.reionchan.demo.observation.DemoContext.DEMO_METRIC_PREFIX;

/**
 * 演示类观测处理器
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Slf4j
@Component
public class DemoObservationHandler implements MeterObservationHandler<DemoContext> {

    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public void onStart(DemoContext context) {
        // 统计次数
        Counter methodCounter = Counter.builder(DEMO_METRIC_PREFIX + context.getMethod().getName() + ".count")
                .description("call %s method times".formatted(context.getMethod().getName()))
                .baseUnit("times")
                .tags(getTags(context))
                .register(meterRegistry);

        // 统计时间
        Timer.Sample sample = Timer.start(meterRegistry);
        context.put("sample", sample);
        methodCounter.increment();
    }

    @Override
    public void onError(DemoContext context) {
        log.error("onError", context.getError());
    }

    @Override
    public void onScopeOpened(DemoContext context) {
        Timer.Sample scopeSample = Timer.start(meterRegistry);
        context.put("scopeSample", scopeSample);
    }

    @Override
    public void onScopeClosed(DemoContext context) {
        Timer.Sample scopeSample = context.get("scopeSample");
        Method scopeMethod = Optional.of((Method) context.get("scope")).get();

        Timer timer = Timer.builder(DEMO_METRIC_PREFIX + scopeMethod.getName() + ".time")
                .description("call %s method used time".formatted(scopeMethod.getName()))
                .tags(getTags(context))
                .register(meterRegistry);
        scopeSample.stop(timer);
        context.remove("scope");
        context.remove("scopeSample");
    }

    @Override
    public void onStop(DemoContext context) {
        Timer.Sample sample = context.get("sample");
        Timer timer = Timer.builder(DEMO_METRIC_PREFIX + context.getMethod().getName() + ".time")
                .description("call %s method used time".formatted(context.getMethod().getName()))
                .tags(getTags(context))
                .register(meterRegistry);
        sample.stop(timer);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof DemoContext;
    }

    private Iterable<Tag> getTags(DemoContext context) {
        return context.getLowCardinalityKeyValues().stream().map(kv -> Tag.of(kv.getKey(), kv.getValue())).collect(Collectors.toList());
    }
}
