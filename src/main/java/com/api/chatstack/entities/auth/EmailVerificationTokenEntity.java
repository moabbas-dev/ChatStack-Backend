package com.api.chatstack.entities.auth;

import jakarta.persistence.*;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_email_verification_tokens")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EmailVerificationTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verification_token")
    private String verificationToken;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime expiresAt;

}
