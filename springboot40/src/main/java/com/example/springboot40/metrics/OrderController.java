package com.example.springboot40.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample #2: exposes {@link OrderService#placeOrder} over HTTP, so its
 * {@code @Counted}/{@code @Timed}/{@code @MeterTag} meters actually get
 * recorded during a normal request - and pushed to the OTLP collector/
 * Grafana (see docker-compose.yml) - instead of only ever running inside
 * {@code OrderServiceMetricsTests}.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{region}/{tier}")
    public OrderConfirmation placeOrder(@PathVariable String region, @PathVariable String tier) {
        return new OrderConfirmation(orderService.placeOrder(region, tier));
    }

    public record OrderConfirmation(String confirmation) {
    }
}
