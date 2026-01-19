package com.api.chatstack.controllers;

import com.api.chatstack.services.UserManagementService;
import com.chatstack.api.UsersManagementApi;
import com.chatstack.dto.SetCurrentUserRequest;
import com.chatstack.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat-stack/api/v1")
public class UserManagementController implements UsersManagementApi {
    private final UserManagementService userManagementService;

    @Override
    public ResponseEntity<User> deleteCurrentUser() {
        return null;
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        return null;
    }

    @Override
    public ResponseEntity<User> getCurrentUser(UUID id) {
        return null;
    }

    @Override
    public ResponseEntity<Resource> getUserAvatar(UUID id, String avatar) {
        Resource userAvatar = userManagementService.getUserAvatar(id, avatar);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(userAvatar);
    }

    @Override
    public ResponseEntity<User> getUserById(UUID id) {
        return null;
    }

    @Override
    public ResponseEntity<User> setCurrentUser(SetCurrentUserRequest setCurrentUserRequest) {
        return null;
    }

    @Override
    public ResponseEntity<User> setUserData(UUID id) {
        return null;
    }
}
