# Microservices interview guide: this project

This guide uses the code in this repository to help you explain microservices in simple words. Read the [properties and annotations reference](PROPERTIES-AND-ANNOTATIONS.md) beside it. Example answers are starting points: explain what the current code actually does, and distinguish it from improvements you would make.

There are separate copies of the currency system. Sections 1–9 primarily describe `02-currency-exchange`; the `03-docker` copy has additional tracing dependencies and a builder-created RestTemplate. Section 10 explains that copy. Its [project reference](01-in28minutes/03-docker/PROJECT-REFERENCE.md) lists every example URL and component.

## 1. Explain the project in one minute

> This is a learning project with two examples. The first uses Spring Cloud Config to supply profile-specific limits to a client. The second has an exchange service that reads sample rates from H2, a conversion service that calls it and calculates a total, Eureka for service discovery, and an API gateway for routing. It also includes a resilience example. The rates and credentials are examples, and the services need more error handling, testing and security for production.

| Application | Default port | Responsibility |
| --- | ---: | --- |
| Config Server | 8888 | Serves settings from a Git backend |
| Limits | 8080 | Returns configured minimum and maximum |
| Eureka naming server | 8761 | Registry of service instances |
| Currency exchange | 8000 | Looks up a sample currency pair in H2 |
| Currency conversion | 8100 | Calls exchange and calculates an amount |
| API gateway | 8765 | Routes external HTTP requests |

**What is a microservice?** A small independently deployable application responsible for a business capability. It communicates with other services through defined interfaces. Splitting services can allow independent scaling and deployment, but adds network failures, deployment complexity, observability needs and data consistency challenges. A small application is not automatically a well-designed microservice.

## 2. Follow a request from gateway to database

Example: `GET http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10`.

1. The gateway matches `/currency-conversion/**`, rewrites the path to `/currency-conversion-feign/from/USD/to/INR/quantity/10`, and routes to `lb://currency-conversion`.
2. Discovery and load balancing can resolve that service name to a registered conversion instance. The gateway's global filter logs the request path.
3. The conversion controller calls its Feign proxy with USD and INR. The proxy names `currency-exchange`, so a discovered exchange instance can be selected.
4. The exchange controller calls `findByFromAndTo`. Spring Data derives a query from the method name. JPA maps `CurrencyExchange` to the database table and returns the row.
5. Exchange returns the sample rate. Conversion multiplies it by 10 using `BigDecimal` and returns the result. With the supplied USD/INR sample rate 85.50, the calculated amount is 855.00.

This assumes the services started, registered successfully, the pair exists, and no external configuration overrides the defaults. The response's port text helps identify instances; it is diagnostic information rather than a business field.

**What if the pair is missing?** The repository can return null; the controller calls `setPort` on it, causing an error. A better implementation would explicitly return a useful 404 response and test it. **What if exchange is down?** The Feign call can fail; this conversion method has no explicit fallback. Timeouts, suitable retry rules and error responses would need to be designed and tested.

## 3. Central configuration and profiles

The limits application identifies itself as `limits-service`, activates `dev`, and imports configuration from port 8888. Config Server finds matching committed files in its configured Git backend. The `dev` file supplies minimum 1 and maximum 100; the default file supplies 5 and 996. The local application file also supplies 2 and 999. Actual results depend on the sources loaded and their precedence. The configured Git URI currently points to an outdated path; set a valid repository URI and search location before trying this.

**Why use a Config Server?** It gives applications a common place to obtain environment-specific settings and allows configuration to be managed separately from application binaries. It adds a dependency to operate and secure; it does not automatically make secrets safe.

**What is a profile?** A named set of settings for an environment or purpose, such as `dev` or `qa`. The profile selects additional configuration; command-line arguments, environment variables and other higher-priority sources can still change effective values.

**Required versus optional import?** Limits uses `configserver:` without `optional:`, so an unavailable server can stop startup. Exchange and conversion use `optional:configserver:`, so they can start without it, using available local settings. Optional startup can also hide a missing expected setting, so check effective values.

**Does changing a Git file immediately update a running service?** Do not assume so. The Git backend serves committed configuration, and a running client needs an appropriate refresh mechanism or restart to pick up changes; this project does not demonstrate a complete automatic refresh flow.

## 4. Discovery, Feign and the gateway

**Why Eureka?** An instance can register its service name and address; clients can discover instances by name instead of hard-coding every host and port. Registration and updates take time. Discovery does not guarantee every returned instance is healthy or eliminate network failures. The standalone naming server sets `register-with-eureka=false` and `fetch-registry=false`.

**Feign versus RestTemplate here:** Feign declares an interface and the HTTP mapping; the implementation is provided by the framework, and the service name can be resolved through discovery. The other controller method constructs a RestTemplate request to a fixed `localhost:8000`, the default exchange port; changing the exchange port also requires changing that URL. Feign is still a network call: it can time out or fail. A fixed URL in the commented Feign example would bypass name-based selection for that client.

**Why a gateway?** It offers a common entry point for routing and shared concerns. This gateway has explicit Java routes: exchange paths go to exchange; Feign conversion paths go to conversion; the shorter conversion path is rewritten first. The `/get` route sends traffic to an external HTTP endpoint after adding a header and parameter. The discovery-generated route properties are commented out. A gateway can become a bottleneck or failure point, so it also needs monitoring and appropriate scaling.

## 5. Database, JPA and money

H2 is in memory, so its data does not survive process shutdown. JPA maps the `CurrencyExchange` entity and its ID and columns. `data.sql` inserts example rows after schema creation because datasource initialization is deferred. The repository extends `JpaRepository`; Spring Data supplies common operations and derives the pair lookup from its method name.

**Why BigDecimal?** Decimal fractions such as 0.1 are not exactly represented by binary floating-point types. `BigDecimal` supports decimal arithmetic, making it more suitable for money. A real system must still decide currency scale, rounding mode, validation, rate source, effective time and precision; using BigDecimal alone does not solve those decisions.

**Is H2 a production database here?** No: this particular URL creates an in-memory learning database with example credentials and sample rates. A production design needs durable storage, migrations, access control and a trusted rate source.

## 6. Resilience and failure scenarios

| Pattern | Simple idea | Example / concern |
| --- | --- | --- |
| Retry | Try a failed operation again. | Useful for brief transient failures; can amplify load and repeat side effects. Bound attempts, use backoff and consider idempotency. |
| Circuit breaker | Stop calls temporarily after enough failures, then allow trial calls. | Usually moves between closed, open and half-open states. It protects callers from repeatedly waiting for a failing dependency. |
| Rate limiter | Limit calls over time. | Two permits per ten seconds are configured in the example, but its annotation is commented out. |
| Bulkhead | Limit simultaneous work. | The example sets a maximum of ten concurrent calls for the default bulkhead. |
| Timeout | Stop waiting after a defined period. | Essential for remote calls; a circuit breaker is not itself a substitute for a timeout. |

The `/sample-api` method calls `localhost:8080/ex` and has circuit breaker and bulkhead annotations. Its fallback returns a fixed message on an applicable failure. Retry and rate limiter annotations are commented out, so their properties alone do not apply them to that method. Success of annotation-based behavior depends on the necessary integration and runtime configuration; test it rather than inferring it from annotation names. The method returns a fixed success string rather than the downstream body.

**When is retry dangerous?** Retrying a payment after a timeout may create a duplicate if the first request succeeded but its response was lost. Use an idempotency key or other safe design, and retry only suitable errors. Avoid blindly retrying validation failures.

## 7. Annotations interview practice

- **What does `@SpringBootApplication` do?** It combines application configuration, automatic configuration and component scanning. Scanning normally starts from the package of the annotated class and its children.
- **`@RestController` and `@GetMapping`?** The former marks a controller whose return values go into the response body; the latter maps a GET request. `@PathVariable` reads a URL segment into a parameter.
- **What is dependency injection?** Spring supplies an object a class needs. Constructor injection makes required dependencies explicit and easier to replace in tests. This project also uses field injection.
- **`@ConfigurationProperties` versus a hard-coded value?** It binds a group of settings to a typed object. This makes related configuration easier to change by environment; validation is still needed if values must obey rules.
- **`@Entity`, `@Id`, `@GeneratedValue`, `@Column`?** They describe a JPA entity, its key, key generation and column mapping. JPA is a specification; the provider performs the mapping.
- **`@Component`, `@Repository`, `@Configuration`, `@Bean`?** They help Spring discover or create managed objects. Repository conveys data-access intent; configuration classes can define beans with methods.
- **What does Lombok do?** It generates Java code such as accessors and constructors at build time. Check generated behavior, especially equality and logging of sensitive fields; `@Data` does more than just getters.
- **What do the test annotations prove?** `@Test` marks a JUnit test. `@SpringBootTest` loads a context; the existing empty context tests do not prove API responses, service discovery or resilience behavior.

See the reference for every annotation and property used, including commented examples.

## 8. Production and testing discussion

A sensible next step is to test controller responses for known and unknown pairs, calculation and rounding, upstream errors, gateway rewrites, and resilience under failures. Integration tests can exercise database initialization and HTTP contracts. Add appropriate timeouts, validated inputs, structured errors, logs and metrics, tracing, authentication and authorization, secure configuration and durable storage. Decide how deployments, health checks, rate updates and schema changes work. These are suggested improvements, not features already supplied by this repository.

**How would you debug a failed conversion?** Check the gateway route and response, Eureka registration, conversion and exchange health and logs, the requested pair, the database data, and network/timeout errors. Compare direct service calls with the gateway call. Check which configuration and profile actually loaded. The direct RestTemplate URL must match the configured exchange port.

## 9. Quick self-test

1. Trace a conversion request from gateway to H2 and back. Which service performs the multiplication?
2. Why can a required Config Server import stop startup even though local defaults exist?
3. What changes if you activate `qa` instead of `dev`? What could still override it?
4. How does a service name become a network address, and what happens when discovery information is stale?
5. Compare a circuit breaker with a retry, a bulkhead and a rate limiter.
6. What happens to the direct RestTemplate example if the exchange port changes?
7. What response does the current code produce for an unknown currency pair, and what would you improve?
8. Why does `BigDecimal` help, and which rounding choices are still missing?
9. Which configured resilience annotations are currently commented out?
10. What does an empty context startup test leave untested?

Continue with the [Docker, Compose, and Kubernetes interview guide](CONTAINER-INTERVIEW-GUIDE.md) for packaging, rollouts, probes, and troubleshooting.

## 10. Distributed tracing in the `03-docker` example

### Explain it in an interview

> The gateway receives a conversion request and routes it to a Eureka-discovered conversion instance. Conversion uses Feign to call exchange, which reads an H2 sample rate. The applications use Micrometer observation and tracing with an OpenTelemetry bridge. Instrumented HTTP clients propagate trace context to the next service, and sampled spans are exported to Zipkin. That lets us inspect the request across services and correlate logs with the trace ID.

This describes the intended configured flow, not proof that every running instance exports traces. Start the applications and Zipkin, make a request and verify the actual trace. Eureka is for discovery; it does not carry or collect tracing data. The gateway logging filter logs paths but is not the tracing exporter.

### Follow a concrete request

`GET http://localhost:8765/currency-conversion/from/CAD/to/INR/quantity/12` matches the gateway's `/currency-conversion/**` route. The gateway rewrites it to `/currency-conversion-feign/from/CAD/to/INR/quantity/12` and selects a `currency-conversion` instance through discovery. Feign calls a discovered `currency-exchange` instance, which looks up the sample rate 62.40. Conversion multiplies by 12 and returns 748.80. Both the gateway and Feign calls can propagate the tracing context. See the [URL catalog](01-in28minutes/03-docker/PROJECT-REFERENCE.md#3-url-catalog) for the direct service and operations URLs.

### Interview questions and answers

**What are logs, metrics and traces?** Logs describe events. Metrics aggregate measurements such as request counts and durations. Traces describe the path and timing of an individual request across operations and services. A trace ID in logs helps correlate events with a trace.

**What is a trace ID compared with a span ID?** A trace ID identifies the distributed request. A span identifies an operation within that trace; parent relationships connect operations. Different services can share a trace ID while recording different spans.

**What does Micrometer Observation do?** It provides an API for recording operations. Observation handlers can produce metrics and tracing data. It is not a storage system, and an observation does not automatically guarantee a trace is exported.

**What does Micrometer Tracing do?** It offers a tracing abstraction and integrates observations with a tracer. Here `micrometer-tracing-bridge-otel` connects that abstraction to OpenTelemetry. The OpenTelemetry Zipkin exporter sends completed sampled spans to Zipkin, which stores and displays them.

**How does one trace cross services?** An instrumented outgoing client injects context into HTTP headers. The receiving service extracts it and creates related server-side work. The exporter sends spans to Zipkin separately. Compatible propagation settings and instrumentation are needed at each hop.

**What does `feign-micrometer` provide?** It supplies Feign's observation capability when the required registry and integration are present. It does not by itself provide a Zipkin server or guarantee export. The POM and active configuration of each service matter.

**Why use `RestTemplateBuilder`?** The `03-docker` conversion service builds its `RestTemplate` bean from Spring Boot's injected builder. Boot can apply observation customizers, enabling HTTP observations and trace propagation when tracing is configured. A `new RestTemplate()` bypasses those builder customizations unless configured separately. The `02-currency-exchange` copy and the `03-docker` `/sample-api` demonstration still construct a RestTemplate directly.

**Does the `03-docker` REST conversion endpoint use Eureka?** No. It calls `localhost:8000` directly. The Feign endpoint uses the named service and discovery. The REST endpoint is available directly on port 8100; the gateway has no `/currency-conversion-rest/**` route.

**What does sampling do?** It controls which eligible traces are recorded/exported. The three traced applications use `management.tracing.sampling.probability=1.0` for this demo. Sampling lowers overhead when reduced, but it also means some requests may not be visible in Zipkin. Configuration, exporter connectivity and client propagation still matter.

**Why might Zipkin show a service but not the gateway?** The request may have gone directly to that service, the gateway may be running an older build without tracing, or it may be unable to export or sample its spans. Check the actual request path, gateway dependencies and logs, Zipkin connectivity, sampling and export delay. A successful response alone is insufficient.

**Why might the gateway return `UnknownHostException`?** Eureka may advertise an instance hostname the gateway cannot resolve. Compare the actual registered address with what the gateway can reach. Exchange and conversion default to `localhost` for same-host execution; override their advertised hostnames for separate containers. Allow registry caches time to refresh after changing them.

**What is `proxyBeanMethods=false`?** It disables interception of direct calls between `@Bean` methods in that configuration. Those calls then create objects like ordinary Java calls; use method-parameter injection for dependencies between beans. It does not turn off tracing.

**What would you improve for production?** Design sampling and trace retention, protect sensitive data in telemetry, add dashboards and alerts, configure timeouts and robust errors, and test trace propagation across real deployment boundaries. These are proposals; the current demo is not evidence that those capabilities are already implemented.

### Practice without looking at the answers

1. Explain how a request changes path at the gateway and where multiplication occurs.
2. What is the difference between context propagation and exporting spans?
3. Why does a call directly to port 8100 not generate a gateway span?
4. What changes when the RestTemplate is constructed with `new` instead of the injected builder?
5. What are the separate jobs of Micrometer Observation, the OpenTelemetry bridge and Zipkin?
6. How would you investigate an UP Eureka registration with a gateway DNS error?
7. Why does `localhost` require different configuration inside separate containers?
8. Which resilience annotations are active on `/sample-api`, and do they protect the Feign conversion method?
