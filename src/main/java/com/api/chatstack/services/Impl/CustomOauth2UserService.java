package com.api.chatstack.services.Impl;

import com.api.chatstack.config.ClientRequestContext;
import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.entities.auth.UserSessionsEntity;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.repositories.UserSessionsRepository;
import com.chatstack.dto.AdminUpdateUserRequest;
import com.chatstack.dto.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRequestContext clientContext;
    private final UserSessionsRepository userSessionsRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = UserEntity.builder()
                    .fullName(oAuth2User.getAttribute("name"))
                    .displayName(generateDisplayName(Objects.requireNonNull(oAuth2User.getAttribute("name"))))
                    .email(email)
                    .emailVerified(true)
                    .passwordHashed(null)
                    .role(AdminUpdateUserRequest.RoleEnum.USER)
                    .status(User.StatusEnum.ONLINE)
                    .lastSeenAt(OffsetDateTime.now())
                    .timezone(ZoneId.systemDefault().toString())
                    .build();
            UserEntity saved = userRepository.save(newUser);
            String pictureUrl = oAuth2User.getAttribute("picture");
            if (pictureUrl != null && !pictureUrl.isBlank()) {
                saved.setAvatarUrl(pictureUrl);
            } else {
                saved.setAvatarUrl("http://localhost:8080/chat-stack/api/v1/users/" + saved.getId() + "/avatar/default.png");
            }
            return saved;
        });

        UserSessionsEntity session = UserSessionsEntity.builder()
                .userEntity(user)
                .deviceType(clientContext.getDeviceType())
                .deviceName(clientContext.extractDeviceName(clientContext.getUserAgent()))
                .ipAddress(clientContext.getClientIp())
                .userAgent(clientContext.getUserAgent())
                .tokenFamily("")
                .revokedReason("")
                .isRevoked(false)
                .revokedAt(null)
                .refreshTokenHash("")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        userSessionsRepository.save(session);

        return oAuth2User;
    }

    private String generateDisplayName(String fullName) {
        String base = fullName.replaceAll("\\s+", "").toLowerCase();
        String name = base;
        int counter = 1;
        while (userRepository.existsByDisplayName(name)) {
            name = base + counter++;
        }
        return name;
    }
}
