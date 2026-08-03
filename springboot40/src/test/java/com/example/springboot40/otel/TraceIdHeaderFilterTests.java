package com.example.springboot40.otel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample #4: verifies that the spring-boot-starter-opentelemetry
 * auto-configuration actually produces a usable Tracer - every request gets
 * a non-blank W3C trace id, with 100% sampling forced just for this test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")
@AutoConfigureRestTestClient
class TraceIdHeaderFilterTests {

    @Autowired
    RestTestClient client;

    @Test
    void everyResponseCarriesAnOpenTelemetryTraceId() {
        // http :8080/api/greetings/Otel
        client.get()
                .uri("/api/greetings/{name}", "Otel")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Trace-Id", traceId -> assertThat(traceId).hasSize(32));
    }
}
