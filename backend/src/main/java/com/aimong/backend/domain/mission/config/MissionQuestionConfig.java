package com.aimong.backend.domain.mission.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MissionQuestionProperties.class)
public class MissionQuestionConfig {
}
