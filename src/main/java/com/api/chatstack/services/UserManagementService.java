package com.api.chatstack.services;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface UserManagementService {

    Resource getUserAvatar(UUID id, String avatar);
}
