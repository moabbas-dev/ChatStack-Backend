package com.api.chatstack.services.impl;

import com.api.chatstack.entities.auth.EmailVerificationTokenEntity;
import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.repositories.EmailVerificationTokenRepository;
import com.api.chatstack.services.MailService;
import com.api.chatstack.utils.FileLoaderUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendPlainText(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    @Override
    public EmailVerificationTokenEntity generateVerificationToken(UserEntity user) {
        EmailVerificationTokenEntity entity = EmailVerificationTokenEntity.builder()
                .user(user)
                .verificationToken(UUID.randomUUID().toString())
                .used(false)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        return emailVerificationTokenRepository.save(entity);
    }

    @Override
    public void sendVerificationEmail(UserEntity user) throws MessagingException, IOException {
        String html = FileLoaderUtil.loadHtmlTemplate("/templates/email/welcome.html");
        html = html.replace("{{fullname}}", user.getFullName());
        html = html.replace("{{displayName}}", user.getDisplayName());

        EmailVerificationTokenEntity token = generateVerificationToken(user);
        String verificationLink = baseUrl + "chat-stack/api/v1/auth/verify-email?token=" + token.getVerificationToken();

        String emailContent = html.replace("{{verification_link}}", verificationLink);
        sendHtml(user.getEmail(), "Chat Stack - Email Activation", emailContent);
    }

    @Override
    public void sendPasswordResetEmail(String email) throws MessagingException, IOException {
        String html = FileLoaderUtil.loadHtmlTemplate("/templates/email/password-reset.html");
        String resetLink = baseUrl + "chat-stack/api/v1/auth/reset-password?email=" + email;
        String emailContent = html.replace("{{reset_link}}", resetLink);
        sendHtml(email, "Chat Stack - Password Reset", emailContent);
    }
}