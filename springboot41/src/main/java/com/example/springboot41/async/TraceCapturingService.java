package com.example.springboot41.async;

import java.util.concurrent.CompletableFuture;

import io.micrometer.tracing.Tracer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sample: Spring Boot 4.1 propagates Micrometer context (trace IDs, spans)
 * across {@code @Async} methods automatically. This method runs on a
 * separate thread-pool thread yet still sees the caller's trace context -
 * no manual {@code TaskDecorator} or {@code ContextSnapshot} wiring needed.
 */
@Service
public class TraceCapturingService {

    private final Tracer tracer;

    TraceCapturingService(Tracer tracer) {
        this.tracer = tracer;
    }

    // @Async itself isn't new - what's new is that the executor Boot builds
    // for it now carries the caller's trace context onto this thread by
    // default, given spring.task.execution.propagate-context=true
    // (see application.yml).
    @Async
    public CompletableFuture<String> currentTraceId() {
        var context = tracer.currentTraceContext().context();
        return CompletableFuture.completedFuture(context != null ? context.traceId() : null);
    }
}
