package com.api.chatstack.controllers;

import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.services.AuthenticationService;
import com.chatstack.api.AuthenticationFlowApi;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@RestController
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
    public ResponseEntity<AuthResponse> authLogin(PasswordLoginRequest passwordLoginRequest) {
        AuthResponse result = authService.login(passwordLoginRequest);
        ResponseCookie refreshCookie = ResponseCookie.from(
                        "refreshToken",
                        result.getAccessToken()
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
    public ResponseEntity<Void> authLogout(AuthLogoutRequest authLogoutRequest) {
        return null;
    }

    @Override
    public ResponseEntity<AuthResponse> authOAuth2Login(String provider, OAuth2LoginRequest oauth2LoginRequest) {
        return null;
    }

    @Override
    public ResponseEntity<RefreshResponse> authRefreshToken() {
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
    public ResponseEntity<SignupResponse> authSignup(SignupRequest signupRequest) {
        try {
            AuthResponse result = authService.signup(signupRequest);
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.getAccessToken())
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
                    .body(new SignupResponse().accessToken(result.getAccessToken()).user(result.getUser()));
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
