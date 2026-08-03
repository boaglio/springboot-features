# AGENTS.md

Guidance for coding agents working in this repository.

## Repository shape

Two **independent** Maven projects, no shared parent POM, no shared code:

- `springboot40/` — Spring Boot 4.0.7 / Spring Framework 7 feature samples
- `springboot41/` — Spring Boot 4.1.0 / Spring Framework 7 feature samples

Each is self-contained: its own `pom.xml`, `Dockerfile`, `docker-compose.yml` (+ `docker-compose.full.yml`), and `README.md`. Always `cd` into the specific project directory before running Maven — there is no root-level build.

Both require **Java 25** (`java.version` in `pom.xml`); Dockerfiles use `maven:3.9-eclipse-temurin-25` / `eclipse-temurin:25-jre-jammy` to match. If you bump `java.version`, update both Dockerfile stages too, or the image build fails with "release version N not supported".

Read each project's `README.md` before making changes — it documents every sample, including hard-won gotchas (scoping bugs, property-prefix renames, config quirks) that are easy to silently reintroduce.

## Purpose of this repo

Educational: each sample demonstrates one specific new API/feature introduced in that Spring Boot version. Production code is intentionally minimal; the point is the feature being exercised, not a realistic app. Keep that spirit when adding to it — favor a small, clear demonstration over a complete feature.

## Commands

Run from inside `springboot40/` or `springboot41/`:

```bash
mvn test                # run all tests for that project
mvn spring-boot:run      # run the app locally; auto-starts/stops docker-compose.yml
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083   # alt port
docker compose -f docker-compose.full.yml up --build   # fully containerized (app + infra), no local JDK needed
```

`mvn test` does **not** trigger Docker Compose auto-start (`spring.docker.compose.skip.in-tests` defaults to `true`) — tests that need real infra use Testcontainers directly instead (Mongo in springboot40 sample #3, Redis in springboot41 sample #5).

### Port collisions to watch for

- Both projects' `@SpringBootTest`s that touch gRPC bind port **9090**; springboot41 also runs a live gRPC server there via `spring-boot:run`. If the user has a live instance running locally, don't run `mvn test`/`mvn spring-boot:run` for that project without checking first — it will collide. Prefer curling/grpcurl-ing their already-running instance over starting a second one.
- Grafana/OTel stacks are port-offset between projects specifically so both can run side by side: springboot40 uses 3000/4317/4318, springboot41 uses 3001/4417/4418.
- Never kill a process you didn't start yourself without asking first. If you do stop one you started, prefer a graceful `kill` (SIGTERM) so Spring Boot's shutdown hook can tear down its Docker Compose containers; if you must `kill -9`, follow up with `docker compose down -v` manually since the shutdown hook gets skipped.

## Observability stack

Each project's `docker-compose.yml` runs the app's real backing service (MongoDB for springboot40, Redis for springboot41) plus an all-in-one `grafana/otel-lgtm` container (OTel Collector + Grafana + Tempo + Prometheus/Mimir + Loki), auto-started/stopped by `spring-boot-docker-compose` (`runtime`, `optional` dependency) whenever `mvn spring-boot:run` runs. No manual connection properties are needed — Boot's service-connection support wires the datastore by container image name, and OTLP exporters default to the ports the collector listens on. Grafana login is `admin`/`admin`.

## Conventions used throughout the code

- **Every new API/method introduced by the Spring Boot version being demonstrated gets a Javadoc or inline comment explaining what it does and why it's interesting** — not what generic code does, but what's *new* about it. Preserve this when editing; it's the point of the repo.
- **Every HTTP call inside a test has an httpie-equivalent comment directly above it**, e.g. `// http :8080/api/products/1 X-API-Version:2`, so a reader can replay the same call from a terminal. Keep this in sync when changing a test's request.
- Gotchas discovered while building a sample (scoping bugs, property renames, silent no-ops) are written up as a `**Gotcha:**` bullet in the relevant README section, not just fixed silently — the mistake itself is often as instructive as the fix.
- Claims about framework behavior in comments/READMEs (e.g. "the deprecation handler never blocks") were verified by decompiling the actual framework class, not from memory — keep that bar when adding new claims.

## Project-specific pitfalls (see each README for full detail)

**springboot40:**
- Mongo property prefix is `spring.mongodb.*` in 4.0, not `spring.data.mongodb.*` (still true in Boot 3.x) — only matters if overriding the connection URI by hand instead of relying on `@ServiceConnection`/compose auto-wiring.
- `configureApiVersioning` (sample #5) wires version resolution into the app's *entire default* `RequestMappingHandlerMapping`, not just controllers that opt in with a `version` attribute — `SunsetEnforcingDeprecationHandler` has to explicitly check `handlerMethod.getBeanType() == ProductController.class` to avoid 401-ing unrelated endpoints.
- Test-autoconfigure jar is modularized: `@WebMvcTest`/`@AutoConfigureRestTestClient` need `spring-boot-starter-webmvc-test`; `@DataMongoTest` needs `spring-boot-starter-data-mongodb-test` — not covered by `spring-boot-starter-test` alone.

**springboot41:**
- `spring-boot-http-client` (`InetAddressFilter`/`FilteredHostException`) and `spring-boot-starter-restclient` (`RestClient.Builder` auto-config) are **not** pulled in transitively by `spring-boot-starter-web` — both are declared explicitly for the SSRF sample.
- Hand-built gRPC `MethodDescriptor`s (no `.proto`/protoc toolchain) mean the service doesn't show up in `grpcurl list` (no `FileDescriptorProto` for reflection) even though calls work fine — see `GreeterServiceTests` Javadoc for the stand-in-`.proto` workaround.
- `@GrpcAdvice` class **and** its `@GrpcExceptionHandler` method must both be `public` — package-private silently falls back to a generic `INTERNAL` status instead of invoking the handler.
- `src/test/resources/application.yml` fully replaces (not merges with) `src/main/resources/application.yml` on the test classpath — properties needed under test (e.g. `propagate-context: true` for the `@Async` sample) must be duplicated there.
- Micrometer context propagation to `@Async` methods requires an active `Observation` (e.g. from an incoming HTTP request); manually starting a span via `Tracer.nextSpan()/withSpan()` bypasses the only registered `ThreadLocalAccessor` and never propagates.
