package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionQuestionSetFactoryTest {

    @Mock
    private ApprovedQuestionProvider approvedQuestionProvider;

    @Mock
    private DynamicQuestionGenerationPort dynamicQuestionGenerationPort;

    @Test
    void returnsExactPackWithoutShuffleFallback() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> pack = createQuestions(10);

        when(approvedQuestionProvider.findApprovedQuestions(missionId, childId, false)).thenReturn(pack);

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).containsExactlyElementsOf(pack);
    }

    @Test
    void usesSyncShortageRefillToReachTenQuestions() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> existing = createQuestions(8);
        List<QuestionBank> generated = createQuestions(2);

        when(approvedQuestionProvider.findApprovedQuestions(missionId, childId, false)).thenReturn(existing);
        when(dynamicQuestionGenerationPort.generateQuestions(missionId, 2, childId, false)).thenReturn(generated);

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).hasSize(10);
        verify(dynamicQuestionGenerationPort).generateQuestions(missionId, 2, childId, false);
    }

    @Test
    void throwsMissionSetNotReadyWhenStillShort() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        when(approvedQuestionProvider.findApprovedQuestions(missionId, childId, false)).thenReturn(createQuestions(7));
        when(dynamicQuestionGenerationPort.generateQuestions(missionId, 3, childId, false)).thenReturn(List.of());

        assertThatThrownBy(() -> factory.create(missionId, childId, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    private List<QuestionBank> createQuestions(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> org.mockito.Mockito.mock(QuestionBank.class))
                .toList();
    }
}
