package com.aimong.backend.domain.mission.config;

import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.service.generation.GeneratedQuestionPersistenceService;
import com.aimong.backend.domain.mission.service.generation.InitialSeedExecutionGuard;
import com.aimong.backend.domain.mission.service.generation.InitialSeedGenerationJob;
import com.aimong.backend.domain.mission.service.generation.MissionCodeResolver;
import com.aimong.backend.domain.mission.service.generation.PackQuotaPlanner;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "aimong.mission.seed", name = "enabled", havingValue = "true")
public class SeedGenerationConfiguration {

    @Bean
    public InitialSeedExecutionGuard initialSeedExecutionGuard(
            MissionRepository missionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionGenerationProperties generationProperties
    ) {
        return new InitialSeedExecutionGuard(
                missionRepository,
                questionBankRepository,
                generationProperties
        );
    }

    @Bean
    public InitialSeedGenerationJob initialSeedGenerationJob(
            MissionRepository missionRepository,
            QuestionGenerationService questionGenerationService,
            GeneratedQuestionPersistenceService persistenceService,
            PackQuotaPlanner packQuotaPlanner,
            QuestionGenerationProperties generationProperties,
            MissionCodeResolver missionCodeResolver,
            QuestionBankRepository questionBankRepository
    ) {
        return new InitialSeedGenerationJob(
                missionRepository,
                questionGenerationService,
                persistenceService,
                packQuotaPlanner,
                generationProperties,
                missionCodeResolver,
                questionBankRepository
        );
    }
}
