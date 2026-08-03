package com.example.springboot41.async;

import java.util.concurrent.TimeUnit;

import io.micrometer.tracing.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes both the caller's trace ID and the trace ID observed inside
 * {@link TraceCapturingService#currentTraceId()} so a test can assert they
 * match. Context propagation relies on Micrometer's
 * {@code ObservationThreadLocalAccessor}, which only has something to
 * capture once an {@code Observation} is active - here, the one Boot's web
 * instrumentation opens automatically for this request. Manually creating a
 * span with {@code Tracer.withSpan(...)} (bypassing the Observation API)
 * would NOT propagate to the async thread.
 */
@RestController
class AsyncTraceController {

    private final Tracer tracer;
    private final TraceCapturingService traceCapturingService;

    AsyncTraceController(Tracer tracer, TraceCapturingService traceCapturingService) {
        this.tracer = tracer;
        this.traceCapturingService = traceCapturingService;
    }

    @GetMapping("/async-trace")
    TraceIds traceIds() throws Exception {
        String callerTraceId = tracer.currentTraceContext().context().traceId();
        String asyncTraceId = traceCapturingService.currentTraceId().get(5, TimeUnit.SECONDS);
        return new TraceIds(callerTraceId, asyncTraceId);
    }

    record TraceIds(String callerTraceId, String asyncTraceId) {
    }
}
