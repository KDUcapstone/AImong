package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionSetReportResponse;
import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionSetReportService {

    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final MissionAnswerResultRepository missionAnswerResultRepository;
    private final ChildActivityService childActivityService;

    @Transactional(readOnly = true)
    public MissionSetReportResponse getReport(UUID childId, String setId) {
        childActivityService.touchLastActiveAt(childId);
        MissionSet missionSet = missionSetRepository.findById(setId)
                .filter(MissionSet::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_SET_NOT_FOUND));
        MissionSetProgress progress = missionSetProgressRepository.findByChildIdAndSetId(childId, setId)
                .orElse(null);

        List<MissionSetReportResponse.ResultResponse> results = progress == null
                || progress.getFirstPassedAttemptId() == null
                ? List.of()
                : missionAnswerResultRepository.findAllByChildIdAndAttemptIdOrderByCreatedAtAsc(
                                childId,
                                progress.getFirstPassedAttemptId()
                        )
                        .stream()
                        .map(result -> new MissionSetReportResponse.ResultResponse(
                                result.getQuestionId(),
                                result.isCorrect()
                        ))
                        .toList();

        return new MissionSetReportResponse(
                missionSet.getSetId(),
                missionSet.getMissionId(),
                missionSet.getStarLevel(),
                missionSet.getVariantNo(),
                progress != null,
                progress == null ? null : progress.getBestScore(),
                progress == null ? null : progress.getTotal(),
                progress == null ? null : progress.getFirstPassedAttemptId(),
                progress == null ? null : progress.getCompletedAt(),
                results
        );
    }
}
