package com.api.chatstack.mappers;

import com.chatstack.dto.AuthResponse;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;

@Builder
@Getter
public class AuthServiceResult {
    private final AuthResponse authResponse;
    private final ResponseCookie refreshCookie;
}