package com.example.springboot40.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample #2: verifies that @Counted/@Timed + @MeterTag actually record tagged
 * meters, using the SpEL-based ValueExpressionResolver that Spring Boot 4.0
 * wires in automatically once management.observations.annotations.enabled=true.
 */
@SpringBootTest
class OrderServiceMetricsTests {

    @Autowired
    OrderService orderService;

    @Autowired
    SimpleMeterRegistry meterRegistry;

    @Test
    void placingAnOrderRecordsCounterAndTimerTaggedBySpelExpressions() {
        orderService.placeOrder("emea", "gold");

        assertThat(meterRegistry.get("orders.placed")
                .tag("region", "emea")
                .tag("tier", "GOLD")
                .counter()
                .count()).isEqualTo(1.0);

        assertThat(meterRegistry.get("orders.placed.latency")
                .tag("region", "emea")
                .tag("tier", "GOLD")
                .timer()
                .count()).isEqualTo(1L);
    }

    @TestConfiguration
    static class MeterRegistryConfig {

        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
