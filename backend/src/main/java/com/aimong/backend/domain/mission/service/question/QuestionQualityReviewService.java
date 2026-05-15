package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.dto.QuestionReportResponse;
import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionQualityIssue;
import com.aimong.backend.domain.mission.entity.QuestionQualityIssueSource;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuestionQualityIssueRepository;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationReport;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionQualityReviewService {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionQualityIssueRepository questionQualityIssueRepository;
    private final MissionQuestionProperties missionQuestionProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuestionReportResponse reportQuestion(
            UUID childId,
            UUID missionId,
            UUID questionId,
            QuestionReportRequest request
    ) {
        QuestionBank question = questionBankRepository.findByIdAndMissionIdAndIsActiveTrue(questionId, missionId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUESTION_NOT_FOUND));

        QuestionQualityIssue issue = QuestionQualityIssue.createUserReport(
                questionId,
                missionId,
                childId,
                normalizeReasonCode(request.reasonCode()),
                request.detail()
        );
        questionQualityIssueRepository.save(issue);

        boolean shouldDeactivate = missionQuestionProperties.reportAutoDeactivateEnabled()
                && isSafetyReason(issue.getReasonCode());
        if (shouldDeactivate) {
            question.deactivate();
            issue.markQuarantined();
            questionBankRepository.save(question);
        }

        return new QuestionReportResponse(
                questionId,
                issue.getId(),
                issue.getIssueStatus().name(),
                !question.isActive()
        );
    }

    @Transactional
    public void recordServingFailure(
            Mission mission,
            QuestionBank question,
            String reasonCode,
            String detailText,
            QuestionValidationReport report
    ) {
        questionQualityIssueRepository.save(QuestionQualityIssue.createServingRevalidationIssue(
                question.getId(),
                mission.getId(),
                normalizeReasonCode(reasonCode),
                detailText,
                report == null ? null : report.recommendedAction().name(),
                report == null ? null : writeJson(report.hardFailReasons()),
                report == null ? null : writeJson(report.repairHints())
        ));
        if (missionQuestionProperties.servingAutoQuarantineEnabled()) {
            question.deactivate();
            questionBankRepository.saveAndFlush(question);
        }
    }

    private boolean isSafetyReason(String reasonCode) {
        return "SAFETY".equals(reasonCode) || "INAPPROPRIATE".equals(reasonCode);
    }

    private String normalizeReasonCode(String reasonCode) {
        return reasonCode == null ? "UNKNOWN" : reasonCode.trim().toUpperCase(Locale.ROOT);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize question quality issue payload", exception);
        }
    }
}
