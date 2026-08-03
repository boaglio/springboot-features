package com.example.springboot41.datasource;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample: {@code spring.datasource.connection-fetch=lazy}.
 * <p>
 * Spring Boot 4.1 wraps the auto-configured pooled DataSource with
 * {@link LazyConnectionDataSourceProxy} when this property is set, so a
 * physical JDBC connection is only pulled from the pool once a statement
 * actually runs - not just because a {@code DataSource} bean was injected.
 */
@SpringBootTest(properties = "spring.datasource.connection-fetch=lazy")
class LazyDataSourceConnectionFetchTests {

    @Autowired
    DataSource dataSource;

    @Autowired
    ProductRepository productRepository;

    @Test
    void wrapsThePooledDataSourceInALazyProxy() {
        assertThat(dataSource).isInstanceOf(LazyConnectionDataSourceProxy.class);
    }

    @Test
    void repositoryOperationsStillWorkThroughTheLazyProxy() {
        Product saved = productRepository.save(new Product("widget"));

        assertThat(productRepository.findById(saved.getId()))
                .get()
                .extracting(Product::getName)
                .isEqualTo("widget");
    }
}
