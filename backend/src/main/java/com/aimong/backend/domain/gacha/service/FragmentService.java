package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FragmentService {

    private final FragmentRepository fragmentRepository;

    public void initializeInventory(UUID childId) {
        if (fragmentRepository.findByChildId(childId).isEmpty()) {
            fragmentRepository.save(Fragment.create(childId));
        }
    }
}
