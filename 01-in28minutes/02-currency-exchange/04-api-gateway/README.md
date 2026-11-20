# API gateway

Start Eureka, exchange and conversion first. Run `.\mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` here with JDK 25. The gateway listens on port 8765.

- `/currency-exchange/**` goes to exchange.
- `/currency-conversion-feign/**` goes to conversion unchanged.
- `/currency-conversion/**` is rewritten to `/currency-conversion-feign/**` before forwarding.
- `/get` adds a header and query parameter and forwards to http://httpbin.org:80; this needs network access.

`curl http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10`

A global filter logs request paths. Discovery-generated routes in the properties file are commented out; the Java routes are active. The existing Maven test checks context startup.


## Configuration, checks and troubleshooting

The default port is 8765; use `SERVER_PORT` to override it. Check `/actuator/health` where available and inspect startup logs if a request fails. Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere from this folder. Maven requires the declared dependencies; existing context tests do not establish end-to-end correctness. See the [system guide](../README.md#checks-and-troubleshooting) for discovery, setup and current verification limits.
