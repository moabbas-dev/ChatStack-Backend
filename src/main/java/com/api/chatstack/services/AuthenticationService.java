package com.api.chatstack.services;

import com.api.chatstack.mappers.AuthServiceResult;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface AuthenticationService {

    void verifyEmail(String token);

    AuthServiceResult signup(SignupRequest signupRequest) throws MessagingException, IOException;

    AuthServiceResult login(PasswordLoginRequest loginRequest) throws IOException;

    void logout(AuthLogoutRequest logoutRequest);

    void resetPassword(AuthResetPasswordRequest resetPasswordRequest);

    void forgotPassword(AuthResendVerificationRequest forgotPasswordRequest) throws MessagingException, IOException;

    void changePassword(AuthChangePasswordRequest resetPasswordRequest);

    void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException;

    RefreshResponse refreshToken();
}
