# Naming server

Eureka provides a registry where services register and find each other. Run `.\mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` here with JDK 25, then open http://localhost:8761/. Start it before exchange, conversion and gateway, and allow time for registration.

The standalone server is configured not to register itself or fetch the registry. See the [reference](../../../PROPERTIES-AND-ANNOTATIONS.md) for the settings. The existing Maven test checks context startup.


## Configuration, checks and troubleshooting

The default port is 8761; use `SERVER_PORT` to override it. Check `/actuator/health` where available and inspect startup logs if a request fails. Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere from this folder. Maven requires the declared dependencies; existing context tests do not establish end-to-end correctness. See the [system guide](../README.md#checks-and-troubleshooting) for discovery, setup and current verification limits.
