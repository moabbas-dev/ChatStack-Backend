package com.api.chatstack.services.Impl;

import com.api.chatstack.config.JwtService;
import com.api.chatstack.entities.auth.EmailVerificationTokenEntity;
import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.entities.auth.UserSessionsEntity;
import com.api.chatstack.exceptions.*;
import com.api.chatstack.mappers.AuthServiceResult;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.config.ClientRequestContext;
import com.api.chatstack.repositories.EmailVerificationTokenRepository;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.repositories.UserSessionsRepository;
import com.api.chatstack.services.AuthenticationService;
import com.api.chatstack.services.MailService;
import com.api.chatstack.utils.ValidationUtils;
import com.chatstack.dto.*;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionsRepository userSessionsRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailSender;
    private final ClientRequestContext clientContext;
    private final HttpServletResponse response;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.auth.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    @Value("${app.auth.cookie-domain:localhost}")
    private String cookieDomain;

    @Override
    public void verifyEmail(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new NoTokenProvidedException("No token provided");
        }

        EmailVerificationTokenEntity emailVerificationTokenEntity = emailVerificationTokenRepository.
                findByVerificationToken(token).orElseThrow(() ->
                    new InvalidVerificationLinkException("Invalid Verification Token"));

        if (emailVerificationTokenEntity.isUsed()) {
            throw new TokenExpiredException("Token is already used");
        }

        if (emailVerificationTokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenExpiredException("Token is expired");
        }

        UserEntity user = getUserEntity(emailVerificationTokenEntity);
        emailVerificationTokenEntity.setUsed(Boolean.TRUE);

        emailVerificationTokenRepository.save(emailVerificationTokenEntity);
        userRepository.save(user);
    }

    @Override
    public AuthServiceResult signup(SignupRequest signupRequest) throws MessagingException, IOException {
        ValidationUtils.validateUsername(signupRequest.getDisplayName());

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new EmailAlreadyExistsException("User with this email already exists");
        }

        if (userRepository.existsByDisplayName(signupRequest.getDisplayName())) {
            throw new DisplayNameAlreadyTakenException("Display name is already taken");
        }

        String fullname = ValidationUtils.validateAndNormalizeFullname(signupRequest.getFullname());

        UserEntity user = UserEntity.builder()
                .fullName(fullname)
                .displayName(signupRequest.getDisplayName())
                .email(signupRequest.getEmail())
                .emailVerified(false)
                .passwordHashed(passwordEncoder.encode(signupRequest.getPassword()))
                .role(AdminUpdateUserRequest.RoleEnum.ADMIN)
                .status(User.StatusEnum.OFFLINE)
                .lastSeenAt(OffsetDateTime.now())
                .timezone(ZoneId.systemDefault().toString())
                .build();

        UserEntity userEntity = userRepository.save(user);
        userEntity.setAvatarUrl(baseUrl + "chat-stack/api/v1/users/" + user.getId() + "/avatar/default.png");

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        mailSender.sendVerificationEmail(userEntity);

        AuthResponse authResponse = new AuthResponse()
                .accessToken(accessToken)
                .user(userMapper.toDto(userEntity));
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // true in production (HTTPS)
                .sameSite("Lax")
                .path("/chat-stack/api/v1/auth/refresh-token")
                .maxAge(Duration.ofDays(7))
                .build();

        return AuthServiceResult.builder()
                .authResponse(authResponse)
                .refreshCookie(refreshCookie)
                .build();
    }

    @Override
    @Transactional
    public AuthServiceResult login(PasswordLoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        UserEntity user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new UnverifiedEmailException("Your email is not verified. Please verify your email before logging in.");
        }

        user.setLastSeenAt(OffsetDateTime.now());
        user.setStatus(User.StatusEnum.ONLINE);
        userRepository.save(user);

        String clientIp = clientContext.getClientIp();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserSessionsEntity session = UserSessionsEntity.builder()
                .userEntity(user)
                .deviceType(clientContext.getDeviceType())
                .deviceName(clientContext.extractDeviceName(clientContext.getUserAgent()))
                .tokenFamily("")
                .revokedReason("")
                .isRevoked(false)
                .revokedAt(null)
                .refreshTokenHash(refreshToken) // In production, hash this token before storing
                .ipAddress(clientIp)
                .userAgent(clientContext.getUserAgent())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        userSessionsRepository.save(session);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // true in production
                .path("/chat-stack/api/v1/auth/refresh-token") // must match your refresh endpoint exactly
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();

        AuthResponse authResponse = new AuthResponse()
                .accessToken(accessToken)
                .user(userMapper.toDto(user));

        return AuthServiceResult.builder()
                .authResponse(authResponse)
                .refreshCookie(refreshTokenCookie)
                .build();
    }

    @Override
    public void logout(AuthLogoutRequest logoutRequest) {
        HttpServletRequest request = clientContext.getRequest();

        String rawToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new MissingRefreshTokenException("Refresh token cookie not found"));
        
        if (logoutRequest.getAllDevices() != null && logoutRequest.getAllDevices()) {
            revokeAllSessionsForUser(rawToken);
        } else {
            revokeSingleSession(rawToken, logoutRequest.getSessionId());
        }
        clearRefreshCookie(response);
    }

    private void revokeSingleSession(String rawToken, UUID targetSessionId) {
        UserSessionsEntity currentSession = userSessionsRepository.findAllByIsRevokedFalse()
                .stream()
                .filter(s -> passwordEncoder.matches(rawToken, s.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        UserSessionsEntity targetSession = userSessionsRepository.findById(targetSessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));

        if (targetSession.getIsRevoked()) {
            throw new InvalidRefreshTokenException("Session already revoked");
        }

        if (!targetSession.getUserEntity().getId().equals(currentSession.getUserEntity().getId())) {
            throw new UnauthorizedException("Cannot revoke another user's session");
        }

        targetSession.setIsRevoked(true);
        targetSession.setRevokedAt(OffsetDateTime.now());
        targetSession.setRevokedReason("logout");
        userSessionsRepository.save(targetSession);
    }

    private void revokeAllSessionsForUser(String rawToken) {
        UserSessionsEntity currentSession = userSessionsRepository.findAllByIsRevokedFalse()
                .stream()
                .filter(s -> passwordEncoder.matches(rawToken, s.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        List<UserSessionsEntity> allUserSessions = userSessionsRepository
                .findAllByUserEntityAndIsRevokedFalse(currentSession.getUserEntity());

        allUserSessions.forEach(s -> {
            s.setIsRevoked(true);
            s.setRevokedAt(OffsetDateTime.now());
            s.setRevokedReason("logout_all_devices");
        });

        userSessionsRepository.saveAll(allUserSessions);
    }

    @Override
    public void resetPassword(AuthResetPasswordRequest resetPasswordRequest) {
        String newPassword = resetPasswordRequest.getNewPassword();
        String userEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String oldPasswordHash = user.getPasswordHashed();

        if (passwordEncoder.matches(newPassword, oldPasswordHash)) {
            throw new SamePasswordException("New password cannot be the same as the old password");
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHashed(newPasswordHash);
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(AuthResendVerificationRequest forgotPasswordRequest) throws MessagingException, IOException {
        String userEmail = forgotPasswordRequest.getEmail();

        if (!userRepository.existsByEmail(userEmail)) {
            throw new UserNotFoundException("User not found");
        }

        mailSender.sendPasswordResetEmail(userEmail);
    }

    @Override
    public void changePassword(AuthChangePasswordRequest resetPasswordRequest) {
        String newPassword = resetPasswordRequest.getNewPassword();

        String userEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String currentPasswordHash = user.getPasswordHashed();

        if (!passwordEncoder.matches(resetPasswordRequest.getCurrentPassword(), currentPasswordHash)) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new SamePasswordException("New password cannot be the same as the old password");
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHashed(newPasswordHash);
        userRepository.save(user);
    }

    @Override
    public void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException {
        UserEntity user = userRepository.findByEmail(authResendVerificationRequest.getEmail()).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        mailSender.sendVerificationEmail(user);
    }

    @Override
    public RefreshResponse refreshToken() {
        HttpServletRequest request = clientContext.getRequest();

        String rawToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new MissingRefreshTokenException("Refresh token cookie not found"));

        UserSessionsEntity session = findSessionByRawToken(rawToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        String tokenFamily = session.getTokenFamily();
        if (session.getIsRevoked()) {
            revokeEntireFamily(tokenFamily);
            clearRefreshCookie(response);
            throw new RefreshTokenExpiredException("Refresh token reuse detected");
        }

        revokeSession(session);

        UserEntity user = session.getUserEntity();
        String newRawRefreshToken = generateSecureToken();
        String newAccessToken    = jwtService.generateAccessToken(user);

        UserSessionsEntity newSession = UserSessionsEntity.builder()
                .userEntity(user)
                .refreshTokenHash(passwordEncoder.encode(newRawRefreshToken))
                .tokenFamily(tokenFamily) // same family to link them together
                .deviceName(session.getDeviceName())
                .deviceType(session.getDeviceType())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .build();

        userSessionsRepository.save(newSession);

        writeRefreshCookie(response, newRawRefreshToken);

        return new RefreshResponse()
                .accessToken(newAccessToken);
    }

    private Optional<UserSessionsEntity> findSessionByRawToken(String rawToken) {
        return userSessionsRepository.findByIsRevokedFalse()
                .stream()
                .filter(s -> passwordEncoder.matches(rawToken, s.getRefreshTokenHash()))
                .findFirst();
    }

    private void revokeEntireFamily(String tokenFamily) {
        List<UserSessionsEntity> familySessions =
                userSessionsRepository.findByIsRevokedFalseAndTokenFamily(tokenFamily);

        familySessions.forEach(s -> {
            s.setIsRevoked(true);
            s.setRevokedAt(OffsetDateTime.now());
            s.setRevokedReason("Token reuse detected");
        });

        userSessionsRepository.saveAll(familySessions);
    }

    private void revokeSession(UserSessionsEntity session) {
        session.setIsRevoked(true);
        session.setRevokedAt(OffsetDateTime.now());
        session.setRevokedReason("rotated");
        userSessionsRepository.save(session);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void writeRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")   // scope cookie to refresh endpoint only
                .maxAge(Duration.ofDays(refreshTokenExpiryDays))
                .domain(cookieDomain)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ZERO)          // immediate expiry
                .domain(cookieDomain)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static UserEntity getUserEntity(EmailVerificationTokenEntity emailVerificationTokenEntity) {
    UserEntity user = emailVerificationTokenEntity.getUser();

    if (user == null) {
        throw new UserNotFoundException("User not found");
    }

    if (user.isEmailVerified()) {
        throw new EmailAlreadyVerifiedException("Email is already verified");
    }

    user.setEmailVerified(true);
    return user;
}

}