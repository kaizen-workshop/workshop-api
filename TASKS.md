# Delivery plan

This backlog is the source of truth for implementation order. A task is a complete,
testable product flow, not a single layer, endpoint or entity. Do not split migrations,
DTOs, services, controllers and tests into separate tasks unless they can be delivered
and reviewed independently.

Status legend:

```text
[ ] pending
[~] in progress
[x] complete
[!] blocked
```

## Milestone 0 — Foundation

### TASK-001 — Foundation and developer experience

Status:

```text
[x]
```

Delivered:

- Spring Boot, Maven Wrapper, Java 21 build and modular package baseline;
- Web, JPA, Security, Validation, PostgreSQL, Flyway, Mail, Actuator, OpenAPI,
  JWT, JUnit, Mockito and Testcontainers dependencies;
- PostgreSQL development profile, Docker Compose and environment-based configuration;
- Flyway migration baseline and Hibernate schema validation;
- public health endpoint, standardized API errors and base security handling;
- OpenAPI/Swagger with JWT bearer scheme;
- Foundation integration tests and documentation.

## Milestone 1 — Identity and access

### TASK-002 — User administration and account provisioning

Status:

```text
[ ]
```

Implement the user domain, roles (`PARTICIPANT`, `ARWEG`, `ADMIN`), status, UUID
constraints and administrative user creation. The complete flow creates a temporary
password, persists only its hash, marks password change as required and sends the
initial-access email. Cover duplicate username/email conflicts, repository behavior,
authorization, migration and OpenAPI.

### TASK-003 — Authentication, credentials and authorization

Status:

```text
[ ]
```

Implement login by username or email, access/refresh JWT lifecycle, logout, mandatory
password change and password recovery. Enforce account status and role access rules for
`/api/v1/admin/**`, `/api/v1/arweg/**` and authenticated resources. Include security
tests for missing, invalid and expired tokens, blocked users and role boundaries.

## Milestone 2 — Profile and preferences

### TASK-004 — Profile, taxonomy and preferences

Status:

```text
[ ]
```

Deliver the self-profile API, administrative themes and categories, and replacement of a
user's selected themes. Users may edit only allowed personal fields; only active valid
themes may be selected. Include migrations, authorization, validation and tests.

## Milestone 3 — Workshop catalogue

### TASK-005 — Workshop management and lifecycle

Status:

```text
[ ]
```

Deliver the workshop domain, migration, CRUD, details and paginated listing with the
documented filters. ARWEG/ADMIN manage workshops; participants cannot view unauthorized
drafts. Enforce date, time, registration-period and capacity rules plus the explicit
state transitions `DRAFT`, `SCHEDULED`, `PUBLISHED`, `CLOSED`, `CANCELLED` and
`ARCHIVED`. Include scheduled publishing, duplication and transition tests.

### TASK-006 — Workshop media and attachments

Status:

```text
[ ]
```

Create storage abstraction and workshop image/attachment flow without storing large
files in the database. Validate size, MIME type, extension and integrity; store only
metadata and storage references. Keep the domain independent from S3, MinIO or another
provider.

## Milestone 4 — Registration and payments

### TASK-007 — Registration and waiting list

Status:

```text
[ ]
```

Deliver registration, cancellation-independent waiting list and ARWEG management flow.
Validate workshop state, registration period, capacity, eligibility and one valid
registration per user/workshop. Preserve waiting-list order and promote the first
eligible participant when a vacancy is released. The PostgreSQL implementation must
prevent overbooking under concurrent requests and include that concurrency test.

### TASK-008 — Payments, cancellation and refunds

Status:

```text
[ ]
```

Deliver the payment domain and a gateway-independent payment service. Support
`PENDING`, `PAID`, `DECLINED`, `CANCELLED`, `REFUNDED` and `EXEMPT`, with auditable,
idempotent state changes. Implement participant/authorized cancellation, eligibility and
refund handling, and integrate vacancy release with TASK-007.

## Milestone 5 — Participant experience

### TASK-009 — History, calendar and workshop evaluations

Status:

```text
[ ]
```

Deliver user workshop history, calendar filters and evaluations. Users evaluate only
eligible completed workshops and only once per workshop. Include evaluation summaries
for ARWEG, pagination and all ownership/eligibility checks.

### TASK-010 — Posts and personalized feed

Status:

```text
[ ]
```

Deliver ARWEG/ADMIN post management, publishing/scheduling, likes, comments and the
paginated personalized feed. The first ranking is deterministic: highlight,
preferences, upcoming workshops, registration availability and recency. Return only
content visible to the requesting user; use cursor pagination when it improves feed
consistency.

### TASK-011 — Workshop groups and chat

Status:

```text
[ ]
```

Deliver exactly one group per workshop, membership derived from a valid registration and
payment rule, and its lifecycle on cancellation/closure. Add cursor-paginated messages,
moderation and WebSocket delivery; REST remains the persistent source of truth. Members
must not send messages to an inactive group.

### TASK-012 — Notifications

Status:

```text
[ ]
```

Deliver the in-app notification centre, automatic domain notifications and a replaceable
push-provider abstraction. Support read/read-all, secure device registration, manual
ARWEG communication and scheduling without coupling business rules to a provider.

## Milestone 6 — Administration and insight

### TASK-013 — Workshop administration

Status:

```text
[ ]
```

Deliver attendance, bulk updates, participant listing and CSV/XLSX exports, plus the
ARWEG dashboard. Restrict all administrative operations and support the documented
filters for registration, payment and attendance status.

### TASK-014 — Metrics and administrative audit

Status:

```text
[ ]
```

Deliver workshop/post metrics and administrative audit history. Record relevant actor,
action, entity, before/after values, timestamp and IP where available. Audit and metric
queries must be paginated, filterable and ADMIN-protected.

## Milestone 7 — Reliability and release

### TASK-015 — Mobile synchronization and idempotency

Status:

```text
[ ]
```

Add `createdAt`/`updatedAt`, incremental synchronization where needed, appropriate HTTP
caching and `Idempotency-Key` support for registration, cancellation, payment and
refund. Design for retries and unstable mobile connections without duplicating critical
operations.

### TASK-016 — Hardening, observability and release readiness

Status:

```text
[ ]
```

Complete authentication hardening, authorization matrix, structured safe logs, technical
metrics and health checks. Consolidate unit, PostgreSQL/Testcontainers, security,
concurrency and API-contract tests. Complete OpenAPI, business-rule documentation and
the first release checklist.

## MVP

The first functional release requires TASK-001 through TASK-012, TASK-015 and TASK-016.
TASK-013 and TASK-014 can follow when the core participant and ARWEG workflow is stable.

## Next delivery

Start with TASK-002. It creates the account model and administrative provisioning needed
by all protected product flows.
