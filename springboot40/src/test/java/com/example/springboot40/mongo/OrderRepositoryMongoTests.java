package com.example.springboot40.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample #3: Testcontainers 2.0 + Spring Boot's @ServiceConnection.
 * <p>
 * Spring Boot 4.0 upgrades to Testcontainers 2.0 (modules now prefixed
 * "testcontainers-", container classes relocated per-module) and adds
 * @ServiceConnection support for MongoDBAtlasLocalContainer, so the
 * MongoTemplate/MongoRepository beans are wired to the container with zero
 * manual property configuration.
 */
@DataMongoTest
@Testcontainers
class OrderRepositoryMongoTests {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void savesAndReadsBackAnOrder() {
        Order saved = orderRepository.save(new Order(null, "widget", 3));

        assertThat(orderRepository.findById(saved.id()))
                .get()
                .extracting(Order::product, Order::quantity)
                .containsExactly("widget", 3);
    }

    @TestConfiguration
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection
        MongoDBAtlasLocalContainer mongoDBAtlasLocalContainer() {
            return new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.9");
        }
    }
}
