package com.aimong.backend.domain.gacha.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.pet.entity.PetGrade;
import org.junit.jupiter.api.Test;

class GachaProbabilityServiceTest {

    private final GachaProbabilityService service = new GachaProbabilityService();

    @Test
    void validatesDocumentedPetTypeCodeByGrade() {
        assertThat(service.isValidPetTypeForGrade(PetGrade.NORMAL, "pet_normal_005")).isTrue();
        assertThat(service.isValidPetTypeForGrade(PetGrade.RARE, "pet_rare_003")).isTrue();
        assertThat(service.isValidPetTypeForGrade(PetGrade.NORMAL, "pet_rare_003")).isFalse();
    }

    @Test
    void returnsDisplayNameForPetTypeCode() {
        assertThat(service.petNameOf("pet_rare_003")).isEqualTo("번개몽");
        assertThat(service.petNameOf("unknown_pet")).isEqualTo("unknown_pet");
    }

    @Test
    void normalTicketReturnsAppliedSrBonusCappedByNormalProbability() {
        GachaProbabilityService.DrawResult result = service.draw(TicketType.NORMAL, 1, 100);

        assertThat(result.appliedSrBonus()).isEqualTo(0.75d);
    }

    @Test
    void rareTicketDoesNotExposeSrBonusInResponseValue() {
        GachaProbabilityService.DrawResult result = service.draw(TicketType.RARE, 1, 100);

        assertThat(result.appliedSrBonus()).isZero();
    }
}
