# Project reference: services, URLs, and interview notes

This reference describes the current `03-docker` source and its defaults. Configuration, running versions, external Config Server values, or environment variables can change the actual behavior. The examples assume all Java applications run on the same machine. They are learning examples, not production-ready services.

## 1. Architecture and responsibilities

| Component | Application name | Port | Responsibility |
| --- | --- | --- | --- |
| Naming server | `naming-server` | 8761 | Eureka registry: tracks service instances and their advertised addresses. |
| Exchange | `currency-exchange` | 8000 | Looks up a sample currency rate in an H2 database. Also hosts a resilience demonstration. |
| Conversion | `currency-conversion` | 8100 | Gets a rate through Feign or RestTemplate and multiplies it by a quantity. |
| Gateway | `api-gateway` | 8765 | Routes selected paths, rewrites one conversion path and uses discovery for destinations. |
| Zipkin | External application | 9411 | Receives and displays exported tracing spans. It is not a module in this directory. |
| Config Server | External, optional | 8888 | Can provide external properties to exchange and conversion. A server is not supplied in this directory. |

```text
                 register / discover
              +------------------------ Eureka :8761
              |                        ^
Client -> Gateway :8765 -> Conversion :8100 -> Exchange :8000 -> H2
              |                 |                   |
              +-----------------+-------------------+--> Zipkin :9411
                                  sampled spans
```

Eureka discovery answers where a service instance is; it is not a reverse proxy. The gateway forwards the HTTP request. Micrometer and OpenTelemetry trace operations, while Zipkin receives the exported spans. The database is local to exchange and stores example rates.

## 2. Start and verify

Requirements are JDK 25, Maven dependencies accessible via the Maven wrapper, and Docker if using it to run Zipkin. Open separate terminals in each application folder and run:

```powershell
.\mvnw.cmd spring-boot:run
```

Start the naming server, then exchange, conversion and gateway. Allow time for registrations and discovery caches to update. On Unix-like systems use `./mvnw spring-boot:run`. Zipkin can be started separately:

```powershell
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin
```

Reuse an existing Zipkin instance if one is already running. The image may need downloading. In the same-host setup, check the Eureka dashboard, then request the gateway URL below, and search for `api-gateway` in Zipkin. Service startup or HTTP success alone does not prove trace export works. Tests are run from each application directory with `.\mvnw.cmd test`; current context tests are not full integration tests.

## 3. URL catalog

All API examples below use HTTP GET and the default ports. `curl.exe` avoids the PowerShell `curl` alias. Replace `{from}` and `{to}` with a supported currency code and `{qty}` with a whole number accepted by the Java `long` parameter. Endpoints may return errors if a dependency is down, discovery is stale, or a pair is absent.

### Business endpoints

| URL | What it tests |
| --- | --- |
| `http://localhost:8000/currency-exchange/from/CAD/to/INR` | Exchange directly: returns the configured rate and exchange port. |
| `http://localhost:8100/currency-conversion-feign/from/CAD/to/INR/quantity/12` | Conversion directly, then Feign and Eureka to exchange. Gateway does not participate. |
| `http://localhost:8100/currency-conversion-rest/from/CAD/to/INR/quantity/12` | Conversion directly, then builder-created RestTemplate to hard-coded `localhost:8000`. No discovery for that call. |
| `http://localhost:8765/currency-exchange/from/CAD/to/INR` | Gateway forwards the unchanged path to an exchange instance discovered by name. |
| `http://localhost:8765/currency-conversion-feign/from/CAD/to/INR/quantity/12` | Gateway forwards the Feign path unchanged to conversion. |
| `http://localhost:8765/currency-conversion/from/CAD/to/INR/quantity/12` | Gateway rewrites the path to the Feign endpoint and forwards it to conversion. |
| `http://localhost:8000/sample-api` | Resilience4j demonstration; attempts an external dependency at `localhost:8080/ex` and may return fallback text. |
| `http://localhost:8765/get` | Gateway adds `MyHeader: MyURI` and query parameter `Param=MyParam`, then forwards to `http://httpbin.org:80/get`. Requires network access and the external service to be available. |

For example:

```powershell
curl.exe http://localhost:8765/currency-conversion/from/CAD/to/INR/quantity/12
```

The seeded rate is 62.40 and the calculated amount is 748.80. The response includes `id`, `from`, `to`, `conversionMultiple`, `quantity`, `totalCalculatedAmount` and `port`. The `port` field is a demo diagnostic, not a stable API contract. The direct exchange response does not contain conversion quantity or total. A missing exchange pair currently causes an exception; there is no custom not-found response or explicit error handling for failed downstream calls.

**Why does `/currency-conversion-rest/...` on port 8765 not work?** There is no gateway route for that prefix. The `/currency-conversion/**` predicate matches a slash after `conversion`, not `-rest`. Use the direct port 8100 REST URL or explicitly add a gateway route if desired.

### Operational and internal URLs

| URL | Purpose and limitations |
| --- | --- |
| `http://localhost:8761/` | Eureka dashboard: inspect registered application names, instance status and advertised host/port. An UP registration does not prove reachability. |
| `http://localhost:8761/eureka/apps` | Eureka registry endpoint used by clients. Browser representation can depend on request headers. |
| `http://localhost:9411/` | Zipkin UI: search service names, inspect trace IDs, spans, durations and errors. |
| `http://localhost:9411/health` | Zipkin health endpoint when supported by the running Zipkin distribution. |
| `http://localhost:8000/actuator/health`, `http://localhost:8100/actuator/health`, `http://localhost:8765/actuator/health`, `http://localhost:8761/actuator/health` | Application health endpoints, subject to Actuator availability, configuration and endpoint exposure. Health does not establish end-to-end correctness. |
| `http://localhost:8000/h2-console` | Local H2 console if enabled and accessible; use JDBC URL `jdbc:h2:mem:testdb`, user `admin`, password `admin`. The database is in memory and resets when the process stops. Local demo credentials and console are not suitable for production. |
| `http://localhost:8888` | Optional Config Server location used by exchange and conversion. The apps can start without it because their import is `optional:`. |
| `http://localhost:8080/ex` | Hard-coded dependency of `/sample-api`; this project does not provide that endpoint. Its failure exercises the fallback. |
| `http://httpbin.org:80/get` | External target of the gateway `/get` demonstration; not hosted by this project. |

The applications use `http://localhost:8761/eureka` for Eureka discovery. Zipkin's configured/default exporter endpoint must be reachable by each traced application; the Zipkin UI URL and the collector endpoint serve different purposes. Actuator endpoints other than health may need explicit web exposure; do not assume `/actuator/metrics` is publicly available by default.

## 4. Supported sample rates

The exchange service initializes H2 from `data.sql`. Rates are demo values, not market data. Lookups are directional: `USD -> INR` does not imply `INR -> USD` is calculated automatically.

| From | To | Rate | From | To | Rate |
| --- | --- | ---: | --- | --- | ---: |
| USD | INR | 85.50 | USD | EUR | 0.92 |
| EUR | INR | 92.75 | USD | GBP | 0.79 |
| GBP | INR | 108.25 | INR | USD | 0.0117 |
| AUD | INR | 55.80 | INR | EUR | 0.0108 |
| CAD | INR | 62.40 | EUR | USD | 1.09 |

The `CurrencyExchange` JPA entity maps to `currency_exchange`; `CurrencyExchangeRepository` derives its query from `findByFromAndTo`. The conversion DTO uses `BigDecimal` for the rate and total, avoiding binary floating-point arithmetic for this multiplication. Quantity is a `long`; this demo does not define input validation, rounding policy or live-rate refresh.

## 5. Gateway, discovery and HTTP clients

`ApiGatewayConfiguration` registers routes in Java. Discovery-generated routes are commented out. The `lb://currency-exchange` and `lb://currency-conversion` URIs ask Spring Cloud's load-balancing/discovery integration to select a registered instance. The gateway filter logs paths; it is separate from the tracing instrumentation. The `/get` route modifies a request, while the conversion route rewrites its path before forwarding.

`@EnableEurekaServer` creates the registry. The standalone naming server has `register-with-eureka=false` and `fetch-registry=false`; it is not acting as its own client. The other services register with Eureka and use their `spring.application.name` values. Registration and client registry caches update asynchronously, so startup and address changes can take time to appear.

`@EnableFeignClients` scans Feign clients. `@FeignClient(name = "currency-exchange")` creates a declarative client proxy; its method mapping defines the downstream endpoint. Feign uses discovery for the service name. Its Micrometer capability observes the call when the required registry and integration are active.

The alternative RestTemplate endpoint uses the bean in `RestTemplateConfiguration`, built by Spring's injected `RestTemplateBuilder`. This lets Spring apply HTTP observation customizers and propagate trace context when configured. The URL is hard-coded, so it neither discovers nor balances exchange instances. In containers `localhost` points to the conversion container, not the exchange container. The `new RestTemplate()` in the separate `/sample-api` demonstration does not use the auto-configured builder and therefore bypasses its observation customization.

`@Configuration(proxyBeanMethods = false)` avoids intercepting direct calls between bean methods in that class. A direct call to another `@Bean` method would be an ordinary Java call; inject dependencies as method parameters when using this mode.

## 6. Observability and tracing

For the detailed trace lifecycle, sampling, troubleshooting and interview questions see the [observability guide](README.md#observability-concepts). The key chain is:

```text
HTTP instrumentation -> Micrometer Observation -> metrics handlers
                                             -> Micrometer Tracing -> OpenTelemetry
                                                                  -> Zipkin exporter -> Zipkin UI
```

An observation can feed different handlers; it is not itself a Zipkin span. A shared trace ID connects the request, while span IDs and parent IDs describe individual operations. HTTP headers propagate context to the next application; an exporter separately sends sampled completed spans to Zipkin. Searching Zipkin for a service only shows traces involving spans that service successfully exported. Direct calls skip the gateway, so its span should not be expected in those traces.

All three business applications set `management.tracing.sampling.probability=1.0` for local experiments and a logging pattern containing trace and span IDs. Sampling does not configure the tracer, exporter, or client instrumentation by itself. Logs, metrics and traces complement each other. Propagation depends on client instrumentation and compatible propagation settings. Asynchronous export means a completed HTTP response can precede trace visibility.

## 7. Resilience4j demonstration

`GET /sample-api` has `@CircuitBreaker(name = "default", fallbackMethod = "errorResponse")` and `@Bulkhead(name = "default")`. It calls `localhost:8080/ex` using a separately constructed RestTemplate and returns `Sample API` if the call completes. Failures can invoke `errorResponse`, which returns `Error Response for failure`. The exact outcome depends on configured aspects, downstream behavior and circuit state.

- **Circuit breaker:** tracks calls and can stop sending calls after a failure threshold. It transitions through closed, open and half-open states according to configuration; a fallback can supply an alternative response.
- **Bulkhead:** limits concurrent calls to isolate resources. The configured default permits up to 10 concurrent calls.
- **Retry:** repeats a failed operation, potentially increasing load. The sample retry annotation is commented out, so the `sample-api` retry properties alone do not activate it.
- **Rate limiter:** restricts calls per time period. The annotation is commented out, so the configured default limit alone does not apply to this endpoint.

These are distinct controls: concurrency is not requests per time period, and a fallback does not fix the downstream dependency. Inspect the actual annotations and names before claiming a setting is active.

## 8. Maven and component glossary

A `pom.xml` is a Maven project descriptor. A dependency declares a library; starters group related dependencies and auto-configuration. Transitive dependencies are included by other dependencies. Use `mvn dependency:tree` in a service folder to inspect the resolved tree; Maven needs access to its artifacts. Parent/BOM dependency management supplies many versions, but does not necessarily add every managed dependency. Here the POMs declare Spring Boot parent 4.1.2, Spring Cloud BOM 2025.1.4 and Java 25; exchange explicitly declares Resilience4j 2.4.0. Verify compatibility and resolved versions in your actual build.

| Dependency or tool | Role in this project |
| --- | --- |
| `spring-boot-starter-webmvc` | Servlet-based web application and REST controllers in the services and naming server. |
| `spring-cloud-starter-gateway-server-webflux` | Reactive Spring Cloud Gateway routing and filters. |
| Eureka server/client starters | Registry server and application registration/discovery clients. |
| `spring-cloud-starter-openfeign` | Declarative HTTP client integration. |
| `feign-micrometer` | Feign observation capability; requires the surrounding observation configuration. |
| `spring-boot-starter-restclient` | Blocking HTTP client support and Boot's `RestTemplateBuilder` in conversion. The naming server also declares the lower-level `spring-boot-restclient` module. |
| `spring-cloud-starter-config` | Config Client for optional external settings in exchange and conversion. |
| `spring-boot-starter-data-jpa`, H2 and H2 console | JPA data access, in-memory database and local database console in exchange. |
| Resilience4j and `spring-boot-starter-aspectj` | Resilience annotations and the aspect infrastructure used by the demonstration. |
| Actuator and the Micrometer/OpenTelemetry/Zipkin dependencies | Management and observability, tracing bridge, exporter and Boot integration; see section 6. |
| Lombok | Generates boilerplate such as accessors and constructors at compile time for the conversion DTO. |
| DevTools | Optional runtime development convenience, not a distributed tracing component. |
| Test starters and `reactor-test` | Test dependencies; they do not prove the running system exports a complete trace. |
| Spring Boot Maven plugin and compiler plugin | Build/run packaging and compiler/annotation processor configuration, respectively. |

## 9. Configuration and Docker networking

`server.port` sets the default listening port, `spring.application.name` supplies the discovery and observability service name, and the Eureka `defaultZone` gives the registry location. Exchange and conversion set `eureka.instance.hostname=${EUREKA_INSTANCE_HOSTNAME:localhost}` for same-host execution. Override the advertised hostname with a name or IP actually reachable by clients when deploying elsewhere. The gateway can also register an unsuitable hostname; inspect its registration as appropriate. Eureka's `UP` status alone does not test another container's DNS.

The exchange and conversion Config Server imports are optional; external configuration can override local values depending on property precedence. H2 initialization is deferred until after JPA creates the schema. `spring.jpa.show-sql=true` logs SQL for learning. Local credentials and full trace sampling are demo choices.

If applications run in separate containers, configure reachable Eureka and Zipkin endpoints and instance hostnames on a shared network. Container `localhost` refers to itself. Published host ports and Docker DNS names solve different connection paths; choose addresses reachable from each actual caller. The direct RestTemplate URL and the `/sample-api` hard-coded URL also need adjustment for a container deployment. This folder does not currently define a full Compose deployment, and these documents do not claim one was tested.

## 10. Common interview comparisons

| Question | Answer |
| --- | --- |
| Gateway vs Eureka? | Gateway forwards requests; Eureka maintains a registry of instance addresses. |
| Feign vs RestTemplate here? | Feign declares a client interface and discovers exchange; RestTemplate explicitly calls a hard-coded URL. Both can be instrumented. |
| Observation vs tracing vs Zipkin? | Observation records operations for handlers; tracing produces correlated spans; Zipkin collects and displays exported spans. |
| Trace ID vs span ID? | Trace ID identifies the distributed request; span ID identifies one operation in it. |
| Propagation vs export? | Propagation carries context with a request; export sends recorded spans to a backend. |
| Circuit breaker vs retry? | A breaker can stop calls to a failing dependency; retry attempts another call. Both require careful configuration. |
| Bulkhead vs rate limiter? | A bulkhead limits simultaneous work; a rate limiter limits calls over time. |
| Health vs readiness of the whole flow? | A health response is a component check; it does not prove discovery, downstream calls or trace export work end to end. |
