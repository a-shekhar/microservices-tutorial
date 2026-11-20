# Docker, Docker Compose, and Kubernetes interview guide

This companion to the [microservices interview guide](INTERVIEW-GUIDE.md) explains how to package, run, and operate an application. The examples use the currency exchange service in this repository. Start with Docker, then use Compose to describe a local environment and Kubernetes to manage an application in a cluster.

The runnable snippets below use the `02-currency-exchange` copy. For the separate `03-docker` copy's complete URL list, tracing setup and current limitations, see its [project reference](01-in28minutes/03-docker/PROJECT-REFERENCE.md) and [observability interview questions](INTERVIEW-GUIDE.md#10-distributed-tracing-in-the-03-docker-example). The `03-docker` folder does not contain a complete Compose deployment.

| Tool | Main question it answers | What it manages |
| --- | --- | --- |
| Docker | How do I package and run this application? | Images, containers, networks, and volumes |
| Docker Compose | How do I start the components together? | An application defined in YAML, typically on one Docker host |
| Kubernetes | How do I keep workloads running across a cluster? | Placement, desired state, traffic, rollouts, and scaling |

An image built with Docker can be used by Kubernetes through a compatible container runtime such as containerd. Kubernetes schedules Pods and reconciles their state. The runtime starts their containers.

## 1. Docker fundamentals

- An **image** is a packaged filesystem and metadata. A **container** is a running or stopped instance of that image with its own writable layer. Many containers can share the same image layers.
- A **registry** stores and distributes images. A **tag** is a name that can be moved to another image. A **digest** identifies specific image content. Deploy an immutable version or digest so you can reproduce a release and roll back.
- A container is an isolated process. On Linux, namespaces separate views of things such as processes and networking, while cgroups track and limit resources. Containers share the host kernel. A virtual machine runs its own kernel. Docker Desktop commonly runs Linux containers through a Linux virtual machine.
- The main process determines the container's lifetime. When it exits, the container stops. Use the JSON form of ENTRYPOINT so the Java process receives termination signals directly in the example below.

| `Dockerfile` instruction | What it does |
| --- | --- |
| FROM | Selects a base image or starts a new build stage |
| WORKDIR | Sets the directory for later instructions |
| COPY | Copies selected files from the build context or a previous stage |
| RUN | Executes a build step such as compiling the application |
| ARG / ENV | ARG gives the build an input. ENV sets a variable that can persist into the running image |
| EXPOSE | Documents the port used by the container. Use docker run -p to publish it on the host |
| USER | Sets the user for subsequent build instructions and the running image |
| ENTRYPOINT / CMD | ENTRYPOINT selects the main executable. CMD can provide default arguments or a default command |

**Build for repeatability.** Keep the context small with .dockerignore. Put inputs that change rarely before frequently changing source when organizing cacheable instructions. In a multi-stage build, compile in a builder image and copy only the packaged application into a smaller runtime image. For releases, pin and regularly update the base image, scan it, record an SBOM, and consider signing the artifact. Supply build credentials through secret mounts and runtime credentials through your deployment platform. [Docker build best practices](https://docs.docker.com/build/building/best-practices/) · [Build cache](https://docs.docker.com/build/cache/optimize/)

**Network and storage.** A user-defined bridge lets connected containers reach one another by name. Within a container, `localhost` refers to that container's own network. Publish ports for host access only when needed. A named volume can outlive a container, while a bind mount points to a host file or directory. The writable layer follows the container lifecycle. This project's H2 URL stores data in memory, so adding a volume alone does not make it durable.

### Practice with the exchange service

Work in [the exchange module](01-in28minutes/02-currency-exchange/01-currency-exchange-service/README.md). You need Java 25, access to Maven dependencies and the base image, and a Docker engine running Linux containers. Build the JAR with your platform's Maven wrapper. The commands below use PowerShell. On macOS or Linux, replace the wrapper with ./mvnw and use curl.

~~~powershell
.\mvnw.cmd -DskipTests package
~~~

Save this example as `Dockerfile` in the module directory. It packages an already built JAR. The module version is 1.0.0 in its POM. Use the corresponding JAR filename if you change the version. [Eclipse Temurin images](https://adoptium.net/installation/containers/)

~~~dockerfile
FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY target/currency-exchange-service-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
~~~

~~~powershell
docker build -t currency-exchange:demo .
docker run -d --name exchange-demo -p 127.0.0.1:8000:8000 -e SPRING_CLOUD_CONFIG_ENABLED=false -e EUREKA_CLIENT_ENABLED=false -e SPRING_H2_CONSOLE_ENABLED=false currency-exchange:demo
docker logs --tail 40 exchange-demo
curl.exe -f http://localhost:8000/currency-exchange/from/USD/to/INR
docker stop exchange-demo
docker rm exchange-demo
~~~

Wait for startup before calling the URL. Expect the sample USD to INR rate of 85.50, with possible differences in JSON number formatting. The lab disables remote configuration, discovery, and the H2 web console for this standalone service. Use `docker ps -a` to find stopped containers and `docker inspect` to check their exit status and port mapping.

## 2. Docker Compose

Compose describes services, networks, volumes, and runtime configuration in compose.yaml. It usually creates a default project network. Other services on that network can call this service at http://exchange:8000. A host browser instead uses the published port. [Docker Compose networking](https://docs.docker.com/compose/how-tos/networking/)

Save this as `compose.yaml` beside the `Dockerfile` from the previous section. Stop the standalone container first so port 8000 is available.

~~~yaml
services:
  exchange:
    image: currency-exchange:demo
    build: .
    environment:
      SPRING_CLOUD_CONFIG_ENABLED: "false"
      EUREKA_CLIENT_ENABLED: "false"
      SPRING_H2_CONSOLE_ENABLED: "false"
    ports:
      - "127.0.0.1:8000:8000"
~~~

~~~powershell
docker compose config
docker compose up -d --build
docker compose ps
docker compose logs --tail 40 exchange
curl.exe -f http://localhost:8000/currency-exchange/from/USD/to/INR
docker compose down
~~~

**Started versus ready.** A process can start before it accepts requests. Short-form `depends_on` controls startup order. Use a `healthcheck` on the dependency and condition: `service_healthy` to gate dependent startup when needed. Healthcheck commands run inside the target container, so the executable must exist in its image. Keep timeouts and reconnection logic in your application since a dependency can fail after startup. [Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/)

**Configuration and persistence.** A project `.env` file can provide values for Compose interpolation. Map variables with environment or use a service's `env_file` when the container itself needs them. Keep real credentials out of committed YAML and plaintext example files. Named volumes suit data that must outlive replacement. Compose down preserves named volumes by default. Adding -v can remove project-owned volumes and their data.

**How would you extend this to the full project?** Add services for Eureka, conversion, and the gateway. Configure the client Eureka URL to use the naming server's Compose service name. If you include Config Server, supply its network name in the import URL as well. The direct RestTemplate endpoint currently uses `localhost:8000` and needs a configurable exchange base URL before it can call a separate container. The Feign path uses Eureka and needs successful registration. Publish the gateway and publish internal ports only for local debugging when needed.

**How would tracing work across containers?** Each instrumented application needs to reach the Zipkin collector using an address reachable from its own network, and each discovered application must advertise an address reachable by its callers. A host browser's `localhost:9411` is not automatically the address an application in another container should use. Trace context travels over the application-to-application HTTP calls; completed sampled spans are sent separately to Zipkin. Verify the actual Compose deployment and trace; starting containers in order does not establish end-to-end connectivity.

**Why might a Eureka instance show UP while gateway calls fail?** Registration can succeed even if the advertised hostname cannot be resolved or reached from the gateway container. Inspect the registered host and port and test DNS and connectivity from the gateway's network. The `03-docker` exchange and conversion services default their advertised hostnames to `localhost` for same-host execution; override them with addresses that work for the deployment, then allow registration and caches to refresh.

## 3. Kubernetes fundamentals

A **cluster** has a control plane and worker nodes. The **API server** accepts requests and **etcd** stores cluster state. The **scheduler** chooses a node for unscheduled Pods. Controllers compare desired state with actual state and act on differences. On each node the **kubelet** cooperates with the runtime to run containers.

| Object | When to use it |
| --- | --- |
| Pod | Smallest scheduling unit. Its containers share a network and can communicate via `localhost` |
| Deployment and ReplicaSet | Run replicas of a stateless service. A Deployment manages ReplicaSets and rolling updates |
| StatefulSet | Give replicas stable identities and, when configured, separate persistent storage |
| DaemonSet | Run a Pod on each eligible node, for example a node agent |
| Job and CronJob | Run a task to completion or start Jobs on a schedule |
| Service | Give a changing set of matching Pods a stable network endpoint |
| ConfigMap and Secret | Provide ordinary configuration and sensitive configuration, respectively |
| PersistentVolume and PersistentVolumeClaim | Represent storage and a request to use it. A StorageClass can enable dynamic provisioning |
| Namespace | Group namespaced resources and scope controls such as quotas and many RBAC rules |

**Follow the traffic.** A Deployment creates Pods whose labels match a Service selector. The Service gives callers a stable DNS name even as Pods are replaced. A ClusterIP Service is for internal traffic. NodePort opens a port on nodes. LoadBalancer requests an external load balancer when the environment provides one. Use Ingress or Gateway API for managed edge routing with a compatible controller. Compare that edge routing with this project's Spring Cloud Gateway, which also performs application-specific path rewrites. [Kubernetes Services](https://kubernetes.io/docs/concepts/services-networking/service/)

**Do you still need Eureka?** Kubernetes Services and DNS can provide internal discovery if applications are configured to call those addresses. In this repository, Feign and the gateway use Eureka with service IDs. Replacing Eureka requires updating how those calls and routes resolve destinations. Deploying the existing code into a cluster does not make that change automatically.

| Probe | What repeated failure does | What it should check |
| --- | --- | --- |
| Startup | Restarts the container if startup never succeeds. Other probes wait for startup to pass | Whether slow initialization has completed |
| Readiness | Marks the Pod unready so normal Service routing stops sending it new traffic | Whether the Pod can serve requests |
| Liveness | Restarts the container | Whether the application is stuck in a state a restart can fix |

Keep liveness independent of shared downstream services when a restart cannot fix their outage. Otherwise, the same dependency failure can cause every replica to restart. [Kubernetes probes](https://kubernetes.io/docs/concepts/workloads/pods/probes/)

**Resources and scaling.** The scheduler uses CPU and memory requests to place Pods. CPU limits can throttle work and memory limit violations can result in OOM kills. Leave room for JVM non-heap memory and measure real usage. An HPA adjusts replica count from metrics. CPU utilization targets need CPU requests and a working metrics pipeline. Node autoscaling handles capacity separately. Set readiness and rollout limits, then watch the rollout. A PodDisruptionBudget helps with voluntary evictions such as node drains. It cannot guarantee availability during a node failure. [Container resources](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) and [Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) · [Horizontal Pod Autoscaling](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/)

**Configuration and security.** Changing a ConfigMap or Secret consumed as environment variables requires new Pods for the new values. Mounted files and application reload have separate behavior. Base64 is an encoding, so use encryption at rest, least-privilege RBAC and appropriate secret management. Give workloads their own ServiceAccount when they need API access. Use NetworkPolicy with a network plugin that enforces it, and run as non-root. Track logs, metrics and traces. Helm packages and templates related manifests, Kustomize applies overlays, and GitOps continuously reconciles reviewed configuration from a source such as Git. [Kubernetes ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)

### Practice with a local Kubernetes cluster

Use a disposable local kind cluster and kubectl. Build currency-exchange:demo in the Docker section first. Save the example below as `exchange.yaml` in the current directory. The same image is used again without an external registry. [kind quick start](https://kind.sigs.k8s.io/docs/user/quick-start/)

~~~yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: exchange
spec:
  replicas: 1
  selector:
    matchLabels:
      app: exchange
  template:
    metadata:
      labels:
        app: exchange
    spec:
      automountServiceAccountToken: false
      containers:
        - name: exchange
          image: currency-exchange:demo
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8000
          env:
            - name: SPRING_CLOUD_CONFIG_ENABLED
              value: "false"
            - name: EUREKA_CLIENT_ENABLED
              value: "false"
            - name: SPRING_H2_CONSOLE_ENABLED
              value: "false"
          securityContext:
            runAsNonRoot: true
            runAsUser: 10001
            allowPrivilegeEscalation: false
          resources:
            requests:
              cpu: 100m
              memory: 256Mi
            limits:
              memory: 512Mi
          startupProbe:
            httpGet:
              path: /actuator/health
              port: http
            periodSeconds: 2
            failureThreshold: 45
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: http
---
apiVersion: v1
kind: Service
metadata:
  name: exchange
spec:
  selector:
    app: exchange
  ports:
    - port: 8000
      targetPort: http
~~~

These resource values are starting points for the lab. The two probes use the same general Actuator health endpoint for simplicity. In a deployed service, use checks that match the different probe purposes.

~~~powershell
kind create cluster --name exchange-interview
kind load docker-image currency-exchange:demo --name exchange-interview
kubectl --context kind-exchange-interview apply -f exchange.yaml
kubectl --context kind-exchange-interview rollout status deployment/exchange --timeout=120s
kubectl --context kind-exchange-interview get pods,services
kubectl --context kind-exchange-interview port-forward service/exchange 8000:8000
~~~

Run `curl.exe -f http://localhost:8000/currency-exchange/from/USD/to/INR` in another terminal. Stop port-forwarding with Ctrl+C afterwards. Once you have finished the lab, `kind delete cluster --name exchange-interview` removes the cluster and workloads created for this walkthrough.

## 4. Troubleshooting and interview questions

| Symptom | First checks |
| --- | --- |
| Docker container exits immediately | Read `docker logs` and inspect the exit status. Check the JAR, entrypoint, Java version, and environment |
| Works on host, fails in Compose | Check the service name, internal port, shared network, application bind address, and readiness |
| Kubernetes Pod Pending | Use `kubectl describe pod` and inspect events for resource shortages, scheduling rules, or unbound storage |
| ImagePullBackOff | Check image name, tag, registry credentials and access. For this lab, check whether the image was loaded into the named kind cluster |
| CrashLoopBackOff | Read `kubectl logs POD --previous` and inspect events, exit cause, limits, and startup or liveness probe settings |
| Running Pod receives no traffic | Check readiness, Service selector, EndpointSlices, `targetPort`, and applicable NetworkPolicies |

1. **Why does changing a container's file rarely fix a deployment?** Replacement starts from the image and configured mounts. Fix the source or configuration and release it so new replicas receive the change.
2. **What happens if you delete a Pod managed by a Deployment?** The controller sees fewer replicas than requested and creates a replacement. The Service keeps its name and routes to ready endpoints.
3. **Does adding a replica improve availability by itself?** It helps only if replicas can run and serve across relevant failure boundaries. Consider node placement, readiness, resource capacity, and shared dependencies.
4. **When would you pick Compose over Kubernetes?** Compose is useful when a team needs a repeatable local or single-host environment. Kubernetes adds multi-node placement, reconciliation and orchestration when the operational benefits justify the complexity.
5. **Can you roll back everything with a Deployment rollback?** It can restore a previous Pod template revision. Database changes and separately managed configuration need their own rollback or forward recovery plan.
6. **What would you improve before shipping this project?** Build and publish immutable images through CI, make remote URLs configurable, add automated end-to-end checks, use durable storage and managed secrets, and verify probes, resource sizing and release recovery on the target platform.
7. **How do you practice this for an interview?** Draw the request path, predict what a wrong Service selector or bad image tag will do, then name the first command you would use to test each prediction.

**Practice a rollout.** In the lab cluster, build an updated image with a new tag, load it into kind, and apply that tag to the Deployment. Watch the rollout and inspect Pod events and logs if it fails. To restore the previous Pod template, use `kubectl --context kind-exchange-interview rollout undo deployment/exchange`.

The walkthroughs are reference examples. They require a successful Maven build and local Docker or kind access for runtime verification.
