package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ParentAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentAccountRepository extends JpaRepository<ParentAccount, String> {

    Optional<ParentAccount> findByParentId(String parentId);
}
