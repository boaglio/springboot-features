package com.example.springboot41.datasource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * A plain JPA entity, backing two Spring Boot 4.1 samples that are about
 * *how* the JPA layer starts up/connects, not about mapping itself - see
 * {@code LazyDataSourceConnectionFetchTests} (lazy connection acquisition)
 * and {@code JpaAsyncBootstrapTests} (background EntityManagerFactory
 * bootstrap).
 */
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    protected Product() {
    }

    public Product(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
