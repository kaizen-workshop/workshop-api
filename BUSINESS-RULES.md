# Implemented business rules and authorization

This document consolidates current behavior. `TASKS.md` and functional documentation
remain the requirements source; `API-DOCS.md` describes the endpoint contracts.

## Authorization matrix

| Operation | PARTICIPANT | ARWEG | ADMIN |
| --- | --- | --- | --- |
| Profile, preferences, registrations, payments, notifications | Own account | Own account | Own account |
| Published workshop/feed consultation, likes/comments | Yes | Yes | Yes |
| Workshop/post creation | No | Yes | Yes |
| Workshop/post management and media | No | Creator's resources | All resources |
| Participant list, attendance, exports and evaluations | No | Managed workshop | All workshops |
| Workshop dashboard | No | Managed workshops | All workshops |
| Group/chat access | Valid member | Member or workshop creator | All groups |
| Message edit | Author | Author | Author |
| Message deletion | Author | Author or workshop creator | Yes |
| Manual notifications | No | Yes | Yes |
| Provision users, manage taxonomy, audit, business/technical metrics | No | No | Yes |

Authentication is also checked for account status and token version. BLOCKED/INACTIVE
accounts cannot use access tokens, refresh or reset passwords. Temporary-password
accounts are limited to changing their password. Password change/reset increments the
token version and revokes refresh sessions; the client must log in again. Refresh and
reset token lookup locks serialize single-use consumption. Public auth endpoints accept
their own credentials/token rather than an access JWT. Logout revokes the supplied
refresh token; an already issued access token remains valid until expiration or version
change. Existing JWTs without the version claim must be replaced by a fresh login.

## Workshop lifecycle

Allowed transitions are `DRAFT -> SCHEDULED/PUBLISHED`, `SCHEDULED -> PUBLISHED`,
`PUBLISHED -> CLOSED/CANCELLED`, and `CLOSED -> ARCHIVED`. Only draft or
scheduled workshops can be edited. Scheduled publication runs periodically. Each
workshop has one group; publication activates it, closure/cancellation/archive deactivates
it. Administrative transitions and updates are recorded transactionally in the audit.

## Participation and payments

Registration validates workshop visibility/state, registration period, account and existing
valid registration. A workshop lock serializes capacity decisions and waiting-list
promotion. Persisted per-user UUID idempotency keys return the previous creation or
cancellation result and reject reuse for another operation. Free registrations are
payment-exempt; paid ones reserve capacity pending payment. Waiting-list promotion
preserves ordering and rechecks eligibility. Failed payments release a reservation.

Payment creation is serialized on the registration. Repeated creation keys return the
existing payment. Repeated simulated confirmations do not duplicate payment events or
notifications. Cancellation refunds paid registrations before the workshop's 48-hour
deadline; championship workshops have the documented refund exception. Cancellation
replay does not repeat the gateway call. Gateway integration is currently simulated.

## Mobile, community and evaluations

Workshop details use private ETag revalidation after authorization. Visible workshop,
published post and owned notification lists support exclusive `updatedAfter`; timestamps
and pagination allow reconciliation. Clients deduplicate by UUID and periodically refresh
the full visible collection to detect removal/archive.

Comment, REST message and evaluation creation accept optional per-user UUID idempotency
keys. Reusing a key for the same resource returns the original result; reuse for another
resource returns a conflict. History supports future, in-progress, completed, cancelled
(including refunded cancellations) and waiting-list views without changing the default
completed view.

Feed publication is restricted to ARWEG/ADMIN with ownership checks. Messages require
group access and are paginated by cursor. STOMP CONNECT validates the stored account
and JWT version; SEND/SUBSCRIBE recheck the current account and token version. Client
SEND is restricted to `/app/groups/{id}/messages` and cannot publish directly to the
broker. Subscriptions validate group access.

Evaluation requires a CONFIRMED registration and a workshop whose end date is not in the
future. A user can evaluate each workshop once; database uniqueness reinforces this.
The end-date rule is date-based. See the tests and API documentation for eligible
registration states and rating constraints.
