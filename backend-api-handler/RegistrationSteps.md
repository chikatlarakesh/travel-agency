# PERSON 1 — Registration Module (Fully Independent)

> **Owner:** Person 1  
> **Module:** User Registration (end-to-end)  
> **Depends on:** Nothing  
> **Parallel with:** Person 2 (Login Module) — NO WAITING  
> **Merge point:** After both are done — combine into single codebase

---

## How Parallel Work Works

Both persons work on **separate Git branches** from the same base project. Each person creates ALL the files they need — including shared ones. Shared files follow the same spec (defined below), so they'll be identical at merge time.

```
main branch
  ├── feature/registration (Person 1)   ← You work here
  └── feature/login (Person 2)          ← Person 2 works here

Step 0: Person 1 makes ONE quick commit (pom.xml + application.yml) → pushes tag "shared-setup"
        Person 2 branches from "shared-setup" tag immediately.
        TOTAL WAIT: ~15 minutes. Then FULLY PARALLEL.
```

> **Person 2 does NOT wait for your full shared foundation.** They only need `pom.xml` + `application.yml` (15-min commit), then they create all other shared files themselves following the same contract.

---

## SHARED FILE CONTRACT

> Both persons create these files independently following the exact same spec.  
> At merge time they will be identical — no conflicts.

### Models

**`AccountStatus`** — `com.epam.edp.demo.model.enums.AccountStatus`
```
Values: ACTIVE, LOCKED, DISABLED
```

**`User`** — `com.epam.edp.demo.model.User`
```
@Document(collection = "users")
Fields: id (@Id String), email (@Indexed unique String), passwordHash (String),
        accountStatus (AccountStatus, default ACTIVE), failedAttempts (int, default 0),
        lockExpiry (Instant nullable), lastLoginAt (Instant),
        createdAt (Instant @CreatedDate), updatedAt (Instant @LastModifiedDate)
```

**`RefreshToken`** — `com.epam.edp.demo.model.RefreshToken`
```
@Document(collection = "refresh_tokens")
Fields: id (@Id String), tokenHash (@Indexed unique String), userId (String),
        deviceInfo (String), issuedAt (Instant), expiresAt (Instant @Indexed TTL),
        revoked (boolean default false), replacedByTokenHash (String nullable)
```

### Repositories

**`UserRepository`** — extends `MongoRepository<User, String>`
```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

**`RefreshTokenRepository`** — extends `MongoRepository<RefreshToken, String>`
```java
Optional<RefreshToken> findByTokenHash(String tokenHash);
List<RefreshToken> findByUserIdAndRevokedFalse(String userId);
void deleteByExpiresAtBefore(Instant now);
```

### Response DTOs

| DTO | Fields |
|-----|--------|
| `MessageResponse` | `String message` |
| `ErrorResponse` | `String error`, `List<String> details` (nullable) |
| `AuthResponse` | `String accessToken`, `String tokenType` ("Bearer"), `int expiresIn` |

### Exceptions

| Exception | Details |
|-----------|---------|
| `AuthFailedException` | extends RuntimeException, no message to client |
| `RateLimitExceededException` | extends RuntimeException, field: `int retryAfterSeconds` |

### `GlobalExceptionHandler` — `@ControllerAdvice`
```
AuthFailedException        → 401, "Invalid email or password"
MethodArgumentNotValidException → 400, field error details list
RateLimitExceededException → 429, Retry-After header
Exception (catch-all)      → 500, "An unexpected error occurred" + log.error server-side
```

### `SecurityUtils` — `com.epam.edp.demo.util.SecurityUtils`
```
Static methods:
  - hashToken(String) → SHA-256 hex
  - maskEmail(String) → "u***@example.com"
  - extractClientIp(HttpServletRequest) → handles X-Forwarded-For
  - createRefreshTokenCookie(String token, int maxAge) → HttpOnly, Secure, SameSite=Strict, Path=/api/auth
  - createExpiredCookie() → Max-Age=0
```

### `MongoConfig` — `@Configuration @EnableMongoAuditing`

### `SecurityConfig` (⚠️ MERGE REQUIRED at end)
**Person 1 creates base:** CSRF disabled, STATELESS, permit public paths, BCrypt (12), CORS  
**Person 2 adds at merge:** filter registrations (RateLimitFilter, JwtAuthenticationFilter)

---

## YOUR WORK — STEP BY STEP

---

### Phase 1: Project Bootstrap (~15 min)

#### Step 1.1 — Update `pom.xml` with ALL dependencies

**File:** `api-handler/pom.xml`

```xml
spring-boot-starter-security, spring-boot-starter-data-mongodb,
spring-boot-starter-validation, spring-boot-starter-actuator,
jjwt-api (0.12.5), jjwt-impl (0.12.5), jjwt-jackson (0.12.5),
bucket4j-core (8.10.1), micrometer-registry-prometheus,
resilience4j-spring-boot3 (2.2.0), lombok
```

#### Step 1.2 — Create `application.yml`

Replace `application.properties` — see Shared Contract above for full content.

#### Step 1.3 — Push bootstrap commit for Person 2

```bash
git checkout -b feature/registration
git add pom.xml src/main/resources/application.yml
git commit -m "chore: project bootstrap - deps and config"
git tag shared-setup
git push origin feature/registration --tags
```

> ✅ **Person 2 can now branch from `shared-setup` and start immediately.**

---

### Phase 2: All Shared Files (~1.5 hours)

Create every file from the Shared File Contract section above:
- Models: `AccountStatus`, `User`, `RefreshToken`
- Repositories: `UserRepository`, `RefreshTokenRepository`
- DTOs: `MessageResponse`, `ErrorResponse`, `AuthResponse`
- Exceptions: `AuthFailedException`, `RateLimitExceededException`, `GlobalExceptionHandler`
- Utils: `SecurityUtils`
- Config: `MongoConfig`, `SecurityConfig` (base — no filters)

**Verify:** `mvn clean compile` passes.

---

### Phase 3: Password Validation Component (~1 hour)

#### Step 3.1 — Create `@ValidPassword` annotation

**File:** `com.epam.edp.demo.validation.ValidPassword`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {
    String message() default "Password does not meet requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### Step 3.2 — Create `PasswordConstraintValidator`

**File:** `com.epam.edp.demo.validation.PasswordConstraintValidator`

```
Implements: ConstraintValidator<ValidPassword, String>

Rules (ALL must pass):
  1. At least one uppercase letter (A–Z)
  2. At least one lowercase letter (a–z)
  3. At least one digit (0–9)
  4. At least one special character (!@#$%^&* etc.)

Error messages per rule:
  "Password must contain at least one uppercase letter"
  "Password must contain at least one lowercase letter"
  "Password must contain at least one digit"
  "Password must contain at least one special character"

SINGLE SOURCE OF TRUTH — never duplicate this logic.
Length handled by @Size on DTO.
```

---

### Phase 4: Registration DTO (~15 min)

**File:** `com.epam.edp.demo.dto.request.RegisterRequest`

```java
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    @ValidPassword
    private String password;
}
```

---

### Phase 5: Registration Service (~1.5 hours)

**File:** `com.epam.edp.demo.service.AuthService`

```
Dependencies: UserRepository, PasswordEncoder, MeterRegistry

register(RegisterRequest request) → MessageResponse:
  1. existsByEmail? → yes: hash password anyway (timing), return 201 same message
  2. no: hash password, create User(ACTIVE, failedAttempts=0), save to MongoDB
  3. Catch DuplicateKeyException → return 201 (race condition safety)
  4. Return MessageResponse("Registration successful")
  5. Log: auth.register.success email=u***@example.com (NEVER log password)
  6. Increment metric: auth.register.success.count

DUMMY_HASH: pre-computed BCrypt hash constant for timing normalization
```

> Optionally add stub methods for login/logout/refresh with `throw UnsupportedOperationException` comments to make merge easier.

---

### Phase 6: Registration Controller (~30 min)

**File:** `com.epam.edp.demo.controller.AuthController`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }
    // Person 2 adds: login(), logout(), refresh()
}
```

**Verify:**
```
POST /api/auth/register {"email":"test@x.com","password":"MyPass1!ab"}  → 201
POST /api/auth/register (same email)                                    → 201 (same)
POST /api/auth/register {"email":"bad","password":"x"}                  → 400
```

---

### Phase 7: Observability (~30 min)

Metrics: `auth.register.success.count`, `auth.register.duplicate.count`  
Logging: masked emails, NEVER passwords  
Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

---

### Phase 8: Testing (~2.5 hours)

#### 8.1 — PasswordConstraintValidator unit tests
```
validPassword_passes, missingUppercase_fails, missingLowercase_fails,
missingDigit_fails, missingSpecialChar_fails, tooShort_fails, tooLong_fails,
exactMinLength_passes, exactMaxLength_passes, multipleRulesFail_reportsAll
```

#### 8.2 — AuthService.register() unit tests
```
register_success_newUser, register_duplicateEmail_returns201_noNewUser,
register_concurrentDuplicate_catchesDuplicateKeyException,
register_hashesPassword_neverStoresPlaintext, register_timingConsistency
```

#### 8.3 — /api/auth/register integration tests
```
valid→201, duplicate→201, invalidEmail→400, noUppercase→400, noLowercase→400,
noDigit→400, noSpecial→400, tooShort→400, tooLong→400, emptyBody→400,
nullEmail→400, nullPassword→400
```

---

### Phase 9: Deployment (~1.5 hours)

#### 9.1 — Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK CMD wget -q --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 9.2 — Helm values.yaml
Add env vars: MONGODB_URI (secret), JWT_PRIVATE_KEY (secret), JWT_PUBLIC_KEY (secret), plus non-secret config vars.

#### 9.3 — deployment.yaml
Resource limits, health probes, replicas: 2.

#### 9.4 — K8s Secret template

---

## VALIDATION CHECKLIST

| # | Check | Status |
|---|-------|--------|
| 1 | `mvn clean compile` passes | ☐ |
| 2 | `@ValidPassword` rejects bad passwords with specific messages | ☐ |
| 3 | Registration returns 201 for new user | ☐ |
| 4 | Registration returns 201 for duplicate (same response) | ☐ |
| 5 | Registration returns 400 for invalid email | ☐ |
| 6 | Registration returns 400 for password rule violations | ☐ |
| 7 | Password stored as BCrypt hash, never plaintext | ☐ |
| 8 | No passwords in logs, emails masked | ☐ |
| 9 | All unit tests pass | ☐ |
| 10 | All integration tests pass | ☐ |
| 11 | Docker image builds | ☐ |
| 12 | Helm chart renders | ☐ |
| 13 | `/actuator/health` responds | ☐ |

---

## ESTIMATED EFFORT

| Phase | Effort |
|-------|--------|
| Phase 1 (Bootstrap) | ~0.25 hours |
| Phase 2 (Shared files) | ~1.5 hours |
| Phase 3 (Password validation) | ~1 hour |
| Phase 4 (DTO) | ~0.25 hours |
| Phase 5 (Service) | ~1.5 hours |
| Phase 6 (Controller) | ~0.5 hours |
| Phase 7 (Observability) | ~0.5 hours |
| Phase 8 (Testing) | ~2.5 hours |
| Phase 9 (Deployment) | ~1.5 hours |
| **Total** | **~9.5 hours** |

---

## MERGE INSTRUCTIONS (After Both Done)

```bash
# Person 1 merges first (clean)
git checkout main && git merge feature/registration

# Person 2 merges second (3 files need manual merge)
git merge feature/login
# Resolve in:
#   SecurityConfig.java → add Person 2's filter registrations
#   AuthService.java    → add Person 2's login/logout/refresh methods
#   AuthController.java → add Person 2's endpoints

mvn clean test  # ALL tests must pass
```
 