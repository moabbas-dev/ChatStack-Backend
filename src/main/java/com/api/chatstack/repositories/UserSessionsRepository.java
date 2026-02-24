package com.api.chatstack.repositories;

import com.api.chatstack.entities.auth.UserSessionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserSessionsRepository extends JpaRepository<UserSessionsEntity, UUID> {

}
