package com.api.chatstack.services;

import com.api.chatstack.config.JwtService;
import com.api.chatstack.entities.EmailVerificationTokenEntity;
import com.api.chatstack.entities.UserEntity;
import com.api.chatstack.enums.Role;
import com.api.chatstack.exception.*;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.repositories.EmailVerificationTokenRepository;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.utils.Validation;
import com.chatstack.dto.*;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailSender;
    @Value("${app.base-url}")
    private String baseUrl;

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

    public AuthResult signup(SignupRequest signupRequest) throws MessagingException, IOException {
        if (!Validation.isPasswordValid(signupRequest.getPassword())
        || !Validation.isEmailValid(signupRequest.getEmail())
        || !Validation.isUsernameValid(signupRequest.getDisplayName())) {
            return null;
        }

        String fullname = Validation.fullnameValidation(signupRequest.getFullname());

        UserEntity user = UserEntity.builder()
                .fullName(fullname)
                .displayName(signupRequest.getDisplayName())
                .email(signupRequest.getEmail())
                .emailVerified(false)
                .passwordHashed(passwordEncoder.encode(signupRequest.getPassword()))
                .role(Role.USER)
                .status(User.StatusEnum.OFFLINE)
                .createdAt(OffsetDateTime.now())
                .lastSeenAt(OffsetDateTime.now())
                .timezone(ZoneId.systemDefault().toString())
                .build();

        UserEntity userEntity = userRepository.save(user);
        userEntity.setAvatarUrl(baseUrl + "chat-stack/api/v1/users/" + user.getId() + "/avatar/default.png");

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);


        mailSender.sendVerificationEmail(userEntity);

        return new AuthResult()
                .accessToken(accessToken)
                .user(userMapper.toDto(userEntity));
    }

    public AuthResult login(PasswordLoginRequest loginRequest) {
        User userDTO = null;
        String accessToken = "";
        String refreshToken = "";

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        if (!Validation.isEmailValid(email)) {
            throw new InvalidEmailException("Your Email is invalid");
        }

        if (!Validation.isPasswordValid(password)) {
            throw new InvalidPasswordException("Your Password is invalid");
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        UserEntity user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new UnverifiedEmailException("Your email is not verified. Please verify your email before logging in.");
        }

        user.setLastSeenAt(OffsetDateTime.now());
        user.setStatus(User.StatusEnum.ONLINE);
        userRepository.save(user);

        accessToken = jwtService.generateAccessToken(user);
        refreshToken = jwtService.generateRefreshToken(user);
        userDTO = userMapper.toDto(user);
        return new AuthResult().accessToken(accessToken).user(userDTO);
    }

    public void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException {
        UserEntity user = userRepository.findByEmail(authResendVerificationRequest.getEmail()).orElseThrow(() ->
                new UserNotFoundException("User not found"));

        mailSender.sendVerificationEmail(user);
    }
}
