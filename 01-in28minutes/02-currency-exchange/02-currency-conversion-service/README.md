# Currency conversion service

This service gets a sample rate and multiplies it by a quantity. Start Eureka and exchange first, then run `.\mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` here with JDK 25. It listens on 8100; Config Server is optional.

`curl http://localhost:8100/currency-conversion-feign/from/USD/to/INR/quantity/10`

Feign finds `currency-exchange` through Eureka. The response includes the rate, quantity, calculated amount and instance ports. The separate `/currency-conversion-rest/from/USD/to/INR/quantity/10` endpoint uses a hard-coded localhost:8000 URL; change the URL if exchange uses another port. Failed calls and missing response bodies are not handled explicitly. The existing Maven test checks context startup.


## Configuration, checks and troubleshooting

The default port is 8100; use `SERVER_PORT` to override it. Check `/actuator/health` where available and inspect startup logs if a request fails. Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere from this folder. Maven requires the declared dependencies; existing context tests do not establish end-to-end correctness. See the [system guide](../README.md#checks-and-troubleshooting) for discovery, setup and current verification limits.
