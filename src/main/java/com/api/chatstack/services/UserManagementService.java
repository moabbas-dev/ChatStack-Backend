package com.api.chatstack.services;

import com.chatstack.dto.UserListResponse;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface UserManagementService {

    Resource getUserAvatar(UUID id, String avatar);

    UserListResponse getAllUsers(Integer page, Integer size);
}
