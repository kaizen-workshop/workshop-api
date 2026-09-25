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

- `Workshop`: catalogue entity associated with active `Theme`, `Category` and the creating user. It stores scheduling, registration window, capacity, modality, price, payment method, image reference and audit fields.
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

## Cross-cutting classes

- `SecurityConfiguration`: stateless security, authorization routes and JSON authentication/authorization errors.
- `PasswordConfiguration`: BCrypt `PasswordEncoder` bean.
- `OpenApiConfiguration`: API metadata and Bearer JWT scheme.
- `ApiErrorFactory`, `ApiErrorResponse`, `ApiFieldError`, `ErrorCode`, `ConflictException`, `GlobalExceptionHandler`: standard error contract.

## Maintenance

Update this file in the same change when an endpoint, DTO, domain class, repository, service, controller, migration or public security behavior changes. Clearly identify incomplete functionality.
