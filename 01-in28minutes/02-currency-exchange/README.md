# Currency exchange system

These services show how applications find and call one another. The rates are sample values, not live market rates.

| Service | Port | Purpose |
| --- | --- | --- |
| [Naming server](03-naming-server/README.md) | 8761 | Eureka registry |
| [Exchange](01-currency-exchange-service/README.md) | 8000 | Finds rates in H2 |
| [Conversion](02-currency-conversion-service/README.md) | 8100 | Multiplies a rate by a quantity |
| [Gateway](04-api-gateway/README.md) | 8765 | Routes requests |

Start them in the order above in separate terminals, and allow time for registration. Config Server on port 8888 is optional for exchange and conversion. See the [reference](../../PROPERTIES-AND-ANNOTATIONS.md) for settings.

`curl http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10`

The direct RestTemplate endpoint uses the exchange service on localhost:8000; it does not use discovery.


## Architecture and request flow

`Client ? Gateway (8765) ? Conversion (8100) ? Exchange (8000) ? H2`

Eureka (8761) provides registered instance addresses to the gateway and Feign client. Exchange and conversion can optionally import configuration from Config Server (8888).

## Quick start

Install Java 25 and allow Maven access to Maven Central. Open four terminals at the repository root. In order, change to each directory below and run `.\mvnw.cmd spring-boot:run` (Windows), or `./mvnw spring-boot:run` (macOS/Linux). Wait for each to start; after starting all four, allow time for Eureka registration.

1. `01-in28minutes/02-currency-exchange/03-naming-server`
2. `01-in28minutes/02-currency-exchange/01-currency-exchange-service`
3. `01-in28minutes/02-currency-exchange/02-currency-conversion-service`
4. `01-in28minutes/02-currency-exchange/04-api-gateway`

From another terminal, use `curl.exe` on Windows or `curl` elsewhere:

`curl http://localhost:8000/currency-exchange/from/USD/to/INR`

An example response has this shape (ID and port can differ):

`{"id":1,"from":"USD","to":"INR","conversionMultiple":85.50,"port":"8000"}`

`curl http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10`

The expected conversion amount is 855.00 using the supplied sample rate; its response also contains the quantity and diagnostic port text. This walkthrough has not been verified end to end in the current environment because Maven Central was unreachable. Exact JSON number formatting can vary.

## Checks and troubleshooting

- Browse `http://localhost:8761/` to inspect Eureka registration. Allow time for registrations to propagate.
- Where Actuator's health endpoint is available, check `/actuator/health` on a service port. This does not prove the full request path works.
- If Maven cannot resolve dependencies, check network access, Maven Central, and the declared versions. Each wrapper may download Maven on its first run.
- If a port is occupied, stop the conflicting process or set `SERVER_PORT` before starting that service. The direct RestTemplate URL is hard-coded, so changing exchange's port also requires updating that URL. Discovery-based calls use registration.
- If the gateway cannot route, check service startup, Eureka's dashboard, registration delay, and the direct exchange and conversion endpoints.
- A missing currency pair currently causes a server error rather than a useful 404. Rates are sample data and vanish on restart.

## Tests and security

Run `.\mvnw.cmd test` or `./mvnw test` in each service folder. The existing tests mainly load a context; they do not verify this end-to-end flow. Naming server's test is in a different package from its application class and may need explicit configuration. Offline test attempts in this environment stopped before compilation because the Spring Boot parent was not cached; a download attempt was blocked by network permissions.

These examples do not implement application-specific authorization. H2 console and sample credentials are for local learning; do not expose them or use them as production secrets. Add authentication, TLS, proper secret management, timeouts, monitoring and automated integration tests before deployment.
