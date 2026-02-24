package com.api.chatstack.services.Impl;

import com.api.chatstack.exceptions.ChatStackException;
import com.api.chatstack.exceptions.UserNotFoundException;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.services.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;

    @Override
    public Resource getUserAvatar(UUID id, String avatar) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Only allow default avatar for now
        if (!"default.png".equals(avatar)) {
            throw new ChatStackException(
                    "Avatar not found",
                    "AVATAR_NOT_FOUND",
                    "The requested avatar does not exist for this user.",
                    HttpStatus.NOT_FOUND
            );
        }

        Resource resource = new ClassPathResource("avatars/default.png");

        if (!resource.exists()) {
            throw new ChatStackException(
                    "Avatar unavailable",
                    "DEFAULT_AVATAR_MISSING",
                    "The default avatar resource is missing from the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        return resource;
    }
}