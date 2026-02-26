package com.api.chatstack.services.Impl;

import com.api.chatstack.exceptions.ChatStackException;
import com.api.chatstack.exceptions.UserNotFoundException;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.services.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
            e.printStackTrace();
            throw new ChatStackException(
                    "Avatar unavailable",
                    "AVATAR_URL_MALFORMED",
                    "The avatar URL is malformed and cannot be accessed.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (resource.exists() && resource.isReadable()) {
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

        return defaultAvatar;
    }
}