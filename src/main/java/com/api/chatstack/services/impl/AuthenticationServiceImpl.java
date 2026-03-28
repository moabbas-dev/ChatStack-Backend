package com.api.chatstack.services.impl;

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
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
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

        UserEntity user = emailVerificationTokenEntity.getUser();
        if (user == null) {
            throw new UserNotFoundException("User not found for the given token");
        }

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenEntity.setUsed(Boolean.TRUE);
        emailVerificationTokenRepository.save(emailVerificationTokenEntity);

        log.info("User {} verified their email successfully", user.getEmail());
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

        UserEntity user = userMapper.populateUserEntityFromSignupRequest(signupRequest);

        UserEntity userEntity = userRepository.save(user);
        userEntity.setAvatarUrl(baseUrl + "chat-stack/api/v1/users/" + user.getId() + "/avatar/default.png");

        String refreshToken = jwtService.generateRefreshToken(user);

        mailSender.sendVerificationEmail(userEntity);

        log.info("New user {} signed up successfully with email {}", user.getDisplayName(), user.getEmail());
        return buildAuthResult(user, refreshToken);
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

        String refreshToken = jwtService.generateRefreshToken(user);
        UserSessionsEntity session = userMapper.populateUserSessionEntityForLogin(user, refreshToken);
        userSessionsRepository.save(session);

        log.info("User {} logged in successfully from IP {} (session {})", user.getEmail(), clientIp, session.getId());
        return buildAuthResult(user, refreshToken);
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
        log.info("User logged out successfully (allDevices={}, sessionId={})", logoutRequest.getAllDevices(), logoutRequest.getSessionId());
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
        log.info("User {} reset their password successfully", user.getEmail());
    }

    @Override
    public void forgotPassword(AuthResendVerificationRequest forgotPasswordRequest) throws MessagingException, IOException {
        String userEmail = forgotPasswordRequest.getEmail();

        if (!userRepository.existsByEmail(userEmail)) {
            log.info("Password reset requested for non-existing user: {}", userEmail);
            return;
        }

        mailSender.sendPasswordResetEmail(userEmail);
        log.info("Sent password reset email to {}", userEmail);
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
        log.info("User {} changed their password successfully", user.getEmail());
    }

    @Override
    public void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException {
        UserEntity user = userRepository.findByEmail(authResendVerificationRequest.getEmail()).orElseThrow(() ->
                new UserNotFoundException("User not found"));
        mailSender.sendVerificationEmail(user);
        log.info("Resent verification email to {}", user.getEmail());
    }

    @Override
    public RefreshResponse refreshToken() {
        HttpServletRequest request = clientContext.getRequest();

        String rawToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new MissingRefreshTokenException("Refresh token cookie not found"));

        UserSessionsEntity session = findSessionByRawToken(rawToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        String tokenFamily = session.getTokenFamily();
        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            revokeEntireFamily(tokenFamily);
            clearRefreshCookie(response);
            throw new RefreshTokenExpiredException("Refresh token reuse detected");
        }

        revokeSession(session);

        UserEntity user = session.getUserEntity();
        String newRawRefreshToken = generateSecureToken();
        String newAccessToken    = jwtService.generateAccessToken(user);

        UserSessionsEntity newSession = userMapper.populateUserSessionEntityForRefresh(session, user, newRawRefreshToken);
        userSessionsRepository.save(newSession);

        writeRefreshCookie(response, newRawRefreshToken);
        log.info("Issued new access token and refresh token for user {} (session {})", user.getEmail(), newSession.getId());
        return new RefreshResponse()
                .accessToken(newAccessToken);
    }

    private AuthServiceResult buildAuthResult(UserEntity user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user);

        AuthResponse authResponse = new AuthResponse()
                .accessToken(accessToken)
                .user(userMapper.toDto(user));

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // true in production
                .path("/chat-stack/api/v1/auth/refresh-token") // must match your refresh endpoint exactly
                .maxAge(Duration.ofDays(refreshTokenExpiryDays))
                .sameSite("Strict")
                .domain(cookieDomain)
                .build();

        return AuthServiceResult.builder()
                .authResponse(authResponse)
                .refreshCookie(refreshCookie)
                .build();
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
                .secure(false) // true in production
                .sameSite("Strict")
                .path("/chat-stack/api/v1/auth/refresh-token")   // scope cookie to refresh endpoint only
                .maxAge(Duration.ofDays(refreshTokenExpiryDays))
                .domain(cookieDomain)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // true in production
                .sameSite("Strict")
                .path("/chat-stack/api/v1/auth/refresh-token")
                .maxAge(Duration.ZERO)          // immediate expiry
                .domain(cookieDomain)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void revokeSingleSession(String rawToken, UUID targetSessionId) {
        UserSessionsEntity currentSession = userSessionsRepository.findAllByIsRevokedFalse()
                .stream()
                .filter(s -> passwordEncoder.matches(rawToken, s.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        UserSessionsEntity targetSession = userSessionsRepository.findById(targetSessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));

        if (Boolean.TRUE.equals(targetSession.getIsRevoked())) {
            throw new InvalidRefreshTokenException("Session already revoked");
        }

        if (!targetSession.getUserEntity().getId().equals(currentSession.getUserEntity().getId())) {
            throw new UnauthorizedException("Cannot revoke another user's session");
        }

        targetSession.setIsRevoked(true);
        targetSession.setRevokedAt(OffsetDateTime.now());
        targetSession.setRevokedReason("logout");
        userSessionsRepository.save(targetSession);
        log.info("Revoked session {} for user {}", targetSession.getId(), currentSession.getUserEntity().getEmail());
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
        log.info("Revoked all sessions for user {}", currentSession.getUserEntity().getEmail());
    }

}