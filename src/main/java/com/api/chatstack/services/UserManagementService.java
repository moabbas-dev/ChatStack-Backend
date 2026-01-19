package com.api.chatstack.services;

import com.api.chatstack.exception.ChatStackException;
import com.api.chatstack.repositories.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public Resource getUserAvatar(UUID id, String avatar) {
        userRepository.findById(id)
                .orElseThrow(() -> new ChatStackException(
                        "User not found",
                        "USER_NOT_FOUND",
                        "No user exists with the given ID.",
                        HttpStatus.NOT_FOUND
                ));

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
