package com.example.springboot41.async;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample: {@code @Async} + automatic Micrometer context propagation.
 * <p>
 * Hits a real endpoint (so an {@code Observation} is already active, the
 * same way it would be in production) and asserts the trace ID captured
 * inside {@link TraceCapturingService#currentTraceId()} - which runs on a
 * separate thread-pool thread - matches the caller's trace ID.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")
@AutoConfigureRestTestClient
class TraceCapturingServiceTests {

    @Autowired
    RestTestClient client;

    @Test
    void asyncMethodSeesTheCallersTraceId() {
        // http :8080/async-trace
        client.get()
                .uri("/async-trace")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AsyncTraceController.TraceIds.class)
                .value(traceIds -> {
                    assertThat(traceIds.callerTraceId()).isNotBlank();
                    assertThat(traceIds.asyncTraceId()).isEqualTo(traceIds.callerTraceId());
                });
    }
}
