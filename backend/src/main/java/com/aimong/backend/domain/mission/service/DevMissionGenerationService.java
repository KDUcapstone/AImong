package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.dto.DevMissionGenerateRequest;
import com.aimong.backend.domain.mission.dto.DevMissionGenerateResponse;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.service.generation.GeneratedQuestionPersistenceService;
import com.aimong.backend.domain.mission.service.generation.ModelRoutingPolicy;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationService;
import com.aimong.backend.domain.mission.service.generation.StructuredQuestionSchema;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev", "test"})
public class DevMissionGenerationService {

    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionGenerationService questionGenerationService;
    private final QuestionValidationService questionValidationService;
    private final GeneratedQuestionPersistenceService persistenceService;
    private final ModelRoutingPolicy modelRoutingPolicy;

    public DevMissionGenerationService(
            MissionRepository missionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionGenerationService questionGenerationService,
            QuestionValidationService questionValidationService,
            GeneratedQuestionPersistenceService persistenceService,
            ModelRoutingPolicy modelRoutingPolicy
    ) {
        this.missionRepository = missionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionGenerationService = questionGenerationService;
        this.questionValidationService = questionValidationService;
        this.persistenceService = persistenceService;
        this.modelRoutingPolicy = modelRoutingPolicy;
    }

    @Transactional
    public DevMissionGenerateResponse generate(DevMissionGenerateRequest request) {
        Mission mission = missionRepository.findById(request.missionId())
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        if (mission.getMissionCode() == null || mission.getMissionCode().isBlank()) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "missionCode가 없는 미션은 GPT 문제 생성을 테스트할 수 없습니다.");
        }

        DifficultyBand difficultyBand = request.difficultyBand() == null ? DifficultyBand.LOW : request.difficultyBand();
        QuestionType type = request.type() == null ? QuestionType.MULTIPLE : request.type();
        int count = request.count() == null ? 1 : request.count();
        int packNo = request.packNo() == null ? 1 : request.packNo();
        int numericDifficulty = inferNumericDifficulty(mission.getStage(), difficultyBand);
        List<String> existingPrompts = questionBankRepository.findAllByMissionIdAndIsActiveTrue(mission.getId()).stream()
                .map(QuestionBank::getPrompt)
                .toList();

        QuestionGenerationService.QuestionGenerationRequest generationRequest =
                new QuestionGenerationService.QuestionGenerationRequest(
                        mission.getMissionCode(),
                        packNo,
                        difficultyBand,
                        type,
                        count,
                        numericDifficulty,
                        0,
                        false,
                        false,
                        false,
                        false,
                        existingPrompts,
                        List.of(),
                        List.of()
                );
        QuestionGenerationService.GenerationBatchResult batchResult =
                questionGenerationService.generateValidatedCandidates(generationRequest);

        List<QuestionBank> savedQuestions = Boolean.TRUE.equals(request.persist())
                ? persistenceService.persistCandidates(
                        mission.getId(),
                        batchResult.accepted(),
                        GenerationPhase.PREGENERATED,
                        "GPT"
                )
                : List.of();

        return toResponse(
                mission,
                difficultyBand,
                type,
                count,
                batchResult,
                savedQuestions
        );
    }

    private DevMissionGenerateResponse toResponse(
            Mission mission,
            DifficultyBand difficultyBand,
            QuestionType type,
            int requestedCount,
            QuestionGenerationService.GenerationBatchResult batchResult,
            List<QuestionBank> savedQuestions
    ) {
        List<DevMissionGenerateResponse.CandidateResponse> accepted = batchResult.accepted().stream()
                .map(candidate -> toAccepted(candidate, batchResult.accepted().indexOf(candidate), savedQuestions))
                .toList();
        List<DevMissionGenerateResponse.RejectedCandidateResponse> rejected = batchResult.rejected().stream()
                .map(rejectedCandidate -> new DevMissionGenerateResponse.RejectedCandidateResponse(
                        rejectedCandidate.candidate().question(),
                        rejectedCandidate.report().hardFailReasons(),
                        rejectedCandidate.report().softWarnings(),
                        rejectedCandidate.report().repairHints(),
                        rejectedCandidate.report().scores()
                ))
                .toList();

        return new DevMissionGenerateResponse(
                mission.getId(),
                mission.getMissionCode(),
                batchResult.routingDecision().selectedModel(),
                batchResult.routingDecision().escalated(),
                difficultyBand,
                type,
                requestedCount,
                accepted.size(),
                rejected.size(),
                savedQuestions.size(),
                accepted,
                rejected
        );
    }

    private DevMissionGenerateResponse.CandidateResponse toAccepted(
            StructuredQuestionSchema candidate,
            int index,
            List<QuestionBank> savedQuestions
    ) {
        UUID savedQuestionId = index < savedQuestions.size() ? savedQuestions.get(index).getId() : null;
        QuestionValidationService.ValidationRequest validationRequest =
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of());
        return new DevMissionGenerateResponse.CandidateResponse(
                savedQuestionId,
                candidate.question(),
                candidate.options(),
                candidate.answer(),
                candidate.explanation(),
                candidate.contentTags(),
                candidate.curriculumRef(),
                candidate.difficulty(),
                questionValidationService.validate(validationRequest).scores()
        );
    }

    private int inferNumericDifficulty(short stage, DifficultyBand difficultyBand) {
        return switch (stage) {
            case 1 -> difficultyBand == DifficultyBand.LOW ? 1 : 2;
            case 2 -> difficultyBand == DifficultyBand.LOW ? 2 : 3;
            case 3 -> difficultyBand == DifficultyBand.LOW ? 3 : 4;
            default -> 1;
        };
    }
}
