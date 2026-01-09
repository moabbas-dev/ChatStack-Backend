package com.api.chatstack.controllers;

import com.api.chatstack.services.AuthenticationService;
import com.chatstack.api.AuthenticationFlowApi;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat/stack/api/v1")
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
    public ResponseEntity<User> authSignup(SignupRequest signupRequest) {
        try {
            User createdUser = authService.signup(signupRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (MessagingException | IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<Void> authVerifyEmail(AuthVerifyEmailRequest authVerifyEmailRequest) {
        return null;
    }
}
