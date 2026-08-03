# springboot40

Five distinct, runnable samples of the headline Spring Boot **4.0** features, built on Spring Boot 4.0.7 / Spring Framework 7. Each sample is a small piece of production code plus a test that actually exercises it.

Requires **Java 25** (see `java.version` in `pom.xml`).

Run everything with:

```bash
mvn test
```

(Sample #3 needs Docker running - it launches a real MongoDB Atlas Local container via Testcontainers.)

## Running the app locally, with observability

```bash
mvn spring-boot:run
```

`docker-compose.yml` defines the app's backing services - MongoDB and an all-in-one `grafana/otel-lgtm` container (OTel Collector + Grafana + Tempo + Prometheus/Mimir + Loki). Because `spring-boot-docker-compose` is on the classpath, Spring Boot auto-starts and stops these containers whenever the app runs locally this way - no manual `docker compose up` needed, and no manual connection properties either (Spring Boot's service-connection support wires Mongo automatically by image name; the OTLP exporters just happen to already default to the same ports the collector listens on).

Once it's up, hit any endpoint and see what actually got recorded at **http://localhost:3000** (login `admin`/`admin`):

- **Explore → Tempo**, search `service.name = springboot40`, for request traces. Tracing is *sampled*, not 100% by default, so a single request might not show up - either call the endpoint several times, or restart with `--management.tracing.sampling.probability=1.0` to force every request to be traced (that's what `TraceIdHeaderFilterTests` does).
- **Explore → Prometheus**, query e.g. `orders_placed_total` or `http_server_requests_seconds_count{uri="/api/greetings/{name}"}`, for metrics.

`docker-compose.full.yml` is a separate, fully containerized variant (app included, built from `Dockerfile`) for anyone who wants to run the whole stack in Docker without a local JDK/Maven - see its comments for the extra environment variables it needs (Spring Boot 4.0 moved the Mongo property prefix to `spring.mongodb.*`, and the default OTLP endpoints resolve to `localhost`, which inside a container is the app itself, not the collector).

## Sample #1: RestTestClient

New fluent HTTP test client replacing the old MockMvc-only API. Works both against a mocked servlet container and a real running server.

- `web/GreetingController.java` - a trivial REST endpoint, no 4.0-specific code.
- `web/GreetingControllerMockMvcTests.java` - `@WebMvcTest` + `@AutoConfigureRestTestClient`: RestTestClient dispatches through MockMvc.
- `web/GreetingControllerLiveServerTests.java` - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureRestTestClient`: the same fluent API against a real socket.

## Sample #2: `@MeterTag` with a SpEL `ValueExpressionResolver`

Micrometer's `@Counted`/`@Timed` method aspects can now attach dynamic tags via `@MeterTag`, resolved through a `ValueExpressionResolver` bean.

- `metrics/OrderService.java` - `placeOrder(...)` is `@Counted` and `@Timed`, with one plain `@MeterTag` and one that runs a SpEL expression (`toUpperCase()`) against the parameter.
- `metrics/MetricsConfig.java` - the `ValueExpressionResolver` bean. **Note:** Micrometer ships the interface but no SpEL implementation, so Boot's auto-configuration only builds the `@MeterTag` handler beans once you supply one yourself (this is easy to miss - without it, `@MeterTag` silently does nothing).
- `metrics/OrderController.java` - `GET /api/orders/{region}/{tier}` calls `placeOrder(...)` over real HTTP, so the meters actually get recorded during a normal request (and pushed to Grafana - see above) instead of only ever running inside a test.
- `metrics/OrderServiceMetricsTests.java` - asserts the counter/timer are recorded with the expected tag values, calling `OrderService` directly against an isolated `SimpleMeterRegistry`.
- `metrics/OrderControllerTests.java` - checks the HTTP wiring/response shape only; the meter assertions live in the test above.
- Requires `management.observations.annotations.enabled=true` (see `application.yml`) and `aspectjweaver` on the classpath.
- **Gotcha:** the resolver only ever receives the *single annotated parameter's* value as the SpEL root - not the other method arguments - so expressions are root-relative (`toUpperCase()`), not `#otherParamName` references.

## Sample #3: Testcontainers 2.0 + `@ServiceConnection` for MongoDB

Spring Boot 4.0 upgrades to Testcontainers 2.0 (modules renamed with a `testcontainers-` prefix, container classes relocated per-module) and adds `@ServiceConnection` support for `MongoDBAtlasLocalContainer`.

- `mongo/Order.java`, `mongo/OrderRepository.java` - a plain `MongoRepository`.
- `mongo/OrderRepositoryMongoTests.java` - `@DataMongoTest` + a `@TestConfiguration`-nested `MongoDBAtlasLocalContainer` bean annotated `@ServiceConnection`: no manual connection properties needed.
- **Gotcha:** the nested container config must be `@TestConfiguration`, not plain `@Configuration` - a plain `@Configuration` class nested in the test replaces the auto-detected `@SpringBootConfiguration` instead of augmenting it, which breaks `AutoConfigurationPackages` and fails context startup.

## Sample #4: `spring-boot-starter-opentelemetry`

One starter dependency wires up the OpenTelemetry SDK, OTLP exporters for metrics/traces, and a Micrometer `Tracer` bean - no collector required for the wiring itself to work (though `docker-compose.yml` happens to run one now - see "Running the app locally" above - so you can actually see what it produces instead of just trusting the wiring).

- `otel/TraceIdHeaderFilter.java` - injects the current trace ID into an `X-Trace-Id` response header, proving the `Tracer` bean is live.
- `otel/TraceIdHeaderFilterTests.java` - hits a real server and asserts a 32-character W3C trace ID comes back (sampling forced to 100% just for the test).
- **Gotcha:** `TraceIdHeaderFilter` is a `Filter` `@Component`, so `@WebMvcTest` auto-detects it - but that slice doesn't load tracing auto-configuration, so `GreetingControllerMockMvcTests` and `ApiVersioningTests` (sample #5) both explicitly exclude it via `excludeFilters`.

## Sample #5: API versioning

New in Spring Framework 7 (carried by `spring-boot-starter-web`): `@GetMapping(version = ...)`/`@RequestMapping(version = ...)` lets several handler methods share one path, selected by request version instead of just by HTTP method.

- `versioning/ApiVersioningConfig.java` - a `WebMvcConfigurer` configuring header-based version resolution (`X-API-Version`), supported versions `1`/`2`, a default of `1`, and a deprecation/sunset policy for version `1`.
- `versioning/ProductController.java` - `GET /api/products/{id}` with two handlers distinguished purely by `version`: v1 (no price) and v2 (adds price).
- `versioning/SunsetEnforcingDeprecationHandler.java` - Spring's built-in `StandardApiVersionDeprecationHandler` only ever sends `Deprecation`/`Sunset`/`Link` headers; decompiling `AbstractHandlerMapping$ApiVersionDeprecationHandlerInterceptor#preHandle` confirms it *always* lets the request through regardless of what the handler does. This wraps it to additionally reject requests with 401 once "now" is past the configured sunset date.
- `versioning/ApiVersioningTests.java` - exercises version resolution/defaulting/rejection against whatever sunset date is currently configured.
- `versioning/SunsetEnforcingDeprecationHandlerTests.java` - unit-tests the handler directly with relative dates (`now().plusDays(...)`/`.minusDays(...)`), so it's correct regardless of what absolute date happens to be configured in the sample.
- **Gotchas:**
  - `configureApiVersioning` wires version resolution into the app's *entire* default `RequestMappingHandlerMapping` - not just controllers that declare a `version` attribute. With a default version configured, every request (even `GreetingController`'s, which never opted in) resolves a version and reaches the deprecation handler. `SunsetEnforcingDeprecationHandler` only acts when the target controller is actually `ProductController` - found by testing that sending an `X-API-Version: 1` header to an unrelated endpoint wrongly 401'd it before that check was added.
  - `ApiVersioningTests`'s deprecated-version-1 assertions are pinned to whatever sunset date is currently configured in `ApiVersioningConfig` - flip that date and the expected status flips between 200 (with warning headers) and 401. `SunsetEnforcingDeprecationHandlerTests` is the date-independent proof the mechanism itself actually works.

## Dependency notes (Spring Boot 4.0's modularization)

Spring Boot 4.0 split the old monolithic `spring-boot-test-autoconfigure` jar into per-feature modules with matching package names. If you're used to older Spring Boot versions, the annotations moved:

| Annotation | Old package | New package (4.0) |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@AutoConfigureRestTestClient` | *(new in 4.0)* | `org.springframework.boot.resttestclient.autoconfigure` |
| `@DataMongoTest` | `org.springframework.boot.test.autoconfigure.data.mongo` | `org.springframework.boot.data.mongodb.test.autoconfigure` |

...and the corresponding Maven artifacts to depend on are `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-mongodb-test`, not just `spring-boot-starter-test`.

Also worth knowing: the Mongo autoconfiguration property prefix moved too, from `spring.data.mongodb.*` (still the case in Boot 3.x) to `spring.mongodb.*` - only matters if you're overriding the connection URI by hand instead of relying on `@ServiceConnection`/docker-compose auto-wiring.
