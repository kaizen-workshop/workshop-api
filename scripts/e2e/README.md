# E2E contract test

Exercises all 78 operations of the OpenAPI contract with the demo `PARTICIPANT`, `ARWEG` and
`ADMIN` accounts, plus an `ARWEG` provisioned on the fly (first-access e-mail read from Mailpit).
It covers authentication, registration, payment (idempotency, refund rules, decline, workshop
cancellation), chat, feed, notifications, administration and authorization boundaries.

```bash
docker compose up -d
./mvnw spring-boot:run          # dev profile, port 8082
PYTHONIOENCODING=utf-8 python -I scripts/e2e/e2e.py
docker exec -i workshop-api-postgres-1 psql -U workshop -d workshop_api -v ON_ERROR_STOP=1 < scripts/e2e/cleanup.sql
```

The run needs only the Python standard library. It leaves data prefixed `E2E`; `cleanup.sql`
removes it in one transaction and restores the `demo.ana` profile and interests to the seed.
