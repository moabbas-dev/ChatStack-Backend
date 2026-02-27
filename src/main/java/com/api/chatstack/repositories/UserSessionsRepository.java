package com.api.chatstack.repositories;

import com.api.chatstack.entities.auth.UserSessionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserSessionsRepository extends JpaRepository<UserSessionsEntity, UUID> {


    @Query("select u from UserSessionsEntity u where u.isRevoked = false")
    List<UserSessionsEntity> findByIsRevokedFalse();

    @Query("select u from UserSessionsEntity u where u.isRevoked = false and u.tokenFamily = ?1")
    List<UserSessionsEntity> findByIsRevokedFalseAndTokenFamily(String tokenFamily);
}
