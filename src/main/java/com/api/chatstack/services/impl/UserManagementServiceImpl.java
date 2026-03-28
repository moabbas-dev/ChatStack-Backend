package com.api.chatstack.services.impl;

import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.exceptions.ChatStackException;
import com.api.chatstack.exceptions.PermissionDeniedException;
import com.api.chatstack.exceptions.UserNotFoundException;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.services.UserManagementService;
import com.chatstack.dto.AdminUpdateUserRequest;
import com.chatstack.dto.User;
import com.chatstack.dto.UserListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;

    @Override
    public Resource getUserAvatar(UUID id, String avatar) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Path avatarPath = Paths.get("uploads/avatars/" + avatar);

        Resource resource;
        try {
            resource = new UrlResource(avatarPath.toUri());
        } catch (MalformedURLException e) {
            log.error("message: {}, stackTrace: {}", e.getMessage(), e.getStackTrace());
            throw new ChatStackException(
                    "Avatar unavailable",
                    "AVATAR_URL_MALFORMED",
                    "The avatar URL is malformed and cannot be accessed.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (resource.exists() && resource.isReadable()) {
            log.info("user avatar found: {}", avatar);
            return resource;
        }

        Resource defaultAvatar = new ClassPathResource("avatars/default.png");

        if (!defaultAvatar.exists()) {
            throw new ChatStackException(
                    "Avatar unavailable",
                    "DEFAULT_AVATAR_MISSING",
                    "The default avatar resource is missing from the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        log.info("User {} requested avatar {}, but it was not found. Serving default avatar instead.", id, avatar);
        return defaultAvatar;
    }

    @Override
    public UserListResponse getAllUsers(Integer page, Integer size) {
        String userEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        UserEntity currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
        if (currentUser.getAuthorities().stream().noneMatch(auth ->
                Objects.equals(auth.getAuthority(), AdminUpdateUserRequest.RoleEnum.ADMIN.name()))
        ) {
            throw new PermissionDeniedException("You do not have permission to access this resource");
        }

        Page<UserEntity> pageResult = userRepository.findAll(PageRequest.of(page, size));
        List<UserEntity> users = pageResult.getContent();
        List<User> userDtos = users.stream()
                .map(user -> new User()
                        .id(user.getId())
                        .email(user.getEmail())
                        .displayName(user.getUsername())
                        .fullname(user.getFullName())
                        .status(user.getStatus())
                        .timezone(user.getTimezone())
                        .lastSeenAt(user.getLastSeenAt())
                        .avatarUrl(user.getAvatarUrl())
                        .emailVerified(user.isEmailVerified())
                        .createdAt(user.getCreatedAt())
                )
                .toList();
        log.info("Retrieved page {} of users with size {}. Total elements: {}, Total pages: {}",
                page, size, pageResult.getTotalElements(), pageResult.getTotalPages());
        return new UserListResponse()
                .page(page)
                .totalElements((int) pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .content(userDtos);
    }
}