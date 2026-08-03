package com.example.springboot40.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample #1: RestTestClient.
 * <p>
 * This controller has no 4.0-specific code itself - it is exercised in
 * {@code GreetingControllerMockMvcTests} (RestTestClient over MockMvc) and
 * {@code GreetingControllerLiveServerTests} (RestTestClient over a real
 * running server), which is where the new Spring Boot 4.0 test client is
 * actually demonstrated.
 */
@RestController
public class GreetingController {

    @GetMapping("/api/greetings/{name}")
    public Greeting greet(@PathVariable String name) {
        return new Greeting("Hello, " + name + "!");
    }

    public record Greeting(String message) {
    }
}
