package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuestionQualityIssueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionQualityReviewServiceTest {

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private QuestionQualityIssueRepository questionQualityIssueRepository;

    @Test
    void reportOnlyRecordsIssueByDefault() {
        QuestionQualityReviewService service = new QuestionQualityReviewService(
                questionBankRepository,
                questionQualityIssueRepository,
                new MissionQuestionProperties(10, 30, false, false, false, false),
                new ObjectMapper()
        );
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        QuestionBank question = question(missionId);

        when(questionBankRepository.findByIdAndMissionIdAndIsActiveTrue(question.getId(), missionId))
                .thenReturn(Optional.of(question));
        when(questionQualityIssueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reportQuestion(
                childId,
                missionId,
                question.getId(),
                new QuestionReportRequest("safety", "asks for sensitive data")
        );

        assertThat(response.quarantined()).isFalse();
        verify(questionBankRepository, never()).save(any());
    }

    private QuestionBank question(UUID missionId) {
        QuestionBank question = QuestionBank.create(
                missionId,
                QuestionType.MULTIPLE,
                "What should you never share?",
                "[\"password\", \"nickname\", \"class subject\", \"favorite color\"]",
                "[\"PRIVACY\",\"SAFETY\"]",
                "KERIS-REF",
                DifficultyBand.MEDIUM,
                "STATIC",
                GenerationPhase.PREGENERATED,
                null,
                DifficultyBand.MEDIUM,
                QuestionPoolStatus.ACTIVE
        );
        return question;
    }
}
