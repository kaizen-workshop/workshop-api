# Workshop API

Backend API for ARWEG workshop discovery, administration and participation.
The project is a modular Spring Boot monolith designed primarily for a mobile client.

## Current status

Foundation, identity, profile/preferences, workshop catalogue, media, registrations,
payments, participant history/calendar/evaluations, posts/feed, workshop groups/chat,
persistent notifications, workshop administration, metrics and administrative audit
and mobile synchronization are implemented, including session hardening and release observability.

The delivery plan is maintained in [TASKS.md](TASKS.md). It is organized as cohesive
product flows instead of small technical fragments.

## Stack

- Java 21 and Spring Boot 3.x
- Spring Web, Data JPA, Security and Bean Validation
- PostgreSQL and Flyway
- Spring Mail, Actuator and OpenAPI/Swagger
- JWT, JUnit 5, Mockito and Testcontainers

## Project structure

```text
br.com.weg.workshop
├── auth             ├── user           ├── preference
├── workshop         ├── registration   ├── payment
├── feed             ├── post           ├── group
├── chat             ├── notification   ├── evaluation
├── audit            ├── file           ├── shared
└── config
```

Modules are created as their domain is implemented. Controllers use DTOs, services own
business rules and repositories are not accessed from controllers.

## Run locally

Prerequisites: JDK 21 and Docker Desktop. `JAVA_HOME` must point to the JDK 21
installation because the Maven Wrapper uses it even when another `java` executable
appears earlier on `PATH`. Confirm the runtime before building:

```bash
./mvnw -version
```

On Windows, use `.\mvnw.cmd -version`.

```bash
docker compose up -d
./mvnw spring-boot:run
```

On Windows:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Useful URLs after startup:

- Health: `http://localhost:8082/actuator/health`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

The development profile uses port `8082` by default to avoid collisions with
other local Tomcat installations. Set `SERVER_PORT` to override it.
It also provides a non-production JWT signing key so local demo accounts work
without extra setup. Set `JWT_SECRET` to override it; production always requires
an externally supplied secret.

## Configuration

The default profile is `dev`. Local defaults are suitable only for the Docker Compose
database and can be overridden with environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SMTP_HOST
SMTP_PORT
SMTP_USERNAME
SMTP_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
PUSH_PROVIDER
EXPO_PUSH_ENDPOINT
EXPO_ACCESS_TOKEN
```

Never commit real credentials. Flyway runs production-safe migrations from
`src/main/resources/db/migration`; the development profile additionally loads
`src/main/resources/db/dev`. Hibernate validates the schema and does not create it.

The development-only demo-data migration creates representative workshops, registrations, posts, chat,
notifications and the following local accounts. They all use the password
`Workshop@2026!`: `demo.carla` (ARWEG), `demo.ana` and `demo.bruno` (participants).

## API conventions

- Base path: `/api/v1`
- JSON request/response DTOs; JPA entities are not exposed
- Protected endpoints require authentication; access rules are enforced by the API
- Errors use a stable envelope:

```json
{
  "timestamp": "2026-09-18T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request.",
  "path": "/api/v1/example",
  "errors": []
}
```

## Verify

Workshop administration supports participant filters, atomic bulk attendance,
CSV/XLSX exports and a manager-scoped dashboard under `/api/v1/arweg`.

```bash
./mvnw clean verify
```

Tests that require PostgreSQL use Testcontainers and run when Docker is available.

## Contribution flow

1. Read `AGENTS.md` and the relevant task in `TASKS.md`.
2. Create one branch for one cohesive delivery: `<type>/TASK-<id>-<description>`.
3. Implement the smallest complete vertical slice, including migration, authorization,
   OpenAPI and tests when applicable.
4. Run the verification command and update the task status only after it passes.
5. Open a pull request; direct pushes to `main` are not allowed.

Business rules and implementation constraints are defined in [AGENTS.md](AGENTS.md).

## Release readiness

Use `SPRING_PROFILES_ACTIVE=prod` with external database, SMTP and JWT settings.
The production profile emits ECS JSON logs and hides health/error details.
`X-Request-Id` correlates requests. Technical metrics at `/actuator/metrics` require ADMIN.
Password changes revoke existing sessions; clients must log in again.
Browser clients may be enabled with a comma-separated `CORS_ALLOWED_ORIGINS` list;
local development allows `http://localhost:8081` and `http://127.0.0.1:8081` by default.
Push remains disabled by default. Set `PUSH_PROVIDER=expo` to use the built-in Expo adapter;
keep any Expo access token in the environment.

The authorization matrix is in [BUSINESS-RULES.md](BUSINESS-RULES.md).
Follow [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) before deployment; production
checks and real payment/push integrations remain operational gates.
