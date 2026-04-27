package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "parent_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParentAccount {

    @Id
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "email")
    private String email;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ParentAccount create(String firebaseUid, String email) {
        return new ParentAccount(UUID.randomUUID(), firebaseUid, email, null, null);
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
