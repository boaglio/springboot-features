package com.example.springboot40.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Sample #1b: RestTestClient against a real, running server.
 * <p>
 * With {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} plus
 * {@code @AutoConfigureRestTestClient}, Spring Boot 4.0 configures a
 * RestTestClient that targets the actual running server over real HTTP, so
 * the same fluent test API from the MockMvc sample also covers full
 * integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GreetingControllerLiveServerTests {

    @Autowired
    RestTestClient client;

    @Test
    void greetsByNameOverRealHttp() {
        // http :8080/api/greetings/Grace
        client.get()
                .uri("/api/greetings/{name}", "Grace")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GreetingController.Greeting.class)
                .isEqualTo(new GreetingController.Greeting("Hello, Grace!"));
    }
}
