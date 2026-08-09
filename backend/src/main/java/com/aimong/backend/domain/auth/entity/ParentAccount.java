package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
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
    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "email")
    private String email;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static ParentAccount create(String firebaseUid, String email) {
        return new ParentAccount(firebaseUid, email, null, null, null);
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateEmail(String email) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
    }

    public void clearFcmToken() {
        this.fcmToken = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void reactivate(String email) {
        this.deletedAt = null;
        updateEmail(email);
    }

    public void withdraw(Instant deletedAt) {
        this.fcmToken = null;
        this.deletedAt = deletedAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
