package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "missions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    @Id
    private UUID id;

    @Column(nullable = false)
    private short stage;

    @Column(nullable = false)
    private String title;

    @Column(name = "mission_code", length = 16)
    private String missionCode;

    @Column
    private String description;

    @Column(name = "unlock_condition")
    private String unlockCondition;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
