package com.example.springboot41.ssrf;

import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sample: SSRF mitigation via {@code InetAddressFilter}.
 * <p>
 * Declaring a single bean of this type is enough - Spring Boot 4.1's
 * {@code HttpClientAutoConfiguration} applies it to every auto-configured
 * HTTP client ({@code RestClient}, {@code WebClient}, {@code RestTemplate}),
 * with no per-call wiring needed. {@code externalAddresses()} blocks
 * loopback, RFC1918 private ranges, link-local addresses and the cloud
 * metadata endpoint (169.254.169.254) - the classic SSRF targets when an
 * application fetches a user-supplied URL server-side.
 */
@Configuration
class SsrfConfig {

    @Bean
    InetAddressFilter inetAddressFilter() {
        return InetAddressFilter.externalAddresses();
    }
}
