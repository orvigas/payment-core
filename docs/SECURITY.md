# Security

Authentication, authorization, and abuse protection in Payment Core, plus the gaps that must be closed before production. The implementation lives in `com.payment.security`, `com.payment.config.SecurityConfig`, and `com.payment.config.RateLimitingConfig`.

Related documents: [ARCHITECTURE.md](ARCHITECTURE.md), [DEPLOYMENT.md](DEPLOYMENT.md).

## Authentication Flow

1. `POST /api/v1/auth/login` with username and password.
2. `AuthController` delegates to Spring Security's `AuthenticationManager`, which loads the user through `CustomUserDetailsService` (backed by `UserRepository.findByUsername`) and verifies the password against the stored BCrypt hash.
3. On success, `JwtTokenProvider` issues an access token (1 hour) and a refresh token (7 hours). Both are HMAC-SHA256 signed JWTs whose subject is the user's UUID `userId` — never the internal database `id`.
4. Clients send `Authorization: Bearer <access token>` on every request. `JwtAuthenticationFilter` validates the signature and expiration and puts the `userId` into the `SecurityContext`. No server-side session exists; the token is the whole authentication state.
5. `POST /api/v1/auth/refresh` accepts a valid refresh token in the Authorization header and returns a new access token.

Design points worth knowing:

- **Login failures are indistinguishable.** Unknown username and wrong password both produce the same "Invalid username or password" response, so the endpoint cannot be used to enumerate valid accounts.
- **Stateless by construction.** Sessions are disabled (`SessionCreationPolicy.STATELESS`) and CSRF protection is off. That is safe only because authentication rides in a header the browser never attaches automatically; if cookie-based auth is ever introduced, CSRF protection must come back.
- **Token parsing uses `parseSignedClaims`.** Tokens are signed, not encrypted; their payload is readable by anyone who holds one. Never put secrets in claims.
- **Accounts can be disabled without deletion** via the `active` flag on `User`, which preserves payment history and audit trails.

## Password Storage

Passwords are hashed with BCrypt (Spring's `BCryptPasswordEncoder`, strength 10). BCrypt is deliberately slow and salted per hash, which makes offline brute-forcing of a leaked table expensive. Plaintext passwords never appear in logs, responses, or the database.

## Endpoint Authorization

From `SecurityConfig.filterChain`:

| Endpoint | Access |
|---|---|
| `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` | Public |
| `GET /actuator/health`, `GET /actuator/prometheus` | Public |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| `/api/v1/payments` and everything else | Authenticated |

The default rule is deny-without-authentication: any endpoint not explicitly permitted requires a valid JWT.

CORS allows `http://localhost:3000` and `http://localhost:8080` with credentials, covering local frontend development. Real deployments need their actual origins listed; wildcard origins with credentials are not an option.

## Payment Ownership

Every payment has exactly one owner, and only that owner can read, confirm, or refund it:

- **Creation.** `CreatePaymentRequest` has no `userId` field. `PaymentController` takes the owner from the authenticated `Authentication` principal (the JWT subject) and `PaymentService.createPayment` writes it onto the entity — a client can never create a payment attributed to someone else.
- **Access.** `PaymentService.getPayment`, `confirmPayment`, and `refundPayment` each take the requester's ID alongside the payment ID and call a shared `requireOwner` check right after loading the payment. A mismatch throws `PaymentAccessDeniedException`, mapped by `GlobalExceptionHandler` to `403 Forbidden`, before any status or business-rule check runs.
- **Defense in depth.** Migration `V008__add_payments_user_fk.sql` adds a foreign key from `payments.user_id` to `users.user_id`, so a payment can no longer be persisted with an owner that isn't a real account — closing the gap left by `payments` (V001) predating `users` (V007).

Payment IDs are still random UUIDs, but ownership is enforced independently of that: even a leaked ID only works for the account that actually owns the payment.

## Rate Limiting

Resilience4j rate limiters (`RateLimitingConfig`) protect the endpoints most attractive to abuse:

| Limiter | Limit | Purpose |
|---|---|---|
| `login` | 10 attempts per minute | Slows online brute-force and credential stuffing against the login endpoint. |
| `payment-creation` | 100 requests per hour | Caps API abuse and runaway clients on the write path. |

When the payment-creation limit trips, the controller fallback converts Resilience4j's `RequestNotPermitted` into `RateLimitExceededException`, which `GlobalExceptionHandler` maps to HTTP 429. The fallback matches only that exception so genuine server errors are not disguised as rate limiting.

Note that these limits are per application instance, not per user or per IP; running multiple replicas multiplies the effective limit. Distributed rate limiting (for example backed by Redis) is needed if that ever matters.

## Error Handling and Information Exposure

`GlobalExceptionHandler` converts all exceptions into structured JSON with timestamp, status, error, and message. Stack traces, SQL, and internal class names never reach clients. Logging follows the same rule: credentials, tokens, and password hashes are never logged; authentication logging is limited to usernames and user IDs.

## Known Gaps Before Production

These are real weaknesses in the current code and configuration, listed so nobody discovers them in an incident:

1. **Default JWT secret fallback.** `app.jwt.secret` falls back to a hardcoded placeholder when `JWT_SECRET` is unset. Anyone who reads the repository can forge tokens against an instance running the default. Deployments must set `JWT_SECRET` to a random value of at least 32 characters from a secret store, and startup should ideally fail without it.
2. **Roles are not enforced.** `JwtAuthenticationFilter` builds the authentication with an empty authority list, so the `USER`/`ADMIN` roles stored on `User` never reach the security context and no endpoint can currently be restricted to admins.
3. **Refresh tokens are only checked for validity, not type.** `POST /api/v1/auth/refresh` accepts any valid signed token, including an access token, because the `type` claim is not verified. Access and refresh tokens also share the same signing key, and there is no revocation: a leaked refresh token works until it expires.
4. **JDWP debug port in the image.** The Dockerfile starts the JVM with a debug agent on port 5005; an attached debugger is remote code execution. Remove it outside local development (see [DEPLOYMENT.md](DEPLOYMENT.md)).
5. **Open actuator and Swagger.** `/actuator/prometheus` and Swagger UI are public, exposing internal metrics and the full API surface to anyone who can reach the service. Restrict them at the network layer or behind authentication.
6. **Seeded credentials.** The `db-seed` service creates `load_test_user` with a password published in this repository. Never run it against a shared environment.
7. **No TLS termination in the stack.** JWTs in headers are only as safe as the transport; production traffic must be HTTPS end to end (typically terminated at a load balancer or ingress).
8. **Destructive database defaults.** `DB_RESET_ON_STARTUP=true` plus `clean-disabled: false` will drop a production schema on boot if pointed at one. Both must be flipped before touching non-disposable data (see [DEPLOYMENT.md](DEPLOYMENT.md)).

## Adding Secured Endpoints

- New endpoints are authenticated by default; add explicit `permitAll` matchers in `SecurityConfig` only with a reason.
- Take the caller's identity from the `SecurityContext` (the JWT subject), never from a request parameter, so users cannot act on behalf of others.
- Rate-limit anything that is expensive or abusable using a named limiter in `RateLimitingConfig` and a `@RateLimiter` annotation with a fallback that only matches `RequestNotPermitted`.
- Test both directions: valid token succeeds, and missing/expired/garbage tokens get 401 — `AuthControllerTest` and `JwtAuthenticationFilterTest` show the pattern.
