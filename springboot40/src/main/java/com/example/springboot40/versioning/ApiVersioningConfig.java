package com.example.springboot40.versioning;

import java.net.URI;
import java.time.ZonedDateTime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sample #5: API versioning, new in Spring Framework 7 (carried by Spring
 * Boot 4.0's spring-boot-starter-web).
 * <p>
 * {@code @GetMapping(version = ...)} lets several handler methods share one
 * path and be selected by request version instead of just by HTTP method -
 * see {@link ProductController}. This configurer wires up how that version
 * is read off the request (a header here, though a path segment, query
 * param or media type parameter all work too), which versions are
 * supported, what to fall back to when a client sends none, and how to warn
 * (and, past the sunset date, block) clients still on a deprecated version -
 * see {@link SunsetEnforcingDeprecationHandler}.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        // Sends RFC 9745/8594 headers for the version(s) configured below,
        // same as Spring's own StandardApiVersionDeprecationHandler, but
        // additionally rejects requests with 401 once "now" is past the
        // registered sunset date - see SunsetEnforcingDeprecationHandler.
        SunsetEnforcingDeprecationHandler deprecationHandler = new SunsetEnforcingDeprecationHandler();
        deprecationHandler
                // "Sunset: <RFC 1123 date>" - and, once passed, the version stops working entirely (401).
                .configureVersion("1", ZonedDateTime.parse("2026-10-31T00:00:00Z"))
                // "Deprecation: @<epoch-seconds>" - this version is deprecated as of this date.
                .setDeprecationDate(ZonedDateTime.parse("2026-01-01T00:00:00Z"))
                // "Link: <uri>; rel=\"sunset\"" - where clients can read about the removal/migration.
                .setSunsetLink(URI.create("https://example.com/api/products/v2-migration-guide"));

        configurer.useRequestHeader("X-API-Version") // read the version from this header (vs. path segment/query param/media type)
                .addSupportedVersions("1", "2") // requests for any other version get rejected with 400
                .setDefaultVersion("1") // used when a request sends no version header at all - also subject to sunset enforcement below
                .setDeprecationHandler(deprecationHandler);
    }
}
