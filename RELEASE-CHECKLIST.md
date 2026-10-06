# First release checklist

## Repository validation

- Run `./mvnw clean verify` with Docker running. No skipped PostgreSQL tests are
  acceptable for a release. Inspect `target/surefire-reports` and the packaged JAR.
- Review the authorization matrix and business rules in `BUSINESS-RULES.md`.
- Check `/v3/api-docs` against the mobile client, including UUID idempotency keys,
  cancellation retries, payment reconciliation and `304` handling.
- Verify Flyway V1 through V21 on an empty PostgreSQL database; rehearse upgrading a
  staging copy with its existing migration history. Never edit applied migrations.
- Merge task branches through reviewed pull requests with passing CI.

## Environment gates — pending deployment validation

- Set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` and a random `JWT_SECRET` of at least
  32 bytes. Use secret storage, TLS ingress and a restricted database account.
- Persist `FILE_STORAGE_LOCAL_DIRECTORY` on backed-up storage. Configure upload
  limits and verify retrieval permissions using real media.
- Confirm initial-password and reset e-mails reach a staging mailbox. SMTP health is
  intentionally excluded from the public health endpoint; delivery needs its own check.
- Test public `/actuator/health`, `/actuator/health/liveness` and
  `/actuator/health/readiness`; keep details hidden. Verify `/actuator/metrics` and
  `/actuator/info` require ADMIN. Monitor request latency, errors, JVM and DB pool metrics.
- Confirm ECS JSON logs contain correlation ID, user ID when authenticated, route
  template, status and duration. Check logs exclude credentials, tokens and payloads.
- Provision the first administrator through the approved operational process. Test
  temporary-password login, mandatory change, a new login, logout and password recovery.
- Exercise participant, ARWEG owner, unrelated ARWEG and ADMIN access separately.
- Rehearse registration contention, waiting-list promotion, cancellation/refund retry,
  workshop closure, chat authorization and incremental mobile reconciliation.
- Back up the database and files; rehearse restoration and rollback of the application.
  Schema rollback requires an explicit migration/recovery plan.

## Current integration limits

Payment delivery currently uses a simulated adapter. Push supports the optional Expo
adapter but still requires real project credentials, device tokens and staging validation
before release. API retry tests cover transactional local operations; they do not prove recovery from an external
gateway success followed by a local database rollback. The in-memory STOMP broker
requires a single application instance. Existing subscriptions are not automatically
disconnected when membership changes; deploy-time lifecycle checks must account for it.
Incremental listing covers currently visible resources and requires periodic full refresh
for removals. The checklist documents readiness work; production deployment and the
environment gates above have not been executed by the repository test suite.
