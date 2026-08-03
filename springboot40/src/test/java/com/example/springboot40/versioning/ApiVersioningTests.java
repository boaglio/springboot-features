package com.example.springboot40.versioning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.springboot40.otel.TraceIdHeaderFilter;

/**
 * Sample #5: exercises the version resolution, default-version fallback,
 * unsupported-version rejection, and deprecation/sunset handling configured
 * in {@link ApiVersioningConfig}. These assertions track whatever sunset
 * date is currently configured there: as written, that date is in the
 * future, so version 1 is deprecated-but-still-served (200 + warning
 * headers); flip it to the past and these two tests should expect 401
 * instead - see {@link SunsetEnforcingDeprecationHandlerTests} for
 * coverage of the enforcement mechanism itself that doesn't depend on
 * today's date either way.
 * <p>
 * {@code TraceIdHeaderFilter} (sample #4) is excluded here for the same
 * reason as in {@code GreetingControllerMockMvcTests}: {@code @WebMvcTest}
 * auto-detects {@code Filter} beans, and this slice does not load the
 * tracing auto-configuration its constructor depends on.
 */
@WebMvcTest(controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TraceIdHeaderFilter.class))
@AutoConfigureRestTestClient
class ApiVersioningTests {

    @Autowired
    RestTestClient client;

    @Test
    void noHeaderFallsBackToTheDefaultVersion() {
        // http :8080/api/products/p1
        client.get()
                .uri("/api/products/{id}", "p1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductController.ProductV1.class)
                .isEqualTo(new ProductController.ProductV1("p1", "Widget"));
    }

    @Test
    void version2ReturnsTheEnrichedShape() {
        // http :8080/api/products/p1 X-API-Version:2
        client.get()
                .uri("/api/products/{id}", "p1")
                .header("X-API-Version", "2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductController.ProductV2.class)
                .isEqualTo(new ProductController.ProductV2("p1", "Widget", 9.99));
    }

    @Test
    void deprecatedVersion1SendsDeprecationAndSunsetHeaders() {
        // http :8080/api/products/p1 X-API-Version:1
        client.get()
                .uri("/api/products/{id}", "p1")
                .header("X-API-Version", "1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Deprecation")
                .expectHeader().exists("Sunset");
    }

    @Test
    void unsupportedVersionIsRejected() {
        // http :8080/api/products/p1 X-API-Version:3
        client.get()
                .uri("/api/products/{id}", "p1")
                .header("X-API-Version", "3")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
