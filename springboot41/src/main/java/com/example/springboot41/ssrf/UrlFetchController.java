package com.example.springboot41.ssrf;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * A classic SSRF vector: fetching a caller-supplied URL server-side. The
 * {@link SsrfConfig#inetAddressFilter()} bean protects every call made
 * through the auto-configured {@link RestClient.Builder} automatically.
 */
@RestController
class UrlFetchController {

    private final RestClient restClient;

    UrlFetchController(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @GetMapping("/fetch")
    String fetch(@RequestParam String url) {
        return restClient.get().uri(url).retrieve().body(String.class);
    }
}
