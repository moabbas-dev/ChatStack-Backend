package com.api.chatstack.services;

import com.api.chatstack.entities.UserEntity;
import com.api.chatstack.enums.Role;
import com.api.chatstack.exception.ChatStackException;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.utils.Validation;
import com.chatstack.dto.SignupRequest;
import com.chatstack.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail()) || userRepository.existsByUsername(signupRequest.getDisplayName())) {
            throw new ChatStackException("User Already Exists",
                    "DUPLICATE_EMAIL",
                    "User with email " + signupRequest.getEmail() + " already exists",
                    HttpStatus.CONFLICT);
        }

        if (!Validation.isPasswordValid(signupRequest.getPassword())
        || !Validation.isEmailValid(signupRequest.getEmail())
        || !Validation.isUsernameValid(signupRequest.getDisplayName())) {
            return null;
        }

        String fullname = Validation.fullnameValidation(signupRequest.getFullname());

        UserEntity user = UserEntity.builder()
                .fullName(fullname)
                .displayName(signupRequest.getDisplayName())
                .email(signupRequest.getEmail())
                .passwordHashed(signupRequest.getPassword())
                .role(Role.USER)
                .status(User.StatusEnum.ONLINE)
                .build();

        return userMapper.toDto(userRepository.save(user));
    }
}
