package com.example.springboot41.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.springboot41.datasource.Product;
import com.example.springboot41.datasource.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample: {@code spring.jpa.bootstrap=async} + Spring Data's
 * {@code spring.data.jpa.repositories.bootstrap-mode=deferred}.
 * <p>
 * With these two properties, the {@code EntityManagerFactory} bootstraps on
 * a background {@code AsyncTaskExecutor} while the rest of the context keeps
 * starting, and repository beans are deferred proxies that transparently
 * block on first use until that background bootstrap completes - so, from
 * the caller's point of view, the repository just works, but overall startup
 * is no longer serialized behind JPA metamodel building.
 */
@SpringBootTest(properties = {
        "spring.jpa.bootstrap=async",
        "spring.data.jpa.repositories.bootstrap-mode=deferred"
})
class JpaAsyncBootstrapTests {

    @Autowired
    ProductRepository productRepository;

    @Test
    void repositoryTransparentlyWaitsForTheBackgroundBootstrapToComplete() {
        Product saved = productRepository.save(new Product("gadget"));

        assertThat(productRepository.findById(saved.getId()))
                .get()
                .extracting(Product::getName)
                .isEqualTo("gadget");
    }
}
