package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.List;
import java.util.UUID;

public interface DynamicQuestionGenerationPort {

    List<QuestionBank> generateQuestions(UUID missionId, int shortage, UUID childId, boolean isReview);
}
