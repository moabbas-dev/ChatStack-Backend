package com.api.chatstack.mappers;

import com.api.chatstack.entities.UserEntity;
import com.chatstack.dto.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper implements Mapper<User, UserEntity> {
    @Override
    public User toDto(UserEntity entity) {
        return new User()
                .fullname(entity.getFullName())
                .id(entity.getId())
//                .passwordHashed(entity.getPasswordHashed())
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
}
