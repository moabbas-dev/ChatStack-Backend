package com.api.chatstack.controllers;

import com.api.chatstack.mappers.AuthServiceResult;
import com.api.chatstack.services.AuthenticationService;
import com.chatstack.api.AuthenticationFlowApi;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
public class AuthenticationController implements AuthenticationFlowApi {

    private final AuthenticationService authService;

    @Override
    public ResponseEntity<Void> authChangePassword(AuthChangePasswordRequest authChangePasswordRequest) {
        authService.changePassword(authChangePasswordRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<Void> authForgotPassword(AuthResendVerificationRequest authResendVerificationRequest) {
        try {
            authService.forgotPassword(authResendVerificationRequest);
        } catch (MessagingException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<AuthResponse> authLogin(PasswordLoginRequest passwordLoginRequest) {
        AuthServiceResult result;
        try {
            result = authService.login(passwordLoginRequest);
        } catch (IOException e) {
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString());

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(result.getAuthResponse());
    }

    @Override
    public ResponseEntity<Void> authLogout(AuthLogoutRequest authLogoutRequest) {
        authService.logout(authLogoutRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<AuthResponse> authOAuth2Login(String provider, OAuth2LoginRequest oauth2LoginRequest) {
        return null;
    }

    @Override
    public ResponseEntity<RefreshResponse> authRefreshToken() {
        RefreshResponse response = authService.refreshToken();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> authResendVerification(AuthResendVerificationRequest authResendVerificationRequest) {
        try {
            authService.resendVerification(authResendVerificationRequest);
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> authResetPassword(AuthResetPasswordRequest authResetPasswordRequest) {
        authService.resetPassword(authResetPasswordRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<SignupResponse> authSignup(SignupRequest signupRequest) {
        try {
            AuthServiceResult result = authService.signup(signupRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString());

            SignupResponse response = new SignupResponse()
                    .accessToken(result.getAuthResponse().getAccessToken())
                    .user(result.getAuthResponse().getUser());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(headers)
                    .body(response);
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<Void> authVerifyEmail(String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }
}
