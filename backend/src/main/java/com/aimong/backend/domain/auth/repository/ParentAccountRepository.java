package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ParentAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentAccountRepository extends JpaRepository<ParentAccount, UUID> {

    Optional<ParentAccount> findByFirebaseUid(String firebaseUid);
}
