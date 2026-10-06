# API documentation

## Endpoints

Paginated responses use the stable `PageResponse<T>` envelope with `content`,
`number`, `size`, `totalElements`, `totalPages`, `first`, `last` and `empty`;
internal Spring Data page implementations are never serialized directly.

| Endpoint | Access | Description |
| --- | --- | --- |
| `POST /api/v1/admin/users` | `ADMIN` | Provisions a user and sends temporary credentials by e-mail. Returns `201` with `UserResponse`. |
| `POST /api/v1/auth/login` | Public | Authenticates by username or e-mail. `LoginRequest` returns `TokenResponse` with a Bearer access JWT. Invalid credentials and unavailable accounts return the same standard `401 UNAUTHORIZED` response. |
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
| `GET /api/v1/workshops` | Authenticated | Lists visible workshops, paginated with optional `status`, `themeId`, `categoryId` and exclusive `updatedAfter` filters. |
| `GET /api/v1/workshops/{id}` | Authenticated | Returns a published workshop; creators and admins can also access non-public states. Sends a private ETag and returns `304` for matching `If-None-Match`. |
| `PUT /api/v1/workshops/{id}` | Creator or `ADMIN` | Replaces editable fields of a draft or scheduled workshop. |
| `PATCH /api/v1/workshops/{id}/schedule` | Creator or `ADMIN` | Schedules publication. |
| `PATCH /api/v1/workshops/{id}/publish`, `/close`, `/cancel`, `/archive` | Creator or `ADMIN` | Performs the corresponding valid lifecycle transition. |
| `POST /api/v1/workshops/{id}/duplicate` | Creator or `ADMIN` | Creates a draft copy. |
| `POST /api/v1/workshops/{id}/registrations` | Authenticated active user | Creates the caller's registration using a required UUID `Idempotency-Key`. Returns `201`; retries with the same user/key/workshop return the original registration, free registrations are confirmed, paid registrations are pending and full workshops use the waiting list. |
| `GET /api/v1/workshops/{id}/registrations/me` | Authenticated | Returns the caller's latest registration for the workshop, including cancelled/refunded state, or `404` when none exists. |
| `PATCH /api/v1/registrations/{id}/cancel` | Registration owner | Requires UUID `Idempotency-Key`. Cancels a valid registration and promotes the first eligible waiting-list participant if capacity is freed. A retry with the same key skips settlement/promotion; a key reused for another cancellation returns `409`. |
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
| `GET /api/v1/users/me/workshops/history` | Authenticated | Lists the caller's workshop history, paginated. Optional `filter` accepts `FUTURE`, `IN_PROGRESS`, `COMPLETED` (default), `CANCELLED` (including refunded cancellations) or `WAITING_LIST`. |
| `GET /api/v1/users/me/workshops/calendar` | Authenticated | Lists the caller's pending or confirmed workshops, paginated, with optional inclusive `from`/`to` start-date filters. |
| `POST /api/v1/workshops/{id}/evaluations` | Authenticated eligible participant | Creates the caller's single evaluation for a completed workshop. An optional UUID `Idempotency-Key` safely replays a lost response. Returns `201`. |
| `GET /api/v1/workshops/{id}/evaluations` | Workshop creator or `ADMIN` | Lists a managed workshop's evaluations, paginated. |
| `GET /api/v1/workshops/{id}/evaluations/summary` | Workshop creator or `ADMIN` | Returns count and average overall/content/instructor/organization ratings. |
| `POST /api/v1/posts`, `PUT /api/v1/posts/{id}` | `ARWEG`, `ADMIN` | Creates or updates a draft post owned by the caller (or any post for ADMIN). |
| `PATCH /api/v1/posts/{id}/schedule`, `/publish`, `/archive` | Post owner or `ADMIN` | Applies a valid publication lifecycle transition. |
| `GET /api/v1/posts/feed` | Authenticated | Lists published posts, paginated by published time, with optional exclusive `updatedAfter` filter; highlights are returned as feed metadata. |
| `PUT` / `DELETE /api/v1/posts/{id}/like` | Authenticated | Adds/removes the caller's idempotent like. |
| `POST` / `GET /api/v1/posts/{id}/comments` | Authenticated | Creates or paginates comments on a published post. Creation accepts an optional UUID `Idempotency-Key` for safe retry. |
| `PATCH /api/v1/posts/{postId}/comments/{commentId}` | Comment author | Edits the caller's comment while the post is published. |
| `DELETE /api/v1/posts/{postId}/comments/{commentId}` | Comment author, post owner or `ADMIN` | Deletes an allowed comment and returns `204`. |
| `GET /api/v1/groups` | Authenticated | Lists groups accessible through a confirmed paid/exempt registration. ARWEG sees managed workshop groups and ADMIN sees all groups. Each item exposes `canSendMessages` and `canModerate`. |
| `GET /api/v1/groups/{id}` | Member, workshop creator or `ADMIN` | Returns a group without bypassing its derived membership rules. |
| `GET /api/v1/groups/{id}/messages` | Member, workshop creator or `ADMIN` | Returns up to `size` messages ordered newest-first. Accepts the previous `nextCursor` UUID and returns `MessagePageResponse`. |
| `POST /api/v1/groups/{id}/messages` | Member, workshop creator or `ADMIN` | Persists a message in an active group and publishes it to `/topic/groups/{id}`. An optional UUID `Idempotency-Key` returns the original persisted message without publishing it twice. Returns `201`. |
| `PATCH /api/v1/groups/{id}/messages/{messageId}` | Message author | Edits a non-deleted message while the group is active. |
| `DELETE /api/v1/groups/{id}/messages/{messageId}` | Message author, workshop creator or `ADMIN` | Soft-deletes a message and publishes its tombstone representation. |
| `STOMP /ws` | Authenticated | Accepts a Bearer JWT in the STOMP `CONNECT` `Authorization` header. Send to `/app/groups/{id}/messages`; authorized subscribers receive committed messages on `/topic/groups/{id}`. |
| `GET /api/v1/notifications` | Authenticated | Lists the caller's persistent notifications, paginated by update time with optional exclusive `updatedAfter` filter. |
| `PATCH /api/v1/notifications/{id}/read` | Notification owner | Idempotently marks one notification as read. |
| `PATCH /api/v1/notifications/read-all` | Authenticated | Marks all caller notifications as read and returns `204`. |
| `POST /api/v1/notification-devices` | Authenticated | Registers, reassigns or reactivates an Android/iOS push token without returning the token. |
| `DELETE /api/v1/notification-devices/{id}` | Device owner | Deactivates a device and returns `204`. |
| `POST /api/v1/arweg/notifications` | `ARWEG`, `ADMIN` | Creates immediate or scheduled manual notifications for the provided user IDs. |
| `GET /api/v1/arweg/workshops/{id}/participants` | Workshop creator or `ADMIN` | Paginates participants with optional registration, payment and attendance status filters. |
| `PATCH /api/v1/arweg/workshops/{id}/attendance` | Workshop creator or `ADMIN` | Atomically records up to 500 attendance updates. |
| `GET /api/v1/arweg/workshops/{id}/participants/export` | Workshop creator or `ADMIN` | Exports the filtered participant list as `CSV` or `XLSX`. |
| `GET /api/v1/arweg/dashboard` | `ARWEG`, `ADMIN` | Returns workshop, registration, waiting-list and attendance totals scoped to managed workshops; admins see all workshops. |
| `GET /api/v1/admin/metrics/workshops` | `ADMIN` | Paged `WorkshopMetricResponse` with registration, confirmed and attended counts. Optional `status`, `createdFrom`, `createdTo`, `page`, `size`, `sort`. Returns `400` for an invalid period. |
| `GET /api/v1/admin/metrics/posts` | `ADMIN` | Paged `PostMetricResponse` with like and comment counts. Optional `status`, `createdFrom`, `createdTo`, `page`, `size`, `sort`. Returns `400` for an invalid period. |
| `GET /api/v1/admin/audit` | `ADMIN` | Paged `AuditResponse`. Optional `userId`, `action`, `entity`, `entityId`, `from`, `to`, `page`, `size`, `sort`. Returns `400` for an invalid period. |
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

## Authentication module

- `AuthenticationService`: validates credentials and account status, records login, issues access token and changes password after validating the previous password.
- `JwtService`: signs and validates JWTs using `JWT_SECRET`; the `dev` profile has an explicitly non-production local default, while production requires an external value. Tokens carry user ID, role, mandatory-password-change state and token version.
- `JwtAuthenticationFilter`: validates Bearer requests, establishes Spring Security authentication and rejects invalid/expired tokens.
- `AuthController`: exposes login, password change, refresh rotation, logout and password recovery.
- `LoginRequest`, `TokenResponse`, `ChangePasswordRequest`: login input, token output and password-change input.
- `V3__create_auth_tokens.sql`: creates storage reserved for refresh and password-reset tokens.

Refresh-token rotation, logout and password recovery are implemented with opaque SHA-256-hashed tokens persisted in PostgreSQL. Password-reset requests always return `202` to avoid e-mail enumeration; reset tokens are single-use and expire after one hour. Invalid login, refresh, current-password and reset-token credentials use the standard `401 UNAUTHORIZED` envelope instead of falling through to an internal error. Authentication coverage includes token integrity/expiration, refresh rotation, blocked accounts and missing-token/role-boundary integration checks.

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
- `WorkshopMediaService` / `WorkshopMediaController`: manage uploads, replacement, listing, downloads and deletion. ARWEG owners and `ADMIN` may write; reads use the same workshop visibility rule as the catalogue. Newly stored objects are removed if the database transaction rolls back, while replaced/deleted objects are removed only after commit, preventing records from pointing to files removed by a failed transaction. Invalid uploads return `422` with `INVALID_FILE`; unavailable or missing storage is not exposed with infrastructure details.
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

## Evaluation module

- `ParticipantWorkshopController` exposes the authenticated user's paginated history and calendar. History accepts `FUTURE`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` and `WAITING_LIST`; `COMPLETED` remains the default for backward compatibility. The calendar includes pending or confirmed registrations and validates that `to` is not before `from`.
- `Evaluation`: one rating/comment record per user and workshop, with separate overall, content, instructor and organization ratings, immutable creation time and update time. `V9__create_evaluations.sql` enforces the one-evaluation constraint and 1–5 ratings.
- `EvaluationService` only accepts an evaluation from a confirmed participant after the workshop's end date. Workshop creators and admins can view evaluation pages and aggregate summaries; other ARWEG users receive the normal concealed not-found result for unmanaged workshops.
- `CreateEvaluationRequest`, `EvaluationResponse` and `EvaluationSummaryResponse` are DTO-only API contracts; the JPA entity is never exposed.

## Post and feed module

- `Post` supports `DRAFT`, `SCHEDULED`, `PUBLISHED` and `ARCHIVED`; only owners or admins manage it. A scheduled worker publishes due posts every minute, configurable with `app.posts.publication-interval-ms`.
- Published posts are the only posts readable through feed, likes and comments. `PostLike` has a composite database key to prevent duplicate likes; comments are separately paginated. Comment authors can edit their content; authors, post owners and admins can delete it. Optional creation idempotency serializes on the user and replays the original comment.
- `PostService` keeps post ownership and transition checks in the service layer. The deterministic feed ranks highlighted posts, the caller's selected workshop themes, upcoming workshops with open registration, then recency; it returns highlight/like metadata without exposing drafts or scheduled content.
- `V10__create_posts.sql` adds posts, likes and comments with lifecycle constraints and feed/comment indexes.

## Group and chat module

- `WorkshopGroup`: exactly one group is linked to each workshop by a database unique constraint. Draft groups start inactive, publishing activates them, and closing/cancelling/archiving deactivates them. Migration `V11__create_workshop_groups_and_messages.sql` also backfills existing workshops consistently with their current status.
- `GroupService`: derives participant access from a `CONFIRMED` registration with `PAID` or `EXEMPT` payment status. Cancelling a registration therefore removes group access without a duplicated membership table. Workshop creators and `ADMIN` can moderate their groups; `GroupResponse` exposes the resulting send/moderation permissions.
- `Message`: stores author, content, sent/edited/deleted timestamps. Deletion is soft and responses hide deleted content while retaining a visible tombstone.
- `ChatService`: persists before publishing, blocks sends and edits in inactive groups, enforces ownership/moderation and provides newest-first cursor pagination. `nextCursor` is the last returned message UUID and must be passed back unchanged. REST sends may carry an optional UUID `Idempotency-Key`; a replay returns the stored message and does not emit another WebSocket event.
- `GroupController` and `ChatController`: REST remains the persistent source of truth for group discovery and message history/actions.
- `WebSocketConfiguration` and `ChatWebSocketController`: STOMP uses `/ws`, authenticates the `CONNECT` frame with the same JWT, authorizes every group subscription, accepts sends at `/app/groups/{groupId}/messages` and publishes only committed message representations to `/topic/groups/{groupId}`.

## Notification module

- `Notification` is the persistent source of truth for the in-app centre. It stores type, safe display content, string metadata, read time, schedule, delivery time and creation time for one user.
- `NotificationDevice` stores an unexposed provider token, platform and active lifecycle. Re-registering a token safely reassigns it to the authenticated user; deletion deactivates rather than exposing or returning the token.
- `NotificationService` enforces ownership, paginates the centre, supports read/read-all, creates immediate or scheduled manual communication and dispatches due notifications through the replaceable `PushProvider` only after persistence commits. `deliveredAt` is recorded only after every active device send succeeds (or when there are no active devices); a provider failure leaves the notification pending for the scheduler to retry and is logged without device tokens.
- `NoOpPushProvider` keeps push inactive by default so persistent notifications continue to work without external configuration. Setting `PUSH_PROVIDER=expo` activates `ExpoPushProvider`; `EXPO_PUSH_ENDPOINT` and optional `EXPO_ACCESS_TOKEN` configure delivery without adding provider credentials to the repository.
- Registration creation, waiting-list entry/promotion and payment confirmation/decline create automatic domain notifications in the same business transaction.
- `V12__create_notifications.sql` creates notifications and devices with user, delivery and scheduling indexes.

## Workshop administration module

- Attendance is stored on the registration as `ATTENDED`, `NOT_ATTENDED`, `JUSTIFIED_ABSENCE` or `ABSENT`, together with the marking actor and timestamp. Only confirmed or refunded registrations can receive attendance.
- Participant queries and exports support the same optional `registrationStatus`, `paymentStatus` and `attendanceStatus` filters. CSV is UTF-8 with a BOM; XLSX is generated as an Office Open XML workbook without exposing entities.
- Bulk attendance is transactional, rejects duplicate IDs, locks the selected registrations and validates that every registration belongs to the managed workshop before applying any change.
- The dashboard scopes counts to the caller's workshops. `ADMIN` receives global counts.
- `V13__add_registration_attendance.sql` adds attendance state/audit columns, consistency checks and a workshop-attendance index.

## Metrics and administrative audit

- `AdminInsightController` exposes only `ADMIN` routes under `/api/v1/admin`. All three responses are paged. Time filters are inclusive at the start and exclusive at the end; an end that is not later than the start returns `400`.
- `MetricService`, `WorkshopRepository` and `PostRepository` count registrations, confirmed registrations, attendance marks, likes and comments in paginated database queries. Workshop registration totals include cancelled records; confirmed and attendance counts use their current statuses.
- `AuditService` stores actor UUID, action, entity, entity UUID, prior/new values, timestamp and the request's remote IP when available. It records user provisioning, workshop/post creation and management, attendance changes and scheduled publication in the same transaction as the business action. The audit deliberately excludes passwords, tokens and payment credentials. Automated scheduled publications have a null actor/IP.
- `AdministrativeAudit`, `AdministrativeAuditRepository`, `AuditResponse`, `WorkshopMetricResponse` and `PostMetricResponse` define persistence and API contracts. `V15__create_administrative_audit.sql` creates the audit table and filtered history indexes.
- PostgreSQL/Testcontainers coverage applies migration V15, validates Hibernate schema mapping, executes both populated metric queries and verifies audit storage/filtering.

## Mobile reliability

- Registration creation now requires a UUID `Idempotency-Key`, persisted under a per-user unique index. Reusing the key for the same workshop returns the original registration without duplicating notifications; reuse for another workshop is rejected.
- The registering user and workshop are pessimistically locked, so concurrent retries for the same operation converge before capacity and waiting-list decisions.
- `V14__add_registration_idempotency.sql` backfills existing registrations with their own IDs and adds the non-null idempotency key/index.
- Registration responses expose `waitingListPosition` only while the registration is on the waiting list. The position is calculated from the stable registration timestamp/UUID order and disappears after promotion.
- Cancellation requires a UUID `Idempotency-Key`. `RegistrationService` locks the workshop and refreshes the registration after acquiring the lock; the key is persisted with a per-user unique index. Retried cancellation skips payment settlement, refunds and waiting-list promotion. Reusing a key on another registration returns `409`.
- Payment creation locks the registration before checking existing payments and invoking the gateway, so concurrent retries converge on one payment and one payment event. Repeated simulation callbacks in the already reached `PAID` or `DECLINED` state return the prior payment without duplicate events/notifications. Payment creation continues to require a UUID `Idempotency-Key`.
- `WorkshopResponse` and `NotificationResponse` expose `createdAt` and `updatedAt`. Workshop catalogue, published post feed and notification centre accept an exclusive `updatedAfter` timestamp; only content already visible to the caller is returned. Clients should deduplicate by resource ID and periodically perform a full page refresh to detect content that left a visible collection, such as archived posts.
- Workshop detail responses use an ETag derived from ID and `updatedAt` with `Cache-Control: private, no-cache`. Clients may send `If-None-Match` for a `304` response after access control is checked.
- `V16__add_cancellation_idempotency.sql` adds the cancellation key index and backfills notification update timestamps. PostgreSQL/Testcontainers tests cover concurrent cancellation and payment retries, incremental queries and the migration.
- `V18__add_mobile_operation_idempotency.sql` adds nullable, per-user unique operation IDs for comments, messages and evaluations. Existing clients remain compatible; clients that send a UUID `Idempotency-Key` can safely retry after a lost response.


## Cross-cutting classes

- `SecurityConfiguration`: stateless security, authorization routes, JSON authentication/authorization errors and an explicit CORS allow-list. `CORS_ALLOWED_ORIGINS` accepts comma-separated browser origins; development defaults cover the Expo web server on localhost port 8081.
- `PasswordConfiguration`: BCrypt `PasswordEncoder` bean.
- `OpenApiConfiguration`: API metadata and Bearer JWT scheme.
- `ApiErrorFactory`, `ApiErrorResponse`, `ApiFieldError`, `ErrorCode`, `ConflictException`, `GlobalExceptionHandler`: standard error contract.

## Maintenance

Update this file in the same change when an endpoint, DTO, domain class, repository, service, controller, migration or public security behavior changes. Clearly identify incomplete functionality.

## Release security and observability (TASK-016)

- `V17__add_user_token_version.sql` adds a nonnegative token version to users.
  `UserEntity.changePassword` advances it. `JwtService` includes it in signed tokens.
- `JwtAuthenticationFilter` verifies current account status, role and version from
  `UserRepository` on every Bearer request. Blocked/inactive/stale sessions return
  the standard `401` envelope. Password change/reset revokes all refresh tokens;
  log in again afterward. Existing tokens without a version claim require a new login.
- Refresh/reset repositories use pessimistic locking for single-use consumption.
  Refresh and reset reject blocked/inactive accounts. Logout revokes only the supplied
  refresh token; issued access tokens retain their bounded validity.
- STOMP CONNECT checks the same stored account/version restrictions. The inbound
  interceptor preserves its principal, rechecks account status, role and token version
  on SEND/SUBSCRIBE, and rejects SEND outside the application chat destination,
  preventing direct publication to `/topic`. Subscriptions check group access.
- `RequestLoggingFilter` generates `X-Request-Id` and logs safe structured request fields:
  request ID, authenticated user ID, method, route template, status and duration.
  No request body, query, authorization header or arbitrary unmatched path is logged.
- Unexpected failures keep the public `500 INTERNAL_ERROR` envelope and are logged
  server-side with the matched route template and stack trace for diagnosis.
- `application-prod.yml` requires external database, SMTP and JWT configuration,
  enables ECS JSON logs and suppresses health/error internals. The unused default
  in-memory Spring user auto-configuration is excluded.
- `/actuator/metrics` and `/actuator/info` require ADMIN. Public health probes expose
  status without details; mail connectivity is excluded. Metrics include standard
  HTTP, JVM and connection-pool measurements available through Actuator.
- `OpenApiConfiguration.authenticationContract` declares Bearer security on protected
  operations and documents `401`/`403`; public auth operations have no access-JWT requirement.
- Regression coverage includes stale/blocked JWTs, revoked sessions, blocked reset,
  concurrent refresh consumption, WebSocket routing/principal, metrics authorization,
  log redaction and generated OpenAPI security.

See `BUSINESS-RULES.md` for the authorization matrix and consolidated business rules,
and `RELEASE-CHECKLIST.md` for deployment gates and current external integration limits.
