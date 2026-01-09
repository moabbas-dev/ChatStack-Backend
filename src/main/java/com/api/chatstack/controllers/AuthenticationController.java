package com.api.chatstack.controllers;

import com.api.chatstack.services.AuthenticationService;
import com.chatstack.api.AuthenticationFlowApi;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationFlowApi {

    private final AuthenticationService authService;

    @Override
    public ResponseEntity<Void> authChangePassword(AuthChangePasswordRequest authChangePasswordRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> authForgotPassword(AuthResendVerificationRequest authResendVerificationRequest) {
        return null;
    }

    @Override
    public ResponseEntity<User> authLogin(LoginRequest loginRequest) {
        return null;
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
        return null;
    }

    @Override
    public ResponseEntity<Void> authResetPassword(AuthResetPasswordRequest authResetPasswordRequest) {
        return null;
    }

    @Override
    public ResponseEntity<User> authSignup(SignupRequest signupRequest) throws MessagingException, IOException {
        User createdUser = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Override
    public ResponseEntity<Void> authVerifyEmail(AuthVerifyEmailRequest authVerifyEmailRequest) {
        return null;
    }
}
