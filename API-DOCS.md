# API documentation

## Endpoints

| Endpoint | Access | Description |
| --- | --- | --- |
| `POST /api/v1/admin/users` | `ADMIN` | Provisions a user and sends temporary credentials by e-mail. Returns `201` with `UserResponse`. |
| `POST /api/v1/auth/login` | Public | Authenticates by username or e-mail. `LoginRequest` returns `TokenResponse` with a Bearer access JWT. |
| `POST /api/v1/auth/change-password` | Authenticated | Validates `ChangePasswordRequest` and replaces the current password. Returns `204`. |
| `GET /api/v1/users/me` | Authenticated | Returns the caller's profile and selected themes. |
| `PATCH /api/v1/users/me` | Authenticated | Updates only `name`, `phone` and `profileImage` for the caller. |
| `PUT /api/v1/users/me/themes` | Authenticated | Atomically replaces the caller's themes. Receives `ReplaceThemesRequest` and returns `204`. |
| `GET /api/v1/themes` | Authenticated | Lists active themes. |
| `POST /api/v1/admin/themes` | `ADMIN` | Creates a theme from `TaxonomyRequest`. Returns `201`. |
| `PATCH /api/v1/admin/themes/{id}` | `ADMIN` | Updates a theme's provided name, description and/or active state. |
| `GET /api/v1/categories` | Authenticated | Lists active categories. |
| `POST /api/v1/admin/categories` | `ADMIN` | Creates a category from `TaxonomyRequest`. Returns `201`. |
| `PATCH /api/v1/admin/categories/{id}` | `ADMIN` | Updates a category's provided name, description and/or active state. |
| `POST /api/v1/workshops` | `ARWEG`, `ADMIN` | Creates a workshop in `DRAFT`. |
| `GET /api/v1/workshops` | Authenticated | Lists visible workshops, paginated with optional `status`, `themeId` and `categoryId` filters. |
| `GET /api/v1/workshops/{id}` | Authenticated | Returns a published workshop; creators and admins can also access non-public states. |
| `PUT /api/v1/workshops/{id}` | Creator or `ADMIN` | Replaces editable fields of a draft or scheduled workshop. |
| `PATCH /api/v1/workshops/{id}/schedule` | Creator or `ADMIN` | Schedules publication. |
| `PATCH /api/v1/workshops/{id}/publish`, `/close`, `/cancel`, `/archive` | Creator or `ADMIN` | Performs the corresponding valid lifecycle transition. |
| `POST /api/v1/workshops/{id}/duplicate` | Creator or `ADMIN` | Creates a draft copy. |
| `POST /api/v1/workshops/{id}/registrations` | Authenticated active user | Creates the caller's registration. Returns `201`; confirms a free registration, creates a pending paid registration or adds the caller to the waiting list when capacity is full. |
| `PATCH /api/v1/registrations/{id}/cancel` | Registration owner | Cancels a valid registration. If it occupied capacity, promotes the first eligible waiting-list participant. |
| `GET /api/v1/workshops/{id}/registrations` | Workshop creator or `ADMIN` | Lists workshop registrations, paginated and optionally filtered by registration `status`. |
| `POST /api/v1/registrations/{id}/payments` | Registration owner | Creates a simulated payment for a pending PIX/card registration. Requires a UUID `Idempotency-Key`; repeated use for the same registration returns the prior `PaymentResponse`. |
| `PATCH /api/v1/payments/{id}/simulate/paid` | `ARWEG`, `ADMIN` | Simulates a successful internal gateway callback and confirms the registration. |
| `PATCH /api/v1/payments/{id}/simulate/declined` | `ARWEG`, `ADMIN` | Simulates a declined internal gateway callback, cancels the registration and releases its vacancy. |
| `POST /api/v1/workshops/{id}/image` | Creator or `ADMIN` | Uploads or replaces the workshop image from multipart part `file`. Accepts JPEG, PNG and WebP up to 10 MB. Returns `200` with `WorkshopFileResponse`. |
| `GET /api/v1/workshops/{id}/image/content` | Authenticated viewer | Downloads the stored workshop image. |
| `DELETE /api/v1/workshops/{id}/image` | Creator or `ADMIN` | Deletes the workshop image. Returns `204`. |
| `POST /api/v1/workshops/{id}/attachments` | Creator or `ADMIN` | Uploads a multipart `file` attachment. Accepts JPEG, PNG and WebP up to 10 MB. Returns `201` with `WorkshopFileResponse`. |
| `GET /api/v1/workshops/{id}/attachments` | Authenticated viewer | Lists attachment metadata in creation order. |
| `GET /api/v1/workshops/{id}/attachments/{attachmentId}/content` | Authenticated viewer | Downloads an attachment. |
| `DELETE /api/v1/workshops/{id}/attachments/{attachmentId}` | Creator or `ADMIN` | Deletes an attachment. Returns `204`. |
| `GET /actuator/health` | Public | Health check. |
| `GET /v3/api-docs`, `/swagger-ui.html` | Public | OpenAPI document and Swagger UI. |

Bearer tokens use `Authorization: Bearer <access-token>`. `/api/v1/admin/**` requires `ADMIN`, `/api/v1/arweg/**` requires `ARWEG` or `ADMIN`, and remaining API routes require authentication. Users with a temporary password can only access password change.

## User module

- `UserEntity`: JPA entity for `workshop.app_user`; owns UUID, profile fields, password hash, role, account status, temporary-password flag and timestamps. It provisions `PENDING` users, activates users after password change and records login timestamps.
- `Role`: `PARTICIPANT`, `ARWEG`, `ADMIN`.
- `UserStatus`: `PENDING`, `ACTIVE`, `BLOCKED`, `INACTIVE`.
- `UserRepository`: persistence, duplicate verification and username/e-mail lookup.
- `CreateUserRequest` / `UserResponse`: validated provisioning input and safe response (no password fields).
- `AdminUserController`: HTTP entry point for provisioning; no repository access.
- `UserAdministrationService`: checks duplicates, generates a secure temporary password, stores only BCrypt hash, persists and sends initial-access e-mail transactionally.
- `TemporaryPasswordGenerator`: 16-character password using `SecureRandom`.
- `InitialAccessMailService`: mail abstraction; `SmtpInitialAccessMailService` implements it via `JavaMailSender` without logging credentials.
- `V2__create_users.sql`: creates users table.

## Authentication module — in progress

- `AuthenticationService`: validates credentials and account status, records login, issues access token and changes password after validating the previous password.
- `JwtService`: signs and validates JWTs using external `JWT_SECRET`; tokens carry user ID, role and mandatory-password-change state.
- `JwtAuthenticationFilter`: validates Bearer requests, establishes Spring Security authentication and rejects invalid/expired tokens.
- `AuthController`: exposes login and change-password endpoints.
- `LoginRequest`, `TokenResponse`, `ChangePasswordRequest`: login input, token output and password-change input.
- `V3__create_auth_tokens.sql`: creates storage reserved for refresh and password-reset tokens.

Refresh-token rotation, logout and password recovery are implemented with opaque SHA-256-hashed tokens persisted in PostgreSQL. Password-reset requests always return `202` to avoid e-mail enumeration; reset tokens are single-use and expire after one hour. Authentication coverage includes token integrity/expiration, refresh rotation, blocked accounts and missing-token/role-boundary integration checks.

## Profile and preference module

- `ProfileController` and `ProfileService`: derive the user from the authenticated JWT subject. `UpdateProfileRequest` permits only name, phone and profile-image reference; username, e-mail, role, status and credentials cannot be changed through this API. `ProfileResponse` includes selected themes.
- `Theme` and `Category`: administrative taxonomy entities with UUIDs, unique names, descriptions, active flags and audit timestamps. Their controllers expose authenticated active listings and `ADMIN` creation/update operations.
- `UserTheme`: the `workshop.user_theme` association with a composite primary key, which prevents duplicate selections.
- `PreferenceService`: validates the full requested set before deleting current selections, so nonexistent or inactive themes never result in partial replacement.
- DTOs: `TaxonomyRequest` creates taxonomy items; `UpdateTaxonomyRequest` accepts one or more optional metadata/status changes; `ReplaceThemesRequest` receives a non-null set of theme UUIDs.
- `V4__create_preferences.sql`: creates `theme`, `category` and `user_theme`, including unique taxonomy names and foreign keys to users/themes.

## Workshop module

- `Workshop`: catalogue entity associated with active `Theme`, `Category` and the creating user. It stores scheduling, registration window, capacity, modality, price, payment method, `championship` flag, image reference and audit fields. The championship flag controls the refund exception and defaults to `false` for existing workshops.
- `WorkshopStatus`: `DRAFT`, `SCHEDULED`, `PUBLISHED`, `CLOSED`, `CANCELLED`, `ARCHIVED`. Allowed transitions are `DRAFT -> SCHEDULED|PUBLISHED`, `SCHEDULED -> PUBLISHED`, `PUBLISHED -> CLOSED|CANCELLED` and `CLOSED -> ARCHIVED`.
- `WorkshopService`: validates workshop periods and active taxonomy references, restricts non-public visibility to the creator or admin, centralizes transitions, duplication and scheduled publication.
- `WorkshopController`: uses authenticated JWT identity for ownership. `ARWEG` and `ADMIN` manage workshops; `ADMIN` can manage every workshop and ARWEG only its own.
- `WorkshopRequest`, `WorkshopResponse` and `SchedulePublicationRequest`: safe request/response DTOs. Listing uses Spring's `page`, `size` and `sort` parameters, defaults to 20 items sorted by start date and supports `status`, `themeId` and `categoryId` filters.
- `V5__create_workshops.sql`: creates the workshop table with UUID/foreign-key relations, capacity, price and period constraints plus catalogue indexes.

## File module

- `WorkshopAttachment`: stores only metadata for a workshop image or attachment: UUID, workshop, type, original filename, MIME type, extension, size, SHA-256 checksum, opaque storage key and creation time. The database enforces valid types, allowed formats, the 10 MB limit and one main image per workshop.
- `FileStorage`: provider-neutral storage abstraction. `LocalFileStorage` is the current development implementation; its directory is configured by `FILE_STORAGE_LOCAL_DIRECTORY`, and no file contents are persisted in PostgreSQL.
- `FileUploadValidator`: permits only JPEG (`image/jpeg`), PNG (`image/png`) and WebP (`image/webp`) files up to 10 MB. It rejects malformed names, incompatible extension/MIME pairs and incompatible binary signatures, and calculates a SHA-256 integrity checksum before storage.
- `WorkshopMediaService` / `WorkshopMediaController`: manage uploads, replacement, listing, downloads and deletion. ARWEG owners and `ADMIN` may write; reads use the same workshop visibility rule as the catalogue. Invalid uploads return `422` with `INVALID_FILE`; unavailable or missing storage is not exposed with infrastructure details.
- `WorkshopFileResponse`: returns attachment metadata without exposing its storage key or local filesystem location.
- `V6__create_workshop_attachments.sql`: creates attachment metadata, size/format/checksum constraints, a unique main-image index and workshop lookup index.

## Registration module

- `Registration`: workshop participation record with UUID, user, workshop, registration status, payment status, registration/cancellation timestamps and audit timestamps. A partial unique index prevents more than one valid (`PENDING`, `CONFIRMED` or `WAITING_LIST`) registration per user and workshop.
- `RegistrationStatus`: `PENDING`, `CONFIRMED`, `WAITING_LIST`, `CANCELLED`, `REFUNDED`. `RegistrationPaymentStatus` mirrors the payment lifecycle values required before TASK-008 introduces the payment aggregate.
- `RegistrationService`: requires an `ACTIVE` user, a `PUBLISHED` workshop and an open registration period. It locks the workshop pessimistically during capacity decisions, so at most one concurrent request occupies the last vacancy. Free registrations are `CONFIRMED`/`EXEMPT`; PIX and credit-card registrations are `PENDING`/`PENDING` until the payment task completes. Waiting lists are enabled for every workshop in this initial flow and remain ordered by registration time.
- Cancelling an occupying registration promotes the oldest `ACTIVE` user in the waiting list. Waiting registrations do not consume capacity. The workshop creator or `ADMIN` may list registrations; a participant can cancel only their own valid registration.
- `RegistrationController` exposes the participant registration/cancellation flow and the ARWEG/ADMIN management listing. `V7__create_registrations.sql` contains the immutable schema, valid-state checks and supporting indexes.
- `RegistrationConcurrencyIntegrationTest` uses PostgreSQL/Testcontainers to assert that two simultaneous requests for one vacancy result in exactly one occupying registration; it runs when Docker is available.

## Payment module

- `Payment`: one payment per registration, with amount, payment method, simulated external reference, idempotency key and lifecycle state. Valid transitions are `PENDING -> PAID|DECLINED|CANCELLED` and `PAID -> REFUNDED`.
- `PaymentEvent`: immutable audit record for payment creation and every state transition; it preserves previous/current status, gateway reference and occurrence time.
- `PaymentGateway`: provider-neutral port. `SimulatedPaymentGateway` is the current internal study implementation and creates opaque `simulated-*` references without calling an external provider.
- `PaymentService`: creates payments only for the owner of a pending PIX/card registration. It uses the required `Idempotency-Key` to safely return prior requests and avoids duplicate payments per registration. ARWEG/Admin simulation endpoints confirm or decline a payment.
- Cancellation retains the registration flow from TASK-007 and settles the payment in the same transaction: a pending payment is cancelled; a paid registration is refunded only when the workshop starts in more than 48 hours or it is marked as a championship. At exactly 48 hours or later, non-championship cancellations keep the payment as `PAID` and receive no refund. Payment failure or cancellation releases the occupied vacancy and promotes the waiting list through the registration service.
- `V8__add_championship_and_payments.sql`: adds the immutable `championship` workshop flag plus payment/event tables, unique idempotency and registration constraints, valid-status checks and audit index.

## Cross-cutting classes

- `SecurityConfiguration`: stateless security, authorization routes and JSON authentication/authorization errors.
- `PasswordConfiguration`: BCrypt `PasswordEncoder` bean.
- `OpenApiConfiguration`: API metadata and Bearer JWT scheme.
- `ApiErrorFactory`, `ApiErrorResponse`, `ApiFieldError`, `ErrorCode`, `ConflictException`, `GlobalExceptionHandler`: standard error contract.

## Maintenance

Update this file in the same change when an endpoint, DTO, domain class, repository, service, controller, migration or public security behavior changes. Clearly identify incomplete functionality.
