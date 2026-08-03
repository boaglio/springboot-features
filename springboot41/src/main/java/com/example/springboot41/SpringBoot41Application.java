package com.example.springboot41;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // required for the TraceCapturingService @Async sample - without it, @Async methods just run synchronously
public class SpringBoot41Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot41Application.class, args);
    }
}
