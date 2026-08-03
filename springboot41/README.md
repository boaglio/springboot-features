# springboot41

Six distinct, runnable samples of the headline Spring Boot **4.1** features, built on Spring Boot 4.1.0 / Spring Framework 7. Each sample is a small piece of production code plus a test that actually exercises it.

Requires **Java 25** (see `java.version` in `pom.xml`).

Run everything with:

```bash
mvn test
```

(Sample #5 needs Docker running - it launches a real Redis container via Testcontainers. Sample #1's gRPC server also binds to port 9090 during tests.)

## Running the app locally, with observability

```bash
mvn spring-boot:run
```

`docker-compose.yml` defines the app's backing services - Redis and an all-in-one `grafana/otel-lgtm` container (OTel Collector + Grafana + Tempo + Prometheus/Mimir + Loki). H2 stays in-process, so it has no container. Because `spring-boot-docker-compose` is on the classpath, Spring Boot auto-starts and stops these containers whenever the app runs locally this way - no manual `docker compose up` needed, and no manual connection properties either (Spring Boot's service-connection support wires Redis automatically by image name).

The Grafana UI and OTLP ports are offset from springboot40's (`3001`/`4417`/`4418` instead of `3000`/`4317`/`4318`) so both projects' stacks can run side by side without colliding. Once it's up, hit any endpoint and see what actually got recorded at **http://localhost:3001** (login `admin`/`admin`):

- **Explore → Tempo**, search `service.name = springboot41` (note: this app never sets `spring.application.name`, so traces currently show up as `unknown_service` - a small, harmless gap in this sample, not a pipeline problem).
- **Explore → Prometheus**, query e.g. `http_server_requests_seconds_count`, for metrics.

`docker-compose.full.yml` is a separate, fully containerized variant (app included, built from `Dockerfile`) for anyone who wants to run the whole stack in Docker without a local JDK/Maven.

## Sample #1: gRPC auto-configuration (`@GrpcService`, `@GrpcAdvice`)

Spring Boot 4.1 auto-configures a Netty-backed gRPC server - `@GrpcService` beans are discovered and bound automatically, no manual `ServerBuilder` wiring.

- `grpc/GreeterService.java` - a `BindableService` built with hand-written `MethodDescriptor`s over the well-known `com.google.protobuf.StringValue` type, so the sample needs **no protoc/protobuf-maven-plugin toolchain**. (The official `io.github.ascopes:protobuf-maven-plugin`, which Boot's parent POM manages for real `.proto` compilation, hit a Guice/Maven-resolver compatibility bug in this environment - see [ascopes/protobuf-maven-plugin#853](https://github.com/ascopes/protobuf-maven-plugin/issues/853). A real project on a working toolchain would generate stubs from a `.proto` file instead.)
- `grpc/GreeterExceptionAdvice.java` - `@GrpcAdvice` + `@GrpcExceptionHandler`, the gRPC equivalent of `@ControllerAdvice`.
- `grpc/GreeterServiceTests.java` - uses `@AutoConfigureTestGrpcTransport` (in-process channel/server, no sockets) to call the service and verify the advice maps `IllegalArgumentException` to `INVALID_ARGUMENT`.
- **Gotcha:** unlike `@ControllerAdvice`, both the `@GrpcAdvice` class and its `@GrpcExceptionHandler` method must be `public` - package-private silently fails with a generic `INTERNAL` status instead of invoking your handler.

## Sample #2: SSRF mitigation (`InetAddressFilter`)

- `ssrf/SsrfConfig.java` - one `InetAddressFilter.externalAddresses()` bean, auto-applied by `HttpClientAutoConfiguration` to every auto-configured HTTP client (`RestClient`, `WebClient`, `RestTemplate`).
- `ssrf/UrlFetchController.java` - a classic SSRF vector: fetches a caller-supplied URL server-side.
- `ssrf/SsrfExceptionHandler.java` - maps the resulting `FilteredHostException` (thrown by `InetAddressFilter` when a target is blocklisted) to a 403 instead of an unhandled 500.
- `ssrf/UrlFetchControllerTests.java` - asks the app to fetch a loopback address and asserts it's blocked.
- **Gotcha:** neither `InetAddressFilter`/`FilteredHostException` (module `spring-boot-http-client`) nor `RestClient.Builder` auto-configuration (module `spring-boot-starter-restclient`) are pulled in transitively by `spring-boot-starter-web` alone - both had to be declared explicitly.

## Sample #3: Lazy JDBC connections (`spring.datasource.connection-fetch=lazy`)

- `datasource/Product.java` / `ProductRepository.java` - a plain JPA entity/repository, shared with sample #4 below.
- `datasource/LazyDataSourceConnectionFetchTests.java` - asserts the auto-configured `DataSource` bean is wrapped in `LazyConnectionDataSourceProxy` when the property is set, and that repository operations still work through the proxy.

## Sample #4: Async JPA bootstrap (`spring.jpa.bootstrap=async`)

- `jpa/JpaAsyncBootstrapTests.java` - combines `spring.jpa.bootstrap=async` with Spring Data's `spring.data.jpa.repositories.bootstrap-mode=deferred`: the `EntityManagerFactory` builds on a background `AsyncTaskExecutor` while the rest of the context keeps starting, and the deferred repository proxy transparently blocks on first use until that finishes.

## Sample #5: `@RedisListener` auto-configuration

- `redis/OrderSubscriber.java` - a `@RedisListener(topic = "orders", consumes = "application/json")` method; Boot auto-registers a default `RedisMessageListenerContainer` since the app doesn't define one. `consumes` tells the listener to JSON-deserialize the raw pub/sub payload straight into an `Order`.
- `redis/OrderSubscriberTests.java` - publishes a JSON message via `StringRedisTemplate` against a real Testcontainers Redis and asserts it's delivered, deserialized.
- **Gotchas:**
  - Testcontainers 2.0 has **no dedicated Redis module** - a plain `GenericContainer` with a `redis:*` image works, because `RedisContainerConnectionDetailsFactory` matches by image name.
  - A `GenericContainer` isn't automatically recognized as a Redis connection source - `@ServiceConnection` needs an explicit name: `@ServiceConnection("redis")`.
  - This sample shares its Spring context configuration class with every other sample (`SpringBoot41Application`), so `OrderSubscriber`'s listener container tries to connect to Redis in *every* test, not just this one. `src/test/resources/application.yml` excludes `DataRedisAutoConfiguration` by default; `OrderSubscriberTests` clears that one property to re-enable it for its own context.

## Sample #6: `@Async` + automatic Micrometer context propagation

- `async/TraceCapturingService.java` - an `@Async` method that reads the current trace ID via `Tracer`. `@Async` itself isn't new - what's new is that the executor Boot builds for it now carries the caller's trace context onto this thread by default, given the property below.
- `async/AsyncTraceController.java` - an endpoint returning both the caller's trace ID and the one seen inside the async method.
- `async/TraceCapturingServiceTests.java` - hits the endpoint and asserts the two trace IDs match.
- **Gotchas (the two hardest-won findings in this whole repo):**
  1. Propagation is **opt-in**, not automatic: it only happens once `spring.task.execution.propagate-context=true` is set (see `application.yml`) - Boot only builds its `ContextPropagatingTaskDecorator` bean when that property is `true`.
  2. Only one `ThreadLocalAccessor` is registered by default: Micrometer's `ObservationThreadLocalAccessor`. Manually creating a span with `Tracer.nextSpan().start()` + `Tracer.withSpan(...)` (bypassing the Observation API) populates OTel's context directly and is **never captured or propagated** - it silently produces `null` on the async thread. Real propagation requires an active `Observation`, which is exactly what happens automatically on an incoming HTTP request (Boot's web instrumentation opens one) - hence this sample drives the test through a real endpoint instead of manufacturing a span by hand.

## A structural gotcha worth knowing: `src/test/resources/application.yml` fully replaces `src/main/resources/application.yml`

Test-classpath resources take priority over main ones for a file with the *same name* - it's a full override, not a layered merge. `propagate-context: true` had to be duplicated into the test YAML for sample #6 to keep working under test, even though it's already set for the real app in `src/main/resources/application.yml`.

## Dependency notes (Spring Boot 4.1's modularization, continuing from 4.0)

Beyond the `spring-boot-starter-webmvc-test` / `spring-boot-starter-data-mongodb-test` split already seen in `springboot40`, 4.1 adds:

| Feature | Artifact(s) |
|---|---|
| gRPC server/client | `spring-boot-starter-grpc-server`, `spring-boot-starter-grpc-client` |
| gRPC in-process testing | `spring-boot-starter-grpc-server-test` (brings `spring-boot-grpc-test`: `@AutoConfigureTestGrpcTransport`, `@LocalGrpcServerPort`) |
| `InetAddressFilter` / SSRF | `spring-boot-http-client` (not pulled in by `spring-boot-starter-web`) |
| `RestClient.Builder` auto-config | `spring-boot-starter-restclient` (also not pulled in by `spring-boot-starter-web`) |
| `@GrpcService` / `@GrpcAdvice` | `org.springframework.grpc:spring-grpc-core` (transitive via the gRPC server/client starters) |
| Local Docker Compose auto-start | `spring-boot-docker-compose` (`runtime`, `optional`) - see "Running the app locally" above |
