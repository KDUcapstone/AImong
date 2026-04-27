package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuestionQualityIssueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
    void repeatedUserReportsQuarantineQuestion() {
        QuestionQualityReviewService service = new QuestionQualityReviewService(
                questionBankRepository,
                questionQualityIssueRepository,
                new ObjectMapper()
        );

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        QuestionBank question = QuestionBank.create(
                missionId,
                com.aimong.backend.domain.mission.entity.QuestionType.OX,
                "AI can be wrong.",
                null,
                "[\"FACT\"]",
                "KERIS-REF",
                (short) 1,
                "GPT",
                GenerationPhase.PREGENERATED,
                (short) 1,
                com.aimong.backend.domain.mission.entity.DifficultyBand.LOW,
                QuestionPoolStatus.ACTIVE
        );

        when(questionBankRepository.findByIdAndMissionIdAndIsActiveTrue(question.getId(), missionId))
                .thenReturn(Optional.of(question));
        when(questionQualityIssueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionBankRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionQualityIssueRepository.countByQuestionIdAndIssueSourceAndIssueStatusIn(
                question.getId(),
                com.aimong.backend.domain.mission.entity.QuestionQualityIssueSource.USER_REPORT,
                List.of(
                        com.aimong.backend.domain.mission.entity.QuestionQualityIssueStatus.OPEN,
                        com.aimong.backend.domain.mission.entity.QuestionQualityIssueStatus.QUARANTINED
                )))
                .thenReturn(1L)
                .thenReturn(2L);

        var firstResponse = service.reportQuestion(
                childId,
                missionId,
                question.getId(),
                new QuestionReportRequest("confusing", "too vague")
        );
        var secondResponse = service.reportQuestion(
                childId,
                missionId,
                question.getId(),
                new QuestionReportRequest("confusing", "still too vague")
        );

        assertThat(firstResponse.quarantined()).isFalse();
        assertThat(secondResponse.quarantined()).isTrue();
        assertThat(question.isQuarantined()).isTrue();
    }

    @Test
    void safetyReportQuarantinesImmediately() {
        QuestionQualityReviewService service = new QuestionQualityReviewService(
                questionBankRepository,
                questionQualityIssueRepository,
                new ObjectMapper()
        );

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        QuestionBank question = QuestionBank.create(
                missionId,
                com.aimong.backend.domain.mission.entity.QuestionType.MULTIPLE,
                "어떤 사진을 올리면 안 될까요?",
                "[\"얼굴 사진\", \"풍경 사진\", \"공책 사진\", \"벽 사진\"]",
                "[\"PRIVACY\",\"SAFETY\"]",
                "KERIS-REF",
                (short) 2,
                "GPT",
                GenerationPhase.RUNTIME,
                (short) 1,
                com.aimong.backend.domain.mission.entity.DifficultyBand.MEDIUM,
                QuestionPoolStatus.ACTIVE
        );

        when(questionBankRepository.findByIdAndMissionIdAndIsActiveTrue(question.getId(), missionId))
                .thenReturn(Optional.of(question));
        when(questionQualityIssueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionBankRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionQualityIssueRepository.countByQuestionIdAndIssueSourceAndIssueStatusIn(any(), any(), any()))
                .thenReturn(1L);

        var response = service.reportQuestion(
                childId,
                missionId,
                question.getId(),
                new QuestionReportRequest("safety", "asks for sensitive data")
        );

        assertThat(response.quarantined()).isTrue();
        assertThat(question.isQuarantined()).isTrue();
    }
}
