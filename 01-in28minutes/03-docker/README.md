# Docker and distributed tracing tutorial

This example has four Spring Boot applications: Eureka (8761), currency exchange (8000), currency conversion (8100), and an API gateway (8765). Zipkin normally listens on 9411. The default `localhost` settings assume the applications run on the same machine. For Docker concepts see the [Docker and Compose walkthrough](../../CONTAINER-INTERVIEW-GUIDE.md#1-docker-fundamentals). This directory does not provide a Compose file.

For every configured URL, component, dependency, sample rate and interview comparison, see the [detailed project reference](PROJECT-REFERENCE.md).

## Request flow

```text
Client -> API gateway -> currency conversion -> currency exchange -> H2
              |                  |                     |
              +----- spans exported to Zipkin ---------+

Eureka supplies instance addresses; it does not forward the request.
```

The gateway matches `/currency-conversion/**`, rewrites it to `/currency-conversion-feign/**`, and uses `lb://currency-conversion` to find an instance. Conversion calls exchange using Feign and multiplies the returned rate by the quantity. The sample CAD to INR rate is 62.40, so 12 produces 748.80; these are not live rates.

```powershell
curl.exe http://localhost:8765/currency-conversion/from/CAD/to/INR/quantity/12
```

The separate RestTemplate example is at `http://localhost:8100/currency-conversion-rest/from/CAD/to/INR/quantity/12`. It calls exchange at `localhost:8000` directly, without Eureka or the gateway. The gateway has no `/currency-conversion-rest/**` route.

## Observability concepts

- **Logs** are individual events. A trace ID in a log helps find related events, but logs alone do not build a distributed trace.
- **Metrics** aggregate measurements, such as HTTP request count and duration (`http.client.requests`). They show trends rather than the details of every request.
- **Traces** describe a request across components. A trace shares a trace ID; its spans represent operations and have their own span IDs and parent relationships. Client and server can each record a span for one HTTP call.
- **Micrometer Observation** records operations through a common API. Registered handlers can produce metrics and tracing data. Spring instruments supported HTTP servers and appropriately configured clients.
- **Micrometer Tracing** provides a tracing API and connects observations to a tracer. `micrometer-tracing-bridge-otel` connects it to OpenTelemetry. OpenTelemetry creates and processes spans, and its Zipkin exporter sends them to Zipkin. Zipkin stores and displays received traces; it does not instrument the application.
- **Actuator** provides management and observability integration. The configured log pattern prints trace and span IDs when available; it does not itself export spans.

### How one traced request fits together

1. The gateway receives a request and records an incoming server operation. Its route selects conversion using Eureka.
2. The instrumented gateway HTTP client sends trace context in headers. Conversion extracts it and records its server operation in the same trace.
3. The Feign Micrometer capability instruments the conversion-to-exchange call and propagates context. Exchange records the incoming request. A RestTemplate built using Spring's auto-configured builder is similarly instrumented for the separate REST endpoint.
4. Services export sampled spans to Zipkin. Search by service name, open a trace, and compare its spans and timings. Export can be asynchronous, so allow a short delay.

`management.tracing.sampling.probability=1.0` samples all eligible traces for this local demo. Production systems commonly sample fewer to control overhead and storage. Context propagation also requires instrumented clients and compatible settings; sampling alone does not install a tracer or exporter.

## Run and check

1. Start Zipkin, for example `docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin`, and open http://localhost:9411. Docker may download the image. Reuse Zipkin if already running.
2. With JDK 25, start the naming server, exchange, conversion and gateway in that order. From each application directory on Windows run `.\mvnw.cmd spring-boot:run`. Allow Eureka time to register instances.
3. Open http://localhost:8761/ and check that the three applications are UP. Call the gateway URL above and search for `api-gateway` in Zipkin.
4. For the REST client example call port 8100 directly and search for `currency-conversion`. Compare its trace with the gateway/Feign trace.

Default Zipkin and Eureka addresses assume apps run on the host. Inside a container, `localhost` means that container: configure reachable Eureka and Zipkin addresses and advertise hostnames reachable by callers. Exchange and conversion default `EUREKA_INSTANCE_HOSTNAME` to `localhost` for the same-host setup; override it for containers or remote hosts.

## Checks and troubleshooting

- A gateway 500 with `UnknownHostException` means the selected instance hostname could not be resolved. Inspect Eureka's registered hostname and port, correct the advertised address and restart/re-register the service. Allow registry caches time to refresh. This caused the observed `LAPTOP-D1AN58KS.mshome.net` failure.
- A missing gateway in Zipkin can mean the running gateway was not rebuilt with tracing dependencies, cannot reach Zipkin, or did not sample/export the request. Check logs and call the gateway rather than a service directly.
- Separate traces can indicate missing propagation, an uninstrumented client, incompatible propagation settings, or a service that was not restarted. Compare trace IDs and inspect each hop. Feign and RestTemplate use separate instrumentation paths.
- A successful request does not prove spans reached Zipkin. An UP instance in Eureka does not prove its advertised address is reachable. Check `/actuator/health`, logs, Zipkin connectivity and the actual URL.
- Run `.\mvnw.cmd test` in each service directory. Existing context tests do not establish end-to-end tracing correctness. Maven needs the declared dependencies available locally or from a reachable repository.

## Interview questions

**Trace ID versus span ID?** The trace ID correlates a request across services. Each span ID identifies an operation; parent IDs connect operations.

**What propagates context?** An instrumented HTTP client writes context into headers; the next service extracts it. Propagation links live requests; exporting sends completed spans to a backend.

**Why use the injected RestTemplateBuilder?** Spring applies its observation customizer, so the built client can record observations and propagate trace context. `new RestTemplate()` bypasses those automatic customizations unless configured manually.

**Why add feign-micrometer?** It enables Feign's Micrometer observation capability when the registry and auto-configuration are present. It instruments Feign calls; it is not the Zipkin backend.

**What is sampling?** A policy deciding which traces are recorded/exported. It reduces overhead and storage, but an unsampled request may not appear in Zipkin.

**Is Eureka part of tracing?** No. It provides discovery. Zipkin collects and displays spans; the gateway routes calls; Micrometer and OpenTelemetry provide observation and tracing instrumentation.

**What does `proxyBeanMethods=false` do?** Spring does not intercept calls between `@Bean` methods in that configuration. Direct calls are ordinary Java calls; inject bean dependencies as parameters instead. It does not disable HTTP tracing.

See the individual [exchange](01-currency-exchange-service/README.md), [conversion](02-currency-conversion-service/README.md), [naming server](03-naming-server/README.md), and [gateway](04-api-gateway/README.md) guides.
