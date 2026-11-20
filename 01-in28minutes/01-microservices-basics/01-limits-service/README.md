# Limits service

A Spring Boot service exposing configured minimum and maximum values.

## Run

Install JDK 25 and start the [Config Server](../02-config-server/README.md). From this directory, run:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use `./mvnw spring-boot:run`. The default port is 8080. The service imports configuration from `http://localhost:8888` and activates the `dev` profile. The import is required: startup can fail if the Config Server is unavailable even though local property values exist. Set `SPRING_PROFILES_ACTIVE=qa` to select the QA profile.

## API

`GET /limits` returns integer `minimum` and `maximum` fields:

```sh
curl http://localhost:8080/limits
```

With the supplied development properties and a working server, the values are 1 and 100. Actual values depend on the profile and property precedence. The application file also supplies local values 2 and 999.

## Concepts and tests

`@ConfigurationProperties` binds settings to a configuration object; constructor injection supplies it to the controller. A Java record models the response. Actuator is included; review management endpoint exposure before deployment.

Run `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere. The existing test checks context startup and may require the Config Server; it does not test the API contract. The current code has no application-specific authorization.
