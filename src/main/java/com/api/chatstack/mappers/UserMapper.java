package com.api.chatstack.mappers;

import com.api.chatstack.config.ClientRequestContext;
import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.entities.auth.UserSessionsEntity;
import com.api.chatstack.utils.ValidationUtils;
import com.chatstack.dto.AdminUpdateUserRequest;
import com.chatstack.dto.SignupRequest;
import com.chatstack.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;


@Component
@RequiredArgsConstructor
public class UserMapper implements Mapper<User, UserEntity> {

    private final PasswordEncoder passwordEncoder;
    private final ClientRequestContext clientContext;

    @Value("${app.auth.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    @Override
    public User toDto(UserEntity entity) {
        return new User()
                .fullname(entity.getFullName())
                .id(entity.getId())
//              .passwordHashed(entity.getPasswordHashed())
                .displayName(entity.getDisplayName())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .timezone(entity.getTimezone())
                .createdAt(entity.getCreatedAt())
                .emailVerified(entity.isEmailVerified())
                .lastSeenAt(entity.getLastSeenAt())
                .avatarUrl(entity.getAvatarUrl());
    }

    @Override
    public UserEntity toEntity(User dto) {
        return UserEntity.builder()
                .id(dto.getId())
                .email(dto.getEmail())
                .emailVerified(dto.getEmailVerified())
//                .passwordHashed(dto.getPasswordHashed())
                .fullName(dto.getFullname())
                .displayName(dto.getDisplayName())
                .status(dto.getStatus())
                .timezone(dto.getTimezone())
                .createdAt(dto.getCreatedAt())
                .avatarUrl(dto.getAvatarUrl())
                .lastSeenAt(dto.getLastSeenAt())
                .build();
    }

    public UserEntity populateUserEntityFromSignupRequest(SignupRequest signupRequest) {
        String fullname = ValidationUtils.validateAndNormalizeFullname(signupRequest.getFullname());

        return UserEntity.builder()
                .fullName(fullname)
                .displayName(signupRequest.getDisplayName())
                .email(signupRequest.getEmail())
                .emailVerified(false)
                .passwordHashed(passwordEncoder.encode(signupRequest.getPassword()))
                .role(AdminUpdateUserRequest.RoleEnum.USER)
                .status(User.StatusEnum.OFFLINE)
                .lastSeenAt(OffsetDateTime.now())
                .timezone(ZoneId.systemDefault().toString())
                .build();
    }

    public UserSessionsEntity populateUserSessionEntityForLogin(UserEntity user, String refreshToken) {
        return UserSessionsEntity.builder()
                .userEntity(user)
                .deviceType(clientContext.getDeviceType())
                .deviceName(clientContext.extractDeviceName(clientContext.getUserAgent()))
                .tokenFamily("")
                .revokedReason("")
                .isRevoked(false)
                .revokedAt(null)
                .refreshTokenHash(passwordEncoder.encode(refreshToken)) // Hash the token before storing
                .ipAddress(clientContext.getClientIp())
                .userAgent(clientContext.getUserAgent())
                .expiresAt(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .build();
    }

    public UserSessionsEntity populateUserSessionEntityForRefresh(UserSessionsEntity existingSession, UserEntity user, String newRawRefreshToken) {
        return UserSessionsEntity.builder()
                .userEntity(user)
                .refreshTokenHash(passwordEncoder.encode(newRawRefreshToken))
                .tokenFamily(existingSession.getTokenFamily()) // same family to link them together
                .deviceName(existingSession.getDeviceName())
                .deviceType(existingSession.getDeviceType())
                .ipAddress(existingSession.getIpAddress())
                .userAgent(existingSession.getUserAgent())
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .build();
    }
}
