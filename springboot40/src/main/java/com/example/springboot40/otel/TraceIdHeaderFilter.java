package com.example.springboot40.otel;

import java.io.IOException;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sample #4: spring-boot-starter-opentelemetry.
 * <p>
 * Just adding the starter as a dependency is enough for Spring Boot 4.0 to
 * auto-configure the OpenTelemetry SDK, a Micrometer {@link Tracer} bean and
 * OTLP exporters for metrics/traces. This filter injects the current trace
 * id into every response so the wiring is observable without a real OTLP
 * collector - see {@code TraceIdHeaderFilterTests}.
 */
@Component
public class TraceIdHeaderFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    TraceIdHeaderFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // currentTraceContext() is thread-local - it holds whatever span the
        // OTel auto-configuration already started for this request, so no
        // manual span creation is needed here.
        TraceContext context = this.tracer.currentTraceContext().context();
        if (context != null) {
            response.setHeader("X-Trace-Id", context.traceId());
        }
        filterChain.doFilter(request, response);
    }
}
