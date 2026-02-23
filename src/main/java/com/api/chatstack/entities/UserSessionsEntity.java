package com.api.chatstack.entities;

import com.chatstack.dto.Session;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "user_sessions")
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSessionsEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "token_family", nullable = false)
    private String tokenFamily;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Session.DeviceTypeEnum deviceType;

    @Column(name = "ip_address", nullable = false)
    private InetAddress ipAddress;

    @Column(name = "user_agent", nullable = false)
    private String userAgent;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked;

    @Column(name = "revoked_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "created_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
