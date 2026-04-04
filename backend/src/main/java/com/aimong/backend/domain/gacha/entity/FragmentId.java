package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.global.enums.PetGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FragmentId implements Serializable {

    @Column(name = "child_id", columnDefinition = "uuid")
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", columnDefinition = "pet_grade_enum")
    private PetGrade grade;
}
