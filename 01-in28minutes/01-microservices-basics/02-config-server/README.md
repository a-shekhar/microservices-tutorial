# Configuration server

Spring Cloud Config Server serves application settings from a Git backend on port 8888.

## Setup and run

Install JDK 25. Point the Git backend at the repository containing the [configuration files](../03-git-local-config/README.md). The checked-in URI points to a different location from that folder and must be overridden for your machine. Git-backed configuration comes from committed files; commit changes in the configuration repository before expecting the server to serve them.

From this directory on Windows:

```powershell
$env:SPRING_CLOUD_CONFIG_SERVER_GIT_URI = 'file:///C:/path/to/config-repository'
.\mvnw.cmd spring-boot:run
```

Use a valid file URI. If the files are inside a repository subdirectory, set the appropriate Git URI and search path. On macOS or Linux, set the environment variable and run `./mvnw spring-boot:run`. A native filesystem backend is mentioned in application properties but is not enabled by default.

## API

```sh
curl http://localhost:8888/limits-service/dev
```

This endpoint returns a configuration environment with property sources for the application and profile; it is distinct from the client service's `/limits` endpoint. Request it to verify the backend, since starting the server alone does not prove its Git URI works.

## Concepts and tests

`@EnableConfigServer` enables configuration serving. Git provides versioned configuration independently of client deployment. No application-specific authentication is configured; protect access, use encrypted transport, and manage secrets appropriately in real environments.

Run `.\mvnw.cmd test` or `./mvnw test` for the existing context startup test.


## Quick local filesystem setup

For a local demo without a separate Git repository, start Config Server with the native profile and an absolute URI for the existing configuration folder. In PowerShell, from the repository root:

```powershell
$configFolder = (Resolve-Path '.\01-in28minutes\01-microservices-basics\03-git-local-config').Path
$env:SPRING_PROFILES_ACTIVE = 'native'
$env:SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS = ([System.Uri]::new($configFolder)).AbsoluteUri
Set-Location '.\01-in28minutes\01-microservices-basics\02-config-server'
.\mvnw.cmd spring-boot:run
```

This only sets variables in that terminal. In a separate terminal, check `curl.exe http://localhost:8888/limits-service/dev`, then start the limits service and request `http://localhost:8080/limits`. For the Git backend instead, use the setup above and point its search path to committed files. This walkthrough has not been verified end to end in the current environment because Maven Central was unreachable.
