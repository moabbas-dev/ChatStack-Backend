package com.api.chatstack.services;

import com.api.chatstack.entities.auth.EmailVerificationTokenEntity;
import com.api.chatstack.entities.auth.UserEntity;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface MailService {

    void sendPlainText(String to, String subject, String body);

    void sendHtml(String to, String subject, String htmlBody) throws MessagingException;

    EmailVerificationTokenEntity generateVerificationToken(UserEntity user);

    void sendVerificationEmail(UserEntity user) throws MessagingException, IOException;
}
