package com.example.springboot40.versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample #5: two handlers sharing {@code /api/products/{id}}, selected by
 * the {@code X-API-Version} header configured in {@link ApiVersioningConfig}.
 * V1 is the deprecated shape (no price); V2 adds it. A request with no
 * header falls back to the configured default version (1); a request for
 * an unsupported version (e.g. "3") is rejected with 400 Bad Request before
 * either method is even invoked - and so is any request resolving to
 * version 1, but with 401, once "now" is past that version's configured
 * sunset date (see {@link SunsetEnforcingDeprecationHandler}).
 */
@RestController
public class ProductController {

    // version is the new @GetMapping/@RequestMapping attribute (Spring
    // Framework 7) - it adds a version match on top of the path, so this and
    // the method below can share the same URL.
    @GetMapping(path = "/api/products/{id}", version = "1")
    public ProductV1 getProductV1(@PathVariable String id) {
        return new ProductV1(id, "Widget");
    }

    @GetMapping(path = "/api/products/{id}", version = "2")
    public ProductV2 getProductV2(@PathVariable String id) {
        return new ProductV2(id, "Widget", 9.99);
    }

    public record ProductV1(String id, String name) {
    }

    public record ProductV2(String id, String name, double price) {
    }
}
