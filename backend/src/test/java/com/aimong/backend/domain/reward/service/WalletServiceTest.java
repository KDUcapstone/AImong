package com.aimong.backend.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;

    @Test
    void getWalletReturnsGearAndCurrencyCosts() {
        WalletService service = new WalletService(childProfileRepository, childActivityService);
        ChildProfile child = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        child.addGear(40);

        when(childProfileRepository.findById(child.getId())).thenReturn(Optional.of(child));

        var response = service.getWallet(child.getId());

        assertThat(response.gear()).isEqualTo(40);
        assertThat(response.costs().heartRevive()).isEqualTo(10);
        assertThat(response.costs().streakShield()).isEqualTo(30);
        verify(childActivityService).touchLastActiveAt(child.getId());
    }
}
