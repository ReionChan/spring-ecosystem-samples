package io.github.reionchan.demo.observation.convention;

import io.github.reionchan.demo.observation.DemoObservationDocument;
import io.github.reionchan.demo.observation.context.DemoContext;
import io.micrometer.common.KeyValues;

import static io.micrometer.common.util.StringUtils.isNotBlank;

public class DefaultDemoObservationConvention implements DemoObservationConvention {
    @Override
    public KeyValues getLowCardinalityKeyValues(DemoContext context) {
        KeyValues keyValues = KeyValues.of(DemoObservationDocument.DemoLowCardinalityKeyNames.CALL_METHOD.withValue(context.getMethod().getName()));
        if (isNotBlank(context.getNote())) {
            keyValues.and(DemoObservationDocument.DemoLowCardinalityKeyNames.NOTE.withValue(context.getNote()));
        }
        return keyValues;
    }
}
