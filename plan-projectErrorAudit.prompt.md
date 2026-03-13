## Plan: Project-Wide Error & Mistake Audit for ChatStack

After a thorough scan of every source file, configuration, and POM in the project, **bugs, security issues, inconsistencies, and code-quality problems** were found across ~20 files. Below is the full categorized list with actionable steps.

---

### Steps

1. **Fix cookie name mismatch (BUG — refresh token flow is broken).** In `signup()` and `login()` the cookie is named `"refreshToken"` ([AuthenticationServiceImpl.java](src/main/java/com/api/chatstack/services/Impl/AuthenticationServiceImpl.java) lines 128, 182), while `extractRefreshTokenFromCookie()` looks for `"refresh_token"` (line 398). The same file's `writeRefreshCookie()`/`clearRefreshCookie()` also use `"refresh_token"`. Pick **one name** (recommend `refresh_token`) and use it everywhere, including in [SecurityConfig.java](src/main/java/com/api/chatstack/config/SecurityConfig.java) OAuth2 success handler (line 147).

2. **Fix cookie path mismatch (BUG — cookies will never be sent to the right endpoint).** `signup()`/`login()` set path `"/chat-stack/api/v1/auth/refresh-token"`, but `writeRefreshCookie()`/`clearRefreshCookie()` set path `"/api/v1/auth/refresh"`. The actual endpoint (with `context-path`) is `/chat-stack/api/v1/auth/refresh-token`. Unify all cookie paths to the correct one.

3. **Fix wrong ObjectMapper import in `SecurityConfig` (likely compile error at runtime).** Line 23 imports `tools.jackson.databind.ObjectMapper` (Jackson 3.x). The correct import is `com.fasterxml.jackson.databind.ObjectMapper`. Change the import in [SecurityConfig.java](src/main/java/com/api/chatstack/config/SecurityConfig.java).

4. **Fix `login()` storing raw refresh token instead of a hash.** In [AuthenticationServiceImpl.java](src/main/java/com/api/chatstack/services/Impl/AuthenticationServiceImpl.java) line 174 the JWT refresh token is stored as-is (`.refreshTokenHash(refreshToken)`), while `refreshToken()` (line 343) and `logout()` compare using `passwordEncoder.matches(rawToken, hash)`. Login sessions will **never** match during refresh/logout. Hash it: `.refreshTokenHash(passwordEncoder.encode(refreshToken))`.

5. **Fix `login()` interface declaring `throws IOException` but impl not throwing it.** [AuthenticationService.java](src/main/java/com/api/chatstack/services/AuthenticationService.java) line 15 declares `throws IOException`, but `AuthenticationServiceImpl.login()` (line 144) does not. The controller wraps it in a pointless try/catch. Remove `throws IOException` from the interface and the try/catch in [AuthenticationController.java](src/main/java/com/api/chatstack/controllers/AuthenticationController.java) `authLogin()`.

6. **Fix `signup()` not creating a `UserSessionsEntity`.** `signup()` sets a refresh cookie but **never creates a `UserSessionsEntity`** (unlike `login()`). The user has no session record → refresh will fail. Add session creation in `signup()` similar to `login()`.

7. **Replace `@Data` with `@Getter @Setter` on JPA entities.** `@Data` generates `hashCode()`/`equals()`/`toString()` that trigger lazy-loading and cause performance issues. Affects [UserEntity.java](src/main/java/com/api/chatstack/entities/auth/UserEntity.java), [UserSessionsEntity.java](src/main/java/com/api/chatstack/entities/auth/UserSessionsEntity.java), [EmailVerificationTokenEntity.java](src/main/java/com/api/chatstack/entities/auth/EmailVerificationTokenEntity.java), [Oauth2ConnectionsEntity.java](src/main/java/com/api/chatstack/entities/auth/Oauth2ConnectionsEntity.java), and [PasswordResetTokensEntity.java](src/main/java/com/api/chatstack/entities/auth/PasswordResetTokensEntity.java).

8. **Remove `io.micrometer.common.util.StringUtils` usage — no Micrometer/Actuator dependency in POM.** Used in [AuthenticationServiceImpl.java](src/main/java/com/api/chatstack/services/Impl/AuthenticationServiceImpl.java) and [ValidationUtils.java](src/main/java/com/api/chatstack/utils/ValidationUtils.java). Replace with `org.springframework.util.StringUtils` or add an explicit dependency.

9. **Fix `Oauth2ConnectionsEntity` and `PasswordResetTokensEntity` missing `@GeneratedValue` on `@Id`.** Both have `@Id private UUID id;` with no generation strategy — inserts will fail. Add `@GeneratedValue(strategy = GenerationType.UUID)`.

10. **Remove hardcoded credentials from `application.yaml`.** OAuth2 client secrets (Google, GitHub), DB password, mail password, JWT secret key are all in plaintext in [application.yaml](src/main/resources/application.yaml). Move them to environment variables via `${ENV_VAR}` placeholders.

11. **Remove deprecated `database-platform` property.** Hibernate auto-detects the dialect from the JDBC URL. Remove from both [application.yaml](src/main/resources/application.yaml) line 47 and [application-local.yaml](src/main/resources/application-local.yaml) line 15.

12. **Fix Java package naming convention — `Impl` → `impl`.** The sub-package `services.Impl` violates Java conventions. Rename to `services.impl` and update all imports project-wide.

13. **Remove unused imports.** `java.security.PublicKey` in [JwtService.java](src/main/java/com/api/chatstack/config/JwtService.java), `java.net.InetAddress` in [UserSessionsEntity.java](src/main/java/com/api/chatstack/entities/auth/UserSessionsEntity.java), `java.util.Collection` in [UserSessionsRepository.java](src/main/java/com/api/chatstack/repositories/UserSessionsRepository.java).

14. **Fix `UserEntity.email` column missing `unique = true` constraint.** A race condition can create duplicate emails. Add `unique = true` to `@Column` on `email` in [UserEntity.java](src/main/java/com/api/chatstack/entities/auth/UserEntity.java).

15. **Fix `getAllUsers()` potential NPE on `size` parameter.** In [UserManagementController.java](src/main/java/com/api/chatstack/controllers/UserManagementController.java) line 38, `userListResponse.getContent().size() < size` can NPE if `size` is `null`. Add null-check or default values.

16. **Fix `AuthenticationManager` bean missing `throws Exception`.** In [ApplicationConfig.java](src/main/java/com/api/chatstack/config/ApplicationConfig.java) line 37, `config.getAuthenticationManager()` throws a checked exception.

17. **Remove unnecessary `throws Exception` in `SecurityConfig.authFilterChain()`.** [SecurityConfig.java](src/main/java/com/api/chatstack/config/SecurityConfig.java) line 53.

18. **Fix `RevokedReasonEnum` not used anywhere.** All revocation reasons are hardcoded strings. Either use the enum in [RevokedReasonEnum.java](src/main/java/com/api/chatstack/enums/RevokedReasonEnum.java) or delete it.

19. **Fix `AuthenticationController` silently swallowing exceptions.** Methods catch `MessagingException | IOException` and return 500 with null body. Let `GlobalExceptionHandler` handle these, or re-throw as `ChatStackException`.

20. **Fix `CustomOauth2UserService` using hardcoded `http://localhost:8080` URLs.** Lines 71, 108, 111 in [CustomOauth2UserService.java](src/main/java/com/api/chatstack/services/Impl/CustomOauth2UserService.java) hardcode the base URL. Inject `@Value("${app.base-url}")` instead.

### Further Considerations

1. **`spring-boot-starter-webmvc` / `spring-boot-starter-security-test` / `spring-boot-starter-webmvc-test` are Spring Boot 4.0 renamed artifacts** — verify they resolve in Maven Central, or revert to `spring-boot-starter-web` / `spring-boot-starter-test` / `spring-security-test`.
2. **`FileLoaderUtil.loadHtmlTemplate()` never closes the `InputStream`** — resource leak. Use try-with-resources in [FileLoaderUtil.java](src/main/java/com/api/chatstack/utils/FileLoaderUtil.java).
3. **`AuthServiceResult` is misplaced in the `mappers` package** — it's a value object, not a mapper. Move it to a `dto` or `model` package.

