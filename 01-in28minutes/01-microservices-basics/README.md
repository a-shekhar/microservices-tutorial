# Microservices basics

Externalized configuration with Spring Cloud Config and a client service.

- [Limits service](01-limits-service/README.md) exposes configured minimum and maximum values.
- [Config Server](02-config-server/README.md) serves configuration from Git.
- [Local configuration](03-git-local-config/README.md) supplies default, development and QA properties.

Start the Config Server first: the limits service requires it at startup. Each service declares Java 25 and includes a Maven wrapper.

## Concepts

Centralized configuration separates settings from application code. Application names and Spring profiles select the appropriate settings. Protect the configuration repository and endpoints, and keep secrets out of version control.
