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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRequestContext clientContext;
    private final UserSessionsRepository userSessionsRepository;
    private final RestTemplate restTemplate;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            String displayName = generateDisplayName(Objects.requireNonNull(oAuth2User.getAttribute("name")));
            UserEntity newUser = UserEntity.builder()
                    .fullName(oAuth2User.getAttribute("name"))
                    .displayName(displayName)
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
            pictureUrl = pictureUrl != null ? pictureUrl : oAuth2User.getAttribute("avatar_url");

            if (pictureUrl != null && !pictureUrl.isBlank()) {
//                saved.setAvatarUrl(pictureUrl);
                pictureUrl = savePictureUrlInResources(pictureUrl, newUser.getId().toString(), displayName);
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

    private String savePictureUrlInResources(String pictureUrl, String id, String displayName) {
        byte[] imageBytes = restTemplate.getForObject(pictureUrl, byte[].class);
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String filename = displayName + ".png";
            Path filePath = uploadPath.resolve(filename);

            assert imageBytes != null;
            Files.write(filePath, imageBytes);

            return "http://localhost:8080/chat-stack/api/v1/users/" + id + "/avatar/" + filename;
        } catch (IOException ex) {
            ex.printStackTrace();
            return "http://localhost:8080/chat-stack/api/v1/users/avatar/default.png";
        }
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
