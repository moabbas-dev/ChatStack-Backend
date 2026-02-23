package com.api.chatstack.services;

import com.chatstack.dto.*;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface AuthenticationService {

    void verifyEmail(String token);

    AuthResponse signup(SignupRequest signupRequest) throws MessagingException, IOException;

    AuthResponse login(PasswordLoginRequest loginRequest);

    void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException;
}
