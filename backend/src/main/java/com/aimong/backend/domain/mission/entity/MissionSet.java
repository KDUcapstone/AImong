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
@Table(name = "mission_sets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSet {

    @Id
    @Column(name = "set_id", length = 32)
    private String setId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "mission_code", nullable = false, length = 16)
    private String missionCode;

    @Column(name = "star_level", nullable = false)
    private int starLevel;

    @Column(name = "variant_no", nullable = false)
    private int variantNo;

    @Column(nullable = false)
    private short stage;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public String starLabel() {
        return switch (starLevel) {
            case 1 -> "쉬움";
            case 2 -> "보통";
            case 3 -> "어려움";
            default -> "알 수 없음";
        };
    }
}
