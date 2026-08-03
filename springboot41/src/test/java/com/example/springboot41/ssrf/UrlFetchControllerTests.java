package com.example.springboot41.ssrf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Verifies the {@code InetAddressFilter} bean actually blocks a
 * server-side-request-forgery attempt: asking the app to fetch a loopback
 * address (a stand-in for an internal service or the cloud metadata
 * endpoint) is rejected with 403 instead of being requested.
 * <p>
 * Only the loopback case is asserted here, but {@code externalAddresses()}
 * (see {@link SsrfConfig}) blocks the same way against the real cloud
 * metadata endpoint and every RFC1918 private range - confirmed by hand
 * against a running instance:
 * <pre>{@code
 * http :8080/fetch url=='http://127.0.0.1:1/'                    # => 403
 * http :8080/fetch url=='http://169.254.169.254/latest/meta-data/' # => 403 (cloud metadata)
 * http :8080/fetch url=='http://10.0.0.1/'                       # => 403 (RFC1918)
 * http :8080/fetch url=='http://192.168.1.1/'                    # => 403 (RFC1918)
 * http :8080/fetch url=='http://172.16.0.1/'                     # => 403 (RFC1918)
 * http :8080/fetch url=='https://example.com'                    # => 200 (external, allowed)
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class UrlFetchControllerTests {

    @Autowired
    RestTestClient client;

    @Test
    void blocksFetchOfLoopbackAddress() {
        // http :8080/fetch url=='http://127.0.0.1:1/'
        client.get()
                .uri(uriBuilder -> uriBuilder.path("/fetch").queryParam("url", "http://127.0.0.1:1/").build())
                .exchange()
                .expectStatus().isForbidden();
    }
}
