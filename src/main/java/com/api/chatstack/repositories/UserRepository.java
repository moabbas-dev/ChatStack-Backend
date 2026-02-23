package com.api.chatstack.repositories;

import com.api.chatstack.entities.auth.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository  extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(@NotNull @Email String email);
    boolean existsByDisplayName(@NotNull String displayName);
}
