# Currency exchange service

This service reads sample rates from an H2 in-memory database. `data.sql` loads the pairs at startup; data disappears when the process stops. Rates are not live market data.

Install JDK 25 and start the naming server for discovery. Run `.\mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` here. The default port is 8000; Config Server is optional.

`curl http://localhost:8000/currency-exchange/from/USD/to/INR`

The response has an ID, currencies, rate and serving port. A missing pair is not handled and currently causes an error. The H2 console is enabled for local experiments; its JDBC URL and example credentials are in `application.properties`.

`GET /sample-api` demonstrates a circuit breaker and bulkhead around a call to `localhost:8080/ex`. A failure can return fallback text. Retry and rate limiter annotations are commented out, so their settings do not apply to this method. The existing Maven test checks context startup, not the API.


## Configuration, checks and troubleshooting

The default port is 8000; use `SERVER_PORT` to override it. Check `/actuator/health` where available and inspect startup logs if a request fails. Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere from this folder. Maven requires the declared dependencies; existing context tests do not establish end-to-end correctness. See the [system guide](../README.md#checks-and-troubleshooting) for discovery, setup and current verification limits.
