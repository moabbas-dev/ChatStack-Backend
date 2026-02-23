package com.api.chatstack.entities.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "t_password_reset_tokens")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetTokensEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    private Boolean used = false;

    @Column(name = "created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime expiresAt;

    @PrePersist
    protected void onCreate() { createdAt = OffsetDateTime.now(); }
}
