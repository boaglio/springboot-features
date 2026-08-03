package com.example.springboot40.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Sample #2: exercises {@link OrderController} over real HTTP, so
 * {@code OrderService}'s meters get recorded through a normal request path.
 * This only checks the HTTP wiring/response shape - see
 * {@code OrderServiceMetricsTests} for assertions on the meters themselves.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class OrderControllerTests {

    @Autowired
    RestTestClient client;

    @Test
    void placesAnOrderAndReturnsConfirmation() {
        // http :8080/api/orders/emea/gold
        client.get()
                .uri("/api/orders/{region}/{tier}", "emea", "gold")
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderController.OrderConfirmation.class)
                .isEqualTo(new OrderController.OrderConfirmation("order-for-emea-gold"));
    }
}
