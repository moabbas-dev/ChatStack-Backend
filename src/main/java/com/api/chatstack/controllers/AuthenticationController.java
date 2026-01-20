package com.api.chatstack.controllers;

import com.api.chatstack.entities.UserEntity;
import com.api.chatstack.enums.LoginType;
import com.api.chatstack.exception.ChatStackException;
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
@RequestMapping("/chat-stack/api/v1")
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
//    login request either: password or provider
    public ResponseEntity<User> authLogin(LoginRequest loginRequest) {
        User loggedUser = authService.login(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(loggedUser);
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
        authService.verifyEmail(authVerifyEmailRequest.getToken());
        return ResponseEntity.noContent().build();
    }
}
