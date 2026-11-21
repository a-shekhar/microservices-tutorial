# Local configuration

Property files for the [Config Server](../02-config-server/README.md) and limits service.

| File | Minimum | Maximum | Purpose |
| --- | ---: | ---: | --- |
| `limits-service.properties` | 5 | 996 | Default |
| `limits-service-dev.properties` | 1 | 100 | Development profile |
| `limits-service-qa.properties` | 1 | 10 | QA profile |

The file prefix matches the application name; the suffix selects a Spring profile. Effective values depend on property precedence and runtime overrides. These example files are included in the parent repository; keep secrets out of version control.

For a fresh clone, see the [Config Server guide](../02-config-server/README.md) for native filesystem setup or a Git backend using the parent repository and a search path. Git-backed configuration is read from commits, so commit changes before expecting the server to serve them.
