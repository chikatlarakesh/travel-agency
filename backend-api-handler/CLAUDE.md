# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
mvn compile                  # Compile only
mvn test                     # Run tests
mvn package                  # Build fat-JAR (target/*.jar)
mvn package -DskipTests      # Build without running tests
mvn clean package            # Clean then build
mvn spring-boot:run          # Run locally on port 8080
```

**Docker:**
```bash
mvn clean package
docker build -t api-handler .
docker run -p 8080:8080 api-handler
```

## Architecture

**Current state:** Spring Boot 3 / Java 17 stub with a single controller (`GET /api/hello`). No service layer, repository, database, or security configured yet.

**Intended implementation:** `openapi.json` at the repo root is the authoritative Sprint 1 specification — a Travel Agency API with three domains:

- **Auth** (`/api/v1/auth`) — JWT-based sign-up/sign-in; roles: `CUSTOMER`, `TRAVEL_AGENT`, `ADMIN`
- **Tours** (`/api/v1/tours`) — Destination autocomplete, paginated/filterable tour listing, tour detail, reviews
- **Bookings** (`/api/v1/bookings`) — Create and list bookings (JWT-protected)

**Key enumerations from the spec:**
- Tour types: `RESORT`, `CRUISE`, `HIKE`
- Meal plans: `BB`, `HB`, `FB`, `AI`
- Sort options: `RATING_DESC`, `RATING_ASC`, `PRICE_DESC`, `PRICE_ASC`, `NEWEST`, `OLDEST`

**Package root:** `com.epam.edp.demo`

## Project Structure

```
src/main/java/com/epam/edp/demo/
  DemoApplication.java          # @SpringBootApplication entry point
  controller/
    HelloEdpController.java     # Stub: GET /api/hello
src/test/java/com/epam/edp/demo/
  DemoApplicationTests.java     # Spring context smoke test (JUnit 4 style)
deploy-templates/               # Helm chart for Kubernetes deployment
openapi.json                    # Full OpenAPI 3.0.3 contract — implement against this
```

## Testing

- JUnit 4 annotations (`@RunWith(SpringRunner.class)`) are used alongside the JUnit 5 starter — keep new tests consistent with existing style unless explicitly migrating.
- Run a single test class: `mvn test -Dtest=DemoApplicationTests`

## Deployment (Kubernetes / Helm)

- Chart: `deploy-templates/`, service on port `8080`, Ingress enabled by default.
- Requires `regcred` image pull secret in the target namespace.
- Host convention: setting `ingress.hosts[].host = edpDefault` resolves to `<release-name>-<namespace>.<dnsWildcard>` (KubeRocketCI platform convention).
- CI/CD platform: KubeRocketCI (formerly EPAM EDP); commit messages referencing `EPMDEDP-*` tickets follow project convention.
