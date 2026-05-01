package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.pet.entity.PetGrade;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FragmentId implements Serializable {

    private UUID childId;
    private PetGrade grade;

    @SuppressWarnings("unused")
    public FragmentId() {
    }

    public FragmentId(UUID childId, PetGrade grade) {
        this.childId = childId;
        this.grade = grade;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FragmentId that)) {
            return false;
        }
        return Objects.equals(childId, that.childId) && grade == that.grade;
    }

    @Override
    public int hashCode() {
        return Objects.hash(childId, grade);
    }
}
