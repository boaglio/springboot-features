package com.example.springboot41.redis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.data.redis.annotation.RedisListener;
import org.springframework.stereotype.Component;

/**
 * Sample: {@code @RedisListener} auto-configuration.
 * <p>
 * Spring Boot 4.1 auto-registers a default {@code RedisMessageListenerContainer}
 * if the application doesn't define one, so this method-level subscription
 * works with no manual container wiring - just this annotation. The
 * {@code consumes} attribute enables automatic JSON deserialization of the
 * published payload into {@link Order}.
 */
@Component
public class OrderSubscriber {

    private final BlockingQueue<Order> received = new LinkedBlockingQueue<>();

    // consumes tells the listener to JSON-deserialize the raw pub/sub
    // payload straight into an Order, instead of handing back raw bytes/String.
    @RedisListener(topic = "orders", consumes = "application/json")
    public void handleMessage(Order order) {
        received.add(order);
    }

    public BlockingQueue<Order> getReceived() {
        return received;
    }
}
