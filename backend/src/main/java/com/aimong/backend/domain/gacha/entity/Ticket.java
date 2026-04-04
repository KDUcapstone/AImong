package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Ticket {

    // child_id가 PK (자녀당 1행)
    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    @Column(name = "normal", nullable = false)
    @Builder.Default
    private Integer normal = 0;

    @Column(name = "rare", nullable = false)
    @Builder.Default
    private Integer rare = 0;

    @Column(name = "epic", nullable = false)
    @Builder.Default
    private Integer epic = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addNormal(int count) { this.normal += count; }
    public void addRare(int count)   { this.rare += count; }
    public void addEpic(int count)   { this.epic += count; }

    public void useNormal() { this.normal--; }
    public void useRare()   { this.rare--; }
    public void useEpic()   { this.epic--; }
}
