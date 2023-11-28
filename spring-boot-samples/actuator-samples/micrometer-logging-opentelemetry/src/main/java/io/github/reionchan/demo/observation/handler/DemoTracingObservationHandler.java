package io.github.reionchan.demo.observation.handler;

import io.github.reionchan.demo.observation.context.DemoContext;
import io.micrometer.observation.Observation;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

/**
 *
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Component
@Order(LOWEST_PRECEDENCE - 2000)
public class DemoTracingObservationHandler implements TracingObservationHandler<DemoContext> {

    @Autowired
    private Tracer tracer;
    @Override
    public void onStart(DemoContext context) {
        Span span = tracer.nextSpan().name(context.getMethod().getName());
        span.tag("note", context.getNote());
        tagSpan(context, span);
        Tracer.SpanInScope scope = tracer.withSpan(span.start());
        context.put("outScope", scope);
        context.put("outSpan", span);
    }

    @Override
    public void onScopeOpened(DemoContext context) {
        Span outSpan = context.get("outSpan");
        Method scopeMethod = Optional.of((Method) context.get("scope")).get();
        Span inSpan = tracer.nextSpan(outSpan).name(scopeMethod.getName());
        Tracer.SpanInScope inScope = tracer.withSpan(inSpan.start());
        inSpan.tag("note", "inner call");
        context.put("inScope", inScope);
        context.put("inSpan", inSpan);
    }

    @Override
    public void onScopeClosed(DemoContext context) {
        ((Span) context.get("inSpan")).end();
        ((Tracer.SpanInScope)context.get("inScope")).close();
    }

    @Override
    public void onStop(DemoContext context) {
        ((Span) context.get("outSpan")).end();
        ((Tracer.SpanInScope) context.get("outScope")).close();
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof DemoContext;
    }

    @Override
    public Tracer getTracer() {
        return tracer;
    }
}
