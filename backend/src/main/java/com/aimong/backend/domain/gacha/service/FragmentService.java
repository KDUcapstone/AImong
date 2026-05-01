package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.FragmentId;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import com.aimong.backend.domain.pet.entity.PetGrade;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FragmentService {

    private final FragmentRepository fragmentRepository;

    public void initializeInventory(UUID childId) {
        Arrays.stream(PetGrade.values())
                .filter(grade -> fragmentRepository.findById(new FragmentId(childId, grade)).isEmpty())
                .map(grade -> Fragment.create(childId, grade))
                .forEach(fragmentRepository::save);
    }
}
