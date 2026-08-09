package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAnswerKeyRepository extends JpaRepository<QuestionAnswerKey, UUID> {

    List<QuestionAnswerKey> findAllByQuestionIdIn(Collection<UUID> questionIds);
}
