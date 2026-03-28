package com.api.chatstack.services;

import com.api.chatstack.exceptions.InvalidVerificationLinkException;
import com.api.chatstack.exceptions.NoTokenProvidedException;
import com.api.chatstack.exceptions.TokenExpiredException;
import com.api.chatstack.mappers.AuthServiceResult;
import com.chatstack.dto.*;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface AuthenticationService {

    /**
     * Verifies a user's email address using a provided verification token.
     *
     * @param token The unique verification token sent to the user's email.
     * @throws NoTokenProvidedException if the token is null or empty.
     * @throws InvalidVerificationLinkException if the token does not exist in the database.
     * @throws TokenExpiredException if the token has already been used or has expired.
     */
    void verifyEmail(String token);

    /**
     * Registers a new user in the system and sends a verification email.
     *
     * @param signupRequest The registration details including email, username, fullname, and password.
     * @return AuthServiceResult containing the registered user's details and status.
     * @throws MessagingException If there is an error sending the verification email.
     * @throws IOException If an error occurs during data processing.
     */
    AuthServiceResult signup(SignupRequest signupRequest) throws MessagingException, IOException;

    /**
     * Authenticates a user using their credentials.
     *
     * @param loginRequest The login credentials (email and password).
     * @return AuthServiceResult containing authentication tokens and user information.
     * @throws IOException If an error occurs during the authentication process.
     */
    AuthServiceResult login(PasswordLoginRequest loginRequest) throws IOException;

    /**
     * Logs out the user by invalidating their current session.
     *
     * @param logoutRequest The request containing the sessionId and
     *                      allDevices boolean (if the user preferred to log out from all devices)
     *                      identifier to be invalidated.
     */
    void logout(AuthLogoutRequest logoutRequest);

    /**
     * Resets a user's password using a verification token.
     *
     * @param resetPasswordRequest The request containing the new password.
     */
    void resetPassword(AuthResetPasswordRequest resetPasswordRequest);

    /**
     * Initiates the forgot password process by sending a reset link to the user's email.
     *
     * @param forgotPasswordRequest The request containing the user's email address.
     * @throws MessagingException If there is an error sending the reset email.
     * @throws IOException If an error occurs during data processing.
     */
    void forgotPassword(AuthResendVerificationRequest forgotPasswordRequest) throws MessagingException, IOException;

    /**
     * Changes the password for an authenticated user.
     *
     * @param resetPasswordRequest The request containing the old and new passwords.
     */
    void changePassword(AuthChangePasswordRequest resetPasswordRequest);

    /**
     * Resends the email verification link to the user.
     *
     * @param authResendVerificationRequest The request containing the user's email.
     * @throws MessagingException If there is an error sending the verification email.
     * @throws IOException If an error occurs during data processing.
     */
    void resendVerification(AuthResendVerificationRequest authResendVerificationRequest) throws MessagingException, IOException;

    /**
     * Refreshes the authentication tokens using a valid refresh token.
     *
     * @return RefreshResponse containing the new access token.
     */
    RefreshResponse refreshToken();
}
