package io.github.reionchan.demo.observation.convention;

import io.github.reionchan.demo.observation.context.DemoContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * 为演示示例的观测条约
 *
 * @author Reion
 * @date 2023-11-23
 **/
public interface DemoObservationConvention extends ObservationConvention<DemoContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof DemoContext;
    }
}
