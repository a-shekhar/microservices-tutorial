# Currency conversion service

## Observability

Feign and RestTemplate are different outgoing clients. `feign-micrometer` enables Feign observations when the registry and integration are active. `RestTemplateConfiguration` uses Spring Boot's injected builder, which applies observation customizations. Both clients can propagate trace context to exchange when tracing is configured; `new RestTemplate()` bypasses automatic builder customizations. The REST endpoint calls `localhost:8000` directly, whereas Feign discovers exchange through Eureka. Only the Feign endpoint is reached through the gateway's conversion rewrite. See the [request flow and interview guide](../README.md#request-flow).

This service gets a sample rate and multiplies it by a quantity. Start Eureka and exchange first, then run `.\mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` here with JDK 25. It listens on 8100; Config Server is optional.

`curl http://localhost:8100/currency-conversion-feign/from/USD/to/INR/quantity/10`

Feign finds `currency-exchange` through Eureka. The response includes the rate, quantity, calculated amount and instance ports. The separate `/currency-conversion-rest/from/USD/to/INR/quantity/10` endpoint uses a hard-coded localhost:8000 URL; change the URL if exchange uses another port. Failed calls and missing response bodies are not handled explicitly. The existing Maven test checks context startup.


## Configuration, checks and troubleshooting

The default port is 8100; use `SERVER_PORT` to override it. Check `/actuator/health` where available and inspect startup logs if a request fails. Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere from this folder. Maven requires the declared dependencies; existing context tests do not establish end-to-end correctness. See the [system guide](../README.md#checks-and-troubleshooting) for discovery, setup and current verification limits.
