package com.example.springboot41.redis;

import java.util.concurrent.TimeUnit;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Publishes a JSON message on the "orders" channel and verifies the
 * auto-configured {@code @RedisListener} container delivers it to
 * {@link OrderSubscriber}, deserialized into an {@link Order}. A plain
 * {@code redis} image is enough - Boot's {@code RedisContainerConnectionDetailsFactory}
 * matches any container whose image name starts with "redis", no dedicated
 * Testcontainers Redis module needed.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude=")
@Testcontainers
class OrderSubscriberTests {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    OrderSubscriber orderSubscriber;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void deliversPublishedMessagesToTheListener() throws Exception {
        redisTemplate.convertAndSend("orders", objectMapper.writeValueAsString(new Order("widget", 3)));

        Order received = orderSubscriber.getReceived().poll(10, TimeUnit.SECONDS);

        assertThat(received).isEqualTo(new Order("widget", 3));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection("redis")
        GenericContainer<?> redisContainer() {
            return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        }
    }
}
