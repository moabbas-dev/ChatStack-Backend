package com.api.chatstack.services.Impl;

import com.api.chatstack.config.JwtService;
import com.api.chatstack.entities.auth.EmailVerificationTokenEntity;
import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.exceptions.*;
import com.api.chatstack.mappers.AuthServiceResult;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.repositories.EmailVerificationTokenRepository;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.services.AuthenticationService;
import com.api.chatstack.services.MailService;
import com.api.chatstack.utils.ValidationUtils;
import com.chatstack.dto.*;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailSender;
    @Value("${app.base-url}")
    private String baseUrl;

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
                .role(AdminUpdateUserRequest.RoleEnum.USER)
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

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(false) // true in production
                .secure(true)
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

    }

    @Override
    public void resetPassword(AuthResetPasswordRequest resetPasswordRequest) {
        String newPassword = resetPasswordRequest.getNewPassword();

        UserEntity user = userRepository.findByEmail(resetPasswordRequest.getEmail())
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
        String email = forgotPasswordRequest.getEmail();

        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException("User not found");
        }

        mailSender.sendPasswordResetEmail(email);
    }

    @Override
    public void changePassword(AuthChangePasswordRequest resetPasswordRequest) {

    }

    @Override
    public void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException {
        UserEntity user = userRepository.findByEmail(authResendVerificationRequest.getEmail()).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        mailSender.sendVerificationEmail(user);
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

    private boolean isPasswordTooSimilar(String oldPassword, String newPassword) {
        int distance = levenshteinDistance(oldPassword, newPassword);
        double similarity = 1.0 - ((double) distance / Math.max(oldPassword.length(), newPassword.length()));
        return similarity > 0.7; // 70% similarity threshold
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }
}