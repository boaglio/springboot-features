package com.example.springboot40.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.springboot40.otel.TraceIdHeaderFilter;

/**
 * Sample #1a: RestTestClient backed by MockMvc.
 * <p>
 * New in Spring Boot 4.0 - with {@code @WebMvcTest} and
 * {@code @AutoConfigureRestTestClient}, you can autowire a RestTestClient
 * that dispatches through the mock servlet container instead of a socket,
 * replacing the older MockMvc-only fluent API for this style of test.
 * <p>
 * {@code TraceIdHeaderFilter} (sample #4) is excluded here because
 * {@code @WebMvcTest} auto-detects {@code Filter} beans, and this slice does
 * not load the tracing auto-configuration its constructor depends on.
 */
@WebMvcTest(controllers = GreetingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TraceIdHeaderFilter.class))
@AutoConfigureRestTestClient
class GreetingControllerMockMvcTests {

    @Autowired
    RestTestClient client;

    @Test
    void greetsByName() {
        // http :8080/api/greetings/Ada
        client.get()
                .uri("/api/greetings/{name}", "Ada")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GreetingController.Greeting.class)
                .isEqualTo(new GreetingController.Greeting("Hello, Ada!"));
    }
}
