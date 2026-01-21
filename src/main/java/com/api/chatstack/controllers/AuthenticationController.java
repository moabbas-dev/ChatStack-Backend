package com.api.chatstack.controllers;

import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.services.AuthenticationService;
import com.chatstack.api.AuthenticationFlowApi;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat-stack/api/v1")
public class AuthenticationController implements AuthenticationFlowApi {

    private final AuthenticationService authService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<Void> authChangePassword(AuthChangePasswordRequest authChangePasswordRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> authForgotPassword(AuthResendVerificationRequest authResendVerificationRequest) {
        return null;
    }
    @Override
//    login request either: password or provider
    public ResponseEntity<AuthResponse> authLogin(LoginRequest loginRequest) {
        AuthResult result = authService.login(loginRequest);
        ResponseCookie refreshCookie = ResponseCookie.from(
                        "refreshToken",
                        result.getRefreshToken()
                )
                .httpOnly(true)
                .secure(false) // true in production (HTTPS)
                .sameSite("Lax")
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new AuthResponse().accessToken(result.getAccessToken()).user(result.getUser()));
    }

    @Override
    public ResponseEntity<Void> authLogout() {
        return null;
    }

    @Override
    public ResponseEntity<Void> authRefreshToken() {
        return null;
    }

    @Override
    public ResponseEntity<Void> authResendVerification(AuthResendVerificationRequest authResendVerificationRequest) {
        try {
            authService.resendVerification(authResendVerificationRequest);
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<Void> authResetPassword(AuthResetPasswordRequest authResetPasswordRequest) {
        return null;
    }

    @Override
    public ResponseEntity<AuthResponse> authSignup(SignupRequest signupRequest) {
        try {
            AuthResult result = authService.signup(signupRequest);
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                    .httpOnly(true)
                    .secure(false) // true in production (HTTPS)
                    .sameSite("Lax")
                    .path("/auth/refresh-token")
                    .maxAge(Duration.ofDays(7))
                    .build();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(headers)
                    .body(new AuthResponse().accessToken(result.getAccessToken()).user(result.getUser()));
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<Void> authVerifyEmail(AuthVerifyEmailRequest authVerifyEmailRequest) {
        authService.verifyEmail(authVerifyEmailRequest.getToken());
        return ResponseEntity.noContent().build();
    }
}
