package com.example.springboot40.metrics;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import org.springframework.stereotype.Service;

/**
 * Sample #2: {@code @MeterTag} on {@code @Counted}/{@code @Timed} methods,
 * resolved with a SpEL-based ValueExpressionResolver (see
 * {@link MetricsConfig}).
 * <p>
 * Spring Boot 4.0's auto-configuration for the Micrometer metrics aspects
 * wires the {@code @MeterTag} handlers automatically, given
 * {@code management.observations.annotations.enabled=true} (see
 * application.yml), AspectJ on the classpath (see the aspectjweaver
 * dependency) and a {@code ValueExpressionResolver} bean.
 * <p>
 * The resolver only ever sees the value of the single annotated parameter,
 * exposed as the SpEL root object - so {@code expression} is written as a
 * root-relative expression (e.g. {@code "toUpperCase()"}), not as a
 * {@code "#paramName"} reference to a sibling parameter.
 * <p>
 * See {@link OrderController} for an HTTP-reachable way to trigger this
 * (and {@code OrderServiceMetricsTests} for direct assertions on the
 * recorded meters).
 */
@Service
public class OrderService {

    @Counted(value = "orders.placed", description = "Number of orders placed")
    @Timed(value = "orders.placed.latency", description = "Time to place an order")
    public String placeOrder(@MeterTag("region") String region,
                              @MeterTag(key = "tier", expression = "toUpperCase()") String customerTier) {
        return "order-for-%s-%s".formatted(region, customerTier);
    }
}
