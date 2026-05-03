package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ParentAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ParentAccountRepository extends JpaRepository<ParentAccount, String> {

    Optional<ParentAccount> findByParentId(String parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ParentAccount> findWithLockByParentId(String parentId);
}
