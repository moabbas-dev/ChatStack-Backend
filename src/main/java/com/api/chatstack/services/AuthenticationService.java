package com.api.chatstack.services;

import com.api.chatstack.config.JwtService;
import com.api.chatstack.entities.EmailVerificationTokenEntity;
import com.api.chatstack.entities.UserEntity;
import com.api.chatstack.enums.Role;
import com.api.chatstack.exception.ChatStackException;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.repositories.EmailVerificationTokenRepository;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.utils.Validation;
import com.chatstack.dto.*;
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
        if (token == null || token.trim().isEmpty()) {
            throw new ChatStackException("No Token Provided",
                    "NO_TOKEN_EXIST",
                    "No Token provided",
                    HttpStatus.BAD_REQUEST);
        }

        EmailVerificationTokenEntity emailVerificationTokenEntity = emailVerificationTokenRepository.
                findByVerificationToken(token).orElseThrow(() ->
                    new ChatStackException("Invalid Token",
                    "INVALID_VERIFICATION_LINK",
                    "Invalid verification link",
                    HttpStatus.BAD_REQUEST));

        if (emailVerificationTokenEntity.isUsed()) {
            throw new ChatStackException("Token is already used",
                    "TOKEN_ALREADY_USED",
                    "Token is already used",
                    HttpStatus.BAD_REQUEST);
        }

        if (emailVerificationTokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ChatStackException("Expired Token",
                    "TOKEN_EXPIRED",
                    "Token is expired",
                    HttpStatus.GONE);
        }

        UserEntity user = getUserEntity(emailVerificationTokenEntity);
        emailVerificationTokenEntity.setUsed(Boolean.TRUE);

        emailVerificationTokenRepository.save(emailVerificationTokenEntity);
        userRepository.save(user);
    }

    private static UserEntity getUserEntity(EmailVerificationTokenEntity emailVerificationTokenEntity) {
        UserEntity user = emailVerificationTokenEntity.getUser();

        if (user == null) {
            throw new ChatStackException("User not found",
                    "USER_NOT_FOUND",
                    "No User found for the provided token",
                    HttpStatus.BAD_REQUEST);
        }

        if (user.isEmailVerified()) {
            throw new ChatStackException("Email already verified",
                    "EMAIL_ALREADY_VERIFIED",
                    "Your email is already verified",
                    HttpStatus.BAD_REQUEST);
        }

        user.setEmailVerified(true);
        return user;
    }

    public AuthResult signup(SignupRequest signupRequest) throws MessagingException, IOException {
        if (userRepository.existsByEmail(signupRequest.getEmail()) || userRepository.existsByDisplayName(signupRequest.getDisplayName())) {
            throw new ChatStackException("User Already Exists",
                    "DUPLICATE_EMAIL",
                    "User with email " + signupRequest.getEmail() + " already exists",
                    HttpStatus.CONFLICT);
        }

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
                .refreshToken(refreshToken)
                .user(userMapper.toDto(userEntity));
    }

    public AuthResult login(LoginRequest loginRequest) {
        User userDTO = null;
        String accessToken = "";
        String refreshToken = "";


        if (loginRequest == null || loginRequest.getLoginType() == null) {
            throw new ChatStackException("Login Type Not Provided",
                    "INCORRECT_LOGIN_TYPE",
                    "Login type not provided",
                    HttpStatus.BAD_REQUEST);
        }
        else if (loginRequest instanceof PasswordLogin passwordLogin) {
            String email = passwordLogin.getEmail();
            String password = passwordLogin.getPassword();

            if (!Validation.isEmailValid(email) || !Validation.isPasswordValid(password)) {
                return null;
            }

            if (passwordLogin.getLoginType() == null || passwordLogin.getLoginType().trim().isEmpty())
                throw new ChatStackException("Login type is not provided",
                        "LOGIN_TYPE_NOT_PROVIDED",
                        "Login type is not supported",
                        HttpStatus.BAD_REQUEST);

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            UserEntity user = userRepository.findByEmail(email).orElseThrow(() ->
                    new ChatStackException("User not found",
                    "INVALID_CREDENTIALS",
                    "Invalid Credentials",
                    HttpStatus.UNAUTHORIZED));

            if (!passwordEncoder.matches(password, user.getPasswordHashed())) {
                throw new ChatStackException("password does not match",
                        "INVALID_CREDENTIALS",
                        "Invalid Credentials",
                        HttpStatus.UNAUTHORIZED);
            }

            if (!user.isEmailVerified()) {
                throw new ChatStackException("email is not verified",
                        "EMAIL_NOT_VERIFIED",
                        "Your Email is not verified",
                        HttpStatus.FORBIDDEN);
            }

            user.setLastSeenAt(OffsetDateTime.now());
            user.setStatus(User.StatusEnum.ONLINE);
            userRepository.save(user);

            accessToken = jwtService.generateAccessToken(user);
            refreshToken = jwtService.generateRefreshToken(user);
            userDTO = userMapper.toDto(user);
        } else if (loginRequest instanceof ProviderLogin providerLogin) {
            if (providerLogin.getLoginType() == null || providerLogin.getLoginType().trim().isEmpty())
                throw new ChatStackException("Login type is not provided",
                        "LOGIN_TYPE_NOT_PROVIDED",
                        "Login type is not supported",
                        HttpStatus.BAD_REQUEST);
        } else {
            throw new ChatStackException("Login Type Not Supported",
                    "INCORRECT_LOGIN_TYPE",
                    "Login type is not supported",
                    HttpStatus.BAD_REQUEST);
        }
        return new AuthResult().accessToken(accessToken).refreshToken(refreshToken).user(userDTO);
    }

    public void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException {
        UserEntity user = userRepository.findByEmail(authResendVerificationRequest.getEmail()).orElseThrow(() ->
                new ChatStackException("User not found",
                        "INVALID_CREDENTIALS",
                        "Invalid Credentials",
                        HttpStatus.NOT_FOUND));

        mailSender.sendVerificationEmail(user);
    }
}
