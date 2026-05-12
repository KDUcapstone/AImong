package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionSetReportResponse;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionSetReportService {

    private static final int MISSION_CLEAR_COIN = 30;

    private final MissionSetRepository missionSetRepository;
    private final MissionAnswerResultRepository missionAnswerResultRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final QuestionBankRepository questionBankRepository;
    private final ChildActivityService childActivityService;
    private final QuestionAnswerMatcher questionAnswerMatcher;

    @Transactional(readOnly = true)
    public MissionSetReportResponse getReport(UUID childId, String setId) {
        childActivityService.touchLastActiveAt(childId);
        MissionSet missionSet = missionSetRepository.findById(setId)
                .filter(MissionSet::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_SET_NOT_FOUND));
        MissionAttempt attempt = missionAttemptRepository.findFirstByChildIdAndSetIdOrderBySubmittedAtDesc(childId, setId)
                .orElseThrow(() -> new AimongException(ErrorCode.REPORT_NOT_FOUND));
        List<MissionAnswerResult> answerResults = missionAnswerResultRepository
                .findAllByChildIdAndAttemptIdOrderByCreatedAtAsc(childId, attempt.getId());
        Map<UUID, QuestionAnswerKey> answerKeys = questionAnswerKeyRepository
                .findAllByQuestionIdIn(answerResults.stream().map(MissionAnswerResult::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionAnswerKey::getQuestionId, Function.identity()));
        Map<UUID, QuestionBank> questions = questionBankRepository
                .findAllByIdIn(answerResults.stream().map(MissionAnswerResult::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));
        int correctCount = (int) answerResults.stream().filter(MissionAnswerResult::isCorrect).count();
        int questionCount = answerResults.size();

        return new MissionSetReportResponse(
                attempt.getId(),
                missionSet.getSetId(),
                missionSet.getMissionId(),
                missionSet.getMissionCode(),
                missionSet.getStarLevel(),
                missionSet.getVariantNo(),
                responseScore(attempt.getScore(), attempt.getTotal()),
                correctCount,
                questionCount - correctCount,
                questionCount,
                attempt.isPassed(),
                questionCount > 0 && correctCount == questionCount,
                attempt.isReview(),
                attempt.getSubmittedAt(),
                new MissionSetReportResponse.RewardsResponse(
                        coinEarned(attempt),
                        attempt.getXpEarned(),
                        List.of()
                ),
                IntStream.range(0, answerResults.size())
                        .mapToObj(index -> toResultResponse(index, answerResults.get(index), answerKeys, questions))
                        .toList()
        );
    }

    private int coinEarned(MissionAttempt attempt) {
        return attempt.isPassed() && !attempt.isReview() ? MISSION_CLEAR_COIN : 0;
    }

    private int responseScore(int correctCount, int total) {
        if (total <= 0) {
            return 0;
        }
        return correctCount * 100 / total;
    }

    private MissionSetReportResponse.ResultResponse toResultResponse(
            int index,
            MissionAnswerResult result,
            Map<UUID, QuestionAnswerKey> answerKeys,
            Map<UUID, QuestionBank> questions
    ) {
        QuestionAnswerKey answerKey = answerKeys.get(result.getQuestionId());
        QuestionBank question = questions.get(result.getQuestionId());
        return new MissionSetReportResponse.ResultResponse(
                result.getQuestionId(),
                index + 1,
                result.isCorrect(),
                answerKey == null || question == null ? null : questionAnswerMatcher.displayAnswer(question, answerKey.getAnswerPayload()),
                result.getSelectedAnswer(),
                answerKey == null ? null : answerKey.getExplanation()
        );
    }
}
