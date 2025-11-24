# Service Starter

This folder contains a minimal Spring Boot service template to speed up creating new microservices.

Quickstart

- Copy this module into `services/<your-service-name>` or use it as a reference.
- Build with Maven: `mvn -DskipTests package`
- Run with: `java -jar target/<artifact>-0.0.1-SNAPSHOT.jar`

Included files

- `pom.xml` — minimal Maven configuration (Java 17, Spring Boot)
- `src/main/java/.../ServiceStarterApplication.java` — entrypoint
- `src/main/resources/application.yml` — basic config
- `Dockerfile` — multi-stage build for container image

Notes

This is intentionally minimal. Extend with OpenAPI (springdoc), resilience (resilience4j), tracing (opentelemetry) and shared DTOs from `platform-libraries/common-dtos` as you implement services.

