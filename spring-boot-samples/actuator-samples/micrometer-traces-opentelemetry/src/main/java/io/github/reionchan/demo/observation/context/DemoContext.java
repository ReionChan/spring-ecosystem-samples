package io.github.reionchan.demo.observation.context;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.handler.TracingObservationHandler;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * 演示类的观测上下文
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Data
public class DemoContext extends Observation.Context {

    public static final String DEMO_METRIC_PREFIX = "demo.";

    // 当前执行方法
    private Method method;

    // 备注（选填）
    private String note;
    
    public DemoContext(Method method) {
        this.method = method;
    }

}
