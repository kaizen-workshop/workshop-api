# Workshop API

Backend API for ARWEG workshop discovery, administration and participation.
The project is a modular Spring Boot monolith designed primarily for a mobile client.

## Current status

The Foundation delivery is complete. It provides the application baseline, PostgreSQL
development configuration, Flyway, health checks, error responses, OpenAPI and tests.
Authentication, users and business modules are still pending.

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

Prerequisites: JDK 21 and Docker Desktop.

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

- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

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
```

Never commit real credentials. Flyway runs migrations from
`src/main/resources/db/migration`; Hibernate validates the schema and does not create it.

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
